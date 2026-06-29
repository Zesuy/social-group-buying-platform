/**
 * 状态文案映射工具
 *
 * 覆盖店铺、团购、订单、支付、配送等 MVP 常用状态。
 * 未知状态返回原值或"未知状态"。
 */

/** 店铺状态 */
export type StoreStatus = 'active' | 'suspended' | 'closed'

/** 团购类型 */
export type GroupBuyType = 'normal'

/** 团购状态 */
export type GroupBuyStatus = 'published' | 'ended'

/** 支付状态 */
export type PayStatus = 'unpaid' | 'paid'

/** 订单状态 */
export type OrderStatus =
  | 'pendingPay'
  | 'paid'
  | 'shipped'
  | 'completed'
  | 'canceled'

/** 配送类型 */
export type DeliveryType = 'express' | 'pickup' | 'local_delivery'

/** 订阅来源 */
export type SubscriptionSource = 'homepage' | 'groupBuyDetail'

/** 会员等级 */
export type MemberLevel = 'V0' | 'V1' | 'V2' | 'V3'

// ── 映射表 ──

const storeStatusMap: Record<string, string> = {
  active: '营业中',
  suspended: '暂停营业',
  closed: '已关闭',
}

const groupBuyStatusMap: Record<string, string> = {
  published: '进行中',
  ended: '已结束',
}

const payStatusMap: Record<string, string> = {
  unpaid: '未支付',
  paid: '已支付',
}

const orderStatusMap: Record<string, string> = {
  pendingPay: '待支付',
  paid: '已支付',
  shipped: '已发货',
  completed: '已完成',
  canceled: '已取消',
}

const deliveryTypeMap: Record<string, string> = {
  express: '快递配送',
  pickup: '到店自提',
  local_delivery: '同城配送',
}

// ── 映射函数 ──

/**
 * 获取店铺状态中文文案
 */
export function getStoreStatusText(status: string): string {
  return storeStatusMap[status] ?? '未知状态'
}

/**
 * 获取团购状态中文文案
 */
export function getGroupBuyStatusText(status: string): string {
  return groupBuyStatusMap[status] ?? '未知状态'
}

/**
 * 获取支付状态中文文案
 */
export function getPayStatusText(status: string): string {
  return payStatusMap[status] ?? '未知状态'
}

/**
 * 获取订单状态中文文案
 */
export function getOrderStatusText(status: string): string {
  return orderStatusMap[status] ?? '未知状态'
}

/**
 * 获取配送方式中文文案
 */
export function getDeliveryTypeText(type: string): string {
  return deliveryTypeMap[type] ?? type
}
