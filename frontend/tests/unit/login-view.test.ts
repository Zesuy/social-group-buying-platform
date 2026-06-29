import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, type VueWrapper } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import LoginView from '@/views/LoginView.vue'

/** LoginView 组件实例的 setup 返回值类型 */
interface LoginViewInstance {
  form: {
    phone: string
    nickname: string
    avatarUrl: string
  }
}

function getLoginViewInstance(wrapper: VueWrapper): LoginViewInstance {
  return wrapper.vm as unknown as LoginViewInstance
}

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', name: 'index', component: { template: '<div>首页</div>' } },
    { path: '/login', name: 'login', component: { template: '<div>登录</div>' } },
    { path: '/profile', name: 'profile', component: { template: '<div>个人中心</div>' } },
    { path: '/orders', name: 'orders', component: { template: '<div>订单</div>' }, meta: { requiresAuth: true } },
  ],
})

const push = vi.fn()
const back = vi.fn()

vi.mock('vue-router', async () => {
  const actual: Record<string, unknown> = await vi.importActual('vue-router')
  return {
    ...actual,
    useRouter: () => ({ push, back }),
    useRoute: () => ({
      query: {},
      path: '/login',
      name: 'login',
    }),
  }
})

const mockLogin = vi.fn()
vi.mock('@/stores', () => ({
  useAuthStore: () => ({
    login: mockLogin,
    isLoggedIn: false,
  }),
}))

describe('LoginView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    push.mockClear()
    back.mockClear()
    mockLogin.mockClear()
  })

  it('should render login form with title', () => {
    const wrapper = mount(LoginView, {
      global: {
        plugins: [router, createPinia()],
      },
    })

    expect(wrapper.text()).toContain('欢迎登录')
    expect(wrapper.text()).toContain('登录')
  })

  it('should render shortcut fill buttons for both test users', () => {
    const wrapper = mount(LoginView, {
      global: {
        plugins: [router, createPinia()],
      },
    })

    expect(wrapper.text()).toContain('买家测试用户')
    expect(wrapper.text()).toContain('团长测试用户')
  })

  it('should fill buyer test user data on shortcut click', async () => {
    const wrapper = mount(LoginView, {
      global: {
        plugins: [router, createPinia()],
      },
    })

    const buttons = wrapper.findAllComponents({ name: 'VanButton' })
    const buyerBtn = buttons.find((b) => b.text().includes('买家测试用户'))
    expect(buyerBtn).toBeTruthy()
    await buyerBtn!.trigger('click')

    // Verify the reactive form data is updated
    const vm = getLoginViewInstance(wrapper)
    expect(vm.form.phone).toBe('13800000000')
    expect(vm.form.nickname).toBe('买家用户')
  })

  it('should fill leader test user data on shortcut click', async () => {
    const wrapper = mount(LoginView, {
      global: {
        plugins: [router, createPinia()],
      },
    })

    const buttons = wrapper.findAllComponents({ name: 'VanButton' })
    const leaderBtn = buttons.find((b) => b.text().includes('团长测试用户'))
    expect(leaderBtn).toBeTruthy()
    await leaderBtn!.trigger('click')

    const vm = getLoginViewInstance(wrapper)
    expect(vm.form.phone).toBe('13700000000')
    expect(vm.form.nickname).toBe('团长用户')
  })
})
