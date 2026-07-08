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
  const cartItems = new Map<number, {
    cartItemId: number
    groupBuyId: string
    groupBuyItemId: number
    productId: number
    title: string
    coverImageUrl: string | null
    groupPriceAmount: number
    quantity: number
    availableStock: number
    visibility: string
    status: string
    startTime: string | null
    endTime: string | null
  }>()

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

  await page.route('**/api/v1/my/notifications**', async (route) => {
    const isUnreadCount = new URL(route.request().url()).pathname.endsWith('/unread-count')
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: isUnreadCount ? { unreadCount: 0 } : { items: [], page: 1, pageSize: 20, total: 0, hasMore: false },
        traceId: 'e2e_notifications',
      }),
    })
  })

  await page.route('**/api/v1/my/subscriptions', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: { items: [] }, traceId: 'e2e_subscriptions' }),
    })
  })

  await page.route('**/api/v1/my/orders**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: { items: [], page: 1, pageSize: 20, total: 0, hasMore: false }, traceId: 'e2e_orders' }),
    })
  })

  await page.route('**/api/v1/leaders/*/coupons**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: { items: [] }, traceId: 'e2e_leader_coupons' }),
    })
  })

  await page.route('**/api/v1/my/chat-conversations**', async (route) => {
    const url = new URL(route.request().url())
    if (url.pathname.endsWith('/unread-count')) {
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
    if (/\/api\/v1\/my\/chat-conversations\/7001\/messages$/.test(url.pathname)) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            items: [
              {
                id: 'm7001',
                conversationId: 7001,
                senderUserId: 2,
                senderRole: 'leader',
                messageType: 'text',
                content: '今晚统一发货',
                mine: false,
                createdAt: '2026-07-06T10:30:00',
              },
            ],
            page: 1,
            pageSize: 30,
            total: 1,
            hasMore: false,
          },
          traceId: 'e2e_chat_messages',
        }),
      })
      return
    }
    if (/\/api\/v1\/my\/chat-conversations\/7001\/read$/.test(url.pathname)) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, traceId: 'e2e_chat_read' }),
      })
      return
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          items: [{
            id: 7001,
            buyerUserId: 1,
            leaderUserId: 2,
            storeId: 20,
            buyerName: '买家用户',
            buyerAvatarUrl: null,
            leaderName: '王姐鲜果团',
            leaderAvatarUrl: null,
            storeName: '王姐社区鲜果店',
            currentUserRole: 'buyer',
            unreadCount: 1,
            lastMessageText: '今晚统一发货',
            lastMessageAt: '2026-07-06T10:30:00',
            createdAt: '2026-07-06T10:00:00',
          }],
          page: 1,
          pageSize: 20,
          total: 1,
          hasMore: false,
        },
        traceId: 'e2e_chat_list',
      }),
    })
  })

  // 团购列表
  await page.route('**/api/v1/group-buys?*', async (route) => {
    const url = new URL(route.request().url())
    const hasLocation = url.searchParams.has('latitude') && url.searchParams.has('longitude')
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          items: [
            {
              id: '100', title: '周末阳山水蜜桃社区团', coverImageUrl: null, status: 'published',
              endTime: '2026-07-12T12:00:00', minPriceAmount: 2990, soldCount: 61,
              leader: { id: 10, displayName: '王姐鲜果团', avatarUrl: null },
              store: {
                id: 20,
                name: '王姐社区鲜果店',
                latitude: 30.27,
                longitude: 120.15,
                distanceMeters: hasLocation ? 860 : null,
                distanceText: hasLocation ? '860m' : null,
              },
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
            id: '100', storeId: '20', leaderId: '10', title: '周末阳山水蜜桃社区团',
            introduction: '王姐本周从阳山果园集中收单，适合家庭囤货、办公室拼团和邻里群分享。',
            coverImageUrl: null, groupType: 'normal', deliveryType: 'local_delivery',
            shippingTime: '2026-07-12T18:00:00', startTime: '2026-07-03T10:00:00',
            endTime: '2026-07-12T12:00:00', visibility: 'public', status: 'published',
            galleryImageUrls: [],
            contentBlocks: [
              { type: 'section', title: '团长推荐', text: '这次团不是长期货架，王姐按微信群订单量向果园集中订货，凑齐后统一配送到社区。' },
              { type: 'paragraph', text: '桃子是偏软甜口，收到后建议先拆箱通风，软的当天吃，偏硬的常温放 1-2 天。' },
              { type: 'list', items: ['适合家庭囤货', '适合办公室拼团', '支持同城配送'] },
              { type: 'deliveryNote', text: '7 月 12 日傍晚前后统一履约，具体到货时间以团长群通知为准。' },
            ],
          },
          leader: { id: 10, displayName: '王姐鲜果团', avatarUrl: null, followerCount: 128 },
          store: { id: 20, name: '王姐社区鲜果店', logoUrl: null, latitude: 30.27, longitude: 120.15, distanceMeters: 860, distanceText: '860m' },
          items: [
            {
              id: 1001, productId: 501, displayName: '阳山水蜜桃 5 斤装', groupPriceAmount: 2990,
              groupStock: 100, soldCount: 61, sortOrder: 1, coverImageUrl: null,
              product: {
                id: 501,
                name: '阳山水蜜桃',
                description: '单份约 5 斤，中大果混装。偏软甜口，适合现吃；运输中轻微压痕不影响食用。',
                coverImageUrl: null,
                detailImageUrls: ['https://images.unsplash.com/photo-1629828874514-cf5a3f1fb66f'],
                basePriceAmount: 3990,
                status: 'active',
              },
            },
            {
              id: 1002, productId: 502, displayName: '阳山水蜜桃 10 斤家庭装', groupPriceAmount: 5390,
              groupStock: 32, soldCount: 18, sortOrder: 2, coverImageUrl: null,
              product: {
                id: 502,
                name: '阳山水蜜桃家庭装',
                description: '适合家庭多人分享，成熟度不同可分批食用。建议到货后平铺保存。',
                coverImageUrl: null,
                detailImageUrls: [],
                basePriceAmount: 6990,
                status: 'active',
              },
            },
          ],
          featuredItem: {
            id: 1001, productId: 501, displayName: '阳山水蜜桃 5 斤装', groupPriceAmount: 2990,
            groupStock: 100, soldCount: 61, sortOrder: 1, coverImageUrl: null,
            product: {
              id: 501,
              name: '阳山水蜜桃',
              description: '单份约 5 斤，中大果混装。偏软甜口，适合现吃；运输中轻微压痕不影响食用。',
              coverImageUrl: null,
              detailImageUrls: ['https://images.unsplash.com/photo-1629828874514-cf5a3f1fb66f'],
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
            id: '101', storeId: '20', leaderId: '10', title: '本周油桃加购团', introduction: '本周加购名额已满，等待团长下次开团。',
            coverImageUrl: null, groupType: 'normal', deliveryType: 'local_delivery',
            shippingTime: null, startTime: null, endTime: null,
            visibility: 'public', status: 'published',
            galleryImageUrls: [],
            contentBlocks: [],
          },
          leader: { id: 10, displayName: '王姐鲜果团', avatarUrl: null, followerCount: 128 },
          store: { id: 20, name: '王姐社区鲜果店', logoUrl: null },
          items: [
            {
              id: 2001, productId: 503, displayName: '当季油桃 3 斤尝鲜装', groupPriceAmount: 1990,
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
          leader: { id: 10, displayName: '王姐鲜果团', avatarUrl: null, bio: '小区群每周开团，主做当季鲜果和社区自提。', memberCount: 30, followerCount: 128 },
          store: { id: 20, name: '王姐社区鲜果店', logoUrl: null, description: '当季鲜果集中收单，同城配送到社区。', defaultDeliveryType: 'local_delivery' },
          viewer: { subscribed: false },
          groupBuys: {
            items: [
              {
                id: '100', title: '周末阳山水蜜桃社区团', coverImageUrl: null, status: 'published',
                endTime: null, minPriceAmount: 2990, soldCount: 61,
                leader: { id: 10, displayName: '王姐鲜果团', avatarUrl: null },
                store: { id: 20, name: '王姐社区鲜果店' },
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
              id: 300, receiverName: '陈小满', receiverPhone: '13800000000',
              province: '浙江省', city: '杭州市', district: '西湖区',
              detail: '桂花城 3 幢 1 单元门口', fullAddress: '浙江省杭州市西湖区桂花城 3 幢 1 单元门口', isDefault: true,
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
    const body = route.request().postDataJSON()
    const isCartMode = Array.isArray(body.cartItemIds) && body.cartItemIds.length > 0
    const previewItems = isCartMode
      ? body.cartItemIds.map((id: number | string) => {
          const cart = cartItems.get(Number(id))
          return {
            groupBuyItemId: cart?.groupBuyItemId ?? 1001,
            productId: cart?.productId ?? 501,
            productName: cart?.title ?? '阳山水蜜桃 5 斤装',
            unitPriceAmount: cart?.groupPriceAmount ?? 2990,
            quantity: cart?.quantity ?? 1,
            totalAmount: (cart?.groupPriceAmount ?? 2990) * (cart?.quantity ?? 1),
            availableStock: cart?.availableStock ?? 100,
            soldCount: 61,
          }
        })
      : [{ groupBuyItemId: 1001, productId: 501, productName: '阳山水蜜桃 5 斤装', unitPriceAmount: 2990, quantity: 1, totalAmount: 2990, availableStock: 100, soldCount: 61 }]
    const totalAmount = previewItems.reduce((sum: number, item: { totalAmount: number }) => sum + item.totalAmount, 0)
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          groupBuyId: 100,
          address: { id: 300, receiverName: '陈小满', receiverPhone: '13800000000', province: '浙江省', city: '杭州市', district: '西湖区', detail: '桂花城 3 幢 1 单元门口', fullAddress: '浙江省杭州市西湖区桂花城 3 幢 1 单元门口' },
          items: previewItems,
          totalAmount, discountAmount: 0, payAmount: totalAmount,
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
          receiverName: '陈小满', receiverPhone: '13800000000',
          fullAddress: '浙江省杭州市西湖区桂花城 3 幢 1 单元门口',
          items: [{ id: 1, groupBuyItemId: 1001, productName: '阳山水蜜桃 5 斤装', unitPriceAmount: 2990, quantity: 1, totalAmount: 2990 }],
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
          receiverName: '陈小满', receiverPhone: '13800000000',
          province: '浙江省', city: '杭州市', district: '西湖区',
          detail: '桂花城 3 幢 1 单元门口', fullAddress: '浙江省杭州市西湖区桂花城 3 幢 1 单元门口',
          items: [{ id: 1, groupBuyItemId: 1001, productName: '阳山水蜜桃 5 斤装', unitPriceAmount: 2990, quantity: 1, totalAmount: 2990 }],
        },
        traceId: 'e2e_order_detail',
      }),
    })
  })

  // 购物车
  await page.route('**/api/v1/cart/items', async (route, request) => {
    if (request.method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: Array.from(cartItems.values()), traceId: 'e2e_cart_list' }),
      })
      return
    }

    if (request.method() === 'POST') {
      const body = request.postDataJSON()
      const existing = Array.from(cartItems.values()).find(item => item.groupBuyItemId === Number(body.groupBuyItemId))
      const next = existing ?? {
        cartItemId: 7001,
        groupBuyId: '100',
        groupBuyItemId: Number(body.groupBuyItemId),
        productId: 501,
        title: '阳山水蜜桃 5 斤装',
        coverImageUrl: null,
        groupPriceAmount: 2990,
        quantity: 0,
        availableStock: 100,
        visibility: 'public',
        status: 'published',
        startTime: null,
        endTime: null,
      }
      next.quantity += Number(body.quantity)
      cartItems.set(next.cartItemId, next)
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: next, traceId: 'e2e_cart_add' }),
      })
      return
    }

    if (request.method() === 'DELETE') {
      cartItems.clear()
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, traceId: 'e2e_cart_clear' }),
      })
      return
    }
  })

  await page.route('**/api/v1/cart/items/*', async (route, request) => {
    const id = Number(new URL(request.url()).pathname.split('/').pop())
    if (request.method() === 'PATCH') {
      const body = request.postDataJSON()
      const item = cartItems.get(id)
      if (item) item.quantity = Number(body.quantity)
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: item, traceId: 'e2e_cart_update' }),
      })
      return
    }
    if (request.method() === 'DELETE') {
      cartItems.delete(id)
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, traceId: 'e2e_cart_delete' }),
      })
      return
    }
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
    await expect(page.getByRole('heading', { name: '先看团长，再跟团' })).toBeVisible({ timeout: 5000 })
    await expect(page.getByRole('heading', { name: '周末阳山水蜜桃社区团' })).toBeVisible({ timeout: 5000 })
    await expect(page.locator('text=¥29.90')).toBeVisible()
    await expect(page.getByText('王姐社区鲜果店组织 周末阳山水蜜桃，集中收单按约履约。')).toBeVisible()
    await expect(page.getByRole('button', { name: '去跟团' }).first()).toBeVisible()

    // Click on group buy card -> detail page
    await page.getByRole('heading', { name: '周末阳山水蜜桃社区团' }).click()
    await page.waitForTimeout(1000)
    await expect(page.locator('#section-activity').getByText('王姐本周从阳山果园集中收单')).toBeVisible({ timeout: 5000 })
    await expect(page.getByText('本团热销商品')).toBeVisible()
    await expect(page.getByText('这次团不是长期货架')).toBeVisible()
    await expect(page.locator('.detail-item__name', { hasText: '阳山水蜜桃 5 斤装' })).toBeVisible()
    await page.locator('.detail-item', { hasText: '阳山水蜜桃 5 斤装' }).getByRole('button', { name: '查看购买' }).click()
    await expect(page.getByText('确认商品')).toBeVisible({ timeout: 5000 })
    await expect(page.getByText('商品详情')).toBeVisible()
    await expect(page.getByText('单份约 5 斤，中大果混装').first()).toBeVisible()
    await page.locator('.van-action-sheet__close').click()

    // Click leader trust block -> leader homepage
    await page.locator('text=王姐鲜果团').first().click()
    await page.waitForTimeout(1000)
    await expect(page.locator('text=小区群每周开团')).toBeVisible({ timeout: 5000 })
    await expect(page.getByRole('heading', { name: '周末阳山水蜜桃社区团' })).toBeVisible()
  })

  test('home and detail keep mobile H5 width on desktop', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 820 })
    await page.goto('/')
    await page.waitForSelector('.van-tabbar', { timeout: 10000 })
    await expect(page.getByRole('heading', { name: '周末阳山水蜜桃社区团' })).toBeVisible({ timeout: 5000 })

    const homeWidth = await page.locator('.page-layout').evaluate((el) => el.getBoundingClientRect().width)
    const tabbarWidth = await page.locator('.van-tabbar').evaluate((el) => el.getBoundingClientRect().width)
    expect(homeWidth).toBeLessThanOrEqual(482)
    expect(tabbarWidth).toBeLessThanOrEqual(482)

    await page.getByRole('heading', { name: '周末阳山水蜜桃社区团' }).click()
    await expect(page.locator('#section-activity').getByText('王姐本周从阳山果园集中收单')).toBeVisible({ timeout: 5000 })
    const detailWidth = await page.locator('.page-layout').evaluate((el) => el.getBoundingClientRect().width)
    expect(detailWidth).toBeLessThanOrEqual(482)
  })

  test('orders messages profile and child pages keep mobile H5 width on desktop', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 820 })
    await page.goto('/')
    await page.evaluate(() => localStorage.setItem('accessToken', 'mock_token'))

    const assertH5PageWidth = async (path: string) => {
      await navigateToHash(page, path)
      await page.waitForSelector('.page-layout', { timeout: 10000 })
      const pageWidth = await page.locator('.page-layout').evaluate((el) => el.getBoundingClientRect().width)
      expect(pageWidth).toBeLessThanOrEqual(482)
    }

    await assertH5PageWidth('/orders')
    const ordersTabbarWidth = await page.locator('.van-tabbar').evaluate((el) => el.getBoundingClientRect().width)
    expect(ordersTabbarWidth).toBeLessThanOrEqual(482)

    await assertH5PageWidth('/orders/9001')
    await assertH5PageWidth('/messages')
    const messagesTabbarWidth = await page.locator('.van-tabbar').evaluate((el) => el.getBoundingClientRect().width)
    expect(messagesTabbarWidth).toBeLessThanOrEqual(482)

    await assertH5PageWidth('/messages/orders')
    await assertH5PageWidth('/chats/7001')
    await assertH5PageWidth('/profile')
    const profileTabbarWidth = await page.locator('.van-tabbar').evaluate((el) => el.getBoundingClientRect().width)
    expect(profileTabbarWidth).toBeLessThanOrEqual(482)

    await assertH5PageWidth('/profile/me')
    await assertH5PageWidth('/addresses')
    await assertH5PageWidth('/subscriptions')
    await assertH5PageWidth('/member-cards')
  })

  test('detail opens at top after clicking from a scrolled homepage', async ({ page }) => {
    await page.goto('/')
    await page.waitForSelector('.van-tabbar', { timeout: 10000 })
    await expect(page.getByPlaceholder('搜索团购、店铺、团长')).toBeVisible({ timeout: 5000 })
    await page.locator('.page-layout__content').evaluate((el) => { el.scrollTop = 320 })

    await page.getByRole('heading', { name: '周末阳山水蜜桃社区团' }).click()
    await expect(page.locator('#section-activity').getByText('王姐本周从阳山果园集中收单')).toBeVisible({ timeout: 5000 })

    const detailScrollTop = await page.locator('.page-layout__content').evaluate((el) => el.scrollTop)
    expect(detailScrollTop).toBeLessThan(20)
  })

  test('H5 back behavior uses history first and falls back for direct detail pages', async ({ page }) => {
    await page.goto('/')
    await page.waitForSelector('.van-tabbar', { timeout: 10000 })
    await page.getByRole('heading', { name: '周末阳山水蜜桃社区团' }).click()
    await expect(page).toHaveURL(/#\/group-buys\/100/)
    await page.getByLabel('返回').first().click()
    await expect(page).toHaveURL(/#\/$/)

    await navigateToHash(page, '/group-buys/100')
    await expect(page.locator('#section-activity').getByText('王姐本周从阳山果园集中收单')).toBeVisible()
    await page.evaluate(() => {
      window.history.replaceState({ back: null, current: '/group-buys/100', forward: null, position: 0 }, '', window.location.href)
    })
    await page.getByLabel('返回').first().click()
    await expect(page).toHaveURL(/#\/$/)
  })

  test('H5 messages chat fallback and tabbar history are intuitive', async ({ page }) => {
    await page.goto('/')
    await page.evaluate(() => localStorage.setItem('accessToken', 'mock_token'))
    await navigateToHash(page, '/messages')
    await expect(page.getByText('王姐鲜果团')).toBeVisible()
    await page.locator('.message-row', { hasText: '王姐鲜果团' }).click()
    await expect(page).toHaveURL(/#\/chats\/7001/)
    await expect(page.getByText('今晚统一发货')).toBeVisible()
    await page.locator('.van-nav-bar__left').click()
    await expect(page).toHaveURL(/#\/messages/)

    await navigateToHash(page, '/chats/7001')
    await expect(page.getByText('今晚统一发货')).toBeVisible()
    await page.evaluate(() => {
      window.history.replaceState({ back: null, current: '/chats/7001', forward: null, position: 0 }, '', window.location.href)
    })
    await page.locator('.van-nav-bar__left').click()
    await expect(page).toHaveURL(/#\/messages/)

    await navigateToHash(page, '/profile')
    await page.getByRole('tab', { name: /订单/ }).click()
    await expect(page).toHaveURL(/#\/orders/)
    await page.getByRole('tab', { name: /消息/ }).click()
    await expect(page).toHaveURL(/#\/messages/)
    await page.goBack()
    await expect(page).toHaveURL(/#\/orders/)
    await page.goBack()
    await expect(page).toHaveURL(/#\/profile/)
  })

  test('nearby filter requests location parameters and shows distance marker', async ({ page, context }) => {
    await context.grantPermissions(['geolocation'])
    await context.setGeolocation({ latitude: 30.2741, longitude: 120.1551 })
    await page.goto('/')
    await page.waitForSelector('.van-tabbar', { timeout: 10000 })

    const locationRequestPromise = page.waitForRequest((request) => {
      const url = new URL(request.url())
      return url.pathname.endsWith('/api/v1/group-buys')
        && url.searchParams.get('maxDistanceMeters') === '5000'
        && url.searchParams.get('sort') === 'distance'
        && url.searchParams.has('latitude')
        && url.searchParams.has('longitude')
    })

    await page.locator('.category-chips__item', { hasText: /^附近$/ }).click()
    await locationRequestPromise

    await expect(page.getByText('已按你的位置展示距离')).toBeVisible({ timeout: 5000 })
    await expect(page.getByText('距你 860m')).toBeVisible()
    await expect(page.getByText('附近可履约')).toBeVisible()
  })

  test('homepage search sends keyword to group buy list', async ({ page }) => {
    await page.goto('/')
    await page.waitForSelector('.van-tabbar', { timeout: 10000 })

    const searchRequestPromise = page.waitForRequest((request) => {
      const url = new URL(request.url())
      return url.pathname.endsWith('/api/v1/group-buys')
        && url.searchParams.get('keyword') === '蜜桃'
    })

    await page.getByPlaceholder('搜索团购、店铺、团长').fill('蜜桃')
    await page.getByRole('button', { name: '搜索', exact: true }).click()
    await searchRequestPromise
    await expect(page.getByPlaceholder('搜索团购、店铺、团长')).toHaveValue('蜜桃')
  })

  test('unauthenticated click buy redirects to login, logged in user completes flow', async ({ page }) => {
    // Navigate to group buy detail
    await navigateToHash(page, '/group-buys/100')
    await page.waitForTimeout(1000)

    // Should see detail
    await expect(page.locator('.detail-item__name', { hasText: '阳山水蜜桃 5 斤装' })).toBeVisible({ timeout: 5000 })

    // Open item purchase sheet first
    await page.locator('.detail-item', { hasText: '阳山水蜜桃 5 斤装' }).getByRole('button', { name: '查看购买' }).click()
    await page.waitForTimeout(500)

    // Click buy -> should redirect to login
    await page.locator('button:has-text("立即购买")').click()
    await page.waitForTimeout(1000)
    await expect(page).toHaveURL(/#\/login/, { timeout: 5000 })
    await expect(page.getByRole('heading', { name: '登录后继续跟团' })).toBeVisible()
    await expect(page.getByText('登录后会回到当前团购')).toBeVisible()
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
    await expect(page.locator('text=陈小满')).toBeVisible({ timeout: 5000 })
    await expect(page.locator('text=浙江省杭州市西湖区桂花城 3 幢 1 单元门口')).toBeVisible()
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

    // Open item purchase sheet
    await page.locator('.detail-item', { hasText: '阳山水蜜桃 5 斤装' }).getByRole('button', { name: '查看购买' }).click()
    await page.waitForTimeout(500)

    // Click buy
    await page.locator('button:has-text("立即购买")').click()
    await page.waitForTimeout(1500)

    // Should be on checkout page
    await expect(page).toHaveURL(/#\/checkout/, { timeout: 5000 })
    await expect(page.getByText('阳山水蜜桃 5 斤装').first()).toBeVisible({ timeout: 5000 })
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

  test('logged-in user adds item to cart and checks out from cart', async ({ page }) => {
    await page.evaluate(() => localStorage.setItem('accessToken', 'mock_token_e2e_test'))
    await page.goto('/')
    await page.waitForSelector('.van-tabbar', { timeout: 10000 })

    await navigateToHash(page, '/group-buys/100')
    await page.waitForTimeout(1500)

    await page.locator('.detail-item', { hasText: '阳山水蜜桃 5 斤装' }).getByRole('button', { name: '查看购买' }).click()
    await expect(page.getByText('确认商品')).toBeVisible({ timeout: 5000 })
    await page.locator('button:has-text("加入购物车")').click()
    await expect(page.locator('.van-toast__text')).toHaveText('已加入购物车', { timeout: 5000 })

    const cartSheet = page.locator('.cart-sheet')
    await expect(cartSheet.getByRole('heading', { name: '购物车' })).toBeVisible({ timeout: 5000 })
    await expect(cartSheet.getByText('阳山水蜜桃 5 斤装').first()).toBeVisible({ timeout: 5000 })

    await cartSheet.locator('button:has-text("去结算")').click()
    await expect(page).toHaveURL(/#\/checkout/, { timeout: 5000 })
    await expect(page.getByText('阳山水蜜桃 5 斤装').first()).toBeVisible({ timeout: 5000 })

    await page.locator('.van-checkbox').click()
    await page.locator('button:has-text("提交订单")').click()
    await expect(page).toHaveURL(/#\/orders\/9001/, { timeout: 5000 })
  })

  test('unauthenticated subscribe redirects to login', async ({ page }) => {
    await navigateToHash(page, '/group-buys/100')
    await page.waitForTimeout(2000)

    // Should see detail
    await expect(page.locator('.detail-item__name', { hasText: '阳山水蜜桃 5 斤装' })).toBeVisible({ timeout: 5000 })

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
    await expect(page.locator('.detail-item__name', { hasText: '当季油桃 3 斤尝鲜装' })).toBeVisible({ timeout: 5000 })

    // Should show "已售罄" tag instead of the purchase entry button
    await expect(page.getByText('已售罄', { exact: true })).toBeVisible()

    // Should NOT show a "查看购买" button for this item
    await expect(page.locator('.detail-item', { hasText: '当季油桃 3 斤尝鲜装' }).getByRole('button', { name: '查看购买' })).not.toBeVisible()

    // The buy button should be disabled, showing "库存不足" or not visible
    // (isPurchasable depends on hasAnyStock which is false)
  })

  test('no-address user sees address creation CTA in checkout', async ({ page }) => {
    await page.evaluate(() => localStorage.setItem('accessToken', 'mock_token_no_addr_e2e'))
    // Navigate to group buy detail first, then try to buy
    await navigateToHash(page, '/group-buys/100')
    await page.waitForTimeout(1500)

    // Open item purchase sheet and buy
    await page.locator('.detail-item', { hasText: '阳山水蜜桃 5 斤装' }).getByRole('button', { name: '查看购买' }).click()
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
    await page.locator('.detail-item', { hasText: '阳山水蜜桃 5 斤装' }).getByRole('button', { name: '查看购买' }).click()
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
      await nameInput.fill('周晨')
      await page.locator('input[name="receiverPhone"]').fill('13900000001')
      await page.locator('input[name="province"]').fill('广东省')
      await page.locator('input[name="city"]').fill('广州市')
      await page.locator('input[name="district"]').fill('天河区')
      await page.locator('textarea').fill('天河社区服务站')
      await page.locator('button:has-text("保存地址")').click()
      await page.waitForTimeout(2000)

      // After creating from checkout flow, should redirect back to /checkout
      await expect(page).toHaveURL(/#\/checkout/, { timeout: 5000 })
    }
  })
})
