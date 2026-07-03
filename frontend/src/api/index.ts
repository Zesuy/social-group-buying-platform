/**
 * API 层入口
 *
 * 输出 Axios 实例和按模块拆分的接口封装。
 */

export { default as request } from './request'
export { getErrorMessage, createApiError, normalizeAxiosError } from './errors'
export * as authApi from './auth'
export * as groupBuysApi from './groupBuys'
export * as leadersApi from './leaders'
export * as addressesApi from './addresses'
export * as ordersApi from './orders'
export * as storesApi from './stores'
export * as productsApi from './products'
export * as leaderGroupBuysApi from './leaderGroupBuys'
export * as leaderOrdersApi from './leaderOrders'
export * as subscriptionsApi from './subscriptions'
export * as memberCardsApi from './memberCards'
export * as cartApi from './cart'
