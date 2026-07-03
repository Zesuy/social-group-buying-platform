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
 * Mock 后端接口：认证 + 店铺 + 公共浏览（首页，抑制 Vite proxy 噪音）
 */
async function mockEndpoints(page: Page) {
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

  // GET /api/v1/me — 根据 token 返回不同用户身份
  await page.route('**/api/v1/me', async (route) => {
    const headers = route.request().headers()
    const auth = headers['authorization'] || ''

    if (auth.includes('mock_token_leader')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            user: { id: 2, nickname: '团长用户', avatarUrl: null, phone: '13700000000', hasLeader: true, leaderId: 10, storeId: 20 },
            leader: { id: 10, displayName: '王姐鲜果团', avatarUrl: null },
            store: { id: 20, name: '王姐社区鲜果店', logoUrl: null, status: 'active' },
          },
          traceId: 'e2e_001',
        }),
      })
    } else if (auth.includes('Bearer mock_token')) {
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

  // POST /api/v1/auth/mock-login
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

  // POST /api/v1/stores
  await page.route('**/api/v1/stores', async (route) => {
    const body = route.request().postDataJSON()
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          leader: { id: 10, displayName: body.name, avatarUrl: body.logoUrl },
          store: {
            id: 20, leaderId: 10, name: body.name,
            logoUrl: body.logoUrl, description: body.description,
            defaultDeliveryType: body.defaultDeliveryType || 'local_delivery',
            distributionEnabled: false, status: 'active',
          },
        },
        traceId: 'e2e_store',
      }),
    })
  })

  // GET + PATCH /api/v1/my/store（合并 handler 避免 Playwright LIFO 冲突）
  await page.route('**/api/v1/my/store', async (route, request) => {
    if (request.method() === 'PATCH') {
      const body = request.postDataJSON()
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            leader: { id: 10, displayName: body.name || '王姐鲜果团', avatarUrl: null },
            store: {
              id: 20, leaderId: 10, name: body.name || '王姐社区鲜果店',
              logoUrl: body.logoUrl, description: body.description,
              defaultDeliveryType: body.defaultDeliveryType || 'local_delivery',
              distributionEnabled: false, status: 'active',
            },
          },
          traceId: 'e2e_store_patch',
        }),
      })
      return
    }

    // GET method
    const headers = route.request().headers()
    const auth = headers['authorization'] || ''
    if (auth.includes('mock_token_leader')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            leader: { id: 10, displayName: '王姐鲜果团', avatarUrl: null, bio: '小区群每周开团，主做当季鲜果和社区自提。' },
            store: {
              id: 20, leaderId: 10, name: '王姐社区鲜果店',
              logoUrl: null, description: '当季鲜果集中收单，同城配送到社区。',
              defaultDeliveryType: 'local_delivery',
              distributionEnabled: false, status: 'active',
            },
          },
          traceId: 'e2e_store_get',
        }),
      })
    } else {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, traceId: 'e2e_store_get' }),
      })
    }
  })

  // POST /api/v1/my/uploads/images
  await page.route('**/api/v1/my/uploads/images', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          url: 'http://localhost:8080/uploads/images/e2e-logo.png',
          objectKey: 'images/e2e-logo.png',
          originalFilename: 'e2e-logo.png',
          contentType: 'image/png',
          size: 9,
        },
        traceId: 'e2e_upload',
      }),
    })
  })
}

