/**
 * 团长团购管理 API
 *
 * 团长查看和管理自己店铺的团购。
 */

import request from './request'
import type {
  ApiResponse,
  PageResponse,
  GroupBuyManageData,
  GroupBuyManageDetailData,
  CreateGroupBuyRequest,
  UpdateGroupBuyRequest,
  EndGroupBuyData,
} from '@/types'

/**
 * 获取我的团购列表
 */
export async function listMyGroupBuys(
  status?: string,
  page = 1,
  pageSize = 20,
): Promise<PageResponse<GroupBuyManageData>> {
  const params: Record<string, string | number> = { page, pageSize }
  if (status) params.status = status
  const res = await request.get('/my/store/group-buys', { params }) as ApiResponse<PageResponse<GroupBuyManageData>>
  return res.data
}

/**
 * 获取我的团购详情
 */
export async function getMyGroupBuy(groupBuyId: string): Promise<GroupBuyManageDetailData> {
  const res = await request.get(`/my/store/group-buys/${groupBuyId}`) as ApiResponse<GroupBuyManageDetailData>
  return res.data
}

/**
 * 创建并发布团购
 */
export async function createGroupBuy(data: CreateGroupBuyRequest): Promise<GroupBuyManageDetailData> {
  const res = await request.post('/my/store/group-buys', data) as ApiResponse<GroupBuyManageDetailData>
  return res.data
}

/**
 * 更新我的团购
 */
export async function updateMyGroupBuy(groupBuyId: string, data: UpdateGroupBuyRequest): Promise<GroupBuyManageDetailData> {
  const res = await request.patch(`/my/store/group-buys/${groupBuyId}`, data) as ApiResponse<GroupBuyManageDetailData>
  return res.data
}

/**
 * 结束团购
 */
export async function endGroupBuy(groupBuyId: string): Promise<EndGroupBuyData> {
  const res = await request.post(`/my/store/group-buys/${groupBuyId}/end`) as ApiResponse<EndGroupBuyData>
  return res.data
}
