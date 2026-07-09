import axios from 'axios'
import { normalizeAxiosError, getErrorMessage } from './errors'
import { STORAGE_KEYS } from '@/constants'
import type { ApiError } from '@/types'
import type { AxiosResponse } from 'axios'

const configuredApiBaseUrl = import.meta.env.MODE === 'android'
  ? import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '')
  : ''

const request = axios.create({
  baseURL: configuredApiBaseUrl ? `${configuredApiBaseUrl}/api/v1` : '/api/v1',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

let authExpiredRedirecting = false

function isAuthExpiredError(error: ApiError): boolean {
  const message = error.message?.toLowerCase() || ''
  return error.code === 'UNAUTHORIZED'
    || message.includes('invalid token')
    || message.includes('expired token')
    || message.includes('令牌')
}

function redirectToHomeAfterAuthExpired() {
  if (typeof window === 'undefined' || authExpiredRedirecting) return
  authExpiredRedirecting = true
  localStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN)
  localStorage.removeItem(STORAGE_KEYS.PROFILE_FEATURE_ROLE)
  window.location.replace(`${window.location.origin}${window.location.pathname}#/`)
}

// ── 请求拦截：注入 Authorization ──
request.interceptors.request.use((config) => {
  const token =
    typeof window !== 'undefined'
      ? localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN)
      : null
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  if (typeof FormData !== 'undefined' && config.data instanceof FormData) {
    delete config.headers['Content-Type']
  }
  return config
})

// ── 响应拦截：解包 / 统一错误处理 ──
request.interceptors.response.use(
  (response: AxiosResponse) => {
    // 返回 HTTP 响应体，调用方直接获得 ApiResponse<T>，无需再 .data 解一层
    return response.data
  },
  (error) => {
    const apiError: ApiError = normalizeAxiosError(error)
    // 补充中文文案（如果后端没给中文 message）
    apiError.message = apiError.message || getErrorMessage(apiError.code)
    if (isAuthExpiredError(apiError)) {
      redirectToHomeAfterAuthExpired()
    }
    return Promise.reject(apiError)
  },
)

export default request
