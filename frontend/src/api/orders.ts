/**
 * 订单 API
 *
 * 订单预览、创建、列表、详情、模拟支付、取消、确认收货。
 */

import request from './request'
import type {
  ApiResponse,
  PageResponse,
  OrderData,
  OrderPreviewData,
  OrderPreviewRequest,
  CreateOrderRequest,
} from '@/types'

/**
 * 预览订单（不创建订单，不扣库存）
 */
export async function previewOrder(data: OrderPreviewRequest): Promise<OrderPreviewData> {
  const res = await request.post('/orders/preview', data) as ApiResponse<OrderPreviewData>
  return res.data
}

/**
 * 创建订单
 */
export async function createOrder(data: CreateOrderRequest): Promise<OrderData> {
  const res = await request.post('/orders', data) as ApiResponse<OrderData>
  return res.data
}

/**
 * 我的订单列表（分页）
 *
 * @param status 可选筛选：pendingPay / paid / shipped / completed / canceled
 * @param page 页码，默认 1
 * @param pageSize 每页数量，默认 20
 */
export async function listMyOrders(
  status?: string,
  page = 1,
  pageSize = 20,
): Promise<PageResponse<OrderData>> {
  const params: Record<string, string | number> = { page, pageSize }
  if (status) {
    params.status = status
  }
  const res = await request.get('/my/orders', { params }) as ApiResponse<PageResponse<OrderData>>
  return res.data
}

/**
 * 我的订单详情
 */
export async function getMyOrder(orderId: string): Promise<OrderData> {
  const res = await request.get(`/my/orders/${orderId}`) as ApiResponse<OrderData>
  return res.data
}

/**
 * 模拟支付（仅待支付订单可用）
 */
export async function simulatePay(orderId: string): Promise<OrderData> {
  const res = await request.post(`/orders/${orderId}/simulate-pay`) as ApiResponse<OrderData>
  return res.data
}

/**
 * 取消订单（仅待支付订单可用）
 */
export async function cancelOrder(orderId: string): Promise<OrderData> {
  const res = await request.post(`/orders/${orderId}/cancel`) as ApiResponse<OrderData>
  return res.data
}

/**
 * 确认收货（仅已发货订单可用）
 */
export async function completeOrder(orderId: string): Promise<OrderData> {
  const res = await request.post(`/orders/${orderId}/complete`) as ApiResponse<OrderData>
  return res.data
}
