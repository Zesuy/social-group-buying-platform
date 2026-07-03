/**
 * 订阅 API
 *
 * 我的订阅列表、取消订阅。
 */

import request from './request'
import type {
  ApiResponse,
  LeaderSubscriberListResponse,
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
 * 获取当前团长的订阅用户列表
 */
export async function listMySubscribers(): Promise<LeaderSubscriberListResponse> {
  const res = await request.get('/my/store/subscribers') as ApiResponse<LeaderSubscriberListResponse>
  return res.data
}

/**
 * 取消订阅团长
 */
export async function unsubscribeLeader(leaderId: string): Promise<void> {
  await request.delete(`/leaders/${leaderId}/subscription`)
}
