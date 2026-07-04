import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import MessagesView from '@/views/MessagesView.vue'
import {
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from '@/api/notifications'
import {
  getChatUnreadCount,
  listChatConversations,
} from '@/api/chats'
import type { NotificationData } from '@/types'

vi.mock('@/api/notifications', () => ({
  listNotifications: vi.fn(),
  getUnreadCount: vi.fn().mockResolvedValue({ unreadCount: 2 }),
  markNotificationRead: vi.fn(),
  markAllNotificationsRead: vi.fn(),
}))

vi.mock('@/api/chats', () => ({
  listChatConversations: vi.fn(),
  getChatUnreadCount: vi.fn().mockResolvedValue({ unreadCount: 0 }),
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

function createTestRouter() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [
      { path: '/', name: 'index', component: { template: '<div>首页</div>' } },
      { path: '/messages', name: 'messages', component: MessagesView, meta: { showTabBar: true } },
      { path: '/orders/:id', name: 'orderDetail', component: { template: '<div>订单详情</div>' } },
    ],
  })
}

async function switchToNotifications(wrapper: ReturnType<typeof mount>) {
  const notificationsTab = wrapper.findAll('.app-tabs__item')
    .find((item) => item.text() === '通知')
  expect(notificationsTab).toBeDefined()
  await notificationsTab!.trigger('click')
  await flushPromises()
}

describe('MessagesView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(listNotifications).mockReset()
    vi.mocked(listChatConversations).mockReset()
    vi.mocked(getChatUnreadCount).mockClear()
    vi.mocked(markNotificationRead).mockReset()
    vi.mocked(markAllNotificationsRead).mockReset()
    vi.mocked(listChatConversations).mockResolvedValue({
      items: [],
      page: 1,
      pageSize: 20,
      total: 0,
      hasMore: false,
    })
    vi.mocked(listNotifications).mockResolvedValue({
      items: [unreadNotification],
      page: 1,
      pageSize: 20,
      total: 1,
      hasMore: false,
    })
  })

  it('loads notifications from API', async () => {
    const router = createTestRouter()
    const wrapper = mount(MessagesView, {
      global: {
        plugins: [router, createPinia()],
      },
    })

    await flushPromises()
    await switchToNotifications(wrapper)

    expect(listNotifications).toHaveBeenCalledWith({ page: 1, pageSize: 20 })
    expect(wrapper.text()).toContain('发货通知')
    expect(wrapper.text()).toContain('团长已填写物流')
  })

  it('filters unread notifications from tab', async () => {
    const router = createTestRouter()
    const wrapper = mount(MessagesView, {
      global: {
        plugins: [router, createPinia()],
      },
    })
    await flushPromises()
    await switchToNotifications(wrapper)

    const unreadTab = wrapper.findAll('.app-tabs__item')
      .find((item) => item.text() === '未读')
    expect(unreadTab).toBeDefined()
    await unreadTab!.trigger('click')
    await flushPromises()

    expect(listNotifications).toHaveBeenLastCalledWith({ page: 1, pageSize: 20, unreadOnly: true })
  })

  it('marks unread notification as read before opening action url', async () => {
    const router = createTestRouter()
    await router.push('/messages')
    vi.mocked(markNotificationRead).mockResolvedValue({
      ...unreadNotification,
      readStatus: 'read',
      readAt: '2026-07-03T10:01:00',
    })

    const wrapper = mount(MessagesView, {
      global: {
        plugins: [router, createPinia()],
      },
    })
    await flushPromises()
    await switchToNotifications(wrapper)

    await wrapper.findComponent({ name: 'NotificationListItem' }).vm.$emit('open', unreadNotification)
    await flushPromises()

    expect(markNotificationRead).toHaveBeenCalledWith('9001')
    expect(router.currentRoute.value.fullPath).toBe('/orders/3001')
  })

  it('marks all visible notifications as read', async () => {
    const router = createTestRouter()
    vi.mocked(markAllNotificationsRead).mockResolvedValue()

    const wrapper = mount(MessagesView, {
      global: {
        plugins: [router, createPinia()],
      },
    })
    await flushPromises()
    await switchToNotifications(wrapper)

    await wrapper.find('.messages-toolbar__read-all').trigger('click')
    await flushPromises()

    expect(markAllNotificationsRead).toHaveBeenCalled()
    expect(wrapper.find('.notification-item__dot').exists()).toBe(false)
  })
})
