import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount, type VueWrapper } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import LoginView from '@/views/LoginView.vue'

/** LoginView 组件实例的 setup 返回值类型 */
interface LoginViewInstance {
  form: {
    phone: string
    code: string
    nickname: string
  }
  agreementChecked: boolean
  handleSubmit: () => Promise<void>
}

function getLoginViewInstance(wrapper: VueWrapper): LoginViewInstance {
  return wrapper.vm as unknown as LoginViewInstance
}

const wrappers: VueWrapper[] = []

function mountLoginView(): VueWrapper {
  const wrapper = mount(LoginView, {
    global: {
      plugins: [router, createPinia()],
    },
  })
  wrappers.push(wrapper)
  return wrapper
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
let routePath = '/login'
let routeName = 'login'
let routeQuery: Record<string, string> = {}

vi.mock('vue-router', async () => {
  const actual: Record<string, unknown> = await vi.importActual('vue-router')
  return {
    ...actual,
    useRouter: () => ({ push, back }),
    useRoute: () => ({
      query: routeQuery,
      path: routePath,
      name: routeName,
    }),
  }
})

const mockLogin = vi.fn()
const mockLoginWithCode = vi.fn()
const mockRegisterWithCode = vi.fn()
const mockSendAuthCode = vi.fn()
vi.mock('@/stores', () => ({
  useAuthStore: () => ({
    sendAuthCode: mockSendAuthCode,
    loginWithCode: mockLoginWithCode,
    registerWithCode: mockRegisterWithCode,
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
    mockLoginWithCode.mockClear()
    mockRegisterWithCode.mockClear()
    mockSendAuthCode.mockClear()
    routePath = '/login'
    routeName = 'login'
    routeQuery = {}
  })

  afterEach(async () => {
    wrappers.splice(0).forEach(wrapper => wrapper.unmount())
    await new Promise(resolve => window.setTimeout(resolve, 120))
  })

  it('should render phone-code login form with title', () => {
    const wrapper = mountLoginView()

    expect(wrapper.text()).toContain('登录邻鲜团')
    expect(wrapper.text()).toContain('获取验证码')
    expect(wrapper.text()).toContain('登录')
  })

  it('should render register mode by route', () => {
    routePath = '/register'
    routeName = 'register'

    const wrapper = mountLoginView()

    expect(wrapper.text()).toContain('注册邻鲜团')
    expect(wrapper.text()).toContain('昵称')
    expect(wrapper.text()).toContain('注册并进入')
  })

  it('should keep dev test users behind collapsible section', async () => {
    const wrapper = mountLoginView()

    expect(wrapper.text()).toContain('开发测试账号')
    expect(wrapper.text()).not.toContain('买家测试用户')

    await wrapper.find('.auth-dev__toggle').trigger('click')
    expect(wrapper.text()).toContain('买家测试用户')
    expect(wrapper.text()).toContain('团长测试用户')
  })

  it('should login as buyer test user on dev shortcut click', async () => {
    mockLogin.mockResolvedValue(undefined)
    const wrapper = mountLoginView()

    await wrapper.find('.auth-dev__toggle').trigger('click')
    const buyerBtn = wrapper.findAll('button').find((b) => b.text().includes('买家测试用户'))
    expect(buyerBtn).toBeTruthy()
    await buyerBtn!.trigger('click')

    // Verify the reactive form data is updated
    const vm = getLoginViewInstance(wrapper)
    expect(vm.form.phone).toBe('13800000000')
    expect(vm.form.nickname).toBe('买家用户')
    expect(mockLogin).toHaveBeenCalledWith({
      phone: '13800000000',
      nickname: '买家用户',
    })
  })

  it('should login as leader test user on dev shortcut click', async () => {
    mockLogin.mockResolvedValue(undefined)
    const wrapper = mountLoginView()

    await wrapper.find('.auth-dev__toggle').trigger('click')
    const leaderBtn = wrapper.findAll('button').find((b) => b.text().includes('团长测试用户'))
    expect(leaderBtn).toBeTruthy()
    await leaderBtn!.trigger('click')

    const vm = getLoginViewInstance(wrapper)
    expect(vm.form.phone).toBe('13700000000')
    expect(vm.form.nickname).toBe('团长用户')
    expect(mockLogin).toHaveBeenCalledWith({
      phone: '13700000000',
      nickname: '团长用户',
    })
  })

  it('should send demo code for current mode', async () => {
    mockSendAuthCode.mockResolvedValue({
      expiresInSeconds: 300,
      devCode: '123456',
    })
    const wrapper = mountLoginView()

    const vm = getLoginViewInstance(wrapper)
    vm.form.phone = '13800000000'
    await wrapper.find('.auth-code-button').trigger('click')

    expect(mockSendAuthCode).toHaveBeenCalledWith({
      phone: '13800000000',
      scene: 'login',
    })
    expect(wrapper.text()).toContain('演示验证码')
  })

  it('should submit phone-code login', async () => {
    mockLoginWithCode.mockResolvedValue(undefined)
    const wrapper = mountLoginView()

    const vm = getLoginViewInstance(wrapper)
    vm.form.phone = '13800000000'
    vm.form.code = '123456'
    vm.agreementChecked = true
    await vm.handleSubmit()

    expect(mockLoginWithCode).toHaveBeenCalledWith({
      phone: '13800000000',
      code: '123456',
    })
    expect(push).toHaveBeenCalledWith('/profile')
  })

  it('should submit phone-code register', async () => {
    routePath = '/register'
    routeName = 'register'
    mockRegisterWithCode.mockResolvedValue(undefined)
    const wrapper = mountLoginView()

    const vm = getLoginViewInstance(wrapper)
    vm.form.phone = '13800000000'
    vm.form.code = '123456'
    vm.form.nickname = '新用户'
    vm.agreementChecked = true
    await vm.handleSubmit()

    expect(mockRegisterWithCode).toHaveBeenCalledWith({
      phone: '13800000000',
      code: '123456',
      nickname: '新用户',
    })
  })
})
