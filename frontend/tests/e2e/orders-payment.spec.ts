import { test, expect, type Page } from '@playwright/test'

/**
 * 在 Hash 路由的 SPA 中跳转到指定路由
 */
async function navigateToHash(page: Page, hashPath: string) {
  await page.goto('/')
  await page.waitForSelector('.van-tabbar', { timeout: 10000 })
  await page.evaluate((path) => {
    window.location.hash = `#${path}`
  }, hashPath)
}

/**
 * Mock 所有涉及的后端接口：订单相关 + 公共浏览（首页，抑制 Vite proxy 噪音）
 *
 * 订单 ID 分配约定：
 *   9xxx = 正向路径（成功场景）
 *   8xxx = 异常路径（错误场景）
 */
async function mockOrderEndpoints(page: Page) {
  // 首页团购列表（抑制 Vite proxy 404 噪音）
  await page.route('**/api/v1/group-buys*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: { items: [], page: 1, pageSize: 20, total: 0, hasMore: false },
        traceId: 'e2e_noise_suppress',
      }),
    })
  })

  // 认证
  await page.route('**/api/v1/me', async (route) => {
    const headers = route.request().headers()
    const auth = headers['authorization'] || ''
    if (auth.includes('Bearer mock_token')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            user: { id: 1, nickname: '买家用户', avatarUrl: null, phone: '13800000000', hasLeader: false, leaderId: null, storeId: null },
          },
          traceId: 'e2e_001',
        }),
      })
    } else {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({
          success: false,
          error: { code: 'UNAUTHORIZED', message: '未登录' },
          traceId: 'e2e_001',
        }),
      })
    }
  })

  await page.route('**/api/v1/auth/mock-login', async (route) => {
    const body = route.request().postDataJSON()
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          accessToken: 'mock_token_e2e_login',
          user: {
            id: 1, nickname: body.nickname || '买家用户', avatarUrl: null, phone: body.phone || '13800000000',
            hasLeader: false, leaderId: null, storeId: null,
          },
        },
        traceId: 'e2e_002',
      }),
    })
  })

  await page.route('**/api/v1/my/chat-conversations**', async (route) => {
    const url = route.request().url()
    if (url.includes('/unread-count')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { unreadCount: 0 },
          traceId: 'e2e_chat_count',
        }),
      })
      return
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: { items: [], page: 1, pageSize: 20, total: 0, hasMore: false },
        traceId: 'e2e_chat_list',
      }),
    })
  })

  // ── 订单列表 ──
  await page.route('**/api/v1/my/orders*', async (route, request) => {
    const url = new URL(request.url())
    const status = url.searchParams.get('status') || ''

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let items: any[] = []

    if (!status || status === 'pendingPay') {
      items = [
        {
          id: 9001, orderNo: '20260629001', groupBuyId: 100, storeId: 20, leaderId: 10,
          totalAmount: 2990, discountAmount: 0, payAmount: 2990,
          payStatus: 'unpaid', orderStatus: 'pendingPay',
          receiverName: '陈小满', receiverPhone: '13800000000',
          province: '浙江省', city: '杭州市', district: '西湖区',
          detail: '桂花城 3 幢 1 单元门口', fullAddress: '浙江省杭州市西湖区桂花城 3 幢 1 单元门口',
          items: [{ id: 1, groupBuyItemId: 1001, productId: 501, productName: '阳山水蜜桃 5 斤装', unitPriceAmount: 2990, quantity: 1, totalAmount: 2990 }],
        },
        {
          id: 9002, orderNo: '20260629002', groupBuyId: 100, storeId: 20, leaderId: 10,
          totalAmount: 1990, discountAmount: 0, payAmount: 1990,
          payStatus: 'unpaid', orderStatus: 'pendingPay',
          receiverName: '陈小满', receiverPhone: '13800000000',
          province: '浙江省', city: '杭州市', district: '西湖区',
          detail: '桂花城南门自提点', fullAddress: '浙江省杭州市西湖区桂花城南门自提点',
          items: [{ id: 2, groupBuyItemId: 1002, productId: 502, productName: '阳山水蜜桃 10 斤家庭装', unitPriceAmount: 1990, quantity: 1, totalAmount: 1990 }],
        },
        // 用于正向取消/支付成功的订单（不被异常 handler 覆盖）
        {
          id: 9010, orderNo: '20260629010', groupBuyId: 100, storeId: 20, leaderId: 10,
          totalAmount: 3990, discountAmount: 0, payAmount: 3990,
          payStatus: 'unpaid', orderStatus: 'pendingPay',
          receiverName: '周晨', receiverPhone: '13900000000',
          province: '广东省', city: '广州市', district: '天河区',
          detail: '天河社区服务站', fullAddress: '广东省广州市天河区天河社区服务站',
          items: [{ id: 10, groupBuyItemId: 1010, productId: 510, productName: '海南玫珑瓜 1 个装', unitPriceAmount: 3990, quantity: 1, totalAmount: 3990 }],
        },
      ]
    } else if (status === 'shipped') {
      items = [
        // 用于异常确认收货测试
        {
          id: 9003, orderNo: '20260628003', groupBuyId: 100, storeId: 20, leaderId: 10,
          totalAmount: 3990, discountAmount: 0, payAmount: 3990,
          payStatus: 'paid', orderStatus: 'shipped',
          receiverName: '陈小满', receiverPhone: '13800000000',
          province: '浙江省', city: '杭州市', district: '西湖区',
          detail: '桂花城 3 幢 1 单元门口', fullAddress: '浙江省杭州市西湖区桂花城 3 幢 1 单元门口',
          paidAt: '2026-06-28T12:00:00',
          items: [{ id: 3, groupBuyItemId: 1003, productId: 503, productName: '当季草莓 2 斤装', unitPriceAmount: 3990, quantity: 1, totalAmount: 3990 }],
        },
      ]
    } else if (status === 'completed') {
      items = [
        {
          id: 9004, orderNo: '20260627004', groupBuyId: 100, storeId: 20, leaderId: 10,
          totalAmount: 5990, discountAmount: 0, payAmount: 5990,
          payStatus: 'paid', orderStatus: 'completed',
          receiverName: '陈小满', receiverPhone: '13800000000',
          province: '浙江省', city: '杭州市', district: '西湖区',
          detail: '桂花城 3 幢 1 单元门口', fullAddress: '浙江省杭州市西湖区桂花城 3 幢 1 单元门口',
          paidAt: '2026-06-27T12:00:00', completedAt: '2026-06-29T12:00:00',
          items: [{ id: 4, groupBuyItemId: 1004, productId: 504, productName: '小台农芒果 3 斤装', unitPriceAmount: 5990, quantity: 1, totalAmount: 5990 }],
        },
      ]
    } else if (status === 'canceled') {
      items = [
        {
          id: 9005, orderNo: '20260625005', groupBuyId: 100, storeId: 20, leaderId: 10,
          totalAmount: 2990, discountAmount: 0, payAmount: 2990,
          payStatus: 'unpaid', orderStatus: 'canceled',
          receiverName: '陈小满', receiverPhone: '13800000000',
          province: '浙江省', city: '杭州市', district: '西湖区',
          detail: '桂花城 3 幢 1 单元门口', fullAddress: '浙江省杭州市西湖区桂花城 3 幢 1 单元门口',
          items: [{ id: 5, groupBuyItemId: 1005, productId: 505, productName: '砀山梨 5 斤装', unitPriceAmount: 2990, quantity: 1, totalAmount: 2990 }],
        },
      ]
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: { items, page: 1, pageSize: 20, total: items.length, hasMore: false },
        traceId: 'e2e_orders',
      }),
    })
  })

  // ── 订单详情（动态根据 orderId） ──
  await page.route('**/api/v1/my/orders/**', async (route, request) => {
    const url = new URL(request.url())
    const match = url.pathname.match(/\/orders\/(\d+)/)
    if (!match) return

    const orderId = parseInt(match[1])
    let payStatus = 'unpaid'
    let orderStatus = 'pendingPay'

    if (orderId === 9003 || orderId === 9011) {
      payStatus = 'paid'
      orderStatus = 'shipped'
    } else if (orderId === 9004) {
      payStatus = 'paid'
      orderStatus = 'completed'
    } else if (orderId === 9005) {
      payStatus = 'unpaid'
      orderStatus = 'canceled'
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          id: orderId, orderNo: `20260629${String(orderId).padStart(4, '0')}`,
          groupBuyId: 100, storeId: 20, leaderId: 10,
          totalAmount: orderId === 9010 ? 3990 : 2990,
          discountAmount: 0, payAmount: orderId === 9010 ? 3990 : 2990,
          payStatus, orderStatus,
          receiverName: orderId === 9010 ? '周晨' : '陈小满',
          receiverPhone: orderId === 9010 ? '13900000000' : '13800000000',
          province: orderId === 9010 ? '广东省' : '浙江省',
          city: orderId === 9010 ? '广州市' : '杭州市',
          district: orderId === 9010 ? '天河区' : '西湖区',
          detail: orderId === 9010 ? '天河社区服务站' : '桂花城 3 幢 1 单元门口',
          fullAddress: orderId === 9010 ? '广东省广州市天河区天河社区服务站' : '浙江省杭州市西湖区桂花城 3 幢 1 单元门口',
          items: orderId === 9010
            ? [{ id: 10, groupBuyItemId: 1010, productId: 510, productName: '海南玫珑瓜 1 个装', unitPriceAmount: 3990, quantity: 1, totalAmount: 3990 }]
            : [{ id: 1, groupBuyItemId: 1001, productId: 501, productName: '阳山水蜜桃 5 斤装', unitPriceAmount: 2990, quantity: 1, totalAmount: 2990 }],
          paidAt: orderStatus === 'paid' || orderStatus === 'shipped' || orderStatus === 'completed' ? '2026-06-28T12:00:00' : null,
          shippedAt: orderStatus === 'shipped' || orderStatus === 'completed' ? '2026-06-29T10:00:00' : null,
          completedAt: orderStatus === 'completed' ? '2026-06-29T12:00:00' : null,
        },
        traceId: 'e2e_detail',
      }),
    })
  })

  // ── 通用成功 handler（先注册；LIFO 下优先级低） ──
  // 所有非特例 orderId 的请求落到这里

  // 模拟支付（默认成功）
  await page.route('**/api/v1/orders/*/simulate-pay', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          id: 9010, orderNo: '20260629010',
          payStatus: 'paid', orderStatus: 'paid',
          payAmount: 3990,
          paidAt: '2026-06-29T12:35:00',
          items: [{ id: 10, groupBuyItemId: 1010, productName: '海南玫珑瓜 1 个装', unitPriceAmount: 3990, quantity: 1, totalAmount: 3990 }],
        },
        traceId: 'e2e_pay',
      }),
    })
  })

  // 取消订单（默认成功）
  await page.route('**/api/v1/orders/*/cancel', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: { id: 9010, orderStatus: 'canceled' },
        traceId: 'e2e_cancel',
      }),
    })
  })

  // 确认收货（默认成功）
  await page.route('**/api/v1/orders/*/complete', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          id: 9011, orderNo: '20260629011',
          payStatus: 'paid', orderStatus: 'completed',
          payAmount: 4990,
          completedAt: '2026-06-29T12:35:00',
          items: [{ id: 11, groupBuyItemId: 1011, productName: '妃子笑荔枝 2 斤装', unitPriceAmount: 4990, quantity: 1, totalAmount: 4990 }],
        },
        traceId: 'e2e_complete',
      }),
    })
  })

  // ── 异常路径 handler（后注册；LIFO 下优先级高，覆盖通用 handler） ──

  // ORDER_ALREADY_PAID: 对 order 9001 的 simulate-pay 返回 409
  await page.route('**/api/v1/orders/9001/simulate-pay', async (route) => {
    await route.fulfill({
      status: 409,
      contentType: 'application/json',
      body: JSON.stringify({
        success: false,
        error: { code: 'ORDER_ALREADY_PAID', message: '订单已支付' },
        traceId: 'e2e_pay_err',
      }),
    })
  })

  // ORDER_NOT_CANCELABLE: 对 order 9001 的 cancel 返回 422
  await page.route('**/api/v1/orders/9001/cancel', async (route) => {
    await route.fulfill({
      status: 422,
      contentType: 'application/json',
      body: JSON.stringify({
        success: false,
        error: { code: 'ORDER_NOT_CANCELABLE', message: '订单不可取消' },
        traceId: 'e2e_cancel_err',
      }),
    })
  })

  // ORDER_NOT_COMPLETABLE: 对 order 9003 的 complete 返回 422
  await page.route('**/api/v1/orders/9003/complete', async (route) => {
    await route.fulfill({
      status: 422,
      contentType: 'application/json',
      body: JSON.stringify({
        success: false,
        error: { code: 'ORDER_NOT_COMPLETABLE', message: '订单不可确认收货' },
        traceId: 'e2e_complete_err',
      }),
    })
  })
}

