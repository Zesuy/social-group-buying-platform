/**
 * 会员卡 API
 *
 * 我的会员卡列表。
 */

import request from './request'
import type {
  ApiResponse,
  MemberCardListResponse,
} from '@/types'

/**
 * 获取我的会员卡列表
 */
export async function listMyMemberCards(): Promise<MemberCardListResponse> {
  const res = await request.get('/my/member-cards') as ApiResponse<MemberCardListResponse>
  return res.data
}
