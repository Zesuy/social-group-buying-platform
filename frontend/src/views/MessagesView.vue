<template>
  <PageLayout show-tab-bar>
    <div class="messages-page">
      <header class="messages-topbar">
        <button type="button" class="messages-topbar__icon" aria-label="搜索消息" @click="onSearch">
          <van-icon name="search" />
        </button>
        <h1>消息</h1>
        <button type="button" class="messages-topbar__icon" aria-label="发起沟通" @click="onCreateChat">
          <van-icon name="plus" />
        </button>
      </header>

      <section class="message-shortcuts" aria-label="消息快捷入口">
        <button
          v-for="shortcut in shortcuts"
          :key="shortcut.key"
          type="button"
          class="message-shortcut"
          @click="onShortcutClick(shortcut.key)"
        >
          <span class="message-shortcut__icon" :class="`message-shortcut__icon--${shortcut.tone}`">
            <van-icon :name="shortcut.icon" />
            <span v-if="shortcut.badge" class="message-shortcut__badge">{{ shortcut.badge }}</span>
          </span>
          <span>{{ shortcut.label }}</span>
        </button>
      </section>

      <div class="message-feed-head">
        <div>
          <strong>{{ feedTitle }}</strong>
          <span>{{ feedSubtitle }}</span>
        </div>
      </div>

      <LoadingView v-if="chatLoading && chatConversations.length === 0" text="正在加载聊天..." />
      <ErrorView v-else-if="chatError" :message="chatError" @retry="reloadChats" />

      <template v-else>
        <div v-if="feedItems.length > 0" class="message-feed">
          <button
            v-for="item in feedItems"
            :key="item.id"
            type="button"
            class="message-row"
            :class="{ 'message-row--unread': item.unreadCount > 0 }"
            @click="openFeedItem(item)"
          >
            <span v-if="item.avatarUrl" class="message-row__avatar">
              <ImageWithFallback
                :src="item.avatarUrl"
                :alt="item.title"
                width="54px"
                height="54px"
                radius="50%"
                demo-kind="store"
              />
            </span>
            <span v-else class="message-row__icon message-row__icon--blue">
              <van-icon :name="item.icon" />
            </span>

            <span class="message-row__body">
              <span class="message-row__title">{{ item.title }}</span>
              <span class="message-row__summary">{{ item.summary }}</span>
            </span>

            <span class="message-row__side">
              <span class="message-row__time">{{ item.timeText }}</span>
              <span v-if="item.unreadCount > 0" class="message-row__badge">
                {{ item.unreadCount > 99 ? '99+' : item.unreadCount }}
              </span>
            </span>
          </button>
        </div>

        <EmptyState
          v-else
          image="chat-o"
          :description="emptyDescription"
        />
      </template>
    </div>
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
import ImageWithFallback from '@/components/ImageWithFallback.vue'
import { listChatConversations } from '@/api/chats'
import { listNotifications } from '@/api/notifications'
import { useChatUnreadPolling, useNotificationPolling } from '@/composables'
import type { ChatConversationData, NotificationData } from '@/types'

type ShortcutKey = 'orders' | 'subscriptions'
type MessageFeedItem = ReturnType<typeof toChatFeedItem>

const router = useRouter()
const { refreshUnreadCount } = useNotificationPolling()
const { unreadCount: chatUnreadCount, refreshUnreadCount: refreshChatUnreadCount } = useChatUnreadPolling()

const notifications = ref<NotificationData[]>([])
const chatConversations = ref<ChatConversationData[]>([])
const chatLoading = ref(false)
const chatError = ref('')

