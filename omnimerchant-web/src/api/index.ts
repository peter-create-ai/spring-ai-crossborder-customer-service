import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const authStore = useAuthStore()
  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`
  }
  return config
})

api.interceptors.response.use(
  (res) => {
    const data = res.data
    if (data.code && data.code !== '200') {
      ElMessage.error(data.message || '请求失败')
      if (data.code === '401') {
        useAuthStore().logout()
        router.push('/login')
      }
      return Promise.reject(new Error(data.message))
    }
    return data
  },
  (err) => {
    if (err.response?.status === 401) {
      useAuthStore().logout()
      router.push('/login')
    }
    ElMessage.error(err.message || '网络错误')
    return Promise.reject(err)
  }
)

export default api
