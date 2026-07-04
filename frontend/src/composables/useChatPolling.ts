import { onMounted, onUnmounted, ref, watch } from 'vue'
import { getChatUnreadCount } from '@/api/chats'
import { useAuthStore } from '@/stores'

export function useChatUnreadPolling(intervalMs = 30000) {
  const authStore = useAuthStore()
  const unreadCount = ref(0)
  const loading = ref(false)
  let timer: number | null = null

  function stopPolling() {
    if (timer !== null) {
      window.clearInterval(timer)
      timer = null
    }
  }

  async function refreshUnreadCount() {
    if (!authStore.isLoggedIn || document.visibilityState === 'hidden') {
      if (!authStore.isLoggedIn) unreadCount.value = 0
      return
    }
    loading.value = true
    try {
      const data = await getChatUnreadCount()
      unreadCount.value = Number.isFinite(data.unreadCount) ? data.unreadCount : 0
    } catch {
      // 轮询失败保持静默，避免打扰主流程。
    } finally {
      loading.value = false
    }
  }

  function startPolling() {
    stopPolling()
    if (!authStore.isLoggedIn) {
      unreadCount.value = 0
      return
    }
    void refreshUnreadCount()
    timer = window.setInterval(() => {
      void refreshUnreadCount()
    }, intervalMs)
  }

  function onVisibilityChange() {
    if (document.visibilityState === 'visible') {
      startPolling()
    } else {
      stopPolling()
    }
  }

  onMounted(() => {
    startPolling()
    document.addEventListener('visibilitychange', onVisibilityChange)
  })

  onUnmounted(() => {
    stopPolling()
    document.removeEventListener('visibilitychange', onVisibilityChange)
  })

  watch(() => authStore.isLoggedIn, () => {
    startPolling()
  })

  return {
    unreadCount,
    loading,
    refreshUnreadCount,
    startPolling,
    stopPolling,
  }
}

export function useChatPolling(intervalMs = 5000) {
  let timer: number | null = null

  function stopPolling() {
    if (timer !== null) {
      window.clearInterval(timer)
      timer = null
    }
  }

  function startPolling(callback: () => void | Promise<void>) {
    stopPolling()
    if (document.visibilityState === 'hidden') return
    timer = window.setInterval(() => {
      if (document.visibilityState === 'visible') void callback()
    }, intervalMs)
  }

  onUnmounted(stopPolling)

  return {
    startPolling,
    stopPolling,
  }
}
