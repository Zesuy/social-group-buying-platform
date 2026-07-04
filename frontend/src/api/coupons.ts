/**
 * 优惠券 API
 *
 * 店铺新人订阅券管理、团长主页领券展示和用户领取。
 */

import request from './request'
import type {
  ApiResponse,
  CreateCouponRequest,
  StoreCouponData,
  StoreCouponOfferData,
  UpdateCouponRequest,
  UserCouponData,
} from '@/types'

export async function listStoreCoupons(): Promise<StoreCouponData[]> {
  const res = await request.get('/my/store/coupons') as ApiResponse<StoreCouponData[]>
  return res.data
}

export async function createStoreCoupon(data: CreateCouponRequest): Promise<StoreCouponData> {
  const res = await request.post('/my/store/coupons', data) as ApiResponse<StoreCouponData>
  return res.data
}

export async function updateStoreCoupon(
  couponId: string,
  data: UpdateCouponRequest,
): Promise<StoreCouponData> {
  const res = await request.patch(`/my/store/coupons/${couponId}`, data) as ApiResponse<StoreCouponData>
  return res.data
}

export async function disableStoreCoupon(couponId: string): Promise<void> {
  await request.post(`/my/store/coupons/${couponId}/disable`)
}

export async function listLeaderHomepageCoupons(leaderId: string): Promise<StoreCouponOfferData[]> {
  const res = await request.get(`/leaders/${leaderId}/coupons`, {
    params: { scene: 'homepage' },
  }) as ApiResponse<StoreCouponOfferData[]>
  return res.data
}

export async function claimCoupon(couponId: string): Promise<UserCouponData> {
  const res = await request.post(`/coupons/${couponId}/claim`) as ApiResponse<UserCouponData>
  return res.data
}
