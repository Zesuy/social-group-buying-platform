/**
 * Live E2E API 辅助
 *
 * 封装向后端 API 发请求、统一响应解包、错误打印、token 管理。
 * 使用 Playwright 内置的 request API，无需额外依赖。
 *
 * 该模块专门用于 **测试数据准备**（创建用户、店铺、商品、团购、地址、订单等），
 * 不用于页面验证。页面验证必须通过真实浏览器进行。
 *
 * 所有 token 存储在内存中，不持久化到 localStorage。
 */

import { request as pwRequest, type APIRequestContext } from '@playwright/test'

/** API 请求配置 */
export interface ApiConfig {
  baseURL: string
}

/** Token 存储桶，按角色名存储 */
const tokenStore: Record<string, string> = {}

/**
 * 获取当前 API 基础 URL
 * 默认使用后端直接地址（绕过 Vite proxy），方便数据准备
 */
export function getApiBaseURL(): string {
  return process.env.API_BASE_URL || 'http://localhost:8080'
}

/**
 * 创建 Playwright API 请求上下文
 *
 * 每个测试文件应在 beforeAll 中调用一次，复用同一上下文。
 */
export async function createApiContext(config?: ApiConfig): Promise<APIRequestContext> {
  const baseURL = config?.baseURL || getApiBaseURL()
  return pwRequest.newContext({
    baseURL: `${baseURL}/api/v1/`,
    extraHTTPHeaders: {
      'Content-Type': 'application/json',
    },
  })
}

/**
 * 健康检查
 *
 * 返回 true 表示后端可用，false 表示不可用。
 * 可用于跳过 Live E2E 测试。
 */
export async function healthCheck(apiContext: APIRequestContext): Promise<boolean> {
  try {
    const res = await apiContext.get('health', { timeout: 5000 })
    return res.ok()
  } catch {
    return false
  }
}

// ── Token 管理 ──

/**
 * 保存 token
 */
export function storeToken(role: string, token: string): void {
  tokenStore[role] = token
}

/**
 * 读取 token
 */
export function getToken(role: string): string | undefined {
  return tokenStore[role]
}

/**
 * 清除所有 token
 */
export function clearTokens(): void {
  Object.keys(tokenStore).forEach((key) => delete tokenStore[key])
}

// ── API 辅助函数（用于测试数据准备） ──

/**
 * 模拟登录
 *
 * @returns accessToken 和 user 信息
 */
export async function apiLogin(
  apiContext: APIRequestContext,
  body: { nickname: string; phone: string; avatarUrl?: string },
): Promise<{ accessToken: string; user: Record<string, unknown> }> {
  const res = await apiContext.post('auth/mock-login', { data: body })
  const json = await res.json()
  if (!json.success) {
    throw new Error(`Login failed: ${json.error?.code} ${json.error?.message}`)
  }
  return json.data
}

/**
 * 获取当前用户信息
 */
export async function apiFetchMe(
  apiContext: APIRequestContext,
  token: string,
): Promise<Record<string, unknown>> {
  const res = await apiContext.get('me', {
    headers: { Authorization: `Bearer ${token}` },
  })
  const json = await res.json()
  if (!json.success) {
    throw new Error(`FetchMe failed: ${json.error?.code} ${json.error?.message}`)
  }
  return json.data
}

/**
 * 创建店铺
 */
export async function apiCreateStore(
  apiContext: APIRequestContext,
  token: string,
  body: {
    name: string
    logoUrl?: string | null
    description?: string | null
    defaultDeliveryType: string
  },
): Promise<Record<string, unknown>> {
  const res = await apiContext.post('stores', {
    headers: { Authorization: `Bearer ${token}` },
    data: body,
  })
  const json = await res.json()
  if (!json.success) {
    throw new Error(`Create store failed: ${json.error?.code} ${json.error?.message}`)
  }
  return json.data
}

/**
 * 创建商品
 */
