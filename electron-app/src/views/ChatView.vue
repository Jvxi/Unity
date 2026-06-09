<template>
  <div class="chat-container" ref="chatContainerRef">
    <!-- Left: Session list -->
    <div class="session-panel">
      <div class="session-header">
        <el-input v-model="searchKey" placeholder="搜索联系人" prefix-icon="Search" size="small" clearable />
        <el-button :icon="Plus" circle size="small" type="primary" @click="showNewChat = true" />
      </div>
      <div class="session-list" ref="sessionListRef">
        <div v-for="s in filteredSessions" :key="s.id" class="session-item"
             :class="{ active: chatStore.currentSession?.id === s.id }"
             @click="chatStore.selectSession(s)">
          <el-badge :value="s.unreadCount" :hidden="s.unreadCount === 0" :max="99">
            <el-avatar :size="40" :src="assetUrl(s.targetAvatarUrl)">
              {{ s.type === 'GROUP' ? '群' : (s.targetNickname || '?')[0] }}
            </el-avatar>
          </el-badge>
          <div class="session-info">
            <div class="session-name">{{ s.type === 'GROUP' ? '[群]' : '' }}{{ s.targetNickname || '未知' }}</div>
            <div class="session-preview">{{ s.lastMessageContent || '' }}</div>
          </div>
          <div class="session-time">{{ formatTime(s.updatedAt) }}</div>
        </div>
        <el-empty v-if="chatStore.sessions.length === 0" description="暂无会话" :image-size="60" />
      </div>
    </div>

    <!-- Center: Chat area -->
    <div class="chat-panel">
      <template v-if="chatStore.currentSession">
        <div class="chat-header">
          <span class="chat-title">
            {{ chatStore.currentSession.type === 'GROUP' ? '[群] ' : '' }}{{ chatStore.currentSession.targetNickname || '未知' }}
            <el-tag v-if="chatStore.currentSession.type === 'PRIVATE' && chatStore.currentSession.targetOnlineStatus === 'ONLINE'" size="small" type="success">在线</el-tag>
          </span>
          <div class="chat-actions">
            <el-button v-if="chatStore.currentSession.type === 'GROUP'" :icon="UserFilled" size="small" @click="showMembers = !showMembers">成员</el-button>
            <el-dropdown trigger="click">
              <el-button :icon="MoreFilled" size="small" circle />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="handleSearch">搜索消息</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
        <div class="messages-area" ref="messagesRef">
          <div v-for="msg in chatStore.messages" :key="msg.id" class="msg-row" :data-message-id="msg.id"
               :class="{ 'msg-self': msg.senderId === myId }">
            <el-avatar :size="32" :src="assetUrl(msg.senderAvatarUrl)" class="msg-avatar">
              {{ (msg.senderNickname || '?')[0] }}
            </el-avatar>
            <div class="msg-body">
              <span class="msg-sender" v-if="chatStore.currentSession?.type === 'GROUP'">{{ msg.senderNickname }}</span>
              <div class="msg-bubble" :class="{ 'msg-recalled': msg.recalled }">
                <template v-if="msg.recalled">{{ msg.content }}</template>
                <template v-else-if="msg.messageType === 'IMAGE'">
                  <el-image :src="assetUrl(msg.fileUrl)" :preview-src-list="[assetUrl(msg.fileUrl)]" style="max-width:200px;max-height:200px;border-radius:8px" />
                </template>
                <template v-else-if="msg.messageType === 'FILE'">
                  <a :href="assetUrl(msg.fileUrl)" target="_blank" class="file-link">
                    <el-icon><Document /></el-icon> {{ msg.fileName || '文件' }}
                    <span class="file-size">({{ formatSize(msg.fileSize) }})</span>
                  </a>
                </template>
                <template v-else>{{ msg.content }}</template>
                <span v-if="!msg.recalled && msg.senderId === myId" class="msg-recall-btn" @click="handleRecall(msg)">撤回</span>
              </div>
              <span class="msg-time">{{ formatTime(msg.createdAt) }}</span>
            </div>
          </div>
        </div>
        <div class="input-area">
          <div class="input-toolbar">
            <el-upload :show-file-list="false" :before-upload="handleFileUpload" accept="image/*">
              <el-button :icon="Picture" circle size="small" title="发送图片" />
            </el-upload>
            <el-upload :show-file-list="false" :before-upload="handleDocUpload">
              <el-button :icon="Paperclip" circle size="small" title="发送文件" />
            </el-upload>
          </div>
          <div class="input-row">
            <el-input v-model="inputText" type="textarea" :rows="2" placeholder="输入消息..."
                      @keydown.enter.exact.prevent="sendMessage" resize="none" />
            <el-button type="primary" @click="sendMessage" :disabled="!inputText.trim()">发送</el-button>
          </div>
        </div>
      </template>
      <div v-else class="empty-chat">
        <el-icon :size="48" color="#d1d5db"><ChatDotRound /></el-icon>
        <p>选择一个会话开始聊天</p>
      </div>
    </div>

    <!-- Right: Group members -->
    <div v-if="showMembers && chatStore.currentSession?.type === 'GROUP'" class="members-panel" ref="membersPanelRef">
      <div class="members-header">
        <span>群成员 ({{ chatStore.groupMembers.length }})</span>
        <el-button size="small" text @click="showGroupManage = true">管理</el-button>
      </div>
      <div class="members-list">
        <div v-for="m in chatStore.groupMembers" :key="m.id" class="member-item">
          <el-avatar :size="28" :src="assetUrl(m.avatarUrl)">{{ (m.nickname || '?')[0] }}</el-avatar>
          <span class="member-name">{{ m.nickname }}</span>
          <el-tag v-if="m.role === 'OWNER'" size="small" type="warning">群主</el-tag>
          <el-tag v-else-if="m.role === 'ADMIN'" size="small">管理</el-tag>
          <el-tag v-if="isMuted(m)" size="small" type="info">禁言</el-tag>
        </div>
      </div>
    </div>

    <!-- New Chat Dialog -->
    <el-dialog v-model="showNewChat" title="新建会话" width="400px" @opened="animateDialogContent">
      <el-tabs v-model="newChatTab">
        <el-tab-pane label="私聊" name="private">
          <el-input v-model="userSearchKey" placeholder="搜索用户昵称或邮箱" @input="onUserSearch" clearable />
          <div class="search-results">
            <div v-for="u in userResults" :key="u.id" class="user-result" @click="startPrivateChat(u)">
              <el-avatar :size="32" :src="assetUrl(u.avatarUrl)">{{ (u.nickname || '?')[0] }}</el-avatar>
              <span>{{ u.nickname }}</span>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="创建群" name="group">
          <el-input v-model="newGroupName" placeholder="群名称" />
          <el-input v-model="groupMemberSearch" placeholder="搜索用户添加" @input="onGroupMemberSearch" clearable style="margin-top:8px" />
          <div class="search-results">
            <div v-for="u in groupSearchResults" :key="u.id" class="user-result" @click="toggleGroupMember(u)">
              <el-avatar :size="28" :src="assetUrl(u.avatarUrl)">{{ (u.nickname || '?')[0] }}</el-avatar>
              <span>{{ u.nickname }}</span>
              <el-icon v-if="selectedGroupMembers.some(m => m.id === u.id)" color="#4ade80"><Check /></el-icon>
            </div>
          </div>
          <div v-if="selectedGroupMembers.length > 0" class="selected-members">
            <el-tag v-for="m in selectedGroupMembers" :key="m.id" closable @close="removeGroupMember(m)">{{ m.nickname }}</el-tag>
          </div>
          <el-button type="primary" style="margin-top:12px;width:100%" @click="handleCreateGroup" :disabled="!newGroupName.trim()">创建群聊</el-button>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- Message Search Dialog -->
    <el-dialog v-model="showSearchDialog" title="搜索消息" width="520px" @opened="animateDialogContent">
      <div class="message-search">
        <el-input
          v-model="messageSearchKey"
          placeholder="输入关键词搜索当前会话"
          clearable
          @keyup.enter="runMessageSearch"
        >
          <template #append>
            <el-button :loading="messageSearching" @click="runMessageSearch">搜索</el-button>
          </template>
        </el-input>
        <div class="message-search-results">
          <div v-for="msg in messageSearchResults" :key="msg.id" class="message-search-item" @click="jumpToMessage(msg)">
            <div class="message-search-title">
              <span>{{ msg.senderNickname || "未知用户" }}</span>
              <time>{{ formatTime(msg.createdAt) }}</time>
            </div>
            <p>{{ msg.recalled ? msg.content : messagePreview(msg) }}</p>
          </div>
          <el-empty v-if="messageSearchDone && messageSearchResults.length === 0" description="没有找到相关消息" :image-size="72" />
        </div>
      </div>
    </el-dialog>

    <!-- Group Manage Dialog -->
    <el-dialog v-model="showGroupManage" title="群管理" width="450px" @opened="animateDialogContent">
      <div v-if="chatStore.currentSession?.targetGroupId">
        <div class="manage-section">
          <h4>邀请成员</h4>
          <el-input v-model="inviteSearch" placeholder="搜索用户" @input="onInviteSearch" clearable />
          <div class="search-results">
            <div v-for="u in inviteResults" :key="u.id" class="user-result" @click="handleInvite(u)">
              <el-avatar :size="28" :src="assetUrl(u.avatarUrl)">{{ (u.nickname || '?')[0] }}</el-avatar>
              <span>{{ u.nickname }}</span>
            </div>
          </div>
        </div>
        <el-divider />
        <div class="manage-section">
          <h4>成员管理</h4>
          <div v-for="m in chatStore.groupMembers" :key="m.id" class="manage-member">
            <el-avatar :size="28" :src="assetUrl(m.avatarUrl)">{{ (m.nickname || '?')[0] }}</el-avatar>
            <span class="manage-name">{{ m.nickname }}</span>
            <el-tag v-if="m.role === 'OWNER'" size="small" type="warning">群主</el-tag>
            <div class="manage-actions" v-if="isOwner && m.role !== 'OWNER'">
              <el-dropdown trigger="click">
                <el-button size="small" text>操作</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-if="m.role !== 'ADMIN'" @click="setRole(m, 'ADMIN')">设为管理员</el-dropdown-item>
                    <el-dropdown-item v-if="m.role !== 'MEMBER'" @click="setRole(m, 'MEMBER')">取消管理员</el-dropdown-item>
                    <el-dropdown-item @click="handleMute(m, 10)">禁言10分钟</el-dropdown-item>
                    <el-dropdown-item @click="handleMute(m, 60)">禁言1小时</el-dropdown-item>
                    <el-dropdown-item @click="handleMute(m, 1440)">禁言1天</el-dropdown-item>
                    <el-dropdown-item @click="handleMute(m, 0)">解除禁言</el-dropdown-item>
                    <el-dropdown-item divided @click="handleRemove(m)">移出群聊</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>
        <el-divider />
        <div class="manage-section">
          <el-button v-if="isOwner" type="danger" @click="handleDissolve">解散群聊</el-button>
          <el-button v-else type="danger" @click="handleLeave">退出群聊</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, UserFilled, MoreFilled, Picture, Paperclip, Document, ChatDotRound, Check } from '@element-plus/icons-vue'
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'
import { resolveAssetUrl } from '@/api/client'
import { panelIn, softPulse, staggerIn } from '@/utils/motion'
import type { ChatMessage, UserSearchResult } from '@/types'

