import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios'
import api, { normalizeAssetPath, resolveAssetUrl } from '@/api/client'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const isLoggedIn = computed(() => !!token.value)

  function persistUser(nextUser: any) {
    if (nextUser?.avatarUrl) {
      nextUser.avatarUrl = normalizeAssetPath(nextUser.avatarUrl)
    }
    user.value = nextUser
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  async function login(loginStr: string, password: string) {
    const res = await api.post('/api/auth/login', { login: loginStr, password })
    if (res.data.success) {
      token.value = res.data.token
      persistUser(res.data.user)
      localStorage.setItem('token', token.value)
    } else {
      throw new Error(res.data.error)
    }
  }

  async function register(nickname: string, email: string, password: string, code: string) {
    const res = await api.post('/api/auth/register', { nickname, email, password, code })
    if (res.data.success) {
      token.value = res.data.token
      persistUser(res.data.user)
      localStorage.setItem('token', token.value)
    } else {
      throw new Error(res.data.error)
    }
  }

  async function sendCode(email: string, purpose: string) {
    const res = await api.post('/api/auth/send-code', { email, purpose })
    if (!res.data.success) throw new Error(res.data.error)
  }

  async function fetchProfile() {
    const res = await api.get('/api/user/profile')
    if (res.data.success) {
      persistUser({ ...user.value, ...res.data.data })
    }
    return res.data.data
  }

  async function updateProfile(updates: Record<string, string>) {
    const res = await api.put('/api/user/profile', updates)
    if (res.data.success) await fetchProfile()
    else throw new Error(res.data.error)
  }

  async function changePassword(oldPassword: string, newPassword: string) {
    const res = await api.put('/api/user/password', { oldPassword, newPassword })
    if (!res.data.success) throw new Error(res.data.error)
  }

  async function uploadAvatar(file: File) {
    const fd = new FormData()
    fd.append('file', file)
    try {
      const res = await api.post('/api/user/avatar', fd)
      if (res.data.success) {
        const avatarUrl = normalizeAssetPath(res.data.avatarUrl)
        persistUser({ ...user.value, avatarUrl })
        return resolveAssetUrl(avatarUrl)
      }
      throw new Error(res.data.error || '上传失败')
    } catch (error) {
      if (axios.isAxiosError(error)) {
        const data = error.response?.data as { error?: string; message?: string } | undefined
        throw new Error(data?.error || data?.message || error.message || '上传失败')
      }
      throw error
    }
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { token, user, isLoggedIn, login, register, sendCode, fetchProfile, updateProfile, changePassword, uploadAvatar, logout }
})