const subscriptionUnreadCount = computed(() => notifications.value.filter((item) => (
  item.type === 'subscription_created' && item.readStatus === 'unread'
)).length)
const orderUnreadCount = computed(() => notifications.value.filter((item) => (
  isOrderNotification(item) && item.readStatus === 'unread'
)).length)
const feedTitle = computed(() => '聊天消息')
const feedSubtitle = computed(() => {
  if (chatUnreadCount.value > 0) return `${chatUnreadCount.value > 99 ? '99+' : chatUnreadCount.value} 条未读`
  return '下单后的履约沟通会收在这里'
})
const emptyDescription = computed(() => '暂无聊天，下单后会自动建立和团长的沟通入口')
const shortcuts = computed(() => [
  {
    key: 'orders' as const,
    label: '订单消息',
    icon: 'orders-o',
    tone: 'rose',
    badge: orderUnreadCount.value > 0 ? String(Math.min(orderUnreadCount.value, 99)) : '',
  },
  {
    key: 'subscriptions' as const,
    label: '新增订阅',
    icon: 'friends',
    tone: 'blue',
    badge: subscriptionUnreadCount.value > 0 ? String(Math.min(subscriptionUnreadCount.value, 99)) : '',
  },
])
const feedItems = computed(() => chatConversations.value.map(toChatFeedItem).sort((a, b) => b.timestamp - a.timestamp))

async function loadNotifications() {
  try {
    const data = await listNotifications({ page: 1, pageSize: 50, unreadOnly: true })
    notifications.value = data.items
  } catch {
    notifications.value = []
  }
}

async function reloadChats() {
  chatLoading.value = true
  chatError.value = ''
  try {
    const data = await listChatConversations({ page: 1, pageSize: 20 })
    chatConversations.value = data.items
  } catch (err) {
    chatError.value = (err as { message?: string }).message || '聊天加载失败，请稍后重试'
  } finally {
    chatLoading.value = false
  }
}

async function reloadAll() {
  await Promise.all([
    reloadChats(),
    loadNotifications(),
    refreshUnreadCount(),
    refreshChatUnreadCount(),
  ])
}

function onShortcutClick(key: ShortcutKey) {
  const path = key === 'orders' ? '/messages/orders' : '/messages/subscriptions'
  void router.push(path)
}

function onSearch() {
  showToast('消息搜索暂未接入')
}

function onCreateChat() {
  showToast('下单后会自动建立履约沟通')
}

async function openFeedItem(item: MessageFeedItem) {
  await router.push(`/chats/${item.raw.id}`)
}

function toChatFeedItem(conversation: ChatConversationData) {
  const lastAt = conversation.lastMessageAt || conversation.createdAt
  const counterpart = conversation.currentUserRole === 'leader'
    ? `买家 ${conversation.buyerName}`
    : `团长 ${conversation.leaderName}`
  return {
    id: `chat:${conversation.id}`,
    kind: 'chat' as const,
    raw: conversation,
    title: conversation.storeName || counterpart,
    summary: chatSummary(conversation, counterpart),
    timeText: formatFeedTime(lastAt),
    timestamp: timeValue(lastAt),
    unreadCount: conversation.unreadCount,
    avatarUrl: conversation.storeLogoUrl || conversation.leaderAvatarUrl || null,
    icon: 'chat-o',
  }
}

function chatSummary(conversation: ChatConversationData, counterpart: string): string {
  if (conversation.lastMessageType === 'image') return `${counterpart} 发来了一张图片`
  if (conversation.lastMessageType === 'card') return conversation.lastMessageText || `${counterpart} 发来订单卡片`
  return conversation.lastMessageText || `${counterpart} 的履约沟通入口`
}

function isOrderNotification(notification: NotificationData): boolean {
  return notification.type.startsWith('order_')
}

