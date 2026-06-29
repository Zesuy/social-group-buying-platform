/**
 * API 层入口
 *
 * 输出 Axios 实例和按模块拆分的接口封装。
 */

export { default as request } from './request'
export { getErrorMessage, createApiError, normalizeAxiosError } from './errors'
export * as authApi from './auth'
