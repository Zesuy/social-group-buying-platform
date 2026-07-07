import { test, expect, type Page } from '@playwright/test'
import { navigateToHash } from './helpers/navigation'

async function mockMerchantEndpoints(page: Page) {
  let orderStatus = 'paid'
  let afterSaleStatus = 'pending'
  let productDeleted = false
  let groupBuyStatus = 'published'
  let couponStatus = 'active'

  await page.route('**/api/v1/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          user: { id: 2, nickname: '团长用户', avatarUrl: null, phone: '13700000000', hasLeader: true, leaderId: 10, storeId: 20 },
          leader: { id: 10, displayName: '王姐', avatarUrl: null },
          store: { id: 20, leaderId: 10, name: '王姐社区鲜果店', logoUrl: null, description: '社区鲜果自提点', defaultDeliveryType: 'express', distributionEnabled: false, status: 'active', latitude: null, longitude: null },
        },
        traceId: 'e2e_me',
      }),
    })
  })

  await page.route('**/api/v1/my/store/workbench-summary', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          store: { id: 20, name: '王姐社区鲜果店', logoUrl: null, status: 'active' },
          leader: { id: 10, displayName: '王姐', avatarUrl: null },
          todos: { paidOrders: 1, pendingAfterSales: 1, unreadLeaderChats: 2, publishedGroupBuys: 1 },
          statusCounts: {
            orders: { paid: 1, shipped: 0, completed: 0, afterSale: 0, canceled: 0 },
            afterSales: { pending: 1, approved: 0, rejected: 0, completed: 0 },
            groupBuys: { draft: 0, published: 1, ended: 0 },
          },
        },
        traceId: 'e2e_summary',
      }),
    })
  })

  await page.route('**/api/v1/my/notifications**', async (route) => {
    const isUnreadCount = /\/unread-count$/.test(new URL(route.request().url()).pathname)
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: isUnreadCount ? { unreadCount: 0 } : { items: [], page: 1, pageSize: 5, total: 0, hasMore: false },
        traceId: 'e2e_notifications',
      }),
    })
  })

  await page.route('**/api/v1/group-buys**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: { items: [], page: 1, pageSize: 20, total: 0, hasMore: false }, traceId: 'e2e_public_group_buys' }),
    })
  })

  await page.route('**/api/v1/my/subscriptions', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: { items: [] }, traceId: 'e2e_my_subscriptions' }),
    })
  })

  await page.route('**/api/v1/my/store/orders**', async (route) => {
    const url = new URL(route.request().url())
    const isDetail = /\/orders\/[^/?]+$/.test(url.pathname)
    const isShip = /\/orders\/[^/?]+\/ship$/.test(url.pathname)
    const order = {
      id: 9001,
      orderNo: '202607060001',
      groupBuyId: 100,
      userId: 1,
      storeId: 20,
      leaderId: 10,
      totalAmount: 3980,
      discountAmount: 0,
      payAmount: 3980,
      payStatus: 'paid',
      orderStatus,
      paidAt: '2026-07-06T10:00:00',
      shippedAt: orderStatus === 'shipped' ? '2026-07-06T11:00:00' : null,
      completedAt: null,
      remark: null,
      receiverName: '李四',
      receiverPhone: '13800000002',
      province: '广东省',
      city: '深圳市',
      district: '南山区',
      detail: '科技园路 100 号',
      fullAddress: '广东省深圳市南山区科技园路 100 号',
      buyerNickname: '小李',
      items: [{ id: 1, groupBuyItemId: 11, productId: 21, productName: '阳山水蜜桃', skuName: null, unitPriceAmount: 1990, quantity: 2, totalAmount: 3980 }],
    }

    if (isShip) {
      orderStatus = 'shipped'
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: { order: { ...order, orderStatus: 'shipped', shippedAt: '2026-07-06T11:00:00' }, shipment: { id: 1, orderId: 9001, deliveryType: 'express', logisticsCompany: '顺丰速运', trackingNo: 'SF123456' } }, traceId: 'e2e_ship' }),
      })
      return
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: isDetail ? order : { items: [order], page: 1, pageSize: 30, total: 1, hasMore: false },
        traceId: 'e2e_orders',
      }),
    })
  })

  await page.route('**/api/v1/my/store/after-sales**', async (route) => {
    const url = new URL(route.request().url())
    const isDetail = /\/after-sales\/[^/?]+$/.test(url.pathname)
    const isApprove = /\/approve$/.test(url.pathname)
    const item = {
      id: 3001,
      orderId: 9001,
      orderNo: '202607060001',
      userId: 1,
      leaderId: 10,
      storeId: 20,
      type: 'refund',
      reason: '商品质量问题',
      status: afterSaleStatus,
      amount: 3980,
      originalOrderStatus: 'paid',
      orderStatus: 'after_sale',
      payStatus: 'paid',
      buyerNickname: '小李',
      receiverName: '李四',
      receiverPhone: '13800000002',
      fullAddress: '广东省深圳市南山区科技园路 100 号',
      createdAt: '2026-07-06T10:20:00',
    }

    if (isApprove) afterSaleStatus = 'approved'
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: isDetail || isApprove ? { ...item, status: afterSaleStatus } : { items: [item], page: 1, pageSize: 30, total: 1, hasMore: false },
        traceId: 'e2e_after_sales',
      }),
    })
  })

  await page.route('**/api/v1/my/chat-conversations**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          items: [{ id: 7001, buyerUserId: 1, leaderUserId: 2, storeId: 20, buyerName: '小李', leaderName: '王姐', storeName: '王姐社区鲜果店', currentUserRole: 'leader', unreadCount: 2, lastMessageText: '什么时候发货', lastMessageAt: '2026-07-06T10:30:00', createdAt: '2026-07-06T10:00:00' }],
          page: 1,
          pageSize: 30,
          total: 1,
          hasMore: false,
        },
        traceId: 'e2e_chats',
      }),
    })
  })

  await page.route('**/api/v1/my/store/products**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    const isDetail = /\/products\/[^/?]+$/.test(url.pathname)
    const product = {
      id: 21,
      storeId: 20,
      name: '阳山水蜜桃',
      description: '香甜多汁',
      coverImageUrl: null,
      detailImageUrls: [],
      basePriceAmount: 1990,
      stock: 80,
      status: 'active',
      createdAt: '2026-07-06T09:00:00',
      updatedAt: '2026-07-06T09:00:00',
    }

    if (request.method() === 'POST') {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { ...product, id: 22, name: '临安山核桃仁' }, traceId: 'e2e_product_create' }) })
      return
    }
    if (request.method() === 'PATCH') {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { ...product, name: '阳山水蜜桃 5 斤装' }, traceId: 'e2e_product_update' }) })
      return
    }
    if (request.method() === 'DELETE') {
      productDeleted = true
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, traceId: 'e2e_product_delete' }) })
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: isDetail ? product : { items: productDeleted ? [] : [product], page: 1, pageSize: 50, total: productDeleted ? 0 : 1, hasMore: false },
        traceId: 'e2e_products',
      }),
    })
  })

  await page.route('**/api/v1/my/store/group-buys/ai-polish', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          title: '周末鲜桃团',
          introduction: '新鲜采摘，社区统一履约。',
          contentBlocks: [{ type: 'list', title: '推荐理由', text: null, url: null, caption: null, items: ['当季鲜桃', '次日发货'] }],
          source: 'local',
        },
        traceId: 'e2e_ai',
      }),
    })
  })

  await page.route('**/api/v1/my/store/group-buys**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    const isAiPolish = /\/ai-polish$/.test(url.pathname)
    const isDetail = /\/group-buys\/[^/?]+$/.test(url.pathname)
    const isShare = /\/share-card$/.test(url.pathname)
    const isEnd = /\/end$/.test(url.pathname)
    const listItem = { id: 100, storeId: 20, leaderId: 10, title: '周末水蜜桃团', introduction: '香甜多汁', coverImageUrl: null, groupType: 'normal', deliveryType: 'express', shippingTime: '48 小时内发货', startTime: null, endTime: '2026-07-10T20:00:00', visibility: 'public', status: groupBuyStatus }

    if (isAiPolish) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            title: '周末鲜桃团',
            introduction: '新鲜采摘，社区统一履约。',
            contentBlocks: [{ type: 'list', title: '推荐理由', text: null, url: null, caption: null, items: ['当季鲜桃', '次日发货'] }],
            source: 'local',
          },
          traceId: 'e2e_ai',
        }),
      })
      return
    }

    if (isShare) {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { shareToken: 'share_100', title: listItem.title, coverImageUrl: null, minPriceAmount: 1990, maxPriceAmount: 1990, storeName: '王姐社区鲜果店', leaderName: '王姐', deliveryType: 'express', shippingTime: '48 小时内发货' }, traceId: 'e2e_share' }) })
      return
    }
    if (isEnd) {
      groupBuyStatus = 'ended'
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { groupBuy: { id: 100, status: 'ended' }, items: [] }, traceId: 'e2e_end' }) })
      return
    }
    if (request.method() === 'POST') {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { groupBuy: listItem, items: [] }, traceId: 'e2e_group_create' }) })
      return
    }
    if (request.method() === 'PATCH') {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: { groupBuy: { ...listItem, title: '周末水蜜桃团升级版' }, items: [] }, traceId: 'e2e_group_update' }) })
      return
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: isDetail
          ? { groupBuy: listItem, items: [{ id: 11, groupBuyId: 100, productId: 21, displayName: '阳山水蜜桃', groupPriceAmount: 1990, groupStock: 80, soldCount: 5, sortOrder: 1 }] }
          : { items: [listItem], page: 1, pageSize: 30, total: 1, hasMore: false },
        traceId: 'e2e_group_buys',
      }),
    })
  })

  await page.route('**/api/v1/my/store/coupons**', async (route) => {
    const request = route.request()
    const coupon = { id: 501, storeId: 20, name: '新客订阅立减券', couponType: 'amount', claimCondition: 'new_subscriber', amount: 1000, thresholdAmount: 0, totalQuantity: 100, claimedQuantity: 3, perUserLimit: 1, startTime: '2026-07-06T00:00:00', endTime: '2026-08-06T00:00:00', status: couponStatus }
    if (request.method() === 'POST' && /\/disable$/.test(new URL(request.url()).pathname)) {
      couponStatus = 'disabled'
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, traceId: 'e2e_coupon_disable' }) })
      return
    }
    if (request.method() === 'POST' || request.method() === 'PATCH') {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: coupon, traceId: 'e2e_coupon_save' }) })
      return
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: [coupon], traceId: 'e2e_coupons' }) })
  })

  await page.route('**/api/v1/my/store/subscribers', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: { items: [{ subscriptionId: 'sub_1001', userId: 1, nickname: '小李', avatarUrl: null, phone: '13800000002', source: 'homepage', subscribedAt: '2026-07-06T10:00:00' }], total: 1 }, traceId: 'e2e_subscribers' }),
    })
  })

  await page.route('**/api/v1/my/store', async (route) => {
    const request = route.request()
    const storeData = {
      leader: { id: 10, userId: 2, displayName: '王姐', avatarUrl: null },
      store: { id: 20, leaderId: 10, name: '王姐社区鲜果店', logoUrl: null, description: '社区鲜果自提点', defaultDeliveryType: 'express', distributionEnabled: false, status: 'active', latitude: 22.5431, longitude: 114.0579 },
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: storeData, traceId: request.method() === 'PATCH' ? 'e2e_store_update' : 'e2e_store' }),
    })
  })
}

