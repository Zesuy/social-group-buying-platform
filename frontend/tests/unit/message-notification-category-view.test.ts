import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import MessageNotificationCategoryView from '@/views/MessageNotificationCategoryView.vue'
import { listNotifications, markNotificationRead } from '@/api/notifications'
import type { NotificationData } from '@/types'

vi.mock('@/api/notifications', () => ({
  listNotifications: vi.fn(),
  getUnreadCount: vi.fn().mockResolvedValue({ unreadCount: 2 }),
  markNotificationRead: vi.fn(),
}))

vi.mock('vant', async () => {
  const actual = await vi.importActual<typeof import('vant')>('vant')
  return {
    ...actual,
    showToast: vi.fn(),
  }
})

const orderNotification: NotificationData = {
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

const subscriptionNotification: NotificationData = {
  id: '9002',
  type: 'subscription_created',
  title: '新增订阅',
  summary: '张阿姨刚刚订阅了你的店铺。',
  body: null,
  targetType: 'leader',
  targetId: '10',
  actionUrl: null,
  readStatus: 'unread',
  readAt: null,
  createdAt: '2026-07-03T10:10:00',
}

function createTestRouter() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [
      { path: '/messages', name: 'messages', component: { template: '<div>消息首页</div>' } },
      { path: '/messages/orders', name: 'messageOrders', component: MessageNotificationCategoryView },
      { path: '/messages/subscriptions', name: 'messageSubscriptions', component: MessageNotificationCategoryView },
      { path: '/orders/:id', name: 'orderDetail', component: { template: '<div>订单详情</div>' } },
    ],
  })
}

describe('MessageNotificationCategoryView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(listNotifications).mockReset()
    vi.mocked(markNotificationRead).mockReset()
    vi.mocked(listNotifications).mockResolvedValue({
      items: [orderNotification, subscriptionNotification],
      page: 1,
      pageSize: 50,
      total: 2,
      hasMore: false,
    })
  })

  it('shows only order notifications on the order page', async () => {
    const router = createTestRouter()
    await router.push('/messages/orders')

    const wrapper = mount(MessageNotificationCategoryView, {
      global: {
        plugins: [router, createPinia()],
      },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('发货通知')
    expect(wrapper.text()).not.toContain('张阿姨刚刚订阅了你的店铺')
  })

  it('shows only subscription notifications on the subscription page', async () => {
    const router = createTestRouter()
    await router.push('/messages/subscriptions')

    const wrapper = mount(MessageNotificationCategoryView, {
      global: {
        plugins: [router, createPinia()],
      },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('新增订阅')
    expect(wrapper.text()).toContain('张阿姨刚刚订阅了你的店铺')
    expect(wrapper.text()).not.toContain('发货通知')
  })

  it('marks unread notification as read before opening action url', async () => {
    const router = createTestRouter()
    await router.push('/messages/orders')
    vi.mocked(markNotificationRead).mockResolvedValue({
      ...orderNotification,
      readStatus: 'read',
      readAt: '2026-07-03T10:01:00',
    })

    const wrapper = mount(MessageNotificationCategoryView, {
      global: {
        plugins: [router, createPinia()],
      },
    })

    await flushPromises()
    await wrapper.findComponent({ name: 'NotificationListItem' }).vm.$emit('open', orderNotification)
    await flushPromises()

    expect(markNotificationRead).toHaveBeenCalledWith('9001')
    expect(router.currentRoute.value.fullPath).toBe('/orders/3001')
  })

  it('marks all visible notifications as read for the current category only', async () => {
    const router = createTestRouter()
    await router.push('/messages/orders')
    vi.mocked(markNotificationRead).mockImplementation(async (id) => ({
      ...(id === '9001' ? orderNotification : subscriptionNotification),
      id,
      readStatus: 'read',
      readAt: '2026-07-03T10:01:00',
    }))

    const wrapper = mount(MessageNotificationCategoryView, {
      global: {
        plugins: [router, createPinia()],
      },
    })

    await flushPromises()
    await wrapper.find('.notice-hero__action').trigger('click')
    await flushPromises()

    expect(markNotificationRead).toHaveBeenCalledTimes(1)
    expect(markNotificationRead).toHaveBeenCalledWith('9001')
  })
})
