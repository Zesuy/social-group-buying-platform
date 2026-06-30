/**
 * 订阅 API
 *
 * 我的订阅列表、取消订阅。
 */

import request from './request'
import type {
  ApiResponse,
  SubscriptionListResponse,
} from '@/types'

/**
 * 获取我的订阅列表
 */
export async function listMySubscriptions(): Promise<SubscriptionListResponse> {
  const res = await request.get('/my/subscriptions') as ApiResponse<SubscriptionListResponse>
  return res.data
}

/**
 * 取消订阅团长
 */
export async function unsubscribeLeader(leaderId: string): Promise<void> {
  await request.delete(`/leaders/${leaderId}/subscription`)
}