const chatStore = useChatStore()
const authStore = useAuthStore()
const myId = computed(() => authStore.user?.id)

const searchKey = ref('')
const inputText = ref('')
const chatContainerRef = ref<HTMLElement | null>(null)
const sessionListRef = ref<HTMLElement | null>(null)
const messagesRef = ref<HTMLElement | null>(null)
const membersPanelRef = ref<HTMLElement | null>(null)
const showMembers = ref(false)
const showNewChat = ref(false)
const showGroupManage = ref(false)
const showSearchDialog = ref(false)
const newChatTab = ref('private')

// New chat - private
const userSearchKey = ref('')
const userResults = ref<UserSearchResult[]>([])

// New chat - group
const newGroupName = ref('')
const groupMemberSearch = ref('')
const groupSearchResults = ref<UserSearchResult[]>([])
const selectedGroupMembers = ref<UserSearchResult[]>([])

// Group manage
const inviteSearch = ref('')
const inviteResults = ref<UserSearchResult[]>([])
const messageSearchKey = ref('')
const messageSearchResults = ref<ChatMessage[]>([])
const messageSearching = ref(false)
const messageSearchDone = ref(false)

const filteredSessions = computed(() => {
  const keyword = searchKey.value.trim().toLowerCase()
  if (!keyword) return chatStore.sessions
  return chatStore.sessions.filter((session) => {
    const name = session.targetNickname?.toLowerCase() || ''
    const preview = session.lastMessageContent?.toLowerCase() || ''
    return name.includes(keyword) || preview.includes(keyword)
  })
})

