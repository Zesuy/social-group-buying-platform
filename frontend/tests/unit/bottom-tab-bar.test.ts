import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import BottomTabBar from '@/components/BottomTabBar.vue'
import { useAuthStore } from '@/stores'
import { showToast } from 'vant'
import { getUnreadCount } from '@/api/notifications'

vi.mock('vant', async () => {
  const actual = await vi.importActual<typeof import('vant')>('vant')
  return {
    ...actual,
    showToast: vi.fn(),
  }
})

vi.mock('@/api/notifications', () => ({
  getUnreadCount: vi.fn(),
}))

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', name: 'index', component: { template: '<div>首页</div>' }, meta: { showTabBar: true } },
    { path: '/orders', name: 'orders', component: { template: '<div>订单</div>' }, meta: { showTabBar: true } },
    { path: '/open-group', name: 'openGroup', component: { template: '<div>一键开团</div>' }, meta: { showTabBar: true } },
    { path: '/messages', name: 'messages', component: { template: '<div>消息</div>' }, meta: { showTabBar: true } },
    { path: '/profile', name: 'profile', component: { template: '<div>我的</div>' }, meta: { showTabBar: true } },
  ],
})

const mountedWrappers: VueWrapper[] = []

function mountBottomTabBar(options: Parameters<typeof mount>[1]) {
  const wrapper = mount(BottomTabBar, {
    ...options,
    global: {
      ...options?.global,
      stubs: {
        ...options?.global?.stubs,
        VanTabbar: {
          name: 'VanTabbar',
          props: ['modelValue'],
          emits: ['change'],
          template: '<div class="van-tabbar"><slot /></div>',
        },
        VanTabbarItem: {
          name: 'VanTabbarItem',
          props: ['to', 'icon', 'badge'],
          template: '<div class="van-tabbar-item"><slot /></div>',
        },
      },
    },
  })
  mountedWrappers.push(wrapper)
  return wrapper
}

describe('BottomTabBar', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(showToast).mockClear()
    vi.mocked(getUnreadCount).mockReset()
    vi.mocked(getUnreadCount).mockResolvedValue({ unreadCount: 0 })
  })

  afterEach(() => {
    while (mountedWrappers.length > 0) {
      mountedWrappers.pop()?.unmount()
    }
  })

  it('should render 5 tabs', () => {
    const wrapper = mountBottomTabBar({
      global: {
        plugins: [router, createPinia()],
      },
    })

    const items = wrapper.findAllComponents({ name: 'VanTabbarItem' })
    expect(items.length).toBe(5)
  })

  it('should have correct tab labels', () => {
    const wrapper = mountBottomTabBar({
      global: {
        plugins: [router, createPinia()],
      },
    })

    const labels = wrapper.findAllComponents({ name: 'VanTabbarItem' })
    expect(labels[0].text()).toContain('首页')
    expect(labels[1].text()).toContain('订单')
    expect(labels[2].text()).toContain('开团')
    expect(labels[3].text()).toContain('消息')
    expect(labels[4].text()).toContain('我的')
  })

  it('should mark current top-level tab as active', async () => {
    await router.push('/orders')
    await router.isReady()

    const wrapper = mountBottomTabBar({
      global: {
        plugins: [router, createPinia()],
      },
    })

    expect(wrapper.findComponent({ name: 'VanTabbar' }).props('modelValue')).toBe(1)
  })

  it('should show login toast when unauthenticated auth tab is selected', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const authStore = useAuthStore()
    authStore.logout()

    const wrapper = mountBottomTabBar({
      global: {
        plugins: [router, pinia],
      },
    })

    await wrapper.findComponent({ name: 'VanTabbar' }).vm.$emit('change', 1)
    expect(showToast).toHaveBeenCalledWith('请先登录')
  })

  it('should show unread badge on messages tab when authenticated', async () => {
    vi.mocked(getUnreadCount).mockResolvedValue({ unreadCount: 7 })
    const pinia = createPinia()
    setActivePinia(pinia)
    const authStore = useAuthStore()
    authStore.accessToken = 'valid_token'
    authStore.user = {
      id: '1',
      nickname: '买家用户',
      avatarUrl: null,
      phone: '13800000000',
      hasLeader: false,
      leaderId: null,
      storeId: null,
    }

    const wrapper = mountBottomTabBar({
      global: {
        plugins: [router, pinia],
      },
    })

    await flushPromises()

    const items = wrapper.findAllComponents({ name: 'VanTabbarItem' })
    expect(items[3].props('badge')).toBe('7')
  })

  it('should hide unread badge when unread count response is malformed', async () => {
    vi.mocked(getUnreadCount).mockResolvedValue({ unreadCount: undefined as unknown as number })
    const pinia = createPinia()
    setActivePinia(pinia)
    const authStore = useAuthStore()
    authStore.accessToken = 'valid_token'
    authStore.user = {
      id: '1',
      nickname: '买家用户',
      avatarUrl: null,
      phone: '13800000000',
      hasLeader: false,
      leaderId: null,
      storeId: null,
    }

    const wrapper = mountBottomTabBar({
      global: {
        plugins: [router, pinia],
      },
    })

    await flushPromises()

    const items = wrapper.findAllComponents({ name: 'VanTabbarItem' })
    expect(items[3].props('badge')).toBeUndefined()
  })

  it('should show login toast when unauthenticated messages tab is selected', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const authStore = useAuthStore()
    authStore.logout()

    const wrapper = mountBottomTabBar({
      global: {
        plugins: [router, pinia],
      },
    })

    await wrapper.findComponent({ name: 'VanTabbar' }).vm.$emit('change', 3)
    expect(showToast).toHaveBeenCalledWith('请先登录')
  })
})
