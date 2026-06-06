import axios from 'axios'
import { showToast } from 'vant'

let unauthorizedHandler = null

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler
}

const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// --- 请求拦截器：注入 Bearer token ---
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// --- 响应拦截器：解包 R<T> + 错误处理 ---
api.interceptors.response.use(
  (response) => {
    const r = response.data // R<T> { code, message, data, timestamp, traceId }
    if (r.code !== 200) {
      if (r.code === 11001) {
        // 未登录
        unauthorizedHandler?.()
      }
      showToast(r.message || '请求失败')
      return Promise.reject(new Error(r.message))
    }
    return r.data // 返回解包后的业务 data
  },
  (error) => {
    if (error.response?.status === 401) {
      unauthorizedHandler?.()
      return Promise.reject(error)
    }
    const msg = error.response?.data?.message || error.message || '网络错误'
    showToast(msg)
    return Promise.reject(error)
  },
)

export default api