const isOwner = computed(() => {
  if (!chatStore.currentSession?.targetGroupId) return false
  const me = chatStore.groupMembers.find(m => m.userId === myId.value)
  return me?.role === 'OWNER'
})

function isMuted(m: any) {
  return m.mutedUntil && new Date(m.mutedUntil) > new Date()
}

function assetUrl(url?: string) {
  return resolveAssetUrl(url)
}

function formatTime(s: string) {
  if (!s) return ''
  const d = new Date(s)
  const now = new Date()
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' }) + ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function formatSize(bytes?: number) {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / 1048576).toFixed(1) + 'MB'
}

function messagePreview(msg: ChatMessage) {
  if (msg.messageType === 'IMAGE') return '[图片]'
  if (msg.messageType === 'FILE') return `[文件] ${msg.fileName || ''}`.trim()
  return msg.content
}

function scrollToBottom() {
  nextTick(() => { if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight })
}

function animateShell() {
  nextTick(() => {
    if (!chatContainerRef.value) return
    staggerIn(chatContainerRef.value.querySelectorAll('.session-panel, .chat-panel, .members-panel'), {
      x: -10,
      y: 0,
      scale: 0.996,
      duration: 380,
      staggerDelay: 70,
    })
    animateSessions()
  })
}

function animateSessions() {
  nextTick(() => {
    staggerIn(sessionListRef.value?.querySelectorAll('.session-item'), {
      x: -12,
      y: 0,
      scale: 1,
      duration: 300,
      staggerDelay: 35,
    })
  })
}

