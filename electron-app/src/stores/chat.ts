import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs'
import api, { getApiBaseUrl } from '@/api/client'
import type { ChatMessage, ChatSession, ChatGroup, ChatGroupMember } from '@/types'

export const useChatStore = defineStore('chat', () => {
  const sessions = ref<ChatSession[]>([])
  const currentSession = ref<ChatSession | null>(null)
  const messages = ref<ChatMessage[]>([])
  const groups = ref<ChatGroup[]>([])
  const groupMembers = ref<ChatGroupMember[]>([])
  const connected = ref(false)
  const unreadTotal = computed(() => sessions.value.reduce((sum, s) => sum + s.unreadCount, 0))

  let stompClient: Client | null = null
  const subscribedGroupIds = new Set<number>()

  function connect(token: string) {
    if (stompClient?.active) return
    stompClient = new Client({
      webSocketFactory: () => new SockJS(getApiBaseUrl() + '/ws'),
      connectHeaders: { Authorization: 'Bearer ' + token },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        connected.value = true
        subscribedGroupIds.clear()
        stompClient!.subscribe('/user/queue/private', (msg) => {
          const body = JSON.parse(msg.body)
          handleIncoming(body, 'PRIVATE')
        })
        groups.value.forEach(g => subscribeGroup(g.id))
      },
      onDisconnect: () => { connected.value = false },
      onStompError: () => { connected.value = false }
    })
    stompClient.activate()
  }

  function disconnect() {
    if (stompClient?.active) {
      stompClient.deactivate()
      stompClient = null
      connected.value = false
      subscribedGroupIds.clear()
    }
  }

  function subscribeGroup(groupId: number) {
    if (!stompClient?.active || subscribedGroupIds.has(groupId)) return
    stompClient.subscribe('/topic/group/' + groupId, (msg) => {
      const body = JSON.parse(msg.body)
      handleIncoming(body, 'GROUP')
    })
    subscribedGroupIds.add(groupId)
  }

  function handleIncoming(body: any, _type: string) {
    if (body.type === 'MESSAGE') {
      const msg = body as ChatMessage
      // If viewing this conversation, add to messages
      if (currentSession.value) {
        const cs = currentSession.value
        if ((cs.type === 'PRIVATE' && (msg.senderId === cs.targetUserId || msg.receiverId === cs.targetUserId)) ||
            (cs.type === 'GROUP' && msg.groupId === cs.targetGroupId)) {
          messages.value.push(msg)
          // Mark as read
          markRead(cs)
        }
      }
      // Update session list
      refreshSessions()
    } else if (body.type === 'RECALL') {
      if (body.messageId) {
        const idx = messages.value.findIndex(m => m.id === body.messageId)
        if (idx >= 0) {
          messages.value[idx].recalled = true
          messages.value[idx].content = '[消息已撤回]'
        }
      }
    }
  }

  function sendPrivateMessage(receiverId: number, content: string, messageType = 'TEXT', fileMeta?: { fileUrl?: string; fileName?: string; fileSize?: number }) {
    if (!stompClient?.active) return
    stompClient.publish({
      destination: '/app/chat.private',
      body: JSON.stringify({ receiverId, content, messageType, ...fileMeta })
    })
  }

  function sendGroupMessage(groupId: number, content: string, messageType = 'TEXT', fileMeta?: { fileUrl?: string; fileName?: string; fileSize?: number }) {
    if (!stompClient?.active) return
    stompClient.publish({
      destination: '/app/chat.group',
      body: JSON.stringify({ groupId, content, messageType, ...fileMeta })
    })
  }

  function recallMessage(messageId: number, receiverId?: number, groupId?: number) {
    if (!stompClient?.active) return
    const payload: any = { messageId }
    if (groupId) payload.groupId = groupId
    else if (receiverId) payload.receiverId = receiverId
    stompClient.publish({
      destination: '/app/chat.recall',
      body: JSON.stringify(payload)
    })
  }

  async function refreshSessions() {
    try {
      const res = await api.get('/api/chat/sessions')
      if (res.data.success) sessions.value = res.data.data
    } catch {}
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
        groups.value = res.data.data
        groups.value.forEach(group => subscribeGroup(group.id))
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
      await loadGroups()
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
    if (res.data.success) await loadGroups()
    else throw new Error(res.data.error)
  }

  async function leaveGroup(groupId: number) {
    const res = await api.post('/api/groups/' + groupId + '/leave')
    if (res.data.success) await loadGroups()
    else throw new Error(res.data.error)
  }

  async function addGroupMembers(groupId: number, userIds: number[]) {
    const res = await api.post('/api/groups/' + groupId + '/members', { userIds })
    if (res.data.success) await loadGroupMembers(groupId)
    else throw new Error(res.data.error)
  }

  async function removeGroupMember(groupId: number, userId: number) {
    const res = await api.delete('/api/groups/' + groupId + '/members/' + userId)
    if (res.data.success) await loadGroupMembers(groupId)
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
    markRead(session)
    if (session.type === 'PRIVATE' && session.targetUserId) {
      loadPrivateHistory(session.targetUserId)
    } else if (session.type === 'GROUP' && session.targetGroupId) {
      loadGroupHistory(session.targetGroupId)
      loadGroupMembers(session.targetGroupId)
    }
  }

  return {
    sessions, currentSession, messages, groups, groupMembers, connected, unreadTotal,
    connect, disconnect, subscribeGroup, sendPrivateMessage, sendGroupMessage, recallMessage,
    refreshSessions, loadPrivateHistory, loadGroupHistory, markRead, loadGroups, loadGroupMembers,
    createGroup, uploadFile, searchUsers, searchMessages, dissolveGroup, leaveGroup,
    addGroupMembers, removeGroupMember, muteMember, updateMemberRole, selectSession
  }
})
