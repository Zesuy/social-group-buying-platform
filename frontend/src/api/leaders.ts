/**
 * 团长相关 API
 *
 * 团长主页、订阅/取消订阅（订阅需要登录）。
 */

import request from './request'
import type {
  ApiResponse,
  EmptySuccessResponse,
  LeaderHomepageData,
  SubscriptionData,
} from '@/types'

/**
 * 获取团长主页
 *
 * @param leaderId 团长 ID
 * @param page 团购列表页码，默认 1
 * @param pageSize 每页数量，默认 20
 */
export async function getLeaderHomepage(
  leaderId: string,
  page = 1,
  pageSize = 20,
): Promise<LeaderHomepageData> {
  const res = await request.get(`/leaders/${leaderId}/homepage`, {
    params: { page, pageSize },
  }) as ApiResponse<LeaderHomepageData>
  return res.data
}

/**
 * 订阅团长
 *
 * @param leaderId 团长 ID
 * @param source 订阅来源（如 'homepage', 'groupBuyDetail'）
 */
export async function subscribeLeader(
  leaderId: string,
  source?: string,
): Promise<SubscriptionData> {
  const res = await request.post(`/leaders/${leaderId}/subscription`, {
    source: source ?? null,
  }) as ApiResponse<SubscriptionData>
  return res.data
}

/**
 * 取消订阅团长
 *
 * @param leaderId 团长 ID
 */
export async function unsubscribeLeader(
  leaderId: string,
): Promise<void> {
  await request.delete(`/leaders/${leaderId}/subscription`) as EmptySuccessResponse
}