function animateLatestMessage() {
  nextTick(() => {
    const rows = messagesRef.value?.querySelectorAll('.msg-row')
    const latest = rows?.[rows.length - 1]
    if (!latest) return
    staggerIn(latest, {
      x: latest.classList.contains('msg-self') ? 18 : -18,
      y: 4,
      scale: 0.96,
      duration: 320,
    })
  })
}

function animateMembersPanel() {
  nextTick(() => {
    panelIn(membersPanelRef.value, { x: 14, y: 0, duration: 300 })
    staggerIn(membersPanelRef.value?.querySelectorAll('.member-item'), {
      x: 10,
      y: 0,
      scale: 1,
      duration: 260,
      delay: 80,
      staggerDelay: 28,
    })
  })
}

function animateDialogContent() {
  nextTick(() => {
    const dialog = document.querySelector('.el-dialog:not([aria-hidden="true"]) .el-dialog__body') || document.querySelector('.el-dialog__body')
    staggerIn(dialog?.querySelectorAll('.el-input, .el-tabs__item, .user-result, .manage-section, .manage-member, .selected-members .el-tag'), {
      y: 8,
      scale: 0.99,
      duration: 280,
      staggerDelay: 28,
    })
  })
}

function sendMessage() {
  const text = inputText.value.trim()
  if (!text || !chatStore.currentSession) return
  const s = chatStore.currentSession
  if (s.type === 'PRIVATE' && s.targetUserId) {
    chatStore.sendPrivateMessage(s.targetUserId, text)
  } else if (s.type === 'GROUP' && s.targetGroupId) {
    chatStore.sendGroupMessage(s.targetGroupId, text)
  }
  inputText.value = ''
  softPulse(document.querySelector('.input-area .el-button--primary'))
}

