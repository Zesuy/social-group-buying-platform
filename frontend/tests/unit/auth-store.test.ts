import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { STORAGE_KEYS } from '@/constants'
import { useAuthStore } from '@/stores/auth'
import type { MockLoginRequest, CurrentUserData } from '@/types'
import * as authApi from '@/api/auth'

// 模拟 authApi
vi.mock('@/api/auth', () => ({
  sendAuthCode: vi.fn(),
  loginWithCode: vi.fn(),
  registerWithCode: vi.fn(),
  mockLogin: vi.fn(),
  fetchMe: vi.fn(),
}))

const mockLoginResponse = {
  accessToken: 'mock_token_test123',
  user: {
    id: 1,
    nickname: '买家用户',
    avatarUrl: null,
    phone: '13800000000',
    hasLeader: false,
    leaderId: null,
    storeId: null,
  },
}

const mockMeResponse: CurrentUserData = {
  user: {
    id: 1,
    nickname: '买家用户',
    avatarUrl: null,
    phone: '13800000000',
    hasLeader: false,
    leaderId: null,
    storeId: null,
  },
}

const mockMeResponseWithLeader: CurrentUserData = {
  user: {
    id: 2,
    nickname: '团长用户',
    avatarUrl: null,
    phone: '13700000000',
    hasLeader: true,
    leaderId: 10,
    storeId: 20,
  },
  leader: {
    id: 10,
    displayName: '王姐鲜果团',
    avatarUrl: null,
  },
  store: {
    id: 20,
    name: '王姐社区鲜果店',
    logoUrl: null,
    status: 'active',
  },
}

