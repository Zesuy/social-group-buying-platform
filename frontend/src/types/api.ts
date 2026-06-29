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

// ── 公开浏览：团购列表 ──

/** GET /api/v1/group-buys 列表项 */
export interface PublicGroupBuyItem {
  id: number
  title: string
  coverImageUrl: string | null
  status: string
  endTime: string | null
  minPriceAmount: number
  soldCount: number
  leader: LeaderLite
  store: StoreLite
}

export interface LeaderLite {
  id: number
  displayName: string
  avatarUrl: string | null
}

export interface StoreLite {
  id: number
  name: string
}

// ── 公开浏览：团购详情 ──

/** 团购活动核心信息（与 OpenAPI GroupBuyDetail 一致） */
export interface GroupBuyDetail {
  id: number
  storeId: number
  leaderId: number
  title: string
  introduction: string | null
  coverImageUrl: string | null
  groupType: string
  deliveryType: string
  shippingTime: string | null
  startTime: string | null
  endTime: string | null
  visibility: string
  status: string
}

export interface GroupBuyDetailData {
  groupBuy: GroupBuyDetail
  leader: LeaderDetail
  store: StoreDetail
  items: PublicGroupBuyDetailItem[]
  viewer: ViewerInfo
}

export interface LeaderDetail {
  id: number
  displayName: string
  avatarUrl: string | null
  followerCount: number
}

export interface StoreDetail {
  id: number
  name: string
  logoUrl: string | null
}

export interface PublicGroupBuyDetailItem {
  id: number
  productId: number
  displayName: string
  groupPriceAmount: number
  groupStock: number
  soldCount: number
  sortOrder: number
  coverImageUrl: string | null
}

export interface ViewerInfo {
  subscribed: boolean
}

// ── 团长主页 ──

export interface LeaderHomepageData {
  leader: LeaderHomepageLeader
  store: LeaderHomepageStore
  viewer: ViewerInfo
  groupBuys: PublicGroupBuyPageData
}

export interface LeaderHomepageLeader {
  id: number
  displayName: string
  avatarUrl: string | null
  bio: string | null
  memberCount: number
  followerCount: number
}

export interface LeaderHomepageStore {
  id: number
  name: string
  logoUrl: string | null
  description: string | null
  defaultDeliveryType: string
}

export interface PublicGroupBuyPageData {
  items: PublicGroupBuyItem[]
  page: number
  pageSize: number
  total: number
  hasMore: boolean
}

// ── 地址管理 ──

export interface AddressData {
  id: number
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detail: string
  fullAddress: string
  isDefault: boolean
}

export interface CreateAddressRequest {
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault?: boolean
}

export interface UpdateAddressRequest {
  receiverName?: string
  receiverPhone?: string
  province?: string
  city?: string
  district?: string
  detail?: string
  isDefault?: boolean
}

// ── 订单 ──

/** 订单详情数据（与 OpenAPI OrderData 一致） */
export interface OrderData {
  id: number
  orderNo: string
  groupBuyId: number
  storeId: number
  leaderId: number
  totalAmount: number
  discountAmount: number
  payAmount: number
  payStatus: string
  orderStatus: string
  paidAt: string | null
  shippedAt: string | null
  completedAt: string | null
  remark: string | null
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detail: string
  fullAddress: string
  items: OrderItemData[]
}

/** 订单商品明细（与 OpenAPI OrderItemData 一致） */
export interface OrderItemData {
  id: number
  groupBuyItemId: number
  productId: number
  productName: string
  skuName?: string
  unitPriceAmount: number
  quantity: number
  totalAmount: number
}

/** 订单操作响应（取消/支付/完成等） */
export interface OrderActionResult {
  id: number
  orderStatus: string
  payStatus?: string
  payAmount?: number
  paidAt?: string | null
  completedAt?: string | null
}

// ── 店铺 ──

/** 创建店铺请求 */
export interface CreateStoreRequest {
  name: string
  logoUrl?: string | null
  description?: string | null
  defaultDeliveryType: string
}

/** 更新店铺请求 */
export interface UpdateStoreRequest {
  name?: string
  logoUrl?: string | null
  description?: string | null
  defaultDeliveryType?: string
}

/** 团长详情（店铺相关） */
export interface LeaderInfo {
  id: number
  displayName: string
  avatarUrl: string | null
}

/** 店铺详情 */
export interface StoreInfo {
  id: number
  leaderId: number
  name: string
  logoUrl: string | null
  description: string | null
  defaultDeliveryType: string
  distributionEnabled: boolean
  status: string
}

/** 创建/更新店铺响应数据 */
export interface StoreResponseData {
  leader: LeaderInfo
  store: StoreInfo
}

/** 我的店铺响应（未开店时 data=null） */
export type MyStoreResponseData = StoreResponseData | null

// ── 订单预览与创建 ──

export interface OrderPreviewData {
  groupBuyId: number
  address: OrderPreviewAddress
  items: OrderPreviewItem[]
  totalAmount: number
  discountAmount: number
  payAmount: number
}

export interface OrderPreviewAddress {
  id: number
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detail: string
  fullAddress: string
}

export interface OrderPreviewItem {
  groupBuyItemId: number
  productId: number
  productName: string
  unitPriceAmount: number
  quantity: number
  totalAmount: number
  availableStock: number
  soldCount: number
}

export interface OrderPreviewRequest {
  groupBuyId: number
  addressId: number
  items: OrderItemEntry[]
}

export interface OrderItemEntry {
  groupBuyItemId: number
  quantity: number
}

export interface CreateOrderRequest {
  groupBuyId: number
  addressId: number
  remark?: string | null
  items: OrderItemEntry[]
}

// ── 订阅 ──

export interface SubscriptionRequest {
  source?: string | null
}

export interface SubscriptionData {
  id: number
  userId: number
  leaderId: number
  storeId: number
  status: string
  source?: string | null
  subscribedAt?: string | null
  canceledAt?: string | null
}