async function handleFileUpload(file: File) {
  try {
    const res = await chatStore.uploadFile(file)
    const s = chatStore.currentSession!
    const isImage = res.contentType?.startsWith('image/')
    const meta = { fileUrl: res.fileUrl, fileName: res.fileName, fileSize: res.fileSize }
    if (s.type === 'PRIVATE' && s.targetUserId) {
      chatStore.sendPrivateMessage(s.targetUserId, isImage ? '[图片]' : '[文件]', isImage ? 'IMAGE' : 'FILE', meta)
    } else if (s.type === 'GROUP' && s.targetGroupId) {
      chatStore.sendGroupMessage(s.targetGroupId, isImage ? '[图片]' : '[文件]', isImage ? 'IMAGE' : 'FILE', meta)
    }
  } catch (e: any) { ElMessage.error(e.message) }
  return false
}

async function handleDocUpload(file: File) {
  try {
    const res = await chatStore.uploadFile(file)
    const s = chatStore.currentSession!
    const meta = { fileUrl: res.fileUrl, fileName: res.fileName, fileSize: res.fileSize }
    if (s.type === 'PRIVATE' && s.targetUserId) {
      chatStore.sendPrivateMessage(s.targetUserId, '[文件]', 'FILE', meta)
    } else if (s.type === 'GROUP' && s.targetGroupId) {
      chatStore.sendGroupMessage(s.targetGroupId, '[文件]', 'FILE', meta)
    }
  } catch (e: any) { ElMessage.error(e.message) }
  return false
}

function handleRecall(msg: ChatMessage) {
  const age = Date.now() - new Date(msg.createdAt).getTime()
  if (age > 120000) { ElMessage.warning('已超过2分钟，无法撤回'); return }
  const s = chatStore.currentSession!
  chatStore.recallMessage(msg.id, s.type === 'PRIVATE' ? s.targetUserId : undefined, s.type === 'GROUP' ? s.targetGroupId : undefined)
}

let searchTimer: any = null
async function onUserSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(async () => {
    if (userSearchKey.value.length >= 1) userResults.value = await chatStore.searchUsers(userSearchKey.value)
    else userResults.value = []
  }, 300)
}

async function startPrivateChat(user: UserSearchResult) {
  // Find or create session via sending first message
  const existing = chatStore.sessions.find(s => s.type === 'PRIVATE' && s.targetUserId === user.id)
  if (existing) {
    chatStore.selectSession(existing)
  } else {
    // Create session by selecting it
    const newSession: any = { id: 0, type: 'PRIVATE', targetUserId: user.id, targetNickname: user.nickname, targetAvatarUrl: user.avatarUrl, targetOnlineStatus: user.onlineStatus, unreadCount: 0, updatedAt: new Date().toISOString() }
    chatStore.currentSession = newSession
    chatStore.messages = []
    chatStore.sessions.unshift(newSession)
  }
  showNewChat.value = false
  userSearchKey.value = ''
  userResults.value = []
}

