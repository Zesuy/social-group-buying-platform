import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHashHistory } from 'vue-router'
import ProfileView from '@/views/ProfileView.vue'
import { useAuthStore } from '@/stores/auth'

vi.mock('vant', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vant')>()
  return {
    ...actual,
    showConfirmDialog: vi.fn(() => Promise.resolve()),
  }
})

function createTestRouter() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [
      { path: '/profile', component: ProfileView },
      { path: '/leader/dashboard', component: { template: '<div>商家工作台</div>' } },
      { path: '/leaders/:id', component: { template: '<div>团长主页</div>' } },
      { path: '/store/create', component: { template: '<div>创建店铺</div>' } },
    ],
  })
}

describe('ProfileView merchant workbench entry', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('shows a single merchant workbench entry for leaders', async () => {
    const router = createTestRouter()
    await router.push('/profile')
    await router.isReady()

    const pinia = createPinia()
    setActivePinia(pinia)
    const auth = useAuthStore()
    auth.accessToken = 'token'
    auth.user = {
      id: '1',
      nickname: '王姐',
      avatarUrl: null,
      phone: '13870010001',
      hasLeader: true,
      leaderId: '10',
      storeId: '20',
    }
    auth.leader = { id: '10', displayName: '王姐', avatarUrl: null }
    auth.store = { id: '20', name: '王姐鲜果团', logoUrl: null, status: 'active' }

    const wrapper = mount(ProfileView, {
      global: { plugins: [router, pinia] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('进入商家工作台')
    expect(wrapper.text()).toContain('商家工作台')
    expect(wrapper.text()).toContain('团长主页')
    expect(wrapper.text()).not.toContain('发布团购')
    expect(wrapper.text()).not.toContain('订单管理')

    await wrapper.findAll('button').find((button) => button.text().includes('进入商家工作台'))?.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/leader/dashboard')
  })

  it('keeps create store entry for logged-in non-leaders', async () => {
    const router = createTestRouter()
    await router.push('/profile')
    await router.isReady()

    const pinia = createPinia()
    setActivePinia(pinia)
    const auth = useAuthStore()
    auth.accessToken = 'token'
    auth.user = {
      id: '2',
      nickname: '小李',
      avatarUrl: null,
      phone: '13870010002',
      hasLeader: false,
      leaderId: null,
      storeId: null,
    }

    const wrapper = mount(ProfileView, {
      global: { plugins: [router, pinia] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('创建店铺')
    expect(wrapper.text()).not.toContain('进入商家工作台')
  })
})
