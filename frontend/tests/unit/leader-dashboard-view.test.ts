import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'
import LeaderDashboardView from '@/views/leader/LeaderDashboardView.vue'
import { getStoreWorkbenchSummary } from '@/api/stores'

vi.mock('@/api/stores', () => ({ getStoreWorkbenchSummary: vi.fn() }))

function createTestRouter() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [
      { path: '/leader/dashboard', component: LeaderDashboardView },
      { path: '/leader/products', component: { template: '<div>商品</div>' } },
      { path: '/leader/orders', component: { template: '<div>订单</div>' } },
      { path: '/leaders/:id', component: { template: '<div>团长主页</div>' } },
    ],
  })
}

describe('LeaderDashboardView', () => {
  beforeEach(() => {
    vi.mocked(getStoreWorkbenchSummary).mockReset()
    vi.mocked(getStoreWorkbenchSummary).mockResolvedValue({
      leader: { id: '10', displayName: '王姐', avatarUrl: null },
      store: {
        id: '20',
        name: '王姐鲜果团',
        logoUrl: null,
        status: 'active',
      },
      todos: {
        paidOrders: 3,
        pendingAfterSales: 1,
        unreadLeaderChats: 3,
        publishedGroupBuys: 4,
      },
      statusCounts: {
        orders: { paid: 3, shipped: 0, completed: 0, afterSale: 0, canceled: 0 },
        afterSales: { pending: 1, approved: 0, rejected: 0, completed: 0 },
        groupBuys: { draft: 0, published: 4, ended: 0 },
      },
    })
  })

  it('renders store summary, todo counts, and navigates from quick entry', async () => {
    const router = createTestRouter()
    await router.push('/leader/dashboard')
    await router.isReady()

    const wrapper = mount(LeaderDashboardView, {
      global: { plugins: [router] },
    })
    await flushPromises()

    expect(getStoreWorkbenchSummary).toHaveBeenCalled()
    expect(wrapper.text()).toContain('王姐鲜果团')
    expect(wrapper.text()).toContain('待发货订单')
    expect(wrapper.text()).toContain('3')
    expect(wrapper.text()).toContain('售后待处理')
    expect(wrapper.text()).toContain('1')
    expect(wrapper.text()).toContain('未读客服消息')

    await wrapper.findAll('button').find((button) => button.text().includes('商品'))?.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/leader/products')
  })
})