function formatFeedTime(value?: string | null): string {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const now = new Date()
  const sameYear = date.getFullYear() === now.getFullYear()
  const sameDay = sameYear && date.getMonth() === now.getMonth() && date.getDate() === now.getDate()
  if (sameDay) return `${pad(date.getHours())}:${pad(date.getMinutes())}`
  if (sameYear) return `${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function timeValue(value?: string | null): number {
  if (!value) return 0
  const time = new Date(value).getTime()
  return Number.isNaN(time) ? 0 : time
}

function pad(value: number): string {
  return String(value).padStart(2, '0')
}

onMounted(() => {
  void reloadAll()
})
</script>

<style scoped>
.messages-page {
  min-height: 100%;
  background: var(--color-bg);
  padding-bottom: calc(var(--safe-area-bottom) + 18px);
}

.messages-topbar {
  position: sticky;
  top: 0;
  z-index: 3;
  display: grid;
  grid-template-columns: 48px 1fr 48px;
  align-items: center;
  min-height: 58px;
  padding: 6px 14px;
  background: color-mix(in srgb, var(--color-bg-card) 94%, transparent);
  backdrop-filter: blur(12px);
}

.messages-topbar h1 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: var(--font-size-xl);
  font-weight: 900;
  line-height: 1.2;
  text-align: center;
}

.messages-topbar__icon {
  width: 44px;
  height: 44px;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--color-text-primary);
  font-size: 25px;
  font-family: inherit;
  cursor: pointer;
}

.message-shortcuts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding: 14px;
}

.message-shortcut {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 0;
  min-height: 112px;
  border: 1px solid rgba(237, 240, 242, 0.82);
  border-radius: var(--radius-card);
  background: var(--color-bg-card);
  color: var(--color-text-primary);
  box-shadow: var(--shadow-card);
  font-family: inherit;
  font-size: 17px;
  font-weight: 800;
  line-height: 1.35;
  cursor: pointer;
}

.message-shortcut__icon {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  margin-bottom: 10px;
  border-radius: 18px;
  font-size: 32px;
}

.message-shortcut__icon--rose {
  background: var(--color-price-light);
  color: var(--color-price);
}

.message-shortcut__icon--blue {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.message-shortcut__badge {
  position: absolute;
  top: -7px;
  right: -7px;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 999px;
  background: var(--color-price);
  color: #fff;
  font-size: 12px;
  font-weight: 900;
  line-height: 22px;
}

.message-feed-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 6px 14px 8px;
}

.message-feed-head div {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.message-feed-head strong {
  color: var(--color-text-primary);
  font-size: 18px;
  font-weight: 900;
  line-height: 1.4;
}

.message-feed-head span {
  color: var(--color-text-secondary);
  font-size: 13px;
  line-height: 1.45;
}

.message-feed-head__action {
  min-width: 78px;
  height: 36px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: var(--color-bg-card);
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 800;
  font-family: inherit;
}

.message-feed {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 0 14px 18px;
}

.message-row {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  width: 100%;
  min-height: 88px;
  padding: 12px;
  border: 1px solid rgba(237, 240, 242, 0.86);
  border-radius: var(--radius-card);
  background: var(--color-bg-card);
  box-shadow: var(--shadow-card);
  color: inherit;
  text-align: left;
  font-family: inherit;
  cursor: pointer;
}

.message-row:active {
  background: var(--color-bg-surface);
}

.message-row__avatar,
.message-row__icon {
  width: 54px;
  height: 54px;
  border-radius: 50%;
  overflow: hidden;
}

.message-row__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 27px;
}

.message-row__icon--blue {
  background: var(--color-primary);
}

.message-row__icon--green {
  background: var(--color-primary);
}

.message-row__icon--orange {
  background: var(--color-price);
}

.message-row__icon--gray {
  background: var(--color-text-secondary);
}

.message-row__body {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.message-row__title {
  overflow: hidden;
  color: var(--color-text-primary);
  font-size: 18px;
  font-weight: 800;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-row__summary {
  overflow: hidden;
  color: var(--color-text-secondary);
  font-size: 15px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-row__side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  gap: 9px;
  min-width: 54px;
}

.message-row__time {
  color: var(--color-text-hint);
  font-size: 13px;
  line-height: 1.2;
  white-space: nowrap;
}

.message-row__badge {
  min-width: 10px;
  height: 10px;
  padding: 0;
  border-radius: 999px;
  background: var(--color-price);
  color: transparent;
  font-size: 0;
}

.message-row--unread .message-row__title {
  font-weight: 900;
}

@media (max-width: 360px) {
  .message-shortcuts {
    padding-inline: 12px;
  }

  .message-row {
    grid-template-columns: 50px minmax(0, 1fr) auto;
    gap: 12px;
    padding-inline: 14px;
  }

  .message-row__avatar,
  .message-row__icon {
    width: 50px;
    height: 50px;
  }
}
</style>
