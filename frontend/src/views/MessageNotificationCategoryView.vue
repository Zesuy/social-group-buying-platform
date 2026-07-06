<template>
  <PageLayout :title="pageTitle" show-back @back="goBack">
    <div class="notice-page">
      <section class="notice-hero">
        <div>
          <h2>{{ pageTitle }}</h2>
          <p>{{ pageSubtitle }}</p>
        </div>
        <button
          v-if="hasVisibleUnread"
          type="button"
          class="notice-hero__action"
          :disabled="markAllLoading"
          @click="onMarkAllRead"
        >
          {{ markAllLoading ? '处理中' : '全部已读' }}
        </button>
      </section>

      <LoadingView v-if="loading && notifications.length === 0" text="正在加载消息..." />
      <ErrorView v-else-if="error" :message="error" @retry="reload" />

      <div v-else-if="visibleNotifications.length > 0" class="notice-list">
        <NotificationListItem
          v-for="notification in visibleNotifications"
          :key="notification.id"
          :notification="notification"
          @open="onOpenNotification"
        />
      </div>

      <EmptyState
        v-else
        image="chat-o"
        :description="emptyDescription"
      />
    </div>
  </PageLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import EmptyState from '@/components/EmptyState.vue'
import NotificationListItem from '@/components/NotificationListItem.vue'
import { listNotifications, markNotificationRead } from '@/api/notifications'
import { useNotificationPolling } from '@/composables'
import type { NotificationData } from '@/types'

type NoticeMode = 'orders' | 'subscriptions'

const route = useRoute()
const router = useRouter()
const { refreshUnreadCount } = useNotificationPolling()

const notifications = ref<NotificationData[]>([])
const loading = ref(false)
const error = ref('')
const markAllLoading = ref(false)

const mode = computed<NoticeMode>(() => (
  route.name === 'messageSubscriptions' ? 'subscriptions' : 'orders'
))
const pageTitle = computed(() => (
  mode.value === 'orders' ? '订单消息' : '新增订阅'
))
const pageSubtitle = computed(() => (
  mode.value === 'orders'
    ? '支付、发货和订单状态更新会集中收在这里。'
    : '用户新订阅后的提醒会集中收在这里。'
))
const visibleNotifications = computed(() => notifications.value.filter((item) => (
  mode.value === 'orders' ? isOrderNotification(item) : item.type === 'subscription_created'
)))
const hasVisibleUnread = computed(() => visibleNotifications.value.some((item) => item.readStatus === 'unread'))
const emptyDescription = computed(() => (
  mode.value === 'orders' ? '暂无订单消息' : '暂无新增订阅消息'
))

async function loadNotifications() {
  loading.value = true
  error.value = ''
  try {
    const data = await listNotifications({ page: 1, pageSize: 50 })
    notifications.value = data.items
  } catch (err) {
    error.value = (err as { message?: string }).message || '消息加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function reload() {
  await Promise.all([
    loadNotifications(),
    refreshUnreadCount(),
  ])
}

async function onOpenNotification(notification: NotificationData) {
  if (notification.readStatus === 'unread') {
    try {
      const updated = await markNotificationRead(notification.id)
      notifications.value = notifications.value.map((item) => item.id === updated.id ? updated : item)
      await refreshUnreadCount()
    } catch (err) {
      showToast((err as { message?: string }).message || '标记已读失败')
      return
    }
  }

  if (notification.actionUrl?.startsWith('/')) {
    await router.push(notification.actionUrl)
  }
}

async function onMarkAllRead() {
  const unreadItems = visibleNotifications.value.filter((item) => item.readStatus === 'unread')
  if (unreadItems.length === 0) return

  markAllLoading.value = true
  try {
    const updatedItems = await Promise.all(unreadItems.map((item) => markNotificationRead(item.id)))
    const updatedMap = new Map(updatedItems.map((item) => [item.id, item]))
    notifications.value = notifications.value.map((item) => updatedMap.get(item.id) ?? item)
    await refreshUnreadCount()
    showToast('已全部标为已读')
  } catch (err) {
    showToast((err as { message?: string }).message || '操作失败，请稍后重试')
  } finally {
    markAllLoading.value = false
  }
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
    return
  }
  void router.push('/messages')
}

function isOrderNotification(notification: NotificationData): boolean {
  return notification.type.startsWith('order_')
}

onMounted(() => {
  void reload()
})
</script>

<style scoped>
.notice-page {
  min-height: 100%;
  padding: 14px;
  background: var(--color-bg);
}

.notice-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
  margin-bottom: 12px;
  border-radius: var(--radius-card);
  background: var(--color-bg-card);
  box-shadow: var(--shadow-card);
}

.notice-hero h2 {
  margin: 0 0 6px;
  color: var(--color-text-primary);
  font-size: var(--font-size-xl);
  font-weight: 900;
}

.notice-hero p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-md);
  line-height: 1.55;
}

.notice-hero__action {
  min-width: 88px;
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: var(--color-bg-card);
  color: var(--color-primary);
  font-family: inherit;
  font-size: var(--font-size-sm);
  font-weight: 800;
}

.notice-hero__action:disabled {
  opacity: 0.55;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
</style>
