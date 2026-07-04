import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'
import LeaderCouponsView from '@/views/leader/LeaderCouponsView.vue'
import {
  createStoreCoupon,
  disableStoreCoupon,
  listStoreCoupons,
  updateStoreCoupon,
} from '@/api/coupons'

const popupStub = {
  props: ['show'],
  template: '<div v-if="show"><slot /></div>',
}

const globalStubs = {
  VanPopup: popupStub,
  'van-popup': popupStub,
  VanNavBar: {
    props: ['title'],
    template: '<header><button type="button" @click="$emit(\'click-left\')">返回</button><span>{{ title }}</span></header>',
  },
  'van-nav-bar': {
    props: ['title'],
    template: '<header><button type="button" @click="$emit(\'click-left\')">返回</button><span>{{ title }}</span></header>',
  },
}

vi.mock('@/api/coupons', () => ({
  listStoreCoupons: vi.fn(),
  createStoreCoupon: vi.fn(),
  updateStoreCoupon: vi.fn(),
  disableStoreCoupon: vi.fn(),
}))

vi.mock('vant', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vant')>()
  return {
    ...actual,
    showToast: vi.fn(),
    showConfirmDialog: vi.fn(() => Promise.resolve()),
  }
})

function createTestRouter() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [
      {
        path: '/leader/coupons',
        name: 'leaderCoupons',
        component: LeaderCouponsView,
      },
    ],
  })
}

describe('LeaderCouponsView', () => {
  beforeEach(() => {
    vi.mocked(listStoreCoupons).mockReset()
    vi.mocked(createStoreCoupon).mockReset()
    vi.mocked(updateStoreCoupon).mockReset()
    vi.mocked(disableStoreCoupon).mockReset()
    vi.mocked(listStoreCoupons).mockResolvedValue([
      {
        id: '1',
        storeId: '10',
        name: '新客订阅立减券',
        couponType: 'amount',
        claimCondition: 'new_subscriber',
        amount: 1000,
        thresholdAmount: 0,
        totalQuantity: 100,
        claimedQuantity: 3,
        perUserLimit: 1,
        startTime: '2026-07-04T10:00:00',
        endTime: '2026-08-04T10:00:00',
        status: 'active',
      },
    ])
  })

  it('renders store coupons and disables an active coupon', async () => {
    const router = createTestRouter()
    await router.push('/leader/coupons')
    await router.isReady()

    const wrapper = mount(LeaderCouponsView, {
      global: {
        plugins: [router],
        stubs: globalStubs,
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('新客订阅立减券')
    expect(wrapper.text()).toContain('已领 3/100')

    await wrapper.findAll('button').find((button) => button.text() === '停用')?.trigger('click')
    await flushPromises()

    expect(disableStoreCoupon).toHaveBeenCalledWith('1')
  })

  it('creates a new subscriber coupon from the form', async () => {
    vi.mocked(createStoreCoupon).mockResolvedValue({
      id: '2',
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
    })
    const router = createTestRouter()
    await router.push('/leader/coupons')
    await router.isReady()

    const wrapper = mount(LeaderCouponsView, {
      global: {
        plugins: [router],
        stubs: globalStubs,
      },
    })
    await flushPromises()

    await wrapper.findAll('button').find((button) => button.text() === '新建新人券')?.trigger('click')
    await flushPromises()

    await wrapper.findAll('input')[0].setValue('新人券')
    await wrapper.findAll('input')[1].setValue('5')
    await wrapper.findAll('button').find((button) => button.text() === '保存')?.trigger('click')
    await flushPromises()

    expect(createStoreCoupon).toHaveBeenCalledWith(expect.objectContaining({
      name: '新人券',
      couponType: 'amount',
      claimCondition: 'new_subscriber',
      amount: 500,
      perUserLimit: 1,
    }))
  })

  it('marks legacy general coupons as not shown on homepage and can convert them', async () => {
    vi.mocked(listStoreCoupons).mockResolvedValue([
      {
        id: '9',
        storeId: '10',
        name: '社区团员 7 元满减券',
        couponType: 'amount',
        claimCondition: 'general',
        amount: 700,
        thresholdAmount: 2990,
        totalQuantity: 200,
        claimedQuantity: 36,
        perUserLimit: 1,
        startTime: '2026-07-04T10:00:00',
        endTime: '2026-08-04T10:00:00',
        status: 'active',
      },
    ])
    const router = createTestRouter()
    await router.push('/leader/coupons')
    await router.isReady()

    const wrapper = mount(LeaderCouponsView, {
      global: {
        plugins: [router],
        stubs: globalStubs,
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('0 张会在主页弹出')
    expect(wrapper.text()).toContain('普通券')
    expect(wrapper.text()).toContain('不会在团长主页弹出')

    await wrapper.findAll('button').find((button) => button.text() === '转新人券')?.trigger('click')
    await flushPromises()

    expect(updateStoreCoupon).toHaveBeenCalledWith('9', { claimCondition: 'new_subscriber' })
  })
})
