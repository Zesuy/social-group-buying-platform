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
    await expect(tabs.nth(2)).toContainText('一键开团')
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

    // Check for test user shortcut buttons
    await expect(page.locator('text=买家测试用户')).toBeVisible({ timeout: 5000 })
    await expect(page.locator('text=团长测试用户')).toBeVisible()
  })

  test('should complete mock login flow', async ({ page }) => {
    await navigateToHash(page, '/login')

    // Click buyer test user shortcut
    await page.locator('text=买家测试用户').click()

    // Click submit button
    await page.locator('button:has-text("登录")').click()

    // Should redirect to /profile after login success
    await expect(page).toHaveURL(/#\/profile/, { timeout: 8000 })
  })

  test('should show messages tab with empty state', async ({ page }) => {
    await navigateToHash(page, '/messages')

    // Check empty state is shown
    await expect(page.locator('text=暂无消息')).toBeVisible({ timeout: 5000 })
  })
})