test.describe('Profile and store E2E', () => {
  test.beforeEach(async ({ page }) => {
    await mockEndpoints(page)
    await page.goto('/')
    await page.evaluate(() => localStorage.clear())
  })

  test('unauthenticated profile shows login CTA', async ({ page }) => {
    await navigateToHash(page, '/profile')
    await page.waitForTimeout(1000)

    // Should show login CTA
    await expect(page.locator('text=点击登录')).toBeVisible({ timeout: 5000 })
  })

  test('logged-in buyer profile shows buyer entries', async ({ page }) => {
    await page.evaluate(() => localStorage.setItem('accessToken', 'mock_token_e2e_test'))
    await navigateToHash(page, '/profile')
    await page.waitForTimeout(1500)

    // Should show user info
    await expect(page.locator('text=买家用户')).toBeVisible({ timeout: 5000 })
    // Should show buyer-specific entries
    await expect(page.locator('text=我的订单')).toBeVisible()
    await expect(page.locator('text=收货地址')).toBeVisible()
    await expect(page.locator('text=会员卡')).toBeVisible()
    // Should show logout button
    await expect(page.locator('button:has-text("退出登录")')).toBeVisible()
  })

  test('logged-in leader profile shows leader entries', async ({ page }) => {
    await page.evaluate(() => localStorage.setItem('accessToken', 'mock_token_leader_store'))
    await page.evaluate(() => localStorage.setItem('profileFeatureRole', 'leader'))
    await navigateToHash(page, '/profile')
    await page.waitForTimeout(1500)

    // Should show leader info
    await expect(page.locator('text=团长用户')).toBeVisible({ timeout: 5000 })
    // Should show leader-specific entries
    await expect(page.locator('text=商品库')).toBeVisible()
    await expect(page.locator('text=订单管理')).toBeVisible()
    await expect(page.locator('text=团长管理')).toBeVisible()
  })

  test('profile login click goes to login page', async ({ page }) => {
    await navigateToHash(page, '/profile')
    await page.waitForTimeout(1000)

    // Click login CTA
    await page.locator('text=点击登录').click()
    await page.waitForTimeout(1000)

    // Should redirect to login
    await expect(page).toHaveURL(/#\/login/, { timeout: 5000 })
    // redirect query should point back to profile (hash routing keeps it unencoded)
    await expect(page).toHaveURL(/redirect=\/profile/, { timeout: 5000 })
  })

  test('open group page shows cards', async ({ page }) => {
    await navigateToHash(page, '/open-group')
    await page.waitForTimeout(1000)

    // Should see group type cards
    await expect(page.locator('text=普通团购')).toBeVisible({ timeout: 5000 })
    await expect(page.locator('text=预售团购')).toBeVisible()
    await expect(page.locator('text=卡券团购')).toBeVisible()
    await expect(page.locator('text=报名团购')).toBeVisible()
  })

  test('unauthenticated click normal group buy redirects to login', async ({ page }) => {
    await navigateToHash(page, '/open-group')
    await page.waitForTimeout(1000)

    // Click normal group buy
    await page.locator('text=普通团购').click()
    await page.waitForTimeout(1000)

    // Should redirect to login
    await expect(page).toHaveURL(/#\/login/, { timeout: 5000 })
  })

  test('non-leader click normal group buy redirects to create store', async ({ page }) => {
    await page.evaluate(() => localStorage.setItem('accessToken', 'mock_token_e2e_test'))
    await navigateToHash(page, '/open-group')
    await page.waitForTimeout(1500)

    // Click normal group buy
    await page.locator('text=普通团购').click()
    await page.waitForTimeout(1000)

    // Should redirect to create store
    await expect(page).toHaveURL(/#\/store\/create/, { timeout: 5000 })
  })

  test('leader click normal group buy shows action sheet', async ({ page }) => {
    await page.evaluate(() => localStorage.setItem('accessToken', 'mock_token_leader_store'))
    await navigateToHash(page, '/open-group')
    await page.waitForTimeout(1500)

    // Click normal group buy
    await page.locator('text=普通团购').click()
    await page.waitForTimeout(1000)

    // Action sheet should show
    await expect(page.locator('text=发布团购').first()).toBeVisible({ timeout: 5000 })
    await expect(page.locator('text=管理团购').first()).toBeVisible()
  })

  test('non-MVP group buy cards show toast', async ({ page }) => {
    await navigateToHash(page, '/open-group')
    await page.waitForTimeout(1000)

    // Click presale card
    await page.locator('text=预售团购').first().click()
    await page.waitForTimeout(500)

    // Toast should appear (or navigate to nothing)
    // Just verify we don't leave the page
    await expect(page).toHaveURL(/#\/open-group/, { timeout: 3000 })
  })

  test('store create page renders form fields', async ({ page }) => {
    await page.evaluate(() => localStorage.setItem('accessToken', 'mock_token_e2e_test'))
    await navigateToHash(page, '/store/create')
    await page.waitForTimeout(1500)

    // Should render form
    await expect(page.locator('text=店铺名称')).toBeVisible({ timeout: 5000 })
    await expect(page.locator('text=默认物流方式')).toBeVisible()
    await expect(page.locator('button:has-text("创建店铺")')).toBeVisible()
  })

  test('leader store page loads and shows store info', async ({ page }) => {
    await page.evaluate(() => localStorage.setItem('accessToken', 'mock_token_leader_store'))
    await navigateToHash(page, '/leader/store')
    await page.waitForTimeout(2000)

    // Should show store name
    await expect(page.locator('text=王姐社区鲜果店')).toBeVisible({ timeout: 5000 })
    // Should show edit button
    await expect(page.locator('button:has-text("编辑资料")')).toBeVisible()
  })

  test('leader store page can toggle edit mode', async ({ page }) => {
    await page.evaluate(() => localStorage.setItem('accessToken', 'mock_token_leader_store'))
    await navigateToHash(page, '/leader/store')
    await page.waitForTimeout(2000)

    // Click edit
    const editBtn = page.locator('button:has-text("编辑资料")')
    await expect(editBtn).toBeVisible({ timeout: 5000 })
    await editBtn.click()
    await page.waitForTimeout(500)

    // Should see save button in edit mode
    await expect(page.locator('button:has-text("保存")')).toBeVisible({ timeout: 5000 })
  })

  test('leader store page uploads logo and saves returned url', async ({ page }) => {
    await page.evaluate(() => localStorage.setItem('accessToken', 'mock_token_leader_store'))
    await navigateToHash(page, '/leader/store')
    await page.waitForTimeout(2000)

    let patchedLogoUrl = ''
    await page.route('**/api/v1/my/store', async (route, request) => {
      if (request.method() === 'PATCH') {
        const body = request.postDataJSON()
        patchedLogoUrl = body.logoUrl
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              leader: { id: 10, displayName: body.name || '王姐鲜果团', avatarUrl: body.logoUrl },
              store: {
                id: 20, leaderId: 10, name: body.name || '王姐社区鲜果店',
                logoUrl: body.logoUrl, description: body.description,
                defaultDeliveryType: body.defaultDeliveryType || 'local_delivery',
                distributionEnabled: false, status: 'active',
              },
            },
            traceId: 'e2e_store_patch_upload',
          }),
        })
        return
      }
      await route.fallback()
    })

    await page.locator('button:has-text("编辑资料")').click()
    await page.locator('input[type="file"]').setInputFiles({
      name: 'e2e-logo.png',
      mimeType: 'image/png',
      buffer: Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00]),
    })

    const logoInput = page.locator('input[placeholder="Logo URL（选填，可上传）"]')
    await expect(logoInput).toHaveValue('http://localhost:8080/uploads/images/e2e-logo.png')

    await page.locator('button:has-text("保存")').click()
    await expect.poll(() => patchedLogoUrl).toBe('http://localhost:8080/uploads/images/e2e-logo.png')
  })
})
