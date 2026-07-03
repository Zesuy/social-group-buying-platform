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
 * Mock 所有涉及的后端接口
 */
async function mockAllEndpoints(page: Page) {
  // 认证相关
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

  // 团购列表
  await page.route('**/api/v1/group-buys?*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          items: [
            {
              id: 100, title: '测试团购', coverImageUrl: null, status: 'published',
              endTime: '2026-07-01T12:00:00', minPriceAmount: 2990, soldCount: 12,
              leader: { id: 10, displayName: '某某团长', avatarUrl: null },
              store: { id: 20, name: '某某的小店' },
            },
          ],
          page: 1, pageSize: 20, total: 1, hasMore: false,
        },
        traceId: 'e2e_003',
      }),
    })
  })

  // 团购详情
  await page.route('**/api/v1/group-buys/100', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          groupBuy: {
            id: 100, storeId: 20, leaderId: 10, title: '测试团购', introduction: '好吃不贵',
            coverImageUrl: null, groupType: 'normal', deliveryType: 'express',
            shippingTime: '2026-06-30T18:00:00', startTime: '2026-06-24T12:00:00',
            endTime: '2026-07-01T12:00:00', visibility: 'public', status: 'published',
            galleryImageUrls: [],
            contentBlocks: [
              { type: 'section', title: '团长推荐', text: '本团主打产地直发，集中收单后统一发货。' },
              { type: 'deliveryNote', text: '预计 6 月 30 日前后发出。' },
            ],
          },
          leader: { id: 10, displayName: '某某团长', avatarUrl: null, followerCount: 50 },
          store: { id: 20, name: '某某的小店', logoUrl: null },
          items: [
            {
              id: 1001, productId: 501, displayName: '蜜桃 5 斤装', groupPriceAmount: 2990,
              groupStock: 100, soldCount: 12, sortOrder: 1, coverImageUrl: null,
              product: {
                id: 501,
                name: '蜜桃',
                description: '商品自己的口感、规格和储存说明。',
                coverImageUrl: null,
                detailImageUrls: [],
                basePriceAmount: 3990,
                status: 'active',
              },
            },
          ],
          featuredItem: {
            id: 1001, productId: 501, displayName: '蜜桃 5 斤装', groupPriceAmount: 2990,
            groupStock: 100, soldCount: 12, sortOrder: 1, coverImageUrl: null,
            product: {
              id: 501,
              name: '蜜桃',
              description: '商品自己的口感、规格和储存说明。',
              coverImageUrl: null,
              detailImageUrls: [],
              basePriceAmount: 3990,
              status: 'active',
            },
          },
          viewer: { subscribed: false },
        },
        traceId: 'e2e_004',
      }),
    })
  })

  // 团购详情 — 零库存商品（用于测试售罄状态）
  await page.route('**/api/v1/group-buys/101', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          groupBuy: {
            id: 101, storeId: 20, leaderId: 10, title: '售罄团购', introduction: '已卖光',
            coverImageUrl: null, groupType: 'normal', deliveryType: 'express',
            shippingTime: null, startTime: null, endTime: null,
            visibility: 'public', status: 'published',
            galleryImageUrls: [],
            contentBlocks: [],
          },
          leader: { id: 10, displayName: '某某团长', avatarUrl: null, followerCount: 50 },
          store: { id: 20, name: '某某的小店', logoUrl: null },
          items: [
            {
              id: 2001, productId: 502, displayName: '已售罄商品', groupPriceAmount: 1990,
              groupStock: 0, soldCount: 100, sortOrder: 1, coverImageUrl: null,
            },
          ],
          viewer: { subscribed: false },
        },
        traceId: 'e2e_004b',
      }),
    })
  })

  // 团长主页
  await page.route('**/api/v1/leaders/10/homepage*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          leader: { id: 10, displayName: '某某团长', avatarUrl: null, bio: '优质水果团长', memberCount: 30, followerCount: 50 },
          store: { id: 20, name: '某某的小店', logoUrl: null, description: '新鲜水果直供', defaultDeliveryType: 'express' },
          viewer: { subscribed: false },
          groupBuys: {
            items: [
              {
                id: 100, title: '测试团购', coverImageUrl: null, status: 'published',
                endTime: null, minPriceAmount: 2990, soldCount: 12,
                leader: { id: 10, displayName: '某某团长', avatarUrl: null },
                store: { id: 20, name: '某某的小店' },
              },
            ],
            page: 1, pageSize: 20, total: 1, hasMore: false,
          },
        },
        traceId: 'e2e_005',
      }),
    })
  })

  // 地址列表 + 创建地址（合并为一个 handler 避免路由覆盖）
  await page.route('**/api/v1/my/addresses', async (route, request) => {
    if (request.method() === 'GET') {
      const headers = route.request().headers()
      const auth = headers['authorization'] || ''

      // 未登录
      if (!auth.includes('Bearer mock_token')) {
        await route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({
            success: false, error: { code: 'UNAUTHORIZED', message: '未登录' },
            traceId: 'e2e_006',
          }),
        })
        return
      }

      // 模拟无地址用户
      if (auth.includes('mock_token_no_addr')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: [], traceId: 'e2e_006' }),
        })
        return
      }

      // 有默认地址的用户
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: [
            {
              id: 300, receiverName: '张三', receiverPhone: '13800000000',
              province: '浙江省', city: '杭州市', district: '西湖区',
              detail: '某某路 1 号', fullAddress: '浙江省杭州市西湖区某某路 1 号', isDefault: true,
            },
          ],
          traceId: 'e2e_006',
        }),
      })
    } else if (request.method() === 'POST') {
      const data = request.postDataJSON()
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            id: 301, ...data, fullAddress: `${data.province}${data.city}${data.district}${data.detail}`,
          },
          traceId: 'e2e_007',
        }),
      })
    } else {
      await route.fulfill({
        status: 404,
        contentType: 'application/json',
        body: JSON.stringify({
          success: false, error: { code: 'NOT_FOUND', message: 'Not found' },
          traceId: 'e2e_006',
        }),
      })
    }
  })

  // 订单预览
  await page.route('**/api/v1/orders/preview', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          groupBuyId: 100,
          address: { id: 300, receiverName: '张三', receiverPhone: '13800000000', province: '浙江省', city: '杭州市', district: '西湖区', detail: '某某路 1 号', fullAddress: '浙江省杭州市西湖区某某路 1 号' },
          items: [{ groupBuyItemId: 1001, productId: 501, productName: '蜜桃 5 斤装', unitPriceAmount: 2990, quantity: 1, totalAmount: 2990, availableStock: 100, soldCount: 12 }],
          totalAmount: 2990, discountAmount: 0, payAmount: 2990,
        },
        traceId: 'e2e_008',
      }),
    })
  })

  // 创建订单
  await page.route('**/api/v1/orders', async (route, request) => {
    if (request.method() !== 'POST') return
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          id: 9001, orderNo: '2026062900001', groupBuyId: 100, storeId: 20, leaderId: 10,
          totalAmount: 2990, discountAmount: 0, payAmount: 2990,
          payStatus: 'unpaid', orderStatus: 'pendingPay',
          receiverName: '张三', receiverPhone: '13800000000',
          fullAddress: '浙江省杭州市西湖区某某路 1 号',
          items: [{ id: 1, groupBuyItemId: 1001, productName: '蜜桃 5 斤装', unitPriceAmount: 2990, quantity: 1, totalAmount: 2990 }],
        },
        traceId: 'e2e_009',
      }),
    })
  })

  // 订单详情（创建订单后跳转的详情页需要）
  await page.route('**/api/v1/my/orders/**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          id: 9001, orderNo: '2026062900001',
          groupBuyId: 100, storeId: 20, leaderId: 10,
          totalAmount: 2990, discountAmount: 0, payAmount: 2990,
          payStatus: 'unpaid', orderStatus: 'pendingPay',
          receiverName: '张三', receiverPhone: '13800000000',
          province: '浙江省', city: '杭州市', district: '西湖区',
          detail: '某某路 1 号', fullAddress: '浙江省杭州市西湖区某某路 1 号',
          items: [{ id: 1, groupBuyItemId: 1001, productName: '蜜桃 5 斤装', unitPriceAmount: 2990, quantity: 1, totalAmount: 2990 }],
        },
        traceId: 'e2e_order_detail',
      }),
    })
  })
}

