import axios, { type AxiosError, type AxiosResponse } from 'axios'
import type { ApiError, ApiResponse } from '@/types'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// ── 请求拦截：注入 Authorization ──
request.interceptors.request.use((config) => {
  const token =
    typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ── 响应拦截：解包 / 统一错误处理 ──
request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<unknown>>) => {
    // 成功响应直接返回 data
    return response
  },
  (error: AxiosError<{ success: false; error: ApiError; traceId?: string }>) => {
    // 标准化错误信息
    if (error.response?.data?.error) {
      const apiError = error.response.data.error
      return Promise.reject(apiError)
    }
    // 网络错误等
    return Promise.reject({
      code: 'NETWORK_ERROR',
      message: '网络异常，请稍后重试',
    } satisfies ApiError)
  },
)

export default request
