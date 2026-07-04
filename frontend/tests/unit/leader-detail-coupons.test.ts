import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import LeaderDetailView from '@/views/LeaderDetailView.vue'
import { useAuthStore } from '@/stores'
import { getLeaderHomepage, subscribeLeader, unsubscribeLeader } from '@/api/leaders'
import { claimCoupon, listLeaderHomepageCoupons } from '@/api/coupons'
import { getMyStore } from '@/api/stores'

let pinia: ReturnType<typeof createPinia>

vi.mock('@/api/leaders', () => ({
  getLeaderHomepage: vi.fn(),
  subscribeLeader: vi.fn(),
  unsubscribeLeader: vi.fn(),
}))

vi.mock('@/api/coupons', () => ({
  claimCoupon: vi.fn(),
  listLeaderHomepageCoupons: vi.fn(),
}))

vi.mock('@/api/stores', () => ({
  getMyStore: vi.fn(),
}))

vi.mock('vant', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vant')>()
  return {
    ...actual,
    showToast: vi.fn(),
  }
})

function createTestRouter() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [
      {
        path: '/leaders/:id',
        name: 'leaderDetail',
        component: LeaderDetailView,
      },
      {
        path: '/login',
        name: 'login',
        component: { template: '<div />' },
      },
    ],
  })
}

function homepageResponse(subscribed = false) {
  return {
    leader: {
      id: '1',
      displayName: '李团长',
      avatarUrl: null,
      bio: null,
      memberCount: 8,
      followerCount: 12,
    },
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
  }
}

describe('LeaderDetailView coupon offers', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    pinia = createPinia()
    setActivePinia(pinia)
    const authStore = useAuthStore()
    authStore.accessToken = 'token'
    authStore.user = {
      id: 'u1',
      nickname: '买家',
      avatarUrl: null,
      phone: '13800000001',
      hasLeader: false,
      leaderId: null,
      storeId: null,
    }
    authStore.leader = null
    authStore.store = null

    vi.mocked(getLeaderHomepage).mockReset()
    vi.mocked(subscribeLeader).mockReset()
    vi.mocked(unsubscribeLeader).mockReset()
    vi.mocked(claimCoupon).mockReset()
    vi.mocked(listLeaderHomepageCoupons).mockReset()
    vi.mocked(getMyStore).mockReset()

    vi.mocked(getLeaderHomepage).mockResolvedValue(homepageResponse(false))
    vi.mocked(subscribeLeader).mockResolvedValue({
      id: 's1',
      userId: 'u1',
      leaderId: '1',
      storeId: '10',
      status: 'active',
      source: 'homepageCoupon',
    })
    vi.mocked(claimCoupon).mockResolvedValue({
      id: 'uc1',
      couponId: 'c1',
      couponName: '新客订阅立减券',
      couponType: 'amount',
      amount: 1000,
      thresholdAmount: 0,
      status: 'unused',
    })
    vi.mocked(listLeaderHomepageCoupons)
      .mockResolvedValueOnce([
        {
          id: 'c1',
          name: '新客订阅立减券',
          couponType: 'amount',
          claimCondition: 'new_subscriber',
          amount: 1000,
          thresholdAmount: 0,
          startTime: '2026-07-04T10:00:00',
          endTime: '2026-08-04T10:00:00',
          totalQuantity: 100,
          claimedQuantity: 0,
          perUserLimit: 1,
          status: 'active',
          claimable: false,
          claimed: false,
          viewerSubscribed: false,
          unavailableReason: '订阅店铺后可领取',
        },
      ])
      .mockResolvedValueOnce([
        {
          id: 'c1',
          name: '新客订阅立减券',
          couponType: 'amount',
          claimCondition: 'new_subscriber',
          amount: 1000,
          thresholdAmount: 0,
          startTime: '2026-07-04T10:00:00',
          endTime: '2026-08-04T10:00:00',
          totalQuantity: 100,
          claimedQuantity: 0,
          perUserLimit: 1,
          status: 'active',
          claimable: true,
          claimed: false,
          viewerSubscribed: true,
          unavailableReason: null,
        },
      ])
      .mockResolvedValueOnce([
        {
          id: 'c1',
          name: '新客订阅立减券',
          couponType: 'amount',
          claimCondition: 'new_subscriber',
          amount: 1000,
          thresholdAmount: 0,
          startTime: '2026-07-04T10:00:00',
          endTime: '2026-08-04T10:00:00',
          totalQuantity: 100,
          claimedQuantity: 1,
          perUserLimit: 1,
          status: 'active',
          claimable: false,
          claimed: true,
          viewerSubscribed: true,
          unavailableReason: '已领取',
        },
      ])
  })

  it('subscribes first and only claims after the second tap', async () => {
    const router = createTestRouter()
    await router.push('/leaders/1')
    await router.isReady()

    const wrapper = mount(LeaderDetailView, {
      global: { plugins: [router, pinia] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('订阅后领取店铺券')
    const subscribeButton = wrapper.findAll('button').find((button) => button.text() === '订阅领券')
    await subscribeButton?.trigger('click')
    await flushPromises()

    expect(subscribeLeader).toHaveBeenCalledWith('1', 'homepageCoupon')
    expect(claimCoupon).not.toHaveBeenCalled()

    const claimButton = wrapper.findAll('button').find((button) => button.text() === '立即领取')
    await claimButton?.trigger('click')
    await flushPromises()

    expect(claimCoupon).toHaveBeenCalledWith('c1')
    expect(wrapper.text()).toContain('已领取')
  })
})
