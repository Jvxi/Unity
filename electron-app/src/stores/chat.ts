import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { Client, type StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs'
import api, { getApiBaseUrl } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import type { ChatMessage, ChatSession, ChatGroup, ChatGroupMember } from '@/types'

type FileMeta = { fileUrl?: string; fileName?: string; fileSize?: number }

export const useChatStore = defineStore('chat', () => {
  const authStore = useAuthStore()
  const sessions = ref<ChatSession[]>([])
  const currentSession = ref<ChatSession | null>(null)
  const messages = ref<ChatMessage[]>([])
  const groups = ref<ChatGroup[]>([])
  const groupMembers = ref<ChatGroupMember[]>([])
  const connected = ref(false)
  const lastError = ref('')
  const typingUserIds = ref<number[]>([])
  const unreadTotal = computed(() => sessions.value.reduce((sum, s) => sum + s.unreadCount, 0))

  let stompClient: Client | null = null
  let refreshTimer: ReturnType<typeof setInterval> | null = null
  let syncing = false
  let pendingSync = false
  const groupSubscriptions = new Map<number, StompSubscription>()

  function connect(token: string) {
    startAutoRefresh()
    if (stompClient?.active) return
    stompClient = new Client({
      webSocketFactory: () => new SockJS(getApiBaseUrl() + '/ws'),
      connectHeaders: { Authorization: 'Bearer ' + token },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        connected.value = true
        groupSubscriptions.clear()
        stompClient!.subscribe('/user/queue/private', (msg) => {
          handleIncoming(JSON.parse(msg.body), 'PRIVATE')
        })
        void refreshChatState()
      },
      onDisconnect: () => {
        connected.value = false
        groupSubscriptions.clear()
      },
      onStompError: () => {
        connected.value = false
      },
    })
    stompClient.activate()
  }

  function disconnect() {
    stopAutoRefresh()
    if (stompClient?.active) {
      stompClient.deactivate()
    }
    stompClient = null
    connected.value = false
    groupSubscriptions.clear()
  }

  function startAutoRefresh(intervalMs = 8000) {
    if (refreshTimer) return
    refreshTimer = setInterval(() => {
      if (authStore.token) void refreshChatState()
    }, intervalMs)
  }

  function stopAutoRefresh() {
    if (!refreshTimer) return
    clearInterval(refreshTimer)
    refreshTimer = null
  }

  function subscribeGroup(groupId: number) {
    if (!stompClient?.active || groupSubscriptions.has(groupId)) return
    const subscription = stompClient.subscribe('/topic/group/' + groupId, (msg) => {
      handleIncoming(JSON.parse(msg.body), 'GROUP')
    })
    groupSubscriptions.set(groupId, subscription)
  }

  function unsubscribeGroup(groupId: number) {
    groupSubscriptions.get(groupId)?.unsubscribe()
    groupSubscriptions.delete(groupId)
  }

  function syncGroupSubscriptions() {
    const activeGroupIds = new Set(groups.value.map(group => group.id))
    for (const groupId of groupSubscriptions.keys()) {
      if (!activeGroupIds.has(groupId)) unsubscribeGroup(groupId)
    }
    groups.value.forEach(group => subscribeGroup(group.id))
  }

  function handleIncoming(body: any, _type: string) {
    if (!body?.type) return
    if (body.type === 'MESSAGE') {
      handleMessage(body as ChatMessage)
    } else if (body.type === 'RECALL') {
      handleRecallEvent(body)
    } else if (body.type === 'GROUP_EVENT') {
      void handleGroupEvent(body)
    } else if (body.type === 'CHAT_ERROR') {
      handleChatError(body)
    } else if (body.type === 'TYPING') {
      handleTyping(body)
    }
  }

  function handleMessage(msg: ChatMessage) {
    if (currentSession.value) {
      const cs = currentSession.value
      const belongsToCurrent =
        (cs.type === 'PRIVATE' && (msg.senderId === cs.targetUserId || msg.receiverId === cs.targetUserId)) ||
        (cs.type === 'GROUP' && msg.groupId === cs.targetGroupId)
      if (belongsToCurrent && !messages.value.some(item => item.id === msg.id)) {
        messages.value.push(msg)
        void markRead(cs)
      }
    }
    void refreshSessions()
  }

  function handleRecallEvent(body: any) {
    if (!body.messageId) return
    const idx = messages.value.findIndex(m => m.id === body.messageId)
    if (idx >= 0) {
      messages.value[idx].recalled = true
      messages.value[idx].content = '[消息已撤回]'
    }
    void refreshSessions()
  }

  function handleChatError(body: any) {
    lastError.value = body.message || '消息发送失败'
    void refreshChatState()
  }

  function handleTyping(body: any) {
    const senderId = Number(body.senderId)
    if (!Number.isFinite(senderId) || isCurrentUser(senderId)) return
    if (!typingUserIds.value.includes(senderId)) typingUserIds.value.push(senderId)
    window.setTimeout(() => {
      typingUserIds.value = typingUserIds.value.filter(id => id !== senderId)
    }, 2500)
  }

  async function handleGroupEvent(body: any) {
    const groupId = Number(body.groupId)
    if (!Number.isFinite(groupId)) {
      await refreshChatState()
      return
    }

    const affectedUserId = body.affectedUserId == null ? null : Number(body.affectedUserId)
    const shouldRemoveGroup =
      body.event === 'GROUP_DISSOLVED' ||
      (['MEMBER_LEFT', 'MEMBER_REMOVED'].includes(body.event) && affectedUserId !== null && Number.isFinite(affectedUserId) && isCurrentUser(affectedUserId))

    if (shouldRemoveGroup) {
      removeGroupLocally(groupId)
    }

    await refreshChatState()
    if (currentSession.value?.type === 'GROUP' && currentSession.value.targetGroupId === groupId) {
      await loadGroupMembers(groupId)
    }
  }

  function isCurrentUser(userId: number) {
    return authStore.user?.id === userId
  }

  function removeGroupLocally(groupId: number) {
    groups.value = groups.value.filter(group => group.id !== groupId)
    sessions.value = sessions.value.filter(session => session.type !== 'GROUP' || session.targetGroupId !== groupId)
    unsubscribeGroup(groupId)
    if (currentSession.value?.type === 'GROUP' && currentSession.value.targetGroupId === groupId) {
      currentSession.value = null
      messages.value = []
      groupMembers.value = []
    }
  }

  function sendPrivateMessage(receiverId: number, content: string, messageType = 'TEXT', fileMeta?: FileMeta) {
    if (!stompClient?.active) {
      lastError.value = '聊天连接未建立，请稍后重试'
      return
    }
    stompClient.publish({
      destination: '/app/chat.private',
      body: JSON.stringify({ receiverId, content, messageType, ...fileMeta }),
    })
  }

  function sendGroupMessage(groupId: number, content: string, messageType = 'TEXT', fileMeta?: FileMeta) {
    if (!stompClient?.active) {
      lastError.value = '聊天连接未建立，请稍后重试'
      return
    }
    stompClient.publish({
      destination: '/app/chat.group',
      body: JSON.stringify({ groupId, content, messageType, ...fileMeta }),
    })
  }

  function recallMessage(messageId: number, receiverId?: number, groupId?: number) {
    if (!stompClient?.active) {
      lastError.value = '聊天连接未建立，请稍后重试'
      return
    }
    const payload: any = { messageId }
    if (groupId) payload.groupId = groupId
    else if (receiverId) payload.receiverId = receiverId
    stompClient.publish({
      destination: '/app/chat.recall',
      body: JSON.stringify(payload),
    })
  }

  function sendTyping(receiverId?: number, groupId?: number) {
    if (!stompClient?.active) return
    const payload: any = {}
    if (groupId) payload.groupId = groupId
    else if (receiverId) payload.receiverId = receiverId
    else return
    stompClient.publish({
      destination: '/app/chat.typing',
      body: JSON.stringify(payload),
    })
  }

  function clearLastError() {
    lastError.value = ''
  }

  async function refreshSessions() {
    try {
      const res = await api.get('/api/chat/sessions')
      if (res.data.success) {
        sessions.value = res.data.data || []
        reconcileCurrentSession()
      }
    } catch {}
  }

  async function refreshChatState() {
    if (syncing) {
      pendingSync = true
      return
    }
    syncing = true
    try {
      await loadGroups()
      await refreshSessions()
      const activeGroupId = currentSession.value?.type === 'GROUP' ? currentSession.value.targetGroupId : undefined
      if (activeGroupId) await loadGroupMembers(activeGroupId)
    } finally {
      syncing = false
      if (pendingSync) {
        pendingSync = false
        await refreshChatState()
      }
    }
  }

  function reconcileCurrentSession() {
    const cs = currentSession.value
    if (!cs) return
    if (cs.type === 'GROUP' && cs.targetGroupId) {
      const fresh = sessions.value.find(s => s.type === 'GROUP' && s.targetGroupId === cs.targetGroupId)
      if (fresh) {
        currentSession.value = fresh
      } else {
        currentSession.value = null
        messages.value = []
        groupMembers.value = []
      }
      return
    }

    if (cs.type === 'PRIVATE' && cs.targetUserId) {
      const fresh = sessions.value.find(s => s.type === 'PRIVATE' && s.targetUserId === cs.targetUserId)
      if (fresh) currentSession.value = fresh
    }
  }

  async function loadPrivateHistory(targetUserId: number, page = 0, size = 50) {
    const res = await api.get('/api/chat/history/private', { params: { targetUserId, page, size } })
    if (res.data.success) {
      if (page === 0) messages.value = res.data.data
      else messages.value = [...res.data.data, ...messages.value]
    }
  }

  async function loadGroupHistory(groupId: number, page = 0, size = 50) {
    const res = await api.get('/api/chat/history/group', { params: { groupId, page, size } })
    if (res.data.success) {
      if (page === 0) messages.value = res.data.data
      else messages.value = [...res.data.data, ...messages.value]
    }
  }

  async function markRead(session: ChatSession) {
    try {
      if (session.type === 'PRIVATE' && session.targetUserId) {
        await api.put('/api/chat/read', null, { params: { targetUserId: session.targetUserId } })
      } else if (session.type === 'GROUP' && session.targetGroupId) {
        await api.put('/api/chat/read', null, { params: { groupId: session.targetGroupId } })
      }
      session.unreadCount = 0
    } catch {}
  }

  async function loadGroups() {
    try {
      const res = await api.get('/api/groups/my')
      if (res.data.success) {
        groups.value = res.data.data || []
        syncGroupSubscriptions()
      }
    } catch {}
  }

  async function loadGroupMembers(groupId: number) {
    const res = await api.get('/api/groups/' + groupId + '/members')
    if (res.data.success) groupMembers.value = res.data.data
  }

  async function createGroup(name: string, memberIds: number[]) {
    const res = await api.post('/api/groups', { name, memberIds })
    if (res.data.success) {
      await refreshChatState()
      subscribeGroup(res.data.data.id)
      return res.data.data
    }
    throw new Error(res.data.error)
  }

  async function uploadFile(file: File) {
    const fd = new FormData()
    fd.append('file', file)
    const res = await api.post('/api/chat/upload', fd)
    if (res.data.success) return res.data
    throw new Error(res.data.error)
  }

  async function searchUsers(keyword: string) {
    const res = await api.get('/api/user/search', { params: { keyword } })
    if (res.data.success) return res.data.data
    return []
  }

  async function searchMessages(keyword: string, targetUserId?: number, groupId?: number) {
    const params: any = { keyword }
    if (targetUserId) params.targetUserId = targetUserId
    if (groupId) params.groupId = groupId
    const res = await api.get('/api/chat/search', { params })
    if (res.data.success) return res.data.data
    return []
  }

  async function dissolveGroup(groupId: number) {
    const res = await api.delete('/api/groups/' + groupId)
    if (res.data.success) {
      removeGroupLocally(groupId)
      await refreshChatState()
    } else {
      throw new Error(res.data.error)
    }
  }

  async function leaveGroup(groupId: number) {
    const res = await api.post('/api/groups/' + groupId + '/leave')
    if (res.data.success) {
      removeGroupLocally(groupId)
      await refreshChatState()
    } else {
      throw new Error(res.data.error)
    }
  }

  async function addGroupMembers(groupId: number, userIds: number[]) {
    const res = await api.post('/api/groups/' + groupId + '/members', { userIds })
    if (res.data.success) await refreshChatState()
    else throw new Error(res.data.error)
  }

  async function removeGroupMember(groupId: number, userId: number) {
    const res = await api.delete('/api/groups/' + groupId + '/members/' + userId)
    if (res.data.success) await refreshChatState()
    else throw new Error(res.data.error)
  }

  async function muteMember(groupId: number, userId: number, minutes: number) {
    const res = await api.put('/api/groups/' + groupId + '/members/' + userId + '/mute', { minutes })
    if (res.data.success) await loadGroupMembers(groupId)
    else throw new Error(res.data.error)
  }

  async function updateMemberRole(groupId: number, userId: number, role: string) {
    const res = await api.put('/api/groups/' + groupId + '/members/' + userId + '/role', { role })
    if (res.data.success) await loadGroupMembers(groupId)
    else throw new Error(res.data.error)
  }

  function selectSession(session: ChatSession) {
    currentSession.value = session
    messages.value = []
    void markRead(session)
    if (session.type === 'PRIVATE' && session.targetUserId) {
      void loadPrivateHistory(session.targetUserId)
    } else if (session.type === 'GROUP' && session.targetGroupId) {
      void loadGroupHistory(session.targetGroupId)
      void loadGroupMembers(session.targetGroupId)
    }
  }

  return {
    sessions, currentSession, messages, groups, groupMembers, connected, lastError, typingUserIds, unreadTotal,
    connect, disconnect, startAutoRefresh, stopAutoRefresh, subscribeGroup, sendPrivateMessage, sendGroupMessage, recallMessage, sendTyping, clearLastError,
    refreshSessions, refreshChatState, loadPrivateHistory, loadGroupHistory, markRead, loadGroups, loadGroupMembers,
    createGroup, uploadFile, searchUsers, searchMessages, dissolveGroup, leaveGroup,
    addGroupMembers, removeGroupMember, muteMember, updateMemberRole, selectSession, removeGroupLocally,
  }
})