async function onGroupMemberSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(async () => {
    if (groupMemberSearch.value.length >= 1) groupSearchResults.value = await chatStore.searchUsers(groupMemberSearch.value)
    else groupSearchResults.value = []
  }, 300)
}

function toggleGroupMember(user: UserSearchResult) {
  const idx = selectedGroupMembers.value.findIndex(m => m.id === user.id)
  if (idx >= 0) selectedGroupMembers.value.splice(idx, 1)
  else selectedGroupMembers.value.push(user)
}

function removeGroupMember(user: UserSearchResult) {
  selectedGroupMembers.value = selectedGroupMembers.value.filter(m => m.id !== user.id)
}

async function handleCreateGroup() {
  try {
    const group = await chatStore.createGroup(newGroupName.value, selectedGroupMembers.value.map(m => m.id))
    ElMessage.success('群聊已创建')
    showNewChat.value = false
    newGroupName.value = ''
    selectedGroupMembers.value = []
    // Navigate to the new group
    const session = chatStore.sessions.find(s => s.type === 'GROUP' && s.targetGroupId === group.id)
    if (session) chatStore.selectSession(session)
  } catch (e: any) { ElMessage.error(e.message) }
}

async function onInviteSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(async () => {
    if (inviteSearch.value.length >= 1) inviteResults.value = await chatStore.searchUsers(inviteSearch.value)
    else inviteResults.value = []
  }, 300)
}

async function handleInvite(user: UserSearchResult) {
  try {
    await chatStore.addGroupMembers(chatStore.currentSession!.targetGroupId!, [user.id])
    ElMessage.success('已邀请')
    inviteSearch.value = ''
    inviteResults.value = []
  } catch (e: any) { ElMessage.error(e.message) }
}

async function handleMute(member: any, minutes: number) {
  try {
    await chatStore.muteMember(chatStore.currentSession!.targetGroupId!, member.userId, minutes)
    ElMessage.success(minutes > 0 ? '已禁言' : '已解除禁言')
  } catch (e: any) { ElMessage.error(e.message) }
}

async function setRole(member: any, role: string) {
  try {
    await chatStore.updateMemberRole(chatStore.currentSession!.targetGroupId!, member.userId, role)
    ElMessage.success('角色已更新')
  } catch (e: any) { ElMessage.error(e.message) }
}

async function handleRemove(member: any) {
  try {
    await ElMessageBox.confirm('确定移除该成员？', '提示')
    await chatStore.removeGroupMember(chatStore.currentSession!.targetGroupId!, member.userId)
    ElMessage.success('已移除')
  } catch {}
}

async function handleDissolve() {
  try {
    await ElMessageBox.confirm('确定解散群聊？此操作不可撤销。', '警告', { type: 'warning' })
    await chatStore.dissolveGroup(chatStore.currentSession!.targetGroupId!)
    chatStore.currentSession = null
    chatStore.messages = []
    ElMessage.success('群已解散')
    showGroupManage.value = false
  } catch {}
}

async function handleLeave() {
  try {
    await ElMessageBox.confirm('确定退出群聊？', '提示')
    await chatStore.leaveGroup(chatStore.currentSession!.targetGroupId!)
    chatStore.currentSession = null
    chatStore.messages = []
    ElMessage.success('已退出群聊')
    showGroupManage.value = false
  } catch {}
}

function handleSearch() {
  if (!chatStore.currentSession) return
  messageSearchKey.value = ''
  messageSearchResults.value = []
  messageSearchDone.value = false
  showSearchDialog.value = true
}

async function runMessageSearch() {
  const keyword = messageSearchKey.value.trim()
  const session = chatStore.currentSession
  if (!session || !keyword) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  messageSearching.value = true
  try {
    messageSearchResults.value = await chatStore.searchMessages(
      keyword,
      session.type === 'PRIVATE' ? session.targetUserId : undefined,
      session.type === 'GROUP' ? session.targetGroupId : undefined
    )
    messageSearchDone.value = true
  } catch (e: any) {
    ElMessage.error(e.message || '搜索失败')
  } finally {
    messageSearching.value = false
  }
}

