import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import MessagesView from '@/views/MessagesView.vue'
import { listNotifications } from '@/api/notifications'
import { getChatUnreadCount, listChatConversations } from '@/api/chats'
import type { ChatConversationData, NotificationData } from '@/types'

vi.mock('@/api/notifications', () => ({
  listNotifications: vi.fn(),
  getUnreadCount: vi.fn().mockResolvedValue({ unreadCount: 2 }),
}))

vi.mock('@/api/chats', () => ({
  listChatConversations: vi.fn(),
  getChatUnreadCount: vi.fn().mockResolvedValue({ unreadCount: 1 }),
}))

vi.mock('vant', async () => {
  const actual = await vi.importActual<typeof import('vant')>('vant')
  return {
    ...actual,
    showToast: vi.fn(),
  }
})

const unreadNotification: NotificationData = {
  id: '9001',
  type: 'order_shipped',
  title: '发货通知',
  summary: '团长已填写物流：顺丰速运 SF1234567890。',
  body: null,
  targetType: 'order',
  targetId: '3001',
  actionUrl: '/orders/3001',
  readStatus: 'unread',
  readAt: null,
  createdAt: '2026-07-03T10:00:00',
}

const chatConversation: ChatConversationData = {
  id: '7001',
  buyerUserId: '1',
  leaderUserId: '2',
  storeId: '20',
  buyerName: '买家小李',
  buyerAvatarUrl: null,
  leaderName: '王姐',
  leaderAvatarUrl: null,
  storeName: '王姐鲜果团',
  storeLogoUrl: null,
  currentUserRole: 'buyer',
  unreadCount: 1,
  lastMessageId: 'm1',
  lastMessageText: '桃子已经打包好了，今晚统一出库。',
  lastMessageType: 'text',
  lastMessageAt: '2026-07-03T11:00:00',
  createdAt: '2026-07-03T09:00:00',
}

function createTestRouter() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [
      { path: '/', name: 'index', component: { template: '<div>首页</div>' } },
      { path: '/messages', name: 'messages', component: MessagesView, meta: { showTabBar: true } },
      { path: '/messages/orders', name: 'messageOrders', component: { template: '<div>订单消息页</div>' } },
      { path: '/messages/subscriptions', name: 'messageSubscriptions', component: { template: '<div>新增订阅页</div>' } },
      { path: '/chats/:conversationId', name: 'chatDetail', component: { template: '<div>聊天详情</div>' } },
    ],
  })
}

describe('MessagesView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(listNotifications).mockReset()
    vi.mocked(listChatConversations).mockReset()
    vi.mocked(getChatUnreadCount).mockClear()
    vi.mocked(listChatConversations).mockResolvedValue({
      items: [chatConversation],
      page: 1,
      pageSize: 20,
      total: 1,
      hasMore: false,
    })
    vi.mocked(listNotifications).mockResolvedValue({
      items: [unreadNotification],
      page: 1,
      pageSize: 50,
      total: 1,
      hasMore: false,
    })
  })

  it('shows chat conversations and does not mix notification rows into the home feed', async () => {
    const router = createTestRouter()
    const wrapper = mount(MessagesView, {
      global: {
        plugins: [router, createPinia()],
      },
    })

    await flushPromises()

    expect(listChatConversations).toHaveBeenCalledWith({ page: 1, pageSize: 20 })
    expect(listNotifications).toHaveBeenCalledWith({ page: 1, pageSize: 50, unreadOnly: true })
    expect(wrapper.text()).toContain('王姐鲜果团')
    expect(wrapper.text()).toContain('桃子已经打包好了')
    expect(wrapper.findAll('.message-row')).toHaveLength(1)
    expect(wrapper.text()).not.toContain('发货通知')
  })

  it('navigates to order messages page from shortcut', async () => {
    const router = createTestRouter()
    await router.push('/messages')

    const wrapper = mount(MessagesView, {
      global: {
        plugins: [router, createPinia()],
      },
    })

    await flushPromises()

    const ordersShortcut = wrapper.findAll('.message-shortcut')
      .find((item) => item.text().includes('订单消息'))
    expect(ordersShortcut?.text()).toContain('1')
    await ordersShortcut!.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/messages/orders')
  })

  it('navigates to subscription messages page from shortcut', async () => {
    const router = createTestRouter()
    await router.push('/messages')

    const wrapper = mount(MessagesView, {
      global: {
        plugins: [router, createPinia()],
      },
    })

    await flushPromises()

    const subscriptionsShortcut = wrapper.findAll('.message-shortcut')
      .find((item) => item.text().includes('新增订阅'))
    expect(subscriptionsShortcut).toBeDefined()
    await subscriptionsShortcut!.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/messages/subscriptions')
  })

  it('opens chat detail when clicking a chat row', async () => {
    const router = createTestRouter()
    await router.push('/messages')

    const wrapper = mount(MessagesView, {
      global: {
        plugins: [router, createPinia()],
      },
    })

    await flushPromises()

    await wrapper.find('.message-row').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/chats/7001')
  })
})
