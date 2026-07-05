import { onMounted, onUnmounted, ref, watch } from 'vue'
import { getUnreadCount } from '@/api/notifications'
import { useAuthStore } from '@/stores'
import { NATIVE_APP_RESUME_EVENT } from '@/utils/native'

export function useNotificationPolling(intervalMs = 30000) {
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
      const data = await getUnreadCount()
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

  function onNativeAppResume() {
    startPolling()
  }

  onMounted(() => {
    startPolling()
    document.addEventListener('visibilitychange', onVisibilityChange)
    window.addEventListener(NATIVE_APP_RESUME_EVENT, onNativeAppResume)
  })

  onUnmounted(() => {
    stopPolling()
    document.removeEventListener('visibilitychange', onVisibilityChange)
    window.removeEventListener(NATIVE_APP_RESUME_EVENT, onNativeAppResume)
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