test.describe('Public browsing and checkout E2E', () => {
  test.beforeEach(async ({ page }) => {
    await mockAllEndpoints(page)
    await page.goto('/')
    await page.evaluate(() => localStorage.clear())
  })

  test('unauthenticated user browses home page, enters group buy detail and leader page', async ({ page }) => {
    await page.goto('/')
    await page.waitForSelector('.van-tabbar', { timeout: 10000 })
    await page.waitForTimeout(1000)

    // Should see group buy cards
    await expect(page.getByRole('heading', { name: '测试团购' })).toBeVisible({ timeout: 5000 })
    await expect(page.locator('text=¥29.90')).toBeVisible()

    // Click on group buy card -> detail page
    await page.getByRole('heading', { name: '测试团购' }).click()
    await page.waitForTimeout(1000)
    await expect(page.locator('#section-activity').getByText('好吃不贵')).toBeVisible({ timeout: 5000 })
    await expect(page.getByText('本团热销商品')).toBeVisible()
    await expect(page.getByText('本团主打产地直发，集中收单后统一发货。')).toBeVisible()
    await expect(page.getByText('商品自己的口感、规格和储存说明。').first()).toBeVisible()
    await expect(page.locator('.detail-item__name', { hasText: '蜜桃 5 斤装' })).toBeVisible()

    // Click leader trust block -> leader homepage
    await page.locator('text=某某团长').first().click()
    await page.waitForTimeout(1000)
    await expect(page.locator('text=优质水果团长')).toBeVisible({ timeout: 5000 })
    await expect(page.getByRole('heading', { name: '测试团购' })).toBeVisible()
  })

  test('unauthenticated click buy redirects to login, logged in user completes flow', async ({ page }) => {
    // Navigate to group buy detail
    await navigateToHash(page, '/group-buys/100')
    await page.waitForTimeout(1000)

    // Should see detail
    await expect(page.locator('.detail-item__name', { hasText: '蜜桃 5 斤装' })).toBeVisible({ timeout: 5000 })

    // Select item first
    await page.locator('button:has-text("选择")').click()
    await page.waitForTimeout(500)

    // Click buy -> should redirect to login
    await page.locator('button:has-text("立即购买")').click()
    await page.waitForTimeout(1000)
    await expect(page).toHaveURL(/#\/login/, { timeout: 5000 })
  })

  test('logged-in user browses and accesses addresses', async ({ page }) => {
    // Set auth token first
    await page.evaluate(() => localStorage.setItem('accessToken', 'mock_token_e2e_test'))
    await page.goto('/')
    await page.waitForSelector('.van-tabbar', { timeout: 10000 })

    // Navigate to addresses
    await navigateToHash(page, '/addresses')
    await page.waitForTimeout(1000)

    // Should see address list
    await expect(page.locator('text=张三')).toBeVisible({ timeout: 5000 })
    await expect(page.locator('text=浙江省杭州市西湖区某某路 1 号')).toBeVisible()
  })

  test('logged-in user navigates to address new page', async ({ page }) => {
    await page.evaluate(() => localStorage.setItem('accessToken', 'mock_token_e2e_test'))
    await page.goto('/')
    await page.waitForSelector('.van-tabbar', { timeout: 10000 })

    await navigateToHash(page, '/addresses/new')
    await page.waitForTimeout(1000)

    // Should show address form
    await expect(page.locator('text=保存地址')).toBeVisible({ timeout: 5000 })
  })

  test('logged-in user completes checkout flow: detail -> checkout -> order success', async ({ page }) => {
    await page.evaluate(() => localStorage.setItem('accessToken', 'mock_token_e2e_test'))
    await page.goto('/')
    await page.waitForSelector('.van-tabbar', { timeout: 10000 })

    // Navigate to group buy detail
    await navigateToHash(page, '/group-buys/100')
    await page.waitForTimeout(1500)

    // Select item
    await page.locator('button:has-text("选择")').click()
    await page.waitForTimeout(500)

    // Click buy
    await page.locator('button:has-text("立即购买")').click()
    await page.waitForTimeout(1500)

    // Should be on checkout page
    await expect(page).toHaveURL(/#\/checkout/, { timeout: 5000 })
    await expect(page.getByText('蜜桃 5 斤装').first()).toBeVisible({ timeout: 5000 })
    await expect(page.locator('text=¥29.90').first()).toBeVisible()

    // Check agreement checkbox
    const checkbox = page.locator('.van-checkbox')
    await checkbox.click()
    await page.waitForTimeout(500)

    // Submit order
    await page.locator('button:has-text("提交订单")').click()
    await page.waitForTimeout(2000)

    // Should redirect to order detail page
    await expect(page).toHaveURL(/#\/orders\/9001/, { timeout: 5000 })
    // New order detail shows order info, not the old placeholder
    await expect(page.locator('text=订单编号')).toBeVisible({ timeout: 5000 })
    await expect(page.locator('text=2026062900001')).toBeVisible()
  })

  test('unauthenticated subscribe redirects to login', async ({ page }) => {
    await navigateToHash(page, '/group-buys/100')
    await page.waitForTimeout(2000)

    // Should see detail
    await expect(page.locator('.detail-item__name', { hasText: '蜜桃 5 斤装' })).toBeVisible({ timeout: 5000 })

    // Click subscribe button
    await page.getByRole('button', { name: '订阅团长' }).click()
    await page.waitForTimeout(1000)
    await expect(page).toHaveURL(/#\/login/, { timeout: 5000 })
  })

  test('out-of-stock item shows 已售罄 and cannot be selected', async ({ page }) => {
    await page.evaluate(() => localStorage.setItem('accessToken', 'mock_token_e2e_test'))
    await navigateToHash(page, '/group-buys/101')
    await page.waitForTimeout(2000)

    // Should see detail with out-of-stock item
    await expect(page.locator('.detail-item__name', { hasText: '已售罄商品' })).toBeVisible({ timeout: 5000 })

    // Should show "已售罄" tag instead of "选择" button
    await expect(page.getByText('已售罄', { exact: true })).toBeVisible()

    // Should NOT show a "选择" button for this item
    await expect(page.locator('button:has-text("选择")')).not.toBeVisible()

    // The buy button should be disabled, showing "库存不足" or not visible
    // (isPurchasable depends on hasAnyStock which is false)
  })

  test('no-address user sees address creation CTA in checkout', async ({ page }) => {
    await page.evaluate(() => localStorage.setItem('accessToken', 'mock_token_no_addr_e2e'))
    // Navigate to group buy detail first, then try to buy
    await navigateToHash(page, '/group-buys/100')
    await page.waitForTimeout(1500)

    // Select item and buy
    await page.locator('button:has-text("选择")').click()
    await page.waitForTimeout(500)
    await page.locator('button:has-text("立即购买")').click()
    await page.waitForTimeout(2000)

    // Should reach checkout - without an address, should see "去添加地址" CTA
    await expect(page).toHaveURL(/#\/checkout/, { timeout: 5000 })
    await expect(page.locator('text=去添加地址')).toBeVisible({ timeout: 5000 })
  })

  test('address creation returns to checkout with new address selected', async ({ page }) => {
    await page.evaluate(() => localStorage.setItem('accessToken', 'mock_token_no_addr_e2e'))
    await page.goto('/')
    await page.waitForSelector('.van-tabbar', { timeout: 10000 })

    // Start from checkout with no address
    await navigateToHash(page, '/group-buys/100')
    await page.waitForTimeout(1500)
    await page.locator('button:has-text("选择")').click()
    await page.waitForTimeout(500)
    await page.locator('button:has-text("立即购买")').click()
    await page.waitForTimeout(1500)

    // Click "去添加地址"
    await page.locator('button:has-text("去添加地址")').click()
    await page.waitForTimeout(1500)

    // Should be on /addresses page (from=checkout was passed)
    await expect(page).toHaveURL(/#\/addresses/, { timeout: 5000 })

    // Click "新增地址" button in empty state
    await page.locator('button:has-text("新增地址")').click()
    await page.waitForTimeout(1000)

    // Should be on address form page
    await expect(page).toHaveURL(/#\/addresses\/new/, { timeout: 5000 })

    // Fill in address form and save
    const nameInput = page.locator('input[name="receiverName"]')
    if (await nameInput.isVisible()) {
      await nameInput.fill('李四')
      await page.locator('input[name="receiverPhone"]').fill('13900000001')
      await page.locator('input[name="province"]').fill('广东省')
      await page.locator('input[name="city"]').fill('广州市')
      await page.locator('input[name="district"]').fill('天河区')
      await page.locator('textarea').fill('某某路 88 号')
      await page.locator('button:has-text("保存地址")').click()
      await page.waitForTimeout(2000)

      // After creating from checkout flow, should redirect back to /checkout
      await expect(page).toHaveURL(/#\/checkout/, { timeout: 5000 })
    }
  })
})
