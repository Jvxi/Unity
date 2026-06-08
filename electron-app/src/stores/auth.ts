import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api/client'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)

  async function login(loginStr: string, password: string) {
    const res = await api.post('/auth/login', { login: loginStr, password })
    if (res.data.success) {
      token.value = res.data.token
      user.value = res.data.user
      localStorage.setItem('token', token.value)
      localStorage.setItem('user', JSON.stringify(user.value))
    } else {
      throw new Error(res.data.error)
    }
  }

  async function register(nickname: string, email: string, password: string, code: string) {
    const res = await api.post('/auth/register', { nickname, email, password, code })
    if (res.data.success) {
      token.value = res.data.token
      user.value = res.data.user
      localStorage.setItem('token', token.value)
      localStorage.setItem('user', JSON.stringify(user.value))
    } else {
      throw new Error(res.data.error)
    }
  }

  async function sendCode(email: string, purpose: string) {
    const res = await api.post('/auth/send-code', { email, purpose })
    if (!res.data.success) {
      throw new Error(res.data.error)
    }
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { token, user, isLoggedIn, login, register, sendCode, logout }
})
