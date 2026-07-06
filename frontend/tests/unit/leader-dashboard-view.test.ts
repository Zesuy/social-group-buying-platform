import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'
import LeaderDashboardView from '@/views/leader/LeaderDashboardView.vue'
import { listChatConversations } from '@/api/chats'
import { listMyGroupBuys } from '@/api/leaderGroupBuys'
import { listLeaderAfterSales } from '@/api/leaderAfterSales'
import { listLeaderOrders } from '@/api/leaderOrders'
import { getMyStore } from '@/api/stores'
import type { AfterSaleData, ChatConversationData } from '@/types'

vi.mock('@/api/stores', () => ({ getMyStore: vi.fn() }))
vi.mock('@/api/leaderOrders', () => ({ listLeaderOrders: vi.fn() }))
vi.mock('@/api/leaderAfterSales', () => ({ listLeaderAfterSales: vi.fn() }))
vi.mock('@/api/chats', () => ({ listChatConversations: vi.fn() }))
vi.mock('@/api/leaderGroupBuys', () => ({ listMyGroupBuys: vi.fn() }))

function page<T>(items: T[], total = items.length) {
  return { items, page: 1, pageSize: 20, total, hasMore: false }
}

function afterSale(id: string, status: string): AfterSaleData {
  return {
    id,
    orderId: `o-${id}`,
    orderNo: `NO-${id}`,
    userId: '1',
    leaderId: '10',
    storeId: '20',
    type: 'refund',
    reason: '商品质量问题',
    status,
    amount: 2990,
    originalOrderStatus: 'paid',
    orderStatus: 'afterSale',
    payStatus: 'paid',
    rejectReason: null,
    createdAt: '2026-07-06T10:00:00',
  }
}

function conversation(id: string, unreadCount: number): ChatConversationData {
  return {
    id,
    buyerUserId: '1',
    leaderUserId: '2',
    storeId: '20',
    buyerName: '小李',
    buyerAvatarUrl: null,
    leaderName: '王姐',
    leaderAvatarUrl: null,
    storeName: '王姐鲜果团',
    storeLogoUrl: null,
    currentUserRole: 'leader',
    unreadCount,
    lastMessageId: null,
    lastMessageText: null,
    lastMessageType: null,
    lastMessageAt: null,
    createdAt: '2026-07-06T10:00:00',
  }
}

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
    vi.mocked(getMyStore).mockReset()
    vi.mocked(listLeaderOrders).mockReset()
    vi.mocked(listLeaderAfterSales).mockReset()
    vi.mocked(listChatConversations).mockReset()
    vi.mocked(listMyGroupBuys).mockReset()

    vi.mocked(getMyStore).mockResolvedValue({
      leader: { id: '10', displayName: '王姐', avatarUrl: null },
      store: {
        id: '20',
        leaderId: '10',
        name: '王姐鲜果团',
        logoUrl: null,
        description: null,
        defaultDeliveryType: 'delivery',
        distributionEnabled: false,
        status: 'active',
        latitude: null,
        longitude: null,
      },
    })
    vi.mocked(listLeaderOrders).mockResolvedValue(page([], 3))
    vi.mocked(listLeaderAfterSales).mockResolvedValue(page([
      afterSale('a1', 'pending'),
      afterSale('a2', 'approved'),
    ]))
    vi.mocked(listChatConversations).mockResolvedValue(page([
      conversation('c1', 2),
      conversation('c2', 1),
    ]))
    vi.mocked(listMyGroupBuys).mockResolvedValue(page([], 4))
  })

  it('renders store summary, todo counts, and navigates from quick entry', async () => {
    const router = createTestRouter()
    await router.push('/leader/dashboard')
    await router.isReady()

    const wrapper = mount(LeaderDashboardView, {
      global: { plugins: [router] },
    })
    await flushPromises()

    expect(listLeaderOrders).toHaveBeenCalledWith('paid', 1, 1)
    expect(listLeaderAfterSales).toHaveBeenCalledWith({ page: 1, pageSize: 50 })
    expect(listChatConversations).toHaveBeenCalledWith({ role: 'leader', page: 1, pageSize: 20 })
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
