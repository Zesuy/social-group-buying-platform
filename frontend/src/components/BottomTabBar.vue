<template>
  <van-tabbar
    :model-value="active"
    :class="{ 'bottom-tabbar--h5-constrained': h5Constrained }"
    @change="onChange"
    active-color="var(--color-primary)"
    inactive-color="var(--color-text-hint)"
    :border="false"
  >
    <van-tabbar-item
      v-for="tab in tabs"
      :key="tab.to"
      :icon="tab.icon"
      :badge="getTabBadge(tab)"
    >
      {{ tab.label }}
    </van-tabbar-item>
  </van-tabbar>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useAuthStore } from '@/stores'
import { useChatUnreadPolling, useNotificationPolling } from '@/composables'

defineProps<{
  h5Constrained?: boolean
}>()

interface TabItem {
  to: string
  icon: string
  label: string
  requiresAuth?: boolean
}

const tabs: TabItem[] = [
  { to: '/', icon: 'home-o', label: '首页' },
  { to: '/orders', icon: 'orders-o', label: '订单', requiresAuth: true },
  { to: '/open-group', icon: 'add-square', label: '开团', requiresAuth: true },
  { to: '/messages', icon: 'chat-o', label: '消息', requiresAuth: true },
  { to: '/profile', icon: 'contact-o', label: '我的', requiresAuth: true },
]

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { unreadCount } = useNotificationPolling()
const { unreadCount: chatUnreadCount } = useChatUnreadPolling()

const active = computed(() => {
  const idx = tabs.findIndex((t) => t.to === route.path)
  return idx >= 0 ? idx : 0
})

function onChange(index: number) {
  const tab = tabs[index]
  if (!tab) return
  if (route.path === tab.to) return
  if (tab.requiresAuth && !authStore.isLoggedIn) {
    showToast('请先登录')
    void router.push({ path: '/login', query: { redirect: tab.to } })
    return
  }
  void router.push(tab.to)
}

function getTabBadge(tab: TabItem) {
  const notificationCount = Number.isFinite(unreadCount.value) ? unreadCount.value : 0
  const chatCount = Number.isFinite(chatUnreadCount.value) ? chatUnreadCount.value : 0
  const count = notificationCount + chatCount
  if (tab.to !== '/messages' || count <= 0) return undefined
  return count > 99 ? '99+' : String(count)
}
</script>

<style scoped>
.bottom-tabbar--h5-constrained {
  right: 50%;
  left: auto;
  width: 100%;
  max-width: 480px;
  transform: translateX(50%);
}
</style>
