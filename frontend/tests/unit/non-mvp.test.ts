import { describe, it, expect } from 'vitest'
import {
  isRealWechatPayDisabled,
  isCartDisabled,
  isCouponDisabled,
  isRefundDisabled,
  isDistributionDisabled,
  isPointsMallDisabled,
  isWechatPushDisabled,
  isAdminPanelDisabled,
  isGroupBuyPublishDisabled,
  isLeaderShippingDisabled,
  isSubscriptionsDisabled,
  isMemberCardsDisabled,
  isFeatureDisabled,
} from '@/utils/non-mvp'

describe('non-MVP gate checks', () => {
  it('should disable real WeChat Pay', () => {
    expect(isRealWechatPayDisabled()).toBe(true)
  })

  it('should disable cart', () => {
    expect(isCartDisabled()).toBe(true)
  })

  it('should disable coupon', () => {
    expect(isCouponDisabled()).toBe(true)
  })

  it('should disable refund', () => {
    expect(isRefundDisabled()).toBe(true)
  })

  it('should disable distribution', () => {
    expect(isDistributionDisabled()).toBe(true)
  })

  it('should disable points mall', () => {
    expect(isPointsMallDisabled()).toBe(true)
  })

  it('should disable WeChat push', () => {
    expect(isWechatPushDisabled()).toBe(true)
  })

  it('should disable admin panel', () => {
    expect(isAdminPanelDisabled()).toBe(true)
  })

  it('should disable later frontend batch features', () => {
    expect(isGroupBuyPublishDisabled()).toBe(true)
    expect(isLeaderShippingDisabled()).toBe(true)
    expect(isSubscriptionsDisabled()).toBe(true)
    expect(isMemberCardsDisabled()).toBe(true)
  })
})

describe('isFeatureDisabled', () => {
  it('should return true for all known features', () => {
    expect(isFeatureDisabled('realWechatPay')).toBe(true)
    expect(isFeatureDisabled('cart')).toBe(true)
    expect(isFeatureDisabled('coupon')).toBe(true)
    expect(isFeatureDisabled('refund')).toBe(true)
    expect(isFeatureDisabled('distribution')).toBe(true)
    expect(isFeatureDisabled('pointsMall')).toBe(true)
    expect(isFeatureDisabled('wechatPush')).toBe(true)
    expect(isFeatureDisabled('adminPanel')).toBe(true)
    expect(isFeatureDisabled('groupBuyPublish')).toBe(true)
    expect(isFeatureDisabled('leaderShipping')).toBe(true)
    expect(isFeatureDisabled('subscriptions')).toBe(true)
    expect(isFeatureDisabled('memberCards')).toBe(true)
  })

  it('should return true for unknown features', () => {
    // @ts-expect-error testing runtime behavior
    expect(isFeatureDisabled('unknownFeature')).toBe(true)
  })
})
