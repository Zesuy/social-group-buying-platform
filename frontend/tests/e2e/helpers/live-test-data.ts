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
export const BUYER_NICKNAME = `测试买家_${RUN_ID}`
export const LEADER_PHONE = `138${PHONE_SUFFIX}`
export const LEADER_NICKNAME = `测试团长_${RUN_ID}`
export const TEST_AVATAR_URL = 'https://example.com/avatar.png'

// ── 测试店铺 ──
export const STORE_NAME = `测试小店_${RUN_ID}`
export const STORE_DESCRIPTION = 'Live E2E 测试店铺'
export const STORE_LOGO_URL = 'https://example.com/logo.png'
export const DEFAULT_DELIVERY_TYPE = 'express'

// ── 测试商品 ──
export const PRODUCT_NAME = `测试蜜桃_${RUN_ID}`
export const PRODUCT_DESCRIPTION = 'Live E2E 测试商品'
export const PRODUCT_COVER_URL = 'https://example.com/product.png'
export const PRODUCT_BASE_PRICE = 2990 // 分
export const PRODUCT_STOCK = 100

// ── 测试团购 ──
export const GROUP_BUY_TITLE = `测试团购_${RUN_ID}`
export const GROUP_BUY_INTRODUCTION = 'Live E2E 测试团购'
export const GROUP_BUY_COVER_URL = 'https://example.com/cover.png'
export const GROUP_BUY_DELIVERY_TYPE = 'express'
export const GROUP_BUY_ITEM_DISPLAY_NAME = `蜜桃 5 斤装_${RUN_ID}`
export const GROUP_BUY_ITEM_PRICE = 2990 // 分
export const GROUP_BUY_ITEM_STOCK = 100

// ── 测试地址 ──
export const ADDRESS_RECEIVER_NAME = '张三'
export const ADDRESS_RECEIVER_PHONE = BUYER_PHONE
export const ADDRESS_PROVINCE = '浙江省'
export const ADDRESS_CITY = '杭州市'
export const ADDRESS_DISTRICT = '西湖区'
export const ADDRESS_DETAIL = '文三路 100 号'
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
