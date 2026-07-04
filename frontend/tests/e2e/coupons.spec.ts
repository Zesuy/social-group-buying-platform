import { test, expect, type Page } from '@playwright/test'

async function mockQuietEndpoints(page: Page) {
  await page.route('**/api/v1/my/notifications/unread-count', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: { unreadCount: 0 }, traceId: 'e2e_notice_count' }),
    })
  })
  await page.route('**/api/v1/my/chat-conversations/unread-count', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: { unreadCount: 0 }, traceId: 'e2e_chat_count' }),
    })
  })
}

async function mockAuth(page: Page, role: 'leader' | 'buyer') {
  await page.addInitScript((token) => {
    window.localStorage.setItem('accessToken', token)
  }, `${role}_token`)

  await page.route('**/api/v1/me', async (route) => {
    const isLeader = role === 'leader'
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          user: {
            id: 'u1',
            nickname: isLeader ? '李团长' : '买家用户',
            avatarUrl: null,
            phone: '13800000000',
            hasLeader: isLeader,
            leaderId: isLeader ? '1' : null,
            storeId: isLeader ? '10' : null,
          },
          leader: isLeader ? { id: '1', displayName: '李团长', avatarUrl: null } : null,
          store: isLeader ? { id: '10', name: '邻里鲜果', logoUrl: null, status: 'active' } : null,
        },
        traceId: 'e2e_me',
      }),
    })
  })
}

test('leader creates a new subscriber coupon', async ({ page }) => {
  await mockQuietEndpoints(page)
  await mockAuth(page, 'leader')

  let createdPayload: unknown = null
  await page.route('**/api/v1/my/store/coupons', async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [], traceId: 'e2e_coupon_list' }),
      })
      return
    }

    createdPayload = route.request().postDataJSON()
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          id: 'c1',
          storeId: '10',
          name: '新人券',
          couponType: 'amount',
          claimCondition: 'new_subscriber',
          amount: 500,
          thresholdAmount: 0,
          totalQuantity: 20,
          claimedQuantity: 0,
          perUserLimit: 1,
          startTime: '2026-07-04T10:00:00',
          endTime: '2026-08-04T10:00:00',
          status: 'active',
        },
        traceId: 'e2e_coupon_create',
      }),
    })
  })

  await page.goto('/#/leader/coupons')
  await expect(page.getByText('店铺优惠券')).toBeVisible()
  await page.getByRole('button', { name: '新建新人券', exact: true }).first().click()
  await page.getByPlaceholder('例如：新客立减 10 元').fill('新人券')
  await page.getByLabel('抵扣金额').fill('5')
  await page.getByLabel('总库存').fill('20')
  await page.getByRole('button', { name: '保存' }).click()

  await expect.poll(() => createdPayload).not.toBeNull()
  expect(createdPayload).toMatchObject({
    name: '新人券',
    couponType: 'amount',
    claimCondition: 'new_subscriber',
    amount: 500,
    totalQuantity: 20,
    perUserLimit: 1,
  })
})

test('buyer subscribes before manually claiming homepage coupon', async ({ page }) => {
  await mockQuietEndpoints(page)
  await mockAuth(page, 'buyer')

  let subscribed = false
  let claimed = false

  await page.route('**/api/v1/leaders/1/homepage*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          leader: { id: '1', displayName: '李团长', avatarUrl: null, bio: null, memberCount: 8, followerCount: 12 },
          store: {
            id: '10',
            name: '邻里鲜果',
            logoUrl: null,
            description: '今日鲜果',
            defaultDeliveryType: 'express',
            latitude: null,
            longitude: null,
            distanceMeters: null,
            distanceText: null,
          },
          viewer: { subscribed },
          groupBuys: { items: [], page: 1, pageSize: 20, total: 0, hasMore: false },
        },
        traceId: 'e2e_leader_home',
      }),
    })
  })

  await page.route('**/api/v1/leaders/1/coupons*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: [
          {
            id: 'c1',
            name: '新客订阅立减券',
            couponType: 'amount',
            claimCondition: 'new_subscriber',
            amount: 1000,
            thresholdAmount: 0,
            totalQuantity: 100,
            claimedQuantity: claimed ? 1 : 0,
            perUserLimit: 1,
            startTime: '2026-07-04T10:00:00',
            endTime: '2026-08-04T10:00:00',
            status: 'active',
            claimable: subscribed && !claimed,
            claimed,
            viewerSubscribed: subscribed,
            unavailableReason: claimed ? '已领取' : (subscribed ? null : '订阅店铺后可领取'),
          },
        ],
        traceId: 'e2e_home_coupons',
      }),
    })
  })

  await page.route('**/api/v1/leaders/1/subscription', async (route) => {
    subscribed = true
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: { id: 's1', userId: 'u1', leaderId: '1', storeId: '10', status: 'active', source: 'homepageCoupon' },
        traceId: 'e2e_subscribe',
      }),
    })
  })

  await page.route('**/api/v1/coupons/c1/claim', async (route) => {
    claimed = true
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: { id: 'uc1', couponId: 'c1', couponName: '新客订阅立减券', couponType: 'amount', amount: 1000, thresholdAmount: 0, status: 'unused' },
        traceId: 'e2e_claim',
      }),
    })
  })

  await page.goto('/#/leaders/1')
  await expect(page.getByText('订阅后领取店铺券')).toBeVisible()
  await page.getByRole('button', { name: '订阅领券' }).click()
  await expect(page.getByRole('button', { name: '立即领取' })).toBeVisible()
  await page.getByRole('button', { name: '立即领取' }).click()
  await expect(page.getByRole('button', { name: '已领取', exact: true })).toBeVisible()
})