test.describe('Orders and payment E2E', () => {
  test.beforeEach(async ({ page }) => {
    await mockOrderEndpoints(page)
    await page.goto('/')
    await page.evaluate(() => {
      localStorage.clear()
      localStorage.setItem('accessToken', 'mock_token_e2e_test')
    })
  })

  // ── 正向路径 ──

  test('should show order list with pending pay tab by default', async ({ page }) => {
    await page.goto('/')
    await page.waitForSelector('.van-tabbar', { timeout: 10000 })
    await navigateToHash(page, '/orders')
    await page.waitForTimeout(2000)

    await expect(page.locator('text=阳山水蜜桃 5 斤装')).toBeVisible({ timeout: 5000 })
    await expect(page.locator('text=阳山水蜜桃 10 斤家庭装')).toBeVisible()
    await expect(page.locator('text=¥29.90').first()).toBeVisible()
  })

  test('should cancel a pending pay order successfully from order detail', async ({ page }) => {
    // 使用正向 ID 9010（不受异常 handler 影响）
    await navigateToHash(page, '/orders/9010')
    await page.waitForTimeout(2000)

    await expect(page.locator('text=订单编号')).toBeVisible({ timeout: 5000 })

    const cancelBtn = page.locator('button:has-text("取消订单")')
    await expect(cancelBtn).toBeVisible()

    await cancelBtn.click()
    // 等待 Vant Dialog 出现，点击"确认"
    await expect(page.locator('.van-dialog')).toBeVisible({ timeout: 5000 })
    await page.locator('.van-dialog button:has-text("确认")').click()
    await page.waitForTimeout(1500)

    // 取消成功后应刷新详情，检查页面正常
    await expect(page.locator('text=订单编号')).toBeVisible({ timeout: 5000 })
  })

  test('should navigate to order detail when clicking order card', async ({ page }) => {
    await navigateToHash(page, '/orders')
    await page.waitForTimeout(2000)

    await page.locator('text=阳山水蜜桃 5 斤装').first().click()
    await page.waitForTimeout(1500)

    await expect(page).toHaveURL(/#\/orders\/9001/, { timeout: 5000 })
  })

  test('should show shipped orders tab', async ({ page }) => {
    await navigateToHash(page, '/orders')
    await page.waitForTimeout(2000)

    await page.locator('.van-tab', { hasText: '已发货' }).click()
    await page.waitForTimeout(1500)

    await expect(page.locator('text=当季草莓 2 斤装')).toBeVisible({ timeout: 5000 })
  })

  test('should show completed tab', async ({ page }) => {
    await navigateToHash(page, '/orders')
    await page.waitForTimeout(2000)

    await page.locator('.van-tab', { hasText: '已完成' }).click()
    await page.waitForTimeout(1500)

    await expect(page.locator('text=小台农芒果 3 斤装')).toBeVisible({ timeout: 5000 })
  })

  test('should show canceled tab', async ({ page }) => {
    await navigateToHash(page, '/orders')
    await page.waitForTimeout(2000)

    await page.locator('.van-tab', { hasText: '已取消' }).click()
    await page.waitForTimeout(1500)

    await expect(page.locator('text=砀山梨 5 斤装')).toBeVisible({ timeout: 5000 })
  })

  // ── 异常路径：错误提示验证 ──

  test('should show ORDER_ALREADY_PAID toast when paying an already-paid order', async ({ page }) => {
    await navigateToHash(page, '/orders/9001')
    await page.waitForTimeout(2000)

    const payBtn = page.locator('button:has-text("模拟支付")')
    await expect(payBtn).toBeVisible({ timeout: 5000 })

    await payBtn.click()
    // 等待 Vant Dialog 出现，点击"确认"
    await expect(page.locator('.van-dialog')).toBeVisible({ timeout: 5000 })
    await page.locator('.van-dialog button:has-text("确认")').click()
    await page.waitForTimeout(1500)

    // Vant showToast 渲染 .van-toast，应包含错误文案
    await expect(page.locator('.van-toast')).toBeVisible({ timeout: 5000 })
    await expect(page.locator('.van-toast__text')).toHaveText('订单已支付')
  })

  test('should show ORDER_NOT_CANCELABLE toast when canceling a non-cancelable order', async ({ page }) => {
    await navigateToHash(page, '/orders/9001')
    await page.waitForTimeout(2000)

    const cancelBtn = page.locator('button:has-text("取消订单")')
    await expect(cancelBtn).toBeVisible({ timeout: 5000 })

    await cancelBtn.click()
    await expect(page.locator('.van-dialog')).toBeVisible({ timeout: 5000 })
    await page.locator('.van-dialog button:has-text("确认")').click()
    await page.waitForTimeout(1500)

    await expect(page.locator('.van-toast')).toBeVisible({ timeout: 5000 })
    await expect(page.locator('.van-toast__text')).toHaveText('订单不可取消')
  })

  test('should show ORDER_NOT_COMPLETABLE toast when completing a non-completable order', async ({ page }) => {
    await navigateToHash(page, '/orders/9003')
    await page.waitForTimeout(2000)

    const completeBtn = page.locator('button:has-text("确认收货")')
    await expect(completeBtn).toBeVisible({ timeout: 5000 })

    await completeBtn.click()
    await expect(page.locator('.van-dialog')).toBeVisible({ timeout: 5000 })
    await page.locator('.van-dialog button:has-text("确认")').click()
    await page.waitForTimeout(1500)

    await expect(page.locator('.van-toast')).toBeVisible({ timeout: 5000 })
    await expect(page.locator('.van-toast__text')).toHaveText('订单不可确认收货')
  })
})
