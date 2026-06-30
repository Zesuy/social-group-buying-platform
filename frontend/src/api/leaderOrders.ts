/**
 * 团长订单 API
 *
 * 团长查看和处理自己店铺的订单。
 */

import request from './request'
import type {
  ApiResponse,
  PageResponse,
  LeaderOrderData,
  ShipRequest,
  ShipResponse,
} from '@/types'

/**
 * 获取我的店铺订单列表
 */
export async function listLeaderOrders(
  status?: string,
  page = 1,
  pageSize = 20,
): Promise<PageResponse<LeaderOrderData>> {
  const params: Record<string, string | number> = { page, pageSize }
  if (status) params.status = status
  const res = await request.get('/my/store/orders', { params }) as ApiResponse<PageResponse<LeaderOrderData>>
  return res.data
}

/**
 * 获取我的店铺订单详情
 */
export async function getLeaderOrder(orderId: string): Promise<LeaderOrderData> {
  const res = await request.get(`/my/store/orders/${orderId}`) as ApiResponse<LeaderOrderData>
  return res.data
}

/**
 * 订单发货
 */
export async function shipOrder(orderId: string, data: ShipRequest): Promise<ShipResponse> {
  const res = await request.post(`/my/store/orders/${orderId}/ship`, data) as ApiResponse<ShipResponse>
  return res.data
}
