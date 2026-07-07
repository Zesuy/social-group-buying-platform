import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'
import MerchantDashboardView from '@/views/merchant/MerchantDashboardView.vue'
import { listChatConversations } from '@/api/chats'
import { listLeaderAfterSales } from '@/api/leaderAfterSales'
import { listLeaderOrdersByParams } from '@/api/leaderOrders'
import { getStoreWorkbenchSummary } from '@/api/stores'

vi.mock('@/api/stores', () => ({ getStoreWorkbenchSummary: vi.fn() }))
vi.mock('@/api/leaderOrders', () => ({ listLeaderOrdersByParams: vi.fn() }))
vi.mock('@/api/leaderAfterSales', () => ({ listLeaderAfterSales: vi.fn() }))
vi.mock('@/api/chats', () => ({ listChatConversations: vi.fn() }))

function page<T>(items: T[], total = items.length) {
  return { items, page: 1, pageSize: 20, total, hasMore: false }
}

function createTestRouter() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [
      { path: '/merchant/dashboard', component: MerchantDashboardView },
      { path: '/merchant/orders', component: { template: '<div>订单</div>' } },
      { path: '/merchant/after-sales', component: { template: '<div>售后</div>' } },
      { path: '/merchant/chats', component: { template: '<div>客服</div>' } },
      { path: '/merchant/group-buys', component: { template: '<div>团购</div>' } },
      { path: '/merchant/orders/:id', component: { template: '<div>订单详情</div>' } },
      { path: '/merchant/after-sales/:id', component: { template: '<div>售后详情</div>' } },
      { path: '/chats/:id', component: { template: '<div>聊天</div>' } },
    ],
  })
}

describe('MerchantDashboardView', () => {
  beforeEach(() => {
    vi.mocked(getStoreWorkbenchSummary).mockReset()
    vi.mocked(listLeaderOrdersByParams).mockReset()
    vi.mocked(listLeaderAfterSales).mockReset()
    vi.mocked(listChatConversations).mockReset()

    vi.mocked(getStoreWorkbenchSummary).mockResolvedValue({
      store: { id: '20', name: '王姐鲜果团', logoUrl: null, status: 'active' },
      leader: { id: '10', displayName: '王姐', avatarUrl: null },
      todos: { paidOrders: 3, pendingAfterSales: 1, unreadLeaderChats: 4, publishedGroupBuys: 5 },
      statusCounts: {
        orders: { paid: 3, shipped: 0, completed: 0, afterSale: 0, canceled: 0 },
        afterSales: { pending: 1, approved: 0, rejected: 0, completed: 0 },
        groupBuys: { draft: 0, published: 5, ended: 0 },
      },
    })
    vi.mocked(listLeaderOrdersByParams).mockResolvedValue(page([
      {
        id: 'o1',
        orderNo: 'NO-1',
        groupBuyId: 'g1',
        userId: 'u1',
        storeId: '20',
        leaderId: '10',
        totalAmount: 1990,
        discountAmount: 0,
        payAmount: 1990,
        payStatus: 'paid',
        orderStatus: 'paid',
        paidAt: '2026-07-06T10:00:00',
        shippedAt: null,
        completedAt: null,
        remark: null,
        receiverName: '小李',
        receiverPhone: '13800000000',
        province: '广东省',
        city: '深圳市',
        district: '南山区',
        detail: '科技园',
        fullAddress: '广东省深圳市南山区科技园',
        items: [{ id: 'i1', groupBuyItemId: 'gi1', productId: 'p1', productName: '鲜果', skuName: null, unitPriceAmount: 1990, quantity: 1, totalAmount: 1990 }],
      },
    ]))
    vi.mocked(listLeaderAfterSales).mockResolvedValue(page([
      {
        id: 'a1',
        orderId: 'o1',
        orderNo: 'NO-1',
        userId: 'u1',
        leaderId: '10',
        storeId: '20',
        type: 'refund',
        reason: '质量问题',
        status: 'pending',
        amount: 1990,
        originalOrderStatus: 'paid',
        orderStatus: 'after_sale',
        payStatus: 'paid',
        createdAt: '2026-07-06T10:00:00',
      },
    ]))
    vi.mocked(listChatConversations).mockResolvedValue(page([
      {
        id: 'c1',
        buyerUserId: 'u1',
        leaderUserId: 'u2',
        storeId: '20',
        buyerName: '小李',
        leaderName: '王姐',
        storeName: '王姐鲜果团',
        currentUserRole: 'leader',
        unreadCount: 2,
        lastMessageText: '什么时候发货',
        lastMessageAt: '2026-07-06T10:00:00',
        createdAt: '2026-07-06T10:00:00',
      },
    ]))
  })

  it('renders todo metrics and recent lists', async () => {
    const router = createTestRouter()
    await router.push('/merchant/dashboard')
    await router.isReady()

    const wrapper = mount(MerchantDashboardView, {
      global: { plugins: [router] },
    })
    await flushPromises()

    expect(getStoreWorkbenchSummary).toHaveBeenCalled()
    expect(listLeaderOrdersByParams).toHaveBeenCalledWith({ status: 'paid', page: 1, pageSize: 5 })
    expect(listLeaderAfterSales).toHaveBeenCalledWith({ status: 'pending', page: 1, pageSize: 5 })
    expect(listChatConversations).toHaveBeenCalledWith({ role: 'leader', page: 1, pageSize: 5 })
    expect(wrapper.text()).toContain('待发货订单')
    expect(wrapper.text()).toContain('3')
    expect(wrapper.text()).toContain('NO-1')
    expect(wrapper.text()).toContain('什么时候发货')
  })
})
