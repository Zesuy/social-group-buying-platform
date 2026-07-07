import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

// 模拟 auth API
vi.mock('@/api/auth', () => ({
  sendAuthCode: vi.fn(),
  loginWithCode: vi.fn(),
  registerWithCode: vi.fn(),
  mockLogin: vi.fn(),
  fetchMe: vi.fn(),
}))

async function setupRouter() {
  // 动态导入 router（保证 auth store mock 已就绪）
  const { default: routerModule } = await import('@/router')
  return routerModule
}

describe('Router guards', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('should allow unauthenticated access to public routes', async () => {
    const router = await setupRouter()
    await router.push('/')
    expect(router.currentRoute.value.name).toBe('index')
  })

  it('should allow unauthenticated access to share token group buy detail', async () => {
    const router = await setupRouter()
    await router.push('/share/group-buys/token-abc')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('groupBuyShareDetail')
  })

  it('should redirect /orders to /login when not authenticated', async () => {
    const router = await setupRouter()
    await router.push('/orders')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/orders')
  })

  it('should allow /open-group without authentication (now public)', async () => {
    const router = await setupRouter()
    await router.push('/open-group')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('openGroup')
  })

  it('should allow /open-group when authenticated non-leader (now public)', async () => {
    const store = useAuthStore()
    store.accessToken = 'valid_token'
    store.user = {
      id: 1,
      nickname: '买家用户',
      avatarUrl: null,
      phone: '13800000000',
      hasLeader: false,
      leaderId: null,
      storeId: null,
    }
    store.isBootstrapped = true

    const router = await setupRouter()
    await router.push('/open-group')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('openGroup')
  })

  it('should redirect /login to /profile when already authenticated', async () => {
    const store = useAuthStore()
    store.accessToken = 'valid_token'
    store.user = {
      id: 1,
      nickname: '买家用户',
      avatarUrl: null,
      phone: '13800000000',
      hasLeader: false,
      leaderId: null,
      storeId: null,
    }
    store.isBootstrapped = true

    const router = await setupRouter()
    await router.push('/login')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('profile')
  })

  it('should redirect /register to /profile when already authenticated', async () => {
    const store = useAuthStore()
    store.accessToken = 'valid_token'
    store.user = {
      id: 1,
      nickname: '买家用户',
      avatarUrl: null,
      phone: '13800000000',
      hasLeader: false,
      leaderId: null,
      storeId: null,
    }
    store.isBootstrapped = true

    const router = await setupRouter()
    await router.push('/register')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('profile')
  })

  it('should pass redirect query to /login', async () => {
    const store = useAuthStore()
    store.accessToken = 'valid_token'
    store.user = {
      id: 1,
      nickname: '买家用户',
      avatarUrl: null,
      phone: '13800000000',
      hasLeader: false,
      leaderId: null,
      storeId: null,
    }
    store.isBootstrapped = true

    const router = await setupRouter()
    await router.push('/login?redirect=/orders')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('orders')
  })

  it('should redirect /messages to /login when not authenticated', async () => {
    const router = await setupRouter()
    await router.push('/messages')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/messages')
  })

  it('should redirect /chats/:conversationId to /login when not authenticated', async () => {
    const router = await setupRouter()
    await router.push('/chats/1')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/chats/1')
  })

  it('should allow /profile without authentication (now public)', async () => {
    const router = await setupRouter()
    await router.push('/profile')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('profile')
  })

  it('should redirect /leader routes to /login when not authenticated', async () => {
    const router = await setupRouter()
    await router.push('/leader/dashboard')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/leader/dashboard')
  })

  it('should redirect /merchant routes to /login when not authenticated', async () => {
    const router = await setupRouter()
    await router.push('/merchant/dashboard')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/merchant/dashboard')
  })

  it('should redirect /merchant routes to create store when authenticated non-leader', async () => {
    const store = useAuthStore()
    store.accessToken = 'valid_token'
    store.user = {
      id: 1,
      nickname: '买家用户',
      avatarUrl: null,
      phone: '13800000000',
      hasLeader: false,
      leaderId: null,
      storeId: null,
    }
    store.isBootstrapped = true

    const router = await setupRouter()
    await router.push('/merchant/orders')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('createStore')
    expect(router.currentRoute.value.query.redirect).toBe('/merchant/orders')
  })

  it('should allow leader to access new merchant parity pages', async () => {
    const store = useAuthStore()
    store.accessToken = 'valid_token'
    store.user = {
      id: 2,
      nickname: '团长用户',
      avatarUrl: null,
      phone: '13700000000',
      hasLeader: true,
      leaderId: 10,
      storeId: 20,
    }
    store.leader = {
      id: 10,
      displayName: '王姐鲜果团',
      avatarUrl: null,
      bio: null,
    }
    store.store = {
      id: 20,
      leaderId: 10,
      name: '王姐社区鲜果店',
      logoUrl: null,
      description: null,
      defaultDeliveryType: 'local_delivery',
      distributionEnabled: false,
      status: 'active',
    }
    store.isBootstrapped = true

    const router = await setupRouter()
    const cases = [
      ['/merchant/products', 'merchantProducts'],
      ['/merchant/products/new', 'merchantProductNew'],
      ['/merchant/products/21/edit', 'merchantProductEdit'],
      ['/merchant/group-buys/new', 'merchantGroupBuyNew'],
      ['/merchant/group-buys/100', 'merchantGroupBuyDetail'],
      ['/merchant/coupons', 'merchantCoupons'],
      ['/merchant/subscribers?subscriptionId=sub_1', 'merchantSubscribers'],
      ['/merchant/store', 'merchantStore'],
    ] as const

    for (const [path, routeName] of cases) {
      await router.push(path)
      await router.isReady()
      expect(router.currentRoute.value.name).toBe(routeName)
    }
  })

  it('should redirect /leader/subscribers to /login when not authenticated', async () => {
    const router = await setupRouter()
    await router.push('/leader/subscribers')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/leader/subscribers')
  })

  it('should allow leader to access subscriber list page', async () => {
    const store = useAuthStore()
    store.accessToken = 'valid_token'
    store.user = {
      id: 2,
      nickname: '团长用户',
      avatarUrl: null,
      phone: '13700000000',
      hasLeader: true,
      leaderId: 10,
      storeId: 20,
    }
    store.leader = {
      id: 10,
      displayName: '王姐鲜果团',
      avatarUrl: null,
      bio: null,
    }
    store.store = {
      id: 20,
      leaderId: 10,
      name: '王姐社区鲜果店',
      logoUrl: null,
      description: null,
      defaultDeliveryType: 'local_delivery',
      distributionEnabled: false,
      status: 'active',
    }
    store.isBootstrapped = true

    const router = await setupRouter()
    await router.push('/leader/subscribers')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('leaderSubscribers')
  })

  it('should redirect legacy product detail path to product edit page', async () => {
    const store = useAuthStore()
    store.accessToken = 'valid_token'
    store.user = {
      id: 2,
      nickname: '团长用户',
      avatarUrl: null,
      phone: '13700000000',
      hasLeader: true,
      leaderId: 10,
      storeId: 20,
    }
    store.leader = {
      id: 10,
      displayName: '王姐鲜果团',
      avatarUrl: null,
      bio: null,
    }
    store.store = {
      id: 20,
      leaderId: 10,
      name: '王姐社区鲜果店',
      logoUrl: null,
      description: null,
      defaultDeliveryType: 'local_delivery',
      distributionEnabled: false,
      status: 'active',
    }
    store.isBootstrapped = true

    const router = await setupRouter()
    await router.push('/leader/products/2')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('leaderProductEdit')
    expect(router.currentRoute.value.params.id).toBe('2')
    expect(router.currentRoute.value.fullPath).toBe('/leader/products/2/edit')
  })
})
