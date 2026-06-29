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
