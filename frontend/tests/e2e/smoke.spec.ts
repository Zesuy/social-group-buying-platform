import { test, expect, type Page } from '@playwright/test'

/**
 * 在 Hash 路由的 SPA 中跳转到指定路由
 *
 * 直接 page.goto('/#/login') 在 Playwright 中可能触发 hash 丢失，
 * 这里采用：先 goto 到根路径 -> 等 SPA 挂载完成 -> 再通过 evaluate 改 hash。
 */
async function navigateToHash(page: Page, hashPath: string) {
  await page.goto('/')
  // 等 Vant TabBar 渲染，说明 SPA 已挂载
  await page.waitForSelector('.van-tabbar', { timeout: 10000 })
  await page.evaluate((path) => {
    window.location.hash = `#${path}`
  }, hashPath)
}

/**
 * Mock 后端响应辅助
 */
async function mockAuthEndpoints(page: Page) {
  await page.route('**/api/v1/share/group-buys/token-1', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          groupBuy: {
            id: '100',
            storeId: '20',
            leaderId: '10',
            title: '周末阳山水蜜桃社区团',
            introduction: '团长集中收单，周末统一发货。',
            coverImageUrl: null,
            groupType: 'normal',
            deliveryType: 'express',
            shippingTime: '2026-07-10T18:00:00',
            startTime: '2026-07-04T10:00:00',
            endTime: '2026-07-08T22:00:00',
            visibility: 'hidden',
            status: 'published',
            galleryImageUrls: [],
            contentBlocks: [],
          },
          leader: {
            id: '10',
            displayName: '王姐',
            avatarUrl: null,
            followerCount: 28,
          },
          store: {
            id: '20',
            name: '王姐鲜果团',
            logoUrl: null,
            latitude: null,
            longitude: null,
            distanceMeters: null,
            distanceText: null,
          },
          items: [
            {
              id: '1001',
              productId: '1',
              displayName: '阳山水蜜桃 5斤装',
              groupPriceAmount: 3990,
              groupStock: 80,
              soldCount: 12,
              sortOrder: 1,
              coverImageUrl: null,
              product: {
                id: '1',
                name: '阳山水蜜桃',
                description: '偏软甜口，适合现吃。',
                coverImageUrl: null,
                detailImageUrls: [],
                basePriceAmount: 4990,
                status: 'active',
              },
            },
          ],
          featuredItem: null,
          viewer: { subscribed: false },
        },
        traceId: 'e2e_share_detail',
      }),
    })
  })

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

  // Mock GET /api/v1/me — 根据 token 返回不同状态
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
            user: {
              id: 1,
              nickname: '买家用户',
              avatarUrl: null,
              phone: '13800000000',
              hasLeader: false,
              leaderId: null,
              storeId: null,
            },
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

  await page.route('**/api/v1/my/notifications/unread-count', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: { unreadCount: 1 },
        traceId: 'e2e_notifications_count',
      }),
    })
  })

  await page.route('**/api/v1/my/notifications**', async (route) => {
    if (route.request().method() !== 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: null, traceId: 'e2e_notifications_write' }),
      })
      return
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          items: [
            {
              id: 9001,
              type: 'order_shipped',
              title: '发货通知',
              summary: '团长已填写物流：顺丰速运 SF1234567890。',
              body: null,
              targetType: 'order',
              targetId: 3001,
              actionUrl: '/orders/3001',
              readStatus: 'unread',
              readAt: null,
              createdAt: '2026-07-03T10:00:00',
            },
          ],
          page: 1,
          pageSize: 20,
          total: 1,
          hasMore: false,
        },
        traceId: 'e2e_notifications_list',
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

  // Mock POST /api/v1/auth/mock-login
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
            id: 1,
            nickname: body.nickname || '买家用户',
            avatarUrl: body.avatarUrl || null,
            phone: body.phone || '13800000000',
            hasLeader: false,
            leaderId: null,
            storeId: null,
          },
        },
        traceId: 'e2e_002',
      }),
    })
  })
}

test.describe('App smoke test', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthEndpoints(page)
    await page.goto('/')
    await page.evaluate(() => localStorage.clear())
  })

  test('should load the app without errors', async ({ page }) => {
    const consoleErrors: string[] = []
    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text())
      }
    })

    // Clear localStorage from beforeEach then navigate fresh
    await page.goto('/')

    // Verify page renders
    const body = page.locator('body')
    await expect(body).not.toBeEmpty()

    // Verify title is set
    const title = await page.title()
    expect(title).toBeTruthy()

    // Verify no critical errors
    const criticalErrors = consoleErrors.filter(
      (e) =>
        !e.includes('favicon') &&
        !e.includes('404') &&
        !e.includes('Failed to load resource'),
    )
    expect(criticalErrors).toEqual([])
  })

  test('should show 5-tab shell in authenticated state', async ({ page }) => {
    // Set token so app restores session on next load
    await page.goto('/')
    await page.evaluate(() => {
      localStorage.setItem('accessToken', 'mock_token_e2e_test')
    })
    await page.reload()

    // Wait for tab bar
    await page.waitForSelector('.van-tabbar')

    // Check 5 tab items
    const tabs = page.locator('.van-tabbar-item')
    await expect(tabs).toHaveCount(5)

    // Check tab labels
    await expect(tabs.nth(0)).toContainText('首页')
    await expect(tabs.nth(1)).toContainText('订单')
    await expect(tabs.nth(2)).toContainText('开团')
    await expect(tabs.nth(3)).toContainText('消息')
    await expect(tabs.nth(4)).toContainText('我的')
  })

  test('should redirect unauthenticated order tab to login', async ({ page }) => {
    await page.goto('/')
    await page.waitForSelector('.van-tabbar')

    // Click on the orders tab
    const orderTab = page.locator('.van-tabbar-item').nth(1)
    await orderTab.click()

    // Should be redirected to login page
    await expect(page).toHaveURL(/#\/login/, { timeout: 5000 })
  })

  test('should show login page with test user shortcuts', async ({ page }) => {
    await navigateToHash(page, '/login')

    await page.locator('button:has-text("开发测试账号")').click()
    await expect(page.locator('text=买家测试用户')).toBeVisible({ timeout: 5000 })
    await expect(page.locator('text=团长测试用户')).toBeVisible()
  })

  test('should complete mock login flow', async ({ page }) => {
    await navigateToHash(page, '/login')

    await page.locator('button:has-text("开发测试账号")').click()
    await page.locator('text=买家测试用户').click()

    // Should redirect to /profile after login success
    await expect(page).toHaveURL(/#\/profile/, { timeout: 8000 })
  })

  test('should show authenticated notification messages', async ({ page }) => {
    await page.goto('/')
    await page.evaluate(() => {
      localStorage.setItem('accessToken', 'mock_token_e2e_test')
    })
    await page.reload()
    await navigateToHash(page, '/messages')
    await page.locator('.app-tabs__item', { hasText: '通知' }).click()

    await expect(page.locator('text=发货通知')).toBeVisible({ timeout: 5000 })
    await expect(page.locator('text=团长已填写物流')).toBeVisible()
  })

  test('should open group buy detail from share token link', async ({ page }) => {
    await navigateToHash(page, '/share/group-buys/token-1')

    await expect(page.locator('h1:has-text("周末阳山水蜜桃社区团")')).toBeVisible({ timeout: 5000 })
    await expect(page.getByText('王姐鲜果团 · 28人关注')).toBeVisible()
    await expect(page.locator('.detail-item__name', { hasText: '阳山水蜜桃 5斤装' })).toBeVisible()
  })
})
