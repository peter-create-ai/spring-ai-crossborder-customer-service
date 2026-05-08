import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const email = ref(localStorage.getItem('email') || '')

  const isLoggedIn = computed(() => !!token.value)

  async function login(loginEmail: string, password: string) {
    const res = await api.post('/admin/login', { email: loginEmail, password })
    token.value = res.data.token
    email.value = res.data.email
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('email', res.data.email)
    return res.data
  }

  function logout() {
    token.value = ''
    email.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('email')
  }

  return { token, email, isLoggedIn, login, logout }
})
