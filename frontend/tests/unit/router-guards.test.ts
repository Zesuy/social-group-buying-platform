import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

// 模拟 auth API
vi.mock('@/api/auth', () => ({
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

  it('should allow /messages without authentication', async () => {
    const router = await setupRouter()
    await router.push('/messages')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('messages')
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
})
