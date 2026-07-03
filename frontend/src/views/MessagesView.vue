<template>
  <PageLayout show-tab-bar>
    <div class="app-topbar">
      消息
      <span v-if="unreadCount > 0" class="badge-num">{{ unreadCountText }}</span>
    </div>

    <AppNoticeStrip
      text="关注公众号，后续可收到活动、订单和物流通知"
      action-label="关注"
      variant="warning"
      @action="onWechatNoticeClick"
    />

    <div class="messages-toolbar">
      <AppTabs :tabs="tabs" :active="activeTab" scrollable @change="onTabChange" />
      <AppButton
        v-if="hasVisibleUnread"
        class="messages-toolbar__read-all"
        variant="ghost"
        icon="passed"
        :loading="markAllLoading"
        @click="onMarkAllRead"
      >
        全部已读
      </AppButton>
    </div>

    <LoadingView v-if="loading && notifications.length === 0" text="正在加载消息..." />
    <ErrorView v-else-if="error" :message="error" @retry="loadNotifications" />

    <template v-else>
      <div v-if="notifications.length > 0" class="messages-list">
        <NotificationListItem
          v-for="notification in notifications"
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
    </template>
  </PageLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import EmptyState from '@/components/EmptyState.vue'
import AppNoticeStrip from '@/components/AppNoticeStrip.vue'
import AppTabs from '@/components/AppTabs.vue'
import AppButton from '@/components/AppButton.vue'
import NotificationListItem from '@/components/NotificationListItem.vue'
import { listNotifications, markAllNotificationsRead, markNotificationRead } from '@/api/notifications'
import { useNotificationPolling } from '@/composables'
import { isFeatureDisabled } from '@/utils/non-mvp'
import type { NotificationData, NotificationListParams } from '@/types'

const router = useRouter()
const { unreadCount, refreshUnreadCount } = useNotificationPolling()

type TabKey = 'all' | 'unread' | 'order_paid' | 'order_shipped' | 'group_buy_published'

interface MessageTab {
  key: TabKey
  label: string
}

const tabs: MessageTab[] = [
  { key: 'all', label: '全部' },
  { key: 'unread', label: '未读' },
  { key: 'order_paid', label: '支付' },
  { key: 'order_shipped', label: '发货' },
  { key: 'group_buy_published', label: '活动' },
]

const activeTab = ref<TabKey>('all')
const notifications = ref<NotificationData[]>([])
const loading = ref(false)
const error = ref('')
const markAllLoading = ref(false)

const unreadCountText = computed(() => unreadCount.value > 99 ? '99+' : String(unreadCount.value))
const hasVisibleUnread = computed(() => notifications.value.some((item) => item.readStatus === 'unread'))

const emptyDescription = computed(() => {
  if (activeTab.value === 'unread') return '暂无未读消息'
  if (activeTab.value === 'order_paid') return '暂无支付消息'
  if (activeTab.value === 'order_shipped') return '暂无发货消息'
  if (activeTab.value === 'group_buy_published') return '暂无活动消息'
  return '暂无消息'
})

function buildParams(): NotificationListParams {
  const params: NotificationListParams = { page: 1, pageSize: 20 }
  if (activeTab.value === 'unread') {
    params.unreadOnly = true
  } else if (activeTab.value !== 'all') {
    params.type = activeTab.value
  }
  return params
}

async function loadNotifications() {
  loading.value = true
  error.value = ''
  try {
    const data = await listNotifications(buildParams())
    notifications.value = data.items
  } catch (err) {
    error.value = (err as { message?: string }).message || '消息加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function onTabChange(key: string) {
  activeTab.value = key as TabKey
  await loadNotifications()
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
  markAllLoading.value = true
  try {
    await markAllNotificationsRead()
    notifications.value = notifications.value.map((item) => ({
      ...item,
      readStatus: 'read',
      readAt: item.readAt || new Date().toISOString(),
    }))
    await refreshUnreadCount()
    showToast('已全部标为已读')
  } catch (err) {
    showToast((err as { message?: string }).message || '操作失败，请稍后重试')
  } finally {
    markAllLoading.value = false
  }
}

function onWechatNoticeClick() {
  if (isFeatureDisabled('wechatPush')) {
    showToast('公众号推送将在后续开放')
  }
}

onMounted(() => {
  void loadNotifications()
})
</script>

<style scoped>
.messages-toolbar {
  position: sticky;
  top: 0;
  z-index: 2;
  background: var(--color-bg-card);
  border-bottom: 1px solid var(--color-border);
}

.messages-toolbar :deep(.app-tabs) {
  border-bottom: 0;
}

.messages-toolbar__read-all {
  position: absolute;
  right: 10px;
  top: 8px;
  min-height: 36px;
  padding: 0 12px;
  font-size: var(--font-size-sm);
}

.messages-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 14px 18px;
}

@media (max-width: 360px) {
  .messages-toolbar__read-all {
    position: static;
    width: calc(100% - 28px);
    margin: 0 14px 10px;
  }
}
</style>
