<template>
  <van-popup
    v-model:show="visible"
    class="new-order-dialog"
    round
    position="center"
    teleport="body"
    :close-on-click-overlay="false"
  >
    <section v-if="activeNotification" class="new-order-dialog__panel" aria-label="新订单提醒">
      <header class="new-order-dialog__header">
        <span class="new-order-dialog__icon">
          <van-icon name="orders-o" />
        </span>
        <button
          type="button"
          class="new-order-dialog__close"
          aria-label="稍后处理新订单"
          @click="dismiss"
        >
          <van-icon name="cross" />
        </button>
      </header>

      <div class="new-order-dialog__body">
        <p class="new-order-dialog__eyebrow">待处理订单</p>
        <h2>新订单来了</h2>
        <p class="new-order-dialog__summary">
          {{ activeNotification.summary || activeNotification.title || '有买家完成支付，等待你处理履约。' }}
        </p>
        <span class="new-order-dialog__time">
          {{ formatDateTime(activeNotification.createdAt) }}
        </span>
      </div>

      <footer class="new-order-dialog__actions">
        <AppButton variant="ghost" block :disabled="opening" @click="dismiss">
          稍后处理
        </AppButton>
        <AppButton variant="primary" block icon="arrow" :loading="opening" @click="openOrder">
          查看订单
        </AppButton>
      </footer>
    </section>
  </van-popup>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import AppButton from '@/components/AppButton.vue'
import { listNotifications, markNotificationRead } from '@/api/notifications'
import { useAuthStore } from '@/stores'
import { formatDateTime } from '@/utils'
import type { NotificationData } from '@/types'

const POLL_INTERVAL_MS = 30000

const authStore = useAuthStore()
const router = useRouter()

const visible = ref(false)
const loading = ref(false)
const opening = ref(false)
const activeNotification = ref<NotificationData | null>(null)
const shownNotificationIds = new Set<string>()
let timer: number | null = null

function canPoll() {
  return authStore.isBootstrapped
    && authStore.isLoggedIn
    && authStore.isLeader
    && document.visibilityState === 'visible'
}

function stopPolling() {
  if (timer !== null) {
    window.clearInterval(timer)
    timer = null
  }
}

function startPolling() {
  stopPolling()
  if (!canPoll()) {
    activeNotification.value = null
    visible.value = false
    return
  }
  void fetchLatestPaidOrder()
  timer = window.setInterval(() => {
    void fetchLatestPaidOrder()
  }, POLL_INTERVAL_MS)
}

async function fetchLatestPaidOrder() {
  if (!canPoll() || loading.value || visible.value) return
  loading.value = true
  try {
    const data = await listNotifications({
      page: 1,
      pageSize: 5,
      unreadOnly: true,
      type: 'order_paid',
    })
    const next = data.items.find((item) => (
      item.type === 'order_paid'
      && item.readStatus === 'unread'
      && !shownNotificationIds.has(item.id)
    ))
    if (next) {
      activeNotification.value = next
      shownNotificationIds.add(next.id)
      visible.value = true
    }
  } catch {
    // 全局轮询不打断用户当前操作。
  } finally {
    loading.value = false
  }
}

function dismiss() {
  visible.value = false
  activeNotification.value = null
}

async function openOrder() {
  const notification = activeNotification.value
  if (!notification) return

  opening.value = true
  try {
    if (notification.readStatus === 'unread') {
      await markNotificationRead(notification.id)
    }
    visible.value = false
    activeNotification.value = null
    await router.push(resolveOrderUrl(notification))
  } catch (err) {
    showToast((err as { message?: string }).message || '订单打开失败')
  } finally {
    opening.value = false
  }
}

function resolveOrderUrl(notification: NotificationData) {
  if (notification.actionUrl?.startsWith('/')) {
    return notification.actionUrl
  }
  if (notification.targetType === 'order' && notification.targetId) {
    return `/leader/orders/${notification.targetId}`
  }
  return '/leader/orders'
}

function handleVisibilityChange() {
  startPolling()
}

onMounted(() => {
  startPolling()
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onUnmounted(() => {
  stopPolling()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})

watch(
  () => [authStore.isBootstrapped, authStore.isLoggedIn, authStore.isLeader],
  () => startPolling(),
)
</script>

<style scoped>
.new-order-dialog {
  width: min(330px, calc(100vw - 32px));
  overflow: hidden;
  background: var(--color-bg-card);
}

.new-order-dialog__panel {
  padding: 16px;
}

.new-order-dialog__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.new-order-dialog__icon {
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-size: 24px;
}

.new-order-dialog__close {
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: var(--color-text-hint);
  font-size: 18px;
  cursor: pointer;
}

.new-order-dialog__body {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.new-order-dialog__eyebrow {
  margin: 0;
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  font-weight: 800;
}

.new-order-dialog__body h2 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 22px;
  font-weight: 900;
  line-height: 1.25;
}

.new-order-dialog__summary {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-md);
  line-height: 1.55;
  word-break: break-word;
}

.new-order-dialog__time {
  color: var(--color-text-hint);
  font-size: var(--font-size-xs);
}

.new-order-dialog__actions {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 10px;
  margin-top: 16px;
}

@media (max-width: 340px) {
  .new-order-dialog__actions {
    grid-template-columns: 1fr;
  }
}
</style>
