/**
 * 店铺售后管理 API
 *
 * 团长查看和处理自己店铺的退款售后请求。
 */

import request from './request'
import type {
  AfterSaleData,
  AfterSaleListParams,
  ApiResponse,
  PageResponse,
  RejectAfterSaleRequest,
} from '@/types'

export async function listLeaderAfterSales(
  params: AfterSaleListParams = {},
): Promise<PageResponse<AfterSaleData>> {
  const requestParams: Record<string, number> = {}
  if (params.page !== undefined) requestParams.page = params.page
  if (params.pageSize !== undefined) requestParams.pageSize = params.pageSize
  const res = await request.get('/my/store/after-sales', { params: requestParams }) as ApiResponse<PageResponse<AfterSaleData>>
  return res.data
}

export async function getLeaderAfterSale(afterSaleId: string): Promise<AfterSaleData> {
  const res = await request.get(`/my/store/after-sales/${afterSaleId}`) as ApiResponse<AfterSaleData>
  return res.data
}

export async function approveLeaderAfterSale(afterSaleId: string): Promise<AfterSaleData> {
  const res = await request.post(`/my/store/after-sales/${afterSaleId}/approve`) as ApiResponse<AfterSaleData>
  return res.data
}

export async function rejectLeaderAfterSale(
  afterSaleId: string,
  data: RejectAfterSaleRequest,
): Promise<AfterSaleData> {
  const res = await request.post(`/my/store/after-sales/${afterSaleId}/reject`, data) as ApiResponse<AfterSaleData>
  return res.data
}

export async function completeLeaderAfterSaleRefund(afterSaleId: string): Promise<AfterSaleData> {
  const res = await request.post(`/my/store/after-sales/${afterSaleId}/complete-refund`) as ApiResponse<AfterSaleData>
  return res.data
}