function jumpToMessage(msg: ChatMessage) {
  const exists = chatStore.messages.some(item => item.id === msg.id)
  if (!exists) chatStore.messages = [msg]
  showSearchDialog.value = false
  nextTick(() => {
    const row = messagesRef.value?.querySelector(`[data-message-id="${msg.id}"]`)
    row?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    softPulse(row, 1.02)
  })
}

watch(() => chatStore.messages.length, (count, oldCount) => {
  scrollToBottom()
  if (count > oldCount) animateLatestMessage()
})

watch(() => chatStore.sessions.length, (count, oldCount) => {
  if (count > 0 && count !== oldCount) animateSessions()
})

watch(searchKey, () => animateSessions())

watch(() => chatStore.currentSession?.id, () => {
  nextTick(() => {
    staggerIn(messagesRef.value?.querySelectorAll('.msg-row'), {
      y: 10,
      scale: 0.985,
      duration: 320,
      staggerDelay: 24,
    })
  })
})

watch(showMembers, (visible) => {
  if (visible) animateMembersPanel()
})

watch(() => chatStore.groupMembers.length, (count, oldCount) => {
  if (showMembers.value && count !== oldCount) animateMembersPanel()
})

onMounted(async () => {
  animateShell()
  const token = authStore.token
  if (token) {
    chatStore.connect(token)
    await chatStore.refreshSessions()
    await chatStore.loadGroups()
    animateSessions()
  }
})

onUnmounted(() => {
  chatStore.disconnect()
})
</script>

