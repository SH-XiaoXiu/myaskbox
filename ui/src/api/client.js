import axios from 'axios'
import { showFailToast } from 'vant'

let unauthorizedHandler = null

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler
}

const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

const HTTP_STATUS_MESSAGES = {
  400: '请求参数有误',
  401: '请先登录',
  403: '权限不足',
  404: '资源不存在',
  405: '请求方法不允许',
  408: '请求超时，请稍后再试',
  413: '上传内容过大',
  415: '不支持的内容格式',
  429: '请求过于频繁，请稍后再试',
}

function normalizeErrorMessage(error, fallback = '请求失败') {
  const response = error?.response
  const data = response?.data
  const serverMessage = typeof data?.message === 'string' ? data.message.trim() : ''

  if (serverMessage) return serverMessage
  if (response?.status >= 500) return '服务器开小差了，请稍后再试'
  if (response?.status && HTTP_STATUS_MESSAGES[response.status]) {
    return HTTP_STATUS_MESSAGES[response.status]
  }
  if (error?.code === 'ECONNABORTED') return '请求超时，请检查网络后重试'
  if (error?.message === 'Network Error') return '网络连接失败，请检查网络'

  return fallback
}

function createApiError(message, cause, extra = {}) {
  const apiError = new Error(message || '请求失败')
  apiError.cause = cause
  apiError.isApiError = true
  Object.assign(apiError, extra)
  return apiError
}

function showApiError(message) {
  showFailToast({
    message: message || '请求失败',
    wordBreak: 'break-word',
  })
}

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
      const message = r.message || '请求失败'
      showApiError(message)
      return Promise.reject(createApiError(message, null, { code: r.code, data: r.data, traceId: r.traceId }))
    }
    return r.data // 返回解包后的业务 data
  },
  (error) => {
    if (error.response?.status === 401) {
      unauthorizedHandler?.()
    }
    const message = normalizeErrorMessage(error, '网络错误')
    showApiError(message)
    return Promise.reject(createApiError(message, error, {
      status: error.response?.status,
      code: error.response?.data?.code,
      data: error.response?.data?.data,
      traceId: error.response?.data?.traceId,
    }))
  },
)

export default api
