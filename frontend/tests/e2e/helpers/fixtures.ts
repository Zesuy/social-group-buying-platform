/**
 * Live E2E 测试数据准备 Fixtures
 *
 * 提供创建买家、创建团长、创建店铺、创建商品、发布团购、创建地址、创建订单等
 * 可复用准备函数。所有函数依赖 Playwright 的 APIRequestContext 进行真实后端调用。
 *
 * 使用约定：
 * - 每个 fixture 函数接受 apiContext 和对应的 accessToken
 * - 返回后端响应中的关键 ID 和字段
 * - 不写死任何 ID，全部从真实响应获取
 */

import type { APIRequestContext } from '@playwright/test'
import {
  apiLogin,
  apiCreateStore,
  apiCreateProduct,
  apiCreateGroupBuy,
  apiCreateAddress,
  apiCreateOrder,
  apiSimulatePay,
  apiShipOrder as apiShipOrderRaw,
} from './liveApi'
import {
  BUYER_NICKNAME,
  BUYER_PHONE,
  LEADER_NICKNAME,
  LEADER_PHONE,
  TEST_AVATAR_URL,
  STORE_NAME,
  STORE_DESCRIPTION,
  STORE_LOGO_URL,
  DEFAULT_DELIVERY_TYPE,
  PRODUCT_NAME,
  PRODUCT_DESCRIPTION,
  PRODUCT_COVER_URL,
  PRODUCT_BASE_PRICE,
  PRODUCT_STOCK,
  GROUP_BUY_TITLE,
  GROUP_BUY_INTRODUCTION,
  GROUP_BUY_COVER_URL,
  GROUP_BUY_DELIVERY_TYPE,
  GROUP_BUY_ITEM_DISPLAY_NAME,
  GROUP_BUY_ITEM_PRICE,
  GROUP_BUY_ITEM_STOCK,
  ADDRESS_RECEIVER_NAME,
  ADDRESS_RECEIVER_PHONE,
  ADDRESS_PROVINCE,
  ADDRESS_CITY,
  ADDRESS_DISTRICT,
  ADDRESS_DETAIL,
  ADDRESS_IS_DEFAULT,
  getGroupBuyStartTime,
  getGroupBuyEndTime,
  getShippingTime,
} from './live-test-data'

// ── 类型定义 ──

export interface BuyerFixture {
  token: string
  userId: number
}

export interface LeaderFixture {
  token: string
  userId: number
  leaderId: number
  storeId: number
}

export interface ProductFixture {
  productId: number
}

export interface GroupBuyFixture {
  groupBuyId: number
  groupBuyItemId: number
}

export interface AddressFixture {
  addressId: number
}

export interface OrderFixture {
  orderId: number
}

export interface PreparedData {
  buyer: BuyerFixture
  leader: LeaderFixture
  product: ProductFixture
  groupBuy: GroupBuyFixture
  address: AddressFixture
}

// ── Fixture 函数 ──

/**
 * 创建买家用户（模拟登录）
 */
export async function createBuyer(apiContext: APIRequestContext): Promise<BuyerFixture> {
  const data = await apiLogin(apiContext, {
    nickname: BUYER_NICKNAME,
    phone: BUYER_PHONE,
    avatarUrl: TEST_AVATAR_URL,
  })
  return {
    token: data.accessToken,
    userId: data.user.id as number,
  }
}

/**
 * 创建团长用户（模拟登录 + 创建店铺）
 */
export async function createLeader(apiContext: APIRequestContext): Promise<LeaderFixture> {
  const data = await apiLogin(apiContext, {
    nickname: LEADER_NICKNAME,
    phone: LEADER_PHONE,
    avatarUrl: TEST_AVATAR_URL,
  })
  const token = data.accessToken

  // 创建店铺
  const storeData = await apiCreateStore(apiContext, token, {
    name: STORE_NAME,
    logoUrl: STORE_LOGO_URL,
    description: STORE_DESCRIPTION,
    defaultDeliveryType: DEFAULT_DELIVERY_TYPE,
  })

  // 检查响应结构
  const leader = storeData.leader as Record<string, unknown>
  const store = storeData.store as Record<string, unknown>

  return {
    token,
    userId: data.user.id as number,
    leaderId: leader.id as number,
    storeId: store.id as number,
  }
}

/**
 * 创建商品
 */
