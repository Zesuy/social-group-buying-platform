/**
 * Live E2E 测试数据常量
 *
 * 所有测试手机号、昵称、图片 URL、默认地址、默认商品、默认团购参数集中管理。
 * 手机号使用唯一后缀避免和历史数据冲突。
 */

import crypto from 'node:crypto'

/** 生成唯一测试手机号后缀 */
function generatePhoneSuffix(): string {
  const uuid = crypto.randomUUID().replace(/\D/g, '')
  return uuid.slice(0, 8).padEnd(8, '0')
}

// ── 测试运行标识 ──
export const RUN_ID = crypto.randomUUID().slice(0, 8)
export const PHONE_SUFFIX = generatePhoneSuffix()

// ── 测试用户 ──
export const BUYER_PHONE = `139${PHONE_SUFFIX}`
export const BUYER_NICKNAME = `社区团员_${RUN_ID}`
export const LEADER_PHONE = `138${PHONE_SUFFIX}`
export const LEADER_NICKNAME = `王姐鲜果团_${RUN_ID}`
export const TEST_AVATAR_URL = 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=300&q=80'

// ── 测试店铺 ──
export const STORE_NAME = `王姐社区鲜果店_${RUN_ID}`
export const STORE_DESCRIPTION = '小区群每周开团，主做当季鲜果和社区自提。'
export const STORE_LOGO_URL = 'https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=400&q=80'
export const DEFAULT_DELIVERY_TYPE = 'local_delivery'

// ── 商品库商品 ──
export const PRODUCT_NAME = `阳山水蜜桃_${RUN_ID}`
export const PRODUCT_DESCRIPTION = '单份约 5 斤，中大果混装。偏软甜口，适合现吃；运输中轻微压痕不影响食用。'
export const PRODUCT_COVER_URL = 'https://images.unsplash.com/photo-1567306226416-28f0efdc88ce?auto=format&fit=crop&w=800&q=80'
export const PRODUCT_BASE_PRICE = 2990 // 分
export const PRODUCT_STOCK = 100

// ── 团购活动 ──
export const GROUP_BUY_TITLE = `周末阳山水蜜桃社区团_${RUN_ID}`
export const GROUP_BUY_INTRODUCTION = '王姐本周从阳山果园集中收单，适合家庭囤货、办公室拼团和邻里群分享。'
export const GROUP_BUY_COVER_URL = 'https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=800&q=80'
export const GROUP_BUY_DELIVERY_TYPE = 'local_delivery'
export const GROUP_BUY_ITEM_DISPLAY_NAME = `阳山水蜜桃 5 斤装_${RUN_ID}`
export const GROUP_BUY_ITEM_PRICE = 2990 // 分
export const GROUP_BUY_ITEM_STOCK = 100

// ── 收货地址 ──
export const ADDRESS_RECEIVER_NAME = '陈小满'
export const ADDRESS_RECEIVER_PHONE = BUYER_PHONE
export const ADDRESS_PROVINCE = '浙江省'
export const ADDRESS_CITY = '杭州市'
export const ADDRESS_DISTRICT = '西湖区'
export const ADDRESS_DETAIL = '桂花城 3 幢 1 单元门口'
export const ADDRESS_IS_DEFAULT = true

// ── 时间常量 ──
/** 团购开始时间（当前时间往前 1 小时） */
export function getGroupBuyStartTime(): string {
  const d = new Date()
  d.setHours(d.getHours() - 1)
  return d.toISOString().replace(/\.\d{3}Z$/, '+08:00')
}

/** 团购结束时间（当前时间往后 30 天） */
export function getGroupBuyEndTime(): string {
  const d = new Date()
  d.setDate(d.getDate() + 30)
  return d.toISOString().replace(/\.\d{3}Z$/, '+08:00')
}

/** 配送时间（当前时间往后 2 天） */
export function getShippingTime(): string {
  const d = new Date()
  d.setDate(d.getDate() + 2)
  return d.toISOString().replace(/\.\d{3}Z$/, '+08:00')
}
