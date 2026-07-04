import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import NewOrderNotificationDialog from '@/components/NewOrderNotificationDialog.vue'
import { listNotifications, markNotificationRead } from '@/api/notifications'
import { useAuthStore } from '@/stores'
import type { NotificationData } from '@/types'

vi.mock('@/api/notifications', () => ({
  listNotifications: vi.fn(),
  markNotificationRead: vi.fn(),
}))

vi.mock('vant', async () => {
  const actual = await vi.importActual<typeof import('vant')>('vant')
  return {
    ...actual,
    showToast: vi.fn(),
  }
})

const paidOrderNotification: NotificationData = {
  id: 'n-order-paid',
  type: 'order_paid',
  title: '订单已支付',
  summary: '买家已支付阳山水蜜桃，等待发货。',
  body: null,
  targetType: 'order',
  targetId: 'o1001',
  actionUrl: '/leader/orders/o1001',
  readStatus: 'unread',
  readAt: null,
  createdAt: '2026-07-04T12:00:00',
}

function createTestRouter() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [
      { path: '/', component: { template: '<div>首页</div>' } },
      { path: '/leader/orders/:id', component: { template: '<div>团长订单详情</div>' } },
    ],
  })
}

function mountDialog(router = createTestRouter()) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()
  authStore.accessToken = 'token'
  authStore.user = { id: 'u1', nickname: '团长' } as typeof authStore.user
  authStore.leader = { id: 'l1', userId: 'u1', nickname: '团长' } as typeof authStore.leader
  authStore.store = { id: 's1', name: '团长小店' } as typeof authStore.store
  authStore.isBootstrapped = true

  return mount(NewOrderNotificationDialog, {
    global: {
      plugins: [pinia, router],
      stubs: {
        VanPopup: {
          props: ['show'],
          template: '<div v-if="show" class="van-popup"><slot /></div>',
        },
        VanIcon: true,
      },
    },
  })
}

describe('NewOrderNotificationDialog', () => {
  beforeEach(() => {
    vi.mocked(listNotifications).mockReset()
    vi.mocked(markNotificationRead).mockReset()
    vi.mocked(markNotificationRead).mockResolvedValue({
      ...paidOrderNotification,
      readStatus: 'read',
      readAt: '2026-07-04T12:01:00',
    })
    vi.mocked(listNotifications).mockResolvedValue({
      items: [paidOrderNotification],
      page: 1,
      pageSize: 5,
      total: 1,
      hasMore: false,
    })
  })

  it('shows unread paid-order notification and opens leader order detail', async () => {
    const router = createTestRouter()
    await router.push('/')
    const wrapper = mountDialog(router)

    await flushPromises()

    expect(listNotifications).toHaveBeenCalledWith({
      page: 1,
      pageSize: 5,
      unreadOnly: true,
      type: 'order_paid',
    })
    expect(wrapper.text()).toContain('新订单来了')
    expect(wrapper.text()).toContain('买家已支付阳山水蜜桃')

    const openButton = wrapper.findAll('button').find((button) => button.text() === '查看订单')
    expect(openButton).toBeDefined()
    await openButton!.trigger('click')
    await flushPromises()

    expect(markNotificationRead).toHaveBeenCalledWith('n-order-paid')
    expect(router.currentRoute.value.fullPath).toBe('/leader/orders/o1001')
  })
})
