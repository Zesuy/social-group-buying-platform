/**
 * 订单 API
 *
 * 订单预览和创建订单（需要登录）。
 */

import request from './request'
import type {
  ApiResponse,
  OrderPreviewData,
  OrderPreviewRequest,
  CreateOrderRequest,
} from '@/types'

/**
 * 预览订单（不创建订单，不扣库存）
 *
 * @param data 预览请求（groupBuyId, addressId, items）
 */
export async function previewOrder(data: OrderPreviewRequest): Promise<OrderPreviewData> {
  const res = await request.post('/orders/preview', data) as ApiResponse<OrderPreviewData>
  return res.data
}

/**
 * 创建订单
 *
 * @param data 创建订单请求
 * @returns 订单数据（含 id, orderNo, payAmount 等）
 */
export async function createOrder(data: CreateOrderRequest): Promise<Record<string, unknown>> {
  const res = await request.post('/orders', data) as ApiResponse<Record<string, unknown>>
  return res.data
}
