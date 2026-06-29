/**
 * 认证状态管理
 *
 * 维护 accessToken、用户、团长和店铺信息的全局状态，
 * 提供登录、退出、恢复会话和守卫逻辑。
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { STORAGE_KEYS } from '@/constants'
import * as authApi from '@/api/auth'
import type {
  MockLoginRequest,
  CurrentUserSummary,
  LeaderSummary,
  StoreSummary,
} from '@/types'

export const useAuthStore = defineStore('auth', () => {
  // ── 状态 ──
  const accessToken = ref<string | null>(null)
  const user = ref<CurrentUserSummary | null>(null)
  const leader = ref<LeaderSummary | null>(null)
  const store = ref<StoreSummary | null>(null)
  const isBootstrapped = ref(false) // 是否已完成启动恢复
  const isLoading = ref(false)

  // ── 计算属性 ──
  const isLoggedIn = computed(() => !!accessToken.value && !!user.value)
  const isLeader = computed(() => !!leader.value && !!store.value)

  // ── 内部辅助 ──
  function persistToken(token: string): void {
    accessToken.value = token
    localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, token)
  }

  function clearAuth(): void {
    accessToken.value = null
    user.value = null
    leader.value = null
    store.value = null
    localStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN)
  }

  /**
   * 调用 GET /api/v1/me 刷新 user/leader/store
   *
   * 当后端返回 UNAUTHORIZED 时清除本地认证状态。
   */
  async function fetchMe(): Promise<void> {
    try {
      const data = await authApi.fetchMe()
      user.value = data.user
      leader.value = data.leader ?? null
      store.value = data.store ?? null
    } catch (err) {
      const apiErr = err as { code?: string }
      if (apiErr.code === 'UNAUTHORIZED') {
        clearAuth()
      }
      throw err
    }
  }

  /**
   * 模拟登录
   *
   * 成功后保存 token 并调用 fetchMe 刷新完整身份。
   * 如果 fetchMe 失败则回滚 token 和用户状态，抛出原始错误。
   */
  async function login(params: MockLoginRequest): Promise<void> {
    isLoading.value = true
    try {
      const data = await authApi.mockLogin(params)
      // 先保存 token（fetchMe 需要它做鉴权）
      persistToken(data.accessToken)
      user.value = data.user

      // 调用 fetchMe 获取完整身份（leader / store）
      // 如果失败则回滚全部状态，避免半登录态
      try {
        await fetchMe()
      } catch (err) {
        clearAuth()
        throw err
      }
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 退出登录：清除 token 和所有用户状态
   */
  function logout(): void {
    clearAuth()
  }

  /**
   * 应用启动时恢复会话
   *
   * 从 localStorage 读取 token，如果存在则调用 fetchMe 恢复用户信息。
   * 无论成功与否都将 isBootstrapped 设为 true，表示启动恢复完成。
   */
  async function restoreSession(): Promise<void> {
    const savedToken = localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN)
    if (!savedToken) {
      isBootstrapped.value = true
      return
    }

    isLoading.value = true
    accessToken.value = savedToken

    try {
      await fetchMe()
    } catch {
      // fetchMe 在 UNAUTHORIZED 时已清除失效 token
      // 其他错误（网络等）保留 token，下次请求再处理
    } finally {
      isLoading.value = false
      isBootstrapped.value = true
    }
  }

  /**
   * 确保已登录，未登录时返回 false
   *
   * 与 ensureLogin 的区别：本方法不执行导航跳转，
   * 由调用方（路由守卫）根据返回值决定跳转逻辑。
   */
  function checkLoggedIn(): boolean {
    return isLoggedIn.value
  }

  return {
    // 状态
    accessToken,
    user,
    leader,
    store,
    isBootstrapped,
    isLoading,
    // 计算属性
    isLoggedIn,
    isLeader,
    // 动作
    login,
    logout,
    fetchMe,
    restoreSession,
    checkLoggedIn,
  }
})
