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
  id: string
  nickname: string
  avatarUrl: string | null
  phone: string
  hasLeader: boolean
  leaderId: string | null
  storeId: string | null
}

/** 团长摘要 */
export interface LeaderSummary {
  id: string
  displayName: string
  avatarUrl: string | null
}

/** 店铺摘要 */
export interface StoreSummary {
  id: string
  name: string
  logoUrl: string | null
  status: string
}

// ── 公开浏览：团购列表 ──

/** GET /api/v1/group-buys 列表项 */
export interface PublicGroupBuyItem {
  id: string
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
  id: string
  displayName: string
  avatarUrl: string | null
}

export interface StoreLite {
  id: string
  name: string
  latitude: number | null
  longitude: number | null
  distanceMeters: number | null
  distanceText: string | null
}

// ── 公开浏览：团购详情 ──

/** 团购活动核心信息（与 OpenAPI GroupBuyDetail 一致） */
export interface GroupBuyDetail {
  id: string
  storeId: string
  leaderId: string
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
  galleryImageUrls?: string[]
  contentBlocks?: ContentBlockData[]
}

export interface GroupBuyDetailData {
  groupBuy: GroupBuyDetail
  leader: LeaderDetail
  store: StoreDetail
  items: PublicGroupBuyDetailItem[]
  featuredItem?: PublicGroupBuyDetailItem | null
  viewer: ViewerInfo
}

export interface ContentBlockData {
  type: 'paragraph' | 'section' | 'image' | 'list' | 'deliveryNote' | string
  text?: string | null
  title?: string | null
  url?: string | null
  caption?: string | null
  items?: string[] | null
}

export interface LeaderDetail {
  id: string
  displayName: string
  avatarUrl: string | null
  followerCount: number
}

export interface StoreDetail {
  id: string
  name: string
  logoUrl: string | null
  latitude: number | null
  longitude: number | null
  distanceMeters: number | null
  distanceText: string | null
}

export interface PublicGroupBuyDetailItem {
  id: string
  productId: string
  displayName: string
  groupPriceAmount: number
  groupStock: number
  soldCount: number
  sortOrder: number
  coverImageUrl: string | null
  product?: ProductSummaryData | null
}

export interface ProductSummaryData {
  id: string
  name: string
  description: string | null
  coverImageUrl: string | null
  detailImageUrls: string[]
  basePriceAmount: number | null
  status: string
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
  id: string
  displayName: string
  avatarUrl: string | null
  bio: string | null
  memberCount: number
  followerCount: number
}

export interface LeaderHomepageStore {
  id: string
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
  id: string
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
  id: string
  orderNo: string
  groupBuyId: string
  storeId: string
  leaderId: string
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
  id: string
  groupBuyItemId: string
  productId: string
  productName: string
  skuName?: string
  unitPriceAmount: number
  quantity: number
  totalAmount: number
}

/** 订单操作响应（取消/支付/完成等） */
export interface OrderActionResult {
  id: string
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
  latitude?: number | null
  longitude?: number | null
}

/** 更新店铺请求 */
export interface UpdateStoreRequest {
  name?: string
  logoUrl?: string | null
  description?: string | null
  defaultDeliveryType?: string
  latitude?: number | null
  longitude?: number | null
}

/** 团长详情（店铺相关） */
export interface LeaderInfo {
  id: string
  displayName: string
  avatarUrl: string | null
}

/** 店铺详情 */
export interface StoreInfo {
  id: string
  leaderId: string
  name: string
  logoUrl: string | null
  description: string | null
  defaultDeliveryType: string
  distributionEnabled: boolean
  status: string
  latitude: number | null
  longitude: number | null
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
  groupBuyId: string
  address: OrderPreviewAddress
  items: OrderPreviewItem[]
  totalAmount: number
  discountAmount: number
  payAmount: number
}

export interface OrderPreviewAddress {
  id: string
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detail: string
  fullAddress: string
}

export interface OrderPreviewItem {
  groupBuyItemId: string
  productId: string
  productName: string
  unitPriceAmount: number
  quantity: number
  totalAmount: number
  availableStock: number
  soldCount: number
}