<style scoped>
.chat-container { display: flex; height: calc(100vh - 120px); background: var(--color-surface); border-radius: var(--radius-md); overflow: hidden; box-shadow: var(--shadow-sm); }
.session-panel { width: 280px; border-right: 1px solid var(--color-border); display: flex; flex-direction: column; flex-shrink: 0; }
.session-header { padding: 12px; display: flex; gap: 8px; align-items: center; }
.session-list { flex: 1; overflow-y: auto; }
.session-item { display: flex; align-items: center; gap: 10px; padding: 12px 14px; cursor: pointer; transition: background 0.2s var(--transition-smooth), box-shadow 0.2s var(--transition-smooth); will-change: transform, opacity; position: relative; }
.session-item::before { content: ''; position: absolute; inset: 6px 8px; border-radius: var(--radius-sm); background: rgba(74,222,128,0.08); opacity: 0; transform: scaleX(0.9); transition: opacity 0.2s var(--transition-smooth), transform 0.2s var(--transition-smooth); pointer-events: none; }
.session-item:hover::before { opacity: 1; transform: scaleX(1); }
.session-item.active { background: var(--color-primary-bg); border-right: 3px solid var(--color-primary); box-shadow: inset 0 0 0 1px rgba(74,222,128,0.08); }
.session-info { flex: 1; min-width: 0; }
.session-name { font-size: 13px; font-weight: 500; color: var(--color-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.session-preview { font-size: 12px; color: var(--color-text-secondary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-top: 2px; }
.session-time { font-size: 11px; color: var(--color-text-secondary); flex-shrink: 0; }

.chat-panel { flex: 1; display: flex; flex-direction: column; }
.chat-header { height: 48px; padding: 0 16px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--color-border); }
.chat-title { font-size: 14px; font-weight: 600; color: var(--color-text); display: flex; align-items: center; gap: 8px; }
.chat-actions { display: flex; gap: 6px; }

.messages-area { flex: 1; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 12px; }
.msg-row { display: flex; gap: 8px; max-width: 75%; will-change: transform, opacity; }
.msg-row.msg-self { flex-direction: row-reverse; align-self: flex-end; }
.msg-avatar { flex-shrink: 0; }
.msg-body { display: flex; flex-direction: column; }
.msg-self .msg-body { align-items: flex-end; }
.msg-sender { font-size: 11px; color: var(--color-text-secondary); margin-bottom: 2px; }
.msg-bubble { background: var(--color-bg); padding: 8px 12px; border-radius: 12px; font-size: 13px; color: var(--color-text); max-width: 400px; word-break: break-word; position: relative; line-height: 1.5; transition: transform 0.2s var(--transition-smooth), box-shadow 0.2s var(--transition-smooth); }
.msg-bubble:hover { transform: translateY(-1px); box-shadow: var(--shadow-sm); }
.msg-self .msg-bubble { background: var(--color-primary-bg); }
.msg-recalled { opacity: 0.6; font-style: italic; }
.msg-time { font-size: 10px; color: var(--color-text-secondary); margin-top: 2px; }
.msg-recall-btn { display: none; font-size: 11px; color: var(--color-text-secondary); cursor: pointer; margin-left: 8px; }
.msg-bubble:hover .msg-recall-btn { display: inline; }
.file-link { color: var(--color-primary-dark); text-decoration: none; display: flex; align-items: center; gap: 4px; font-size: 13px; }
.file-size { font-size: 11px; color: var(--color-text-secondary); }

.input-area { border-top: 1px solid var(--color-border); padding: 8px 16px; }
.input-toolbar { display: flex; gap: 4px; margin-bottom: 6px; }
.input-row { display: flex; gap: 8px; align-items: flex-end; }
.input-row .el-input { flex: 1; }

.empty-chat { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; color: var(--color-text-secondary); }

.members-panel { width: 220px; border-left: 1px solid var(--color-border); display: flex; flex-direction: column; flex-shrink: 0; will-change: transform, opacity; }
.members-header { padding: 12px; font-size: 13px; font-weight: 600; border-bottom: 1px solid var(--color-border); display: flex; justify-content: space-between; align-items: center; }
.members-list { flex: 1; overflow-y: auto; }
.member-item { display: flex; align-items: center; gap: 8px; padding: 8px 12px; will-change: transform, opacity; transition: background 0.2s var(--transition-smooth); }
.member-item:hover { background: var(--color-primary-bg); }
.member-name { font-size: 13px; flex: 1; }

.search-results { max-height: 200px; overflow-y: auto; margin-top: 8px; }
.user-result { display: flex; align-items: center; gap: 8px; padding: 8px; cursor: pointer; border-radius: 8px; transition: background 0.2s var(--transition-smooth), transform 0.2s var(--transition-smooth); will-change: transform, opacity; }
.user-result:hover { background: var(--color-primary-bg); transform: translateX(2px); }
.selected-members { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 8px; }
.message-search { display: flex; flex-direction: column; gap: 12px; }
.message-search-results { max-height: 360px; overflow-y: auto; display: flex; flex-direction: column; gap: 8px; }
.message-search-item { padding: 10px 12px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); cursor: pointer; background: var(--color-bg); transition: transform 0.2s var(--transition-smooth), background 0.2s var(--transition-smooth); }
.message-search-item:hover { transform: translateX(2px); background: var(--color-primary-bg); }
.message-search-title { display: flex; justify-content: space-between; gap: 12px; color: var(--color-text); font-size: 13px; font-weight: 600; }
.message-search-title time { flex-shrink: 0; color: var(--color-text-secondary); font-size: 11px; font-weight: 400; }
.message-search-item p { margin: 6px 0 0; color: var(--color-text-secondary); font-size: 13px; line-height: 1.5; word-break: break-word; }

.manage-section h4 { font-size: 13px; margin-bottom: 8px; }
.manage-member { display: flex; align-items: center; gap: 8px; padding: 6px 0; }
.manage-name { flex: 1; font-size: 13px; }
.manage-actions { flex-shrink: 0; }
</style>