describe('authStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  describe('initial state', () => {
    it('should start with null auth state', () => {
      const store = useAuthStore()
      expect(store.accessToken).toBeNull()
      expect(store.user).toBeNull()
      expect(store.leader).toBeNull()
      expect(store.store).toBeNull()
      expect(store.isBootstrapped).toBe(false)
      expect(store.isLoading).toBe(false)
      expect(store.isLoggedIn).toBe(false)
      expect(store.isLeader).toBe(false)
    })
  })

  describe('login', () => {
    const loginParams: MockLoginRequest = {
      phone: '13800000000',
      nickname: '买家用户',
    }

    it('should login, save token, and fetch user', async () => {
      vi.mocked(authApi.mockLogin).mockResolvedValue(mockLoginResponse)
      vi.mocked(authApi.fetchMe).mockResolvedValue(mockMeResponse)

      const store = useAuthStore()
      await store.login(loginParams)

      // 验证 token 保存
      expect(store.accessToken).toBe('mock_token_test123')
      expect(localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN)).toBe('mock_token_test123')

      // 验证 API 调用顺序
      expect(authApi.mockLogin).toHaveBeenCalledWith(loginParams)
      expect(authApi.fetchMe).toHaveBeenCalledOnce()

      // 验证用户信息
      expect(store.user).toEqual(mockMeResponse.user)
      expect(store.isLoggedIn).toBe(true)
      expect(store.isLeader).toBe(false)
    })

    it('should handle login with leader/ store info', async () => {
      vi.mocked(authApi.mockLogin).mockResolvedValue({
        accessToken: 'mock_token_leader',
        user: {
          id: 2,
          nickname: '团长用户',
          avatarUrl: null,
          phone: '13700000000',
          hasLeader: true,
          leaderId: 10,
          storeId: 20,
        },
      })
      vi.mocked(authApi.fetchMe).mockResolvedValue(mockMeResponseWithLeader)

      const store = useAuthStore()
      await store.login({
        phone: '13700000000',
        nickname: '团长用户',
      })

      expect(store.isLoggedIn).toBe(true)
      expect(store.isLeader).toBe(true)
      expect(store.leader).toEqual(mockMeResponseWithLeader.leader)
      expect(store.store).toEqual(mockMeResponseWithLeader.store)
    })

    it('should handle mock-login API failure', async () => {
      vi.mocked(authApi.mockLogin).mockRejectedValue({
        code: 'VALIDATION_ERROR',
        message: '手机号格式不正确',
      })

      const store = useAuthStore()
      await expect(store.login(loginParams)).rejects.toEqual({
        code: 'VALIDATION_ERROR',
        message: '手机号格式不正确',
      })

      // 登录失败不应保存 token
      expect(store.accessToken).toBeNull()
      expect(localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN)).toBeNull()
      expect(store.isLoading).toBe(false)
    })

    it('should roll back auth state when fetchMe fails after mock-login success', async () => {
      vi.mocked(authApi.mockLogin).mockResolvedValue(mockLoginResponse)
      vi.mocked(authApi.fetchMe).mockRejectedValue({
        code: 'NETWORK_ERROR',
        message: '网络异常',
      })

      const store = useAuthStore()
      await expect(store.login(loginParams)).rejects.toEqual({
        code: 'NETWORK_ERROR',
        message: '网络异常',
      })

      // fetchMe 失败后应回滚 token 和 user 状态
      expect(store.accessToken).toBeNull()
      expect(store.user).toBeNull()
      expect(localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN)).toBeNull()
      expect(store.isLoggedIn).toBe(false)
      expect(store.isLoading).toBe(false)
    })

    it('should set isLoading during login', async () => {
      vi.mocked(authApi.mockLogin).mockResolvedValue(mockLoginResponse)
      vi.mocked(authApi.fetchMe).mockResolvedValue(mockMeResponse)

      const store = useAuthStore()

      // Start login but don't await yet
      const loginPromise = store.login(loginParams)
      expect(store.isLoading).toBe(true)

      await loginPromise
      expect(store.isLoading).toBe(false)
    })
  })

  describe('phone-code auth', () => {
    it('should send auth code', async () => {
      vi.mocked(authApi.sendAuthCode).mockResolvedValue({
        expiresInSeconds: 300,
        devCode: '123456',
      })

      const store = useAuthStore()
      const response = await store.sendAuthCode({
        phone: '13800000000',
        scene: 'login',
      })

      expect(authApi.sendAuthCode).toHaveBeenCalledWith({
        phone: '13800000000',
        scene: 'login',
      })
      expect(response.devCode).toBe('123456')
    })

    it('should login with phone code and fetch user', async () => {
      vi.mocked(authApi.loginWithCode).mockResolvedValue(mockLoginResponse)
      vi.mocked(authApi.fetchMe).mockResolvedValue(mockMeResponse)

      const store = useAuthStore()
      await store.loginWithCode({
        phone: '13800000000',
        code: '123456',
      })

      expect(authApi.loginWithCode).toHaveBeenCalledWith({
        phone: '13800000000',
        code: '123456',
      })
      expect(store.accessToken).toBe('mock_token_test123')
      expect(store.user).toEqual(mockMeResponse.user)
      expect(store.isLoggedIn).toBe(true)
    })

    it('should register with phone code and fetch user', async () => {
      vi.mocked(authApi.registerWithCode).mockResolvedValue(mockLoginResponse)
      vi.mocked(authApi.fetchMe).mockResolvedValue(mockMeResponse)

      const store = useAuthStore()
      await store.registerWithCode({
        phone: '13800000000',
        code: '123456',
        nickname: '买家用户',
      })

      expect(authApi.registerWithCode).toHaveBeenCalledWith({
        phone: '13800000000',
        code: '123456',
        nickname: '买家用户',
      })
      expect(store.accessToken).toBe('mock_token_test123')
      expect(store.isLoggedIn).toBe(true)
    })
  })

  describe('logout', () => {
    it('should clear auth state and token', () => {
      localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, 'some_token')
      const store = useAuthStore()
      store.accessToken = 'some_token'
      store.user = mockMeResponse.user
      store.isBootstrapped = true

      store.logout()

      expect(store.accessToken).toBeNull()
      expect(store.user).toBeNull()
      expect(store.leader).toBeNull()
      expect(store.store).toBeNull()
      expect(localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN)).toBeNull()
      expect(store.isLoggedIn).toBe(false)
    })
  })

  describe('fetchMe', () => {
    it('should fetch and update user info', async () => {
      vi.mocked(authApi.fetchMe).mockResolvedValue(mockMeResponseWithLeader)

      const store = useAuthStore()
      await store.fetchMe()

      expect(store.user).toEqual(mockMeResponseWithLeader.user)
      expect(store.leader).toEqual(mockMeResponseWithLeader.leader)
      expect(store.store).toEqual(mockMeResponseWithLeader.store)
      expect(store.isLeader).toBe(true)
    })

    it('should clear auth on UNAUTHORIZED', async () => {
      vi.mocked(authApi.fetchMe).mockRejectedValue({
        code: 'UNAUTHORIZED',
        message: '未登录或令牌已过期',
      })

      const store = useAuthStore()
      store.accessToken = 'invalid_token'

      await expect(store.fetchMe()).rejects.toEqual({
        code: 'UNAUTHORIZED',
        message: '未登录或令牌已过期',
      })

      expect(store.accessToken).toBeNull()
      expect(store.user).toBeNull()
    })

    it('should not clear auth on other errors', async () => {
      vi.mocked(authApi.fetchMe).mockRejectedValue({
        code: 'NETWORK_ERROR',
        message: '网络异常',
      })

      const store = useAuthStore()
      store.accessToken = 'stored_token'
      store.user = mockMeResponse.user

      await expect(store.fetchMe()).rejects.toEqual({
        code: 'NETWORK_ERROR',
        message: '网络异常',
      })

      // Network errors should preserve token
      expect(store.accessToken).toBe('stored_token')
      expect(store.user).not.toBeNull()
    })
  })

  describe('restoreSession', () => {
    it('should restore session when token exists', async () => {
      vi.mocked(authApi.fetchMe).mockResolvedValue(mockMeResponse)

      localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, 'existing_token')
      const store = useAuthStore()
      await store.restoreSession()

      expect(store.accessToken).toBe('existing_token')
      expect(store.user).toEqual(mockMeResponse.user)
      expect(store.isBootstrapped).toBe(true)
      expect(store.isLoggedIn).toBe(true)
    })

    it('should skip restore when no token', async () => {
      const store = useAuthStore()
      await store.restoreSession()

      expect(authApi.fetchMe).not.toHaveBeenCalled()
      expect(store.isBootstrapped).toBe(true)
      expect(store.isLoggedIn).toBe(false)
    })

    it('should handle invalid token during restore', async () => {
      vi.mocked(authApi.fetchMe).mockRejectedValue({
        code: 'UNAUTHORIZED',
        message: '未登录或令牌已过期',
      })

      localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, 'expired_token')
      const store = useAuthStore()
      expect(store.isBootstrapped).toBe(false)

      await store.restoreSession()

      // 失效 token 应被清除
      expect(store.accessToken).toBeNull()
      expect(store.user).toBeNull()
      expect(store.isBootstrapped).toBe(true)
      expect(store.isLoggedIn).toBe(false)
    })
  })
})