export interface OrderPreviewRequest {
  groupBuyId: string
  addressId: string
  items: OrderItemEntry[]
}

export interface OrderItemEntry {
  groupBuyItemId: string
  quantity: number
}

export interface CreateOrderRequest {
  groupBuyId: string
  addressId: string
  remark?: string | null
  items: OrderItemEntry[]
}

// ── 订阅 ──

export interface SubscriptionRequest {
  source?: string | null
}

export interface SubscriptionData {
  id: string
  userId: string
  leaderId: string
  storeId: string
  status: string
  source?: string | null
  subscribedAt?: string | null
  canceledAt?: string | null
}

// ── 商品管理 ──
export interface ProductData {
  id: string
  storeId: string
  name: string
  description: string | null
  coverImageUrl: string | null
  basePriceAmount: number
  stock: number
  status: string
  createdAt?: string | null
  updatedAt?: string | null
}

export interface CreateProductRequest {
  name: string
  description?: string | null
  coverImageUrl?: string | null
  basePriceAmount: number
  stock: number
}

export interface UpdateProductRequest {
  name?: string
  description?: string | null
  coverImageUrl?: string | null
  basePriceAmount?: number
  stock?: number
  status?: string
}

// ── 团购管理 ──
export interface GroupBuyManageItem {
  id: string
  groupBuyId: string
  productId: string | null
  displayName: string
  groupPriceAmount: number
  groupStock: number
  soldCount: number
  sortOrder: number
}

export interface GroupBuyManageData {
  id: string
  storeId: string
  leaderId: string
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
  createdAt?: string | null
  updatedAt?: string | null
}

export interface GroupBuyManageDetailData {
  groupBuy: GroupBuyManageData
  items: GroupBuyManageItem[]
}

export interface CreateGroupBuyItemRequest {
  productId?: string
  product?: CreateProductRequest
  displayName: string
  groupPriceAmount: number
  groupStock: number
  sortOrder: number
}

export interface CreateGroupBuyRequest {
  title: string
  introduction?: string | null
  coverImageUrl?: string | null
  deliveryType: string
  shippingTime?: string | null
  startTime?: string | null
  endTime?: string | null
  items: CreateGroupBuyItemRequest[]
}

export interface UpdateGroupBuyRequest {
  title?: string
  introduction?: string | null
  coverImageUrl?: string | null
  deliveryType?: string
  shippingTime?: string | null
  startTime?: string | null
  endTime?: string | null
}

export interface EndGroupBuyData {
  groupBuy: { id: string; status: string }
  items: []
}

// ── 团长订单 ──
export interface LeaderOrderData {
  id: string
  orderNo: string
  groupBuyId: string
  userId: string
  storeId: string
  leaderId: string
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
  buyerNickname?: string
  buyerAvatarUrl?: string | null
  items: OrderItemData[]
}

export interface ShipRequest {
  deliveryType: string
  logisticsCompany: string
  trackingNo: string
}

export interface ShipmentData {
  id: string
  orderId: string
  deliveryType: string
  logisticsCompany: string
  trackingNo: string
}

export interface ShipResponse {
  order: { id: string; orderStatus: string; payStatus: string }
  shipment: ShipmentData
}

// ── 订阅列表 ──
export interface SubscriptionListItem {
  id: string
  userId: string
  leaderId: string
  storeId: string
  status: string
  source: string | null
  subscribedAt: string | null
  /**
   * 团长详情（后端可能返回 leader 嵌套对象）
   */
  leader?: { id: string; displayName: string; avatarUrl: string | null } | null
  /**
   * 店铺详情（后端可能返回 store 嵌套对象）
   */
  store?: { id: string; name: string; logoUrl: string | null } | null
}

export interface SubscriptionListResponse {
  items: SubscriptionListItem[]
}

// ── 会员卡 ──
export interface MemberCardData {
  id: string
  leader: { id: string; displayName: string; avatarUrl: string | null }
  store: { id: string; name: string; logoUrl: string | null }
  levelName: string
  growthValue: number
  totalOrderAmount: number
  totalOrders: number
  lastOrderAt: string | null
}

export interface MemberCardListResponse {
  items: MemberCardData[]
}
