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

/**
 * 获取公开团购列表（分页）
 *
 * @param page 页码，默认 1
 * @param pageSize 每页数量，默认 20
 */
export async function listPublicGroupBuys(
  page = 1,
  pageSize = 20,
): Promise<PageResponse<PublicGroupBuyItem>> {
  const res = await request.get('/group-buys', {
    params: { page, pageSize },
  }) as ApiResponse<PageResponse<PublicGroupBuyItem>>
  return res.data
}

/**
 * 获取公开团购详情
 *
 * @param groupBuyId 团购 ID
 */
export async function getPublicGroupBuyDetail(
  groupBuyId: number,
): Promise<GroupBuyDetailData> {
  const res = await request.get(`/group-buys/${groupBuyId}`) as ApiResponse<GroupBuyDetailData>
  return res.data
}
