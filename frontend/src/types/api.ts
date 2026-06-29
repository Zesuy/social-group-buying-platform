/** 后端成功响应包装 */
export interface ApiResponse<T> {
  success: true
  data: T
  traceId: string
}

/** 后端分页响应中的分页数据 */
export interface PageResponse<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
  hasMore: boolean
}

/** 后端错误详情 */
export interface ApiError {
  code: string
  message: string
  details?: Record<string, unknown> | null
}

/** 后端错误响应包装 */
export interface ErrorResponse {
  success: false
  error: ApiError
  traceId: string
}

/** 后端空成功响应（仅有 success + traceId） */
export interface EmptySuccessResponse {
  success: true
  traceId: string
}

/** 分页查询参数 */
export interface PaginationParams {
  page?: number
  pageSize?: number
}

// ── 认证与用户 ──

/** 模拟登录请求参数 */
export interface MockLoginRequest {
  nickname: string
  avatarUrl?: string
  phone: string
}

/** 模拟登录响应数据 */
export interface MockLoginData {
  accessToken: string
  user: CurrentUserSummary
}

/** GET /api/v1/me 响应数据 */
export interface CurrentUserData {
  user: CurrentUserSummary
  leader?: LeaderSummary | null
  store?: StoreSummary | null
}

/** 当前用户摘要 */
export interface CurrentUserSummary {
  id: number
  nickname: string
  avatarUrl: string | null
  phone: string
  hasLeader: boolean
  leaderId: number | null
  storeId: number | null
}

/** 团长摘要 */
export interface LeaderSummary {
  id: number
  displayName: string
  avatarUrl: string | null
}

/** 店铺摘要 */
export interface StoreSummary {
  id: number
  name: string
  logoUrl: string | null
  status: string
}
