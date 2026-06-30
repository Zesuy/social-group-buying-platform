<template>
  <van-tabbar
    :model-value="active"
    @change="onChange"
    active-color="var(--color-primary)"
    inactive-color="var(--color-text-hint)"
    route
    placeholder
    safe-area-inset-bottom
    :border="false"
  >
    <van-tabbar-item
      v-for="tab in tabs"
      :key="tab.to"
      :to="tab.to"
      :icon="tab.icon"
    >
      {{ tab.label }}
    </van-tabbar-item>
  </van-tabbar>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { showToast } from 'vant'
import { useAuthStore } from '@/stores'

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
  { to: '/messages', icon: 'chat-o', label: '消息' },
  { to: '/profile', icon: 'contact-o', label: '我的', requiresAuth: true },
]

const route = useRoute()
const authStore = useAuthStore()

const active = computed(() => {
  const idx = tabs.findIndex((t) => t.to === route.path)
  return idx >= 0 ? idx : 0
})

function onChange(index: number) {
  const tab = tabs[index]
  // 需要登录的 tab，未登录时拦截
  if (tab.requiresAuth && !authStore.isLoggedIn) {
    showToast('请先登录')
  }
}
</script>