export async function createProduct(
  apiContext: APIRequestContext,
  leaderToken: string,
): Promise<ProductFixture> {
  const data = await apiCreateProduct(apiContext, leaderToken, {
    name: PRODUCT_NAME,
    description: PRODUCT_DESCRIPTION,
    coverImageUrl: PRODUCT_COVER_URL,
    basePriceAmount: PRODUCT_BASE_PRICE,
    stock: PRODUCT_STOCK,
  })
  return {
    productId: data.id as number,
  }
}

/**
 * 发布团购
 */
export async function createGroupBuy(
  apiContext: APIRequestContext,
  leaderToken: string,
  productId: number,
): Promise<GroupBuyFixture> {
  const data = await apiCreateGroupBuy(apiContext, leaderToken, {
    title: GROUP_BUY_TITLE,
    introduction: GROUP_BUY_INTRODUCTION,
    coverImageUrl: GROUP_BUY_COVER_URL,
    deliveryType: GROUP_BUY_DELIVERY_TYPE,
    shippingTime: getShippingTime(),
    startTime: getGroupBuyStartTime(),
    endTime: getGroupBuyEndTime(),
    items: [
      {
        productId,
        displayName: GROUP_BUY_ITEM_DISPLAY_NAME,
        groupPriceAmount: GROUP_BUY_ITEM_PRICE,
        groupStock: GROUP_BUY_ITEM_STOCK,
        sortOrder: 1,
      },
    ],
  })

  const groupBuy = data.groupBuy as Record<string, unknown>
  const items = data.items as Array<Record<string, unknown>>

  return {
    groupBuyId: groupBuy.id as number,
    groupBuyItemId: items[0].id as number,
  }
}

/**
 * 创建地址
 */
export async function createAddress(
  apiContext: APIRequestContext,
  buyerToken: string,
): Promise<AddressFixture> {
  const data = await apiCreateAddress(apiContext, buyerToken, {
    receiverName: ADDRESS_RECEIVER_NAME,
    receiverPhone: ADDRESS_RECEIVER_PHONE,
    province: ADDRESS_PROVINCE,
    city: ADDRESS_CITY,
    district: ADDRESS_DISTRICT,
    detail: ADDRESS_DETAIL,
    isDefault: ADDRESS_IS_DEFAULT,
  })
  return {
    addressId: data.id as number,
  }
}

/**
 * 创建订单（买家创建待支付订单）
 */
export async function createOrder(
  apiContext: APIRequestContext,
  buyerToken: string,
  groupBuyId: number,
  addressId: number,
  groupBuyItemId: number,
  quantity = 1,
): Promise<OrderFixture> {
  const data = await apiCreateOrder(apiContext, buyerToken, {
    groupBuyId,
    addressId,
    items: [{ groupBuyItemId, quantity }],
  })
  return {
    orderId: data.id as number,
  }
}

/**
 * 模拟支付订单
 */
export async function payOrder(
  apiContext: APIRequestContext,
  buyerToken: string,
  orderId: number,
): Promise<void> {
  await apiSimulatePay(apiContext, buyerToken, orderId)
}

/**
 * 团长发货
 */
export async function shipOrder(
  apiContext: APIRequestContext,
  leaderToken: string,
  orderId: number,
): Promise<void> {
  await apiShipOrderRaw(apiContext, leaderToken, orderId, {
    deliveryType: 'express',
    logisticsCompany: '顺丰速运',
    trackingNo: `SF${Date.now()}`,
  })
}

/**
 * 完整的测试数据准备
 *
 * 一次性创建买家、团长、店铺、商品、团购、地址，返回所有关键 ID。
 * 适用于需要完整链路（下单 → 支付 → 发货 → 确认收货）的测试。
 */
export async function prepareFullTestData(
  apiContext: APIRequestContext,
): Promise<PreparedData> {
  // 1. 创建买家
  const buyer = await createBuyer(apiContext)

  // 2. 创建团长 + 店铺
  const leader = await createLeader(apiContext)

  // 3. 创建商品
  const product = await createProduct(apiContext, leader.token)

  // 4. 发布团购
  const groupBuy = await createGroupBuy(apiContext, leader.token, product.productId)

  // 5. 创建买家地址
  const address = await createAddress(apiContext, buyer.token)

  return {
    buyer,
    leader,
    product,
    groupBuy,
    address,
  }
}