test.describe('merchant web admin', () => {
  test.beforeEach(async ({ page }) => {
    await mockMerchantEndpoints(page)
    await page.goto('/')
    await page.evaluate(() => localStorage.setItem('accessToken', 'mock_token_leader_store'))
  })

  test('dashboard to orders and ship flow', async ({ page }) => {
    await navigateToHash(page, '/merchant/dashboard')
    await expect(page.getByText('履约运营台')).toBeVisible()
    await expect(page.getByText('待发货订单')).toBeVisible()

    await page.locator('.merchant-sidebar').getByRole('link', { name: /订单/ }).click()
    await expect(page).toHaveURL(/#\/merchant\/orders/)
    await expect(page.getByText('202607060001')).toBeVisible()

    await page.getByRole('link', { name: '发货' }).click()
    await expect(page).toHaveURL(/#\/merchant\/orders\/9001/)
    await page.getByPlaceholder('如 顺丰速运').fill('顺丰速运')
    await page.getByPlaceholder('可选').fill('SF123456')
    await page.getByRole('button', { name: '确认发货' }).click()
    await expect(page.getByText('已发货')).toBeVisible()
  })

  test('after-sales and chats links work', async ({ page }) => {
    await navigateToHash(page, '/merchant/after-sales')
    await expect(page.getByText('商品质量问题')).toBeVisible()
    await page.getByRole('link', { name: '审核' }).click()
    await expect(page).toHaveURL(/#\/merchant\/after-sales\/3001/)

    await navigateToHash(page, '/merchant/chats')
    await expect(page.getByText('什么时候发货')).toBeVisible()
    await page.getByRole('link', { name: '进入会话' }).click()
    await expect(page).toHaveURL(/#\/chats\/7001/)
  })

  test('products new edit and delete flow', async ({ page }) => {
    await navigateToHash(page, '/merchant/products')
    await expect(page.getByRole('row', { name: /阳山水蜜桃/ })).toBeVisible()

    await page.getByRole('link', { name: /新建商品/ }).click()
    await page.getByPlaceholder('例如：临安山核桃仁').fill('临安山核桃仁')
    await page.getByPlaceholder('规格、产地、口感、保存方式和配送注意事项').fill('小袋装，适合社群拼团')
    await page.getByPlaceholder('0.00').fill('29.9')
    await page.getByPlaceholder('库存数量').fill('30')
    await page.getByRole('button', { name: '保存商品' }).click()
    await expect(page).toHaveURL(/#\/merchant\/products/)

    await page.getByRole('link', { name: '编辑' }).click()
    await page.getByPlaceholder('例如：临安山核桃仁').fill('阳山水蜜桃 5 斤装')
    await page.getByRole('button', { name: '保存商品' }).click()
    await expect(page).toHaveURL(/#\/merchant\/products/)

    await page.getByRole('button', { name: '删除' }).click()
    await page.getByRole('button', { name: '确认' }).click()
    await expect(page.getByText('暂无商品')).toBeVisible()
  })

  test('group buy new detail share and end flow', async ({ page }) => {
    await navigateToHash(page, '/merchant/group-buys')
    await expect(page.getByRole('row', { name: /周末水蜜桃团/ })).toBeVisible()

    await page.getByRole('link', { name: /新建团购/ }).click()
    await page.getByPlaceholder('例如：周末阳山水蜜桃社区团').fill('周末水蜜桃团')
    await page.getByRole('button', { name: /阳山水蜜桃/ }).click()
    await page.getByRole('button', { name: 'AI 润色' }).click()
    await expect(page.getByText('AI 润色建议')).toBeVisible()
    await page.getByRole('button', { name: '采用建议' }).click()
    await page.getByRole('button', { name: '发布团购' }).click()
    await expect(page).toHaveURL(/#\/merchant\/group-buys/)

    await page.getByRole('link', { name: '详情' }).click()
    await expect(page).toHaveURL(/#\/merchant\/group-buys\/100/)
    await page.getByRole('button', { name: '分享' }).first().click()
    await expect(page.getByRole('button', { name: '复制链接' })).toBeVisible()
    await page.getByLabel('关闭分享').click()
    await page.getByRole('button', { name: '结束团购' }).click()
    await page.getByRole('button', { name: '确认' }).click()
    await expect(page.getByText('已结束', { exact: true })).toBeVisible()
  })

  test('coupons subscribers and store pages render key operations', async ({ page }) => {
    await navigateToHash(page, '/merchant/coupons')
    await expect(page.getByText('新客订阅立减券')).toBeVisible()
    await page.getByRole('button', { name: '停用' }).click()
    await page.getByRole('button', { name: '确认' }).click()
    await expect(page.getByRole('cell', { name: '已停用' })).toBeVisible()

    await navigateToHash(page, '/merchant/subscribers?subscriptionId=sub_1001')
    await expect(page.getByText('小李')).toBeVisible()
    await expect(page.getByText('sub_1001')).toBeVisible()

    await navigateToHash(page, '/merchant/store')
    await page.getByPlaceholder('请输入店铺名称').fill('王姐社区鲜果店 PC')
    await page.getByRole('button', { name: '同城配送' }).click()
    await page.getByRole('button', { name: '保存资料' }).click()
    await expect(page.getByText('保存成功')).toBeVisible()
  })
})
