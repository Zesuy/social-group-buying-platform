/**
 * 公开团购 API
 *
 * 首页团购列表与团购详情（无需登录）。
 */

import request from './request'
import type {
  ApiResponse,
  PageResponse,
  PublicGroupBuyItem,
  GroupBuyDetailData,
} from '@/types'

export interface ListPublicGroupBuysParams {
  page?: number
  pageSize?: number
  keyword?: string
  latitude?: number
  longitude?: number
  maxDistanceMeters?: number
  sort?: 'distance'
}

/**
 * 获取公开团购列表（分页）
 *
 * @param page 页码，默认 1
 * @param pageSize 每页数量，默认 20
 */
export async function listPublicGroupBuys(
  pageOrParams: number | ListPublicGroupBuysParams = 1,
  pageSize = 20,
): Promise<PageResponse<PublicGroupBuyItem>> {
  const params: ListPublicGroupBuysParams = typeof pageOrParams === 'number'
    ? { page: pageOrParams, pageSize }
    : { page: 1, pageSize: 20, ...pageOrParams }
  const res = await request.get('/group-buys', {
    params,
  }) as ApiResponse<PageResponse<PublicGroupBuyItem>>
  return res.data
}

/**
 * 获取公开团购详情
 *
 * @param groupBuyId 团购 ID
 */
export async function getPublicGroupBuyDetail(
  groupBuyId: string,
  params?: { latitude?: number; longitude?: number },
): Promise<GroupBuyDetailData> {
  const res = await request.get(`/group-buys/${groupBuyId}`, { params }) as ApiResponse<GroupBuyDetailData>
  return res.data
}

export async function getGroupBuyDetailByShareToken(
  shareToken: string,
): Promise<GroupBuyDetailData> {
  const res = await request.get(`/share/group-buys/${shareToken}`) as ApiResponse<GroupBuyDetailData>
  return res.data
}