export async function apiCreateProduct(
  apiContext: APIRequestContext,
  token: string,
  body: {
    name: string
    description?: string | null
    coverImageUrl?: string | null
    basePriceAmount: number
    stock: number
    categoryId: number
  },
): Promise<Record<string, unknown>> {
  const res = await apiContext.post('my/store/products', {
    headers: { Authorization: `Bearer ${token}` },
    data: body,
  })
  const json = await res.json()
  if (!json.success) {
    throw new Error(`Create product failed: ${json.error?.code} ${json.error?.message}`)
  }
  return json.data
}

/**
 * 发布团购
 */
export async function apiCreateGroupBuy(
  apiContext: APIRequestContext,
  token: string,
  body: Record<string, unknown>,
): Promise<Record<string, unknown>> {
  const res = await apiContext.post('my/store/group-buys', {
    headers: { Authorization: `Bearer ${token}` },
    data: body,
  })
  const json = await res.json()
  if (!json.success) {
    throw new Error(`Create group buy failed: ${json.error?.code} ${json.error?.message}`)
  }
  return json.data
}

/**
 * 创建地址
 */
export async function apiCreateAddress(
  apiContext: APIRequestContext,
  token: string,
  body: {
    receiverName: string
    receiverPhone: string
    province: string
    city: string
    district: string
    detail: string
    isDefault?: boolean
  },
): Promise<Record<string, unknown>> {
  const res = await apiContext.post('my/addresses', {
    headers: { Authorization: `Bearer ${token}` },
    data: body,
  })
  const json = await res.json()
  if (!json.success) {
    throw new Error(`Create address failed: ${json.error?.code} ${json.error?.message}`)
  }
  return json.data
}

/**
 * 创建订单
 */
export async function apiCreateOrder(
  apiContext: APIRequestContext,
  token: string,
  body: {
    groupBuyId: string
    addressId: string
    items: Array<{ groupBuyItemId: string; quantity: number }>
    remark?: string | null
  },
): Promise<Record<string, unknown>> {
  const res = await apiContext.post('orders', {
    headers: { Authorization: `Bearer ${token}` },
    data: body,
  })
  const json = await res.json()
  if (!json.success) {
    throw new Error(`Create order failed: ${json.error?.code} ${json.error?.message}`)
  }
  return json.data
}

/**
 * 模拟支付
 */
export async function apiSimulatePay(
  apiContext: APIRequestContext,
  token: string,
  orderId: string,
): Promise<Record<string, unknown>> {
  const res = await apiContext.post(`orders/${orderId}/simulate-pay`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  const json = await res.json()
  if (!json.success) {
    throw new Error(`Simulate pay failed: ${json.error?.code} ${json.error?.message}`)
  }
  return json.data
}

/**
 * 取消订单
 */
export async function apiCancelOrder(
  apiContext: APIRequestContext,
  token: string,
  orderId: string,
): Promise<Record<string, unknown>> {
  const res = await apiContext.post(`orders/${orderId}/cancel`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  const json = await res.json()
  if (!json.success) {
    throw new Error(`Cancel order failed: ${json.error?.code} ${json.error?.message}`)
  }
  return json.data
}

/**
 * 发货（团长）
 */
export async function apiShipOrder(
  apiContext: APIRequestContext,
  token: string,
  orderId: string,
  body: {
    deliveryType: string
    logisticsCompany?: string
    trackingNo?: string
  },
): Promise<Record<string, unknown>> {
  const res = await apiContext.post(`my/store/orders/${orderId}/ship`, {
    headers: { Authorization: `Bearer ${token}` },
    data: body,
  })
  const json = await res.json()
  if (!json.success) {
    throw new Error(`Ship order failed: ${json.error?.code} ${json.error?.message}`)
  }
  return json.data
}

/**
 * 确认收货
 */
export async function apiCompleteOrder(
  apiContext: APIRequestContext,
  token: string,
  orderId: string,
): Promise<Record<string, unknown>> {
  const res = await apiContext.post(`orders/${orderId}/complete`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  const json = await res.json()
  if (!json.success) {
    throw new Error(`Complete order failed: ${json.error?.code} ${json.error?.message}`)
  }
  return json.data
}
