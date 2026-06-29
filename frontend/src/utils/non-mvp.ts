/**
 * 非 MVP 入口灰态判断工具
 *
 * 覆盖：真实微信支付、购物车、优惠券/红包、完整售后退款、帮卖分销、
 * 积分商城、公众号推送、平台后台等非 MVP 入口。
 *
 * 返回 true 表示该功能"当前不可用"（灰态/禁用），
 * 返回 false 表示"允许进入"。
 */

/**
 * 真实微信支付是否可用（MVP 阶段不可用，使用模拟支付）
 */
export function isRealWechatPayDisabled(): boolean {
  return true
}

/**
 * 购物车功能是否可用（MVP 阶段不可用，直接购买）
 */
export function isCartDisabled(): boolean {
  return true
}

/**
 * 优惠券/红包功能是否可用（MVP 阶段不可用）
 */
export function isCouponDisabled(): boolean {
  return true
}

/**
 * 完整售后/退款流程是否可用（MVP 阶段不可用，只预留状态）
 */
export function isRefundDisabled(): boolean {
  return true
}

/**
 * 帮卖分销是否可用（MVP 阶段不可用）
 */
export function isDistributionDisabled(): boolean {
  return true
}

/**
 * 积分商城是否可用（MVP 阶段不可用）
 */
export function isPointsMallDisabled(): boolean {
  return true
}

/**
 * 公众号推送是否可用（MVP 阶段不可用）
 */
export function isWechatPushDisabled(): boolean {
  return true
}

/**
 * 平台后台是否可用（MVP 阶段不可用）
 */
export function isAdminPanelDisabled(): boolean {
  return true
}

/**
 * 非 MVP 功能统一检查
 *
 * @param feature 功能标识
 * @returns true = 禁用/灰态, false = 可用
 */
export type NonMvpFeature =
  | 'realWechatPay'
  | 'cart'
  | 'coupon'
  | 'refund'
  | 'distribution'
  | 'pointsMall'
  | 'wechatPush'
  | 'adminPanel'

const disabledFeatureMap: Record<NonMvpFeature, () => boolean> = {
  realWechatPay: isRealWechatPayDisabled,
  cart: isCartDisabled,
  coupon: isCouponDisabled,
  refund: isRefundDisabled,
  distribution: isDistributionDisabled,
  pointsMall: isPointsMallDisabled,
  wechatPush: isWechatPushDisabled,
  adminPanel: isAdminPanelDisabled,
}

export function isFeatureDisabled(feature: NonMvpFeature): boolean {
  return disabledFeatureMap[feature]?.() ?? true
}
