<template>
  <div class="merchant-page">
    <div class="page-head">
      <div>
        <p>订单上下文客服</p>
        <h1>客服会话</h1>
      </div>
      <button type="button" class="refresh-button" :disabled="loading" @click="loadChats">
        刷新会话
      </button>
    </div>

    <LoadingView v-if="loading && chats.length === 0" text="正在加载客服会话..." />
    <ErrorView v-else-if="error" :message="error" @retry="loadChats" />

    <section v-else class="chat-workbench">
      <aside class="chat-list" aria-label="买家会话列表">
        <div class="chat-list__head">
          <strong>买家咨询</strong>
          <span>{{ unreadTotal }} 条未读</span>
        </div>

        <div v-if="chats.length > 0" class="chat-list__body">
          <button
            v-for="chat in chats"
            :key="chat.id"
            type="button"
            class="conversation-row"
            :class="{ 'conversation-row--active': String(chat.id) === selectedConversationId }"
            @click="selectConversation(chat.id)"
          >
            <ImageWithFallback
              :src="counterpart(chat).avatarUrl"
              :alt="counterpart(chat).title"
              width="44px"
              height="44px"
              radius="12px"
              demo-kind="avatar"
            />
            <span class="conversation-row__body">
              <span class="conversation-row__head">
                <strong>{{ counterpart(chat).title }}</strong>
                <small>{{ chat.lastMessageAt ? formatDateTime(chat.lastMessageAt) : '' }}</small>
              </span>
              <span class="conversation-row__store">{{ chat.storeName || '当前店铺' }} · 关联订单咨询</span>
              <span class="conversation-row__summary">{{ messageSummary(chat) }}</span>
            </span>
            <mark v-if="chat.unreadCount > 0">{{ chat.unreadCount > 99 ? '99+' : chat.unreadCount }}</mark>
          </button>
        </div>

        <EmptyState v-else image="chat-o" description="暂无买家咨询" />
      </aside>

      <main class="chat-panel">
        <template v-if="selectedConversation">
          <header class="chat-panel__head">
            <ImageWithFallback
              :src="selectedCounterpart?.avatarUrl"
              :alt="selectedCounterpart?.title || '买家'"
              width="48px"
              height="48px"
              radius="12px"
              demo-kind="avatar"
            />
            <div>
              <p>当前沟通对象</p>
              <h2>{{ selectedCounterpart?.title }}</h2>
              <span>{{ selectedConversation.storeName }} · 订单上下文客服</span>
            </div>
          </header>

          <div class="order-context">
            <strong>关联订单咨询</strong>
            <span>仅用于下单后的履约沟通，不包含客服分配、撤回或评价。</span>
          </div>

          <LoadingView v-if="messagesLoading && messages.length === 0" text="正在加载消息..." />
          <ErrorView v-else-if="messagesError" :message="messagesError" @retry="loadSelectedMessages" />

          <div v-else ref="messageListRef" class="message-stream">
            <div
              v-for="message in messages"
              :key="message.id"
              class="message-item"
              :class="{
                'message-item--mine': message.mine,
                'message-item--system': message.senderRole === 'system',
              }"
            >
              <div
                class="message-bubble"
                :class="{ 'message-bubble--media': message.messageType === 'image' || message.messageType === 'card' }"
              >
                <p v-if="message.messageType === 'text'">{{ message.content }}</p>

                <button
                  v-else-if="message.messageType === 'image'"
                  type="button"
                  class="message-image"
                  aria-label="查看聊天图片"
                  @click="previewImage(message)"
                >
                  <ImageWithFallback
                    :src="message.imageUrl"
                    alt="聊天图片"
                    width="180px"
                    height="180px"
                    radius="10px"
                    demo-kind="cover"
                  />
                </button>

                <button
                  v-else-if="message.messageType === 'card'"
                  type="button"
                  class="message-card"
                  @click="openCard(message)"
                >
                  <span>{{ cardTag(message.cardType) }}</span>
                  <strong>{{ cardTitle(message) }}</strong>
                  <b v-if="cardAmount(message.cardPayload)">{{ cardAmount(message.cardPayload) }}</b>
                  <small>{{ message.cardPayload?.summary || '查看这笔订单的商品与履约信息' }}</small>
                </button>
              </div>
              <time>{{ formatDateTime(message.createdAt) }}</time>
            </div>

            <EmptyState v-if="messages.length === 0" image="chat-o" description="暂无聊天消息" />
          </div>

          <form class="composer" @submit.prevent="sendText">
            <button type="button" class="composer__icon" aria-label="发送图片" @click="chooseImage">
              <van-icon name="photo-o" />
            </button>
            <input
              ref="fileInputRef"
              class="composer__file"
              type="file"
              accept="image/jpeg,image/png,image/webp"
              @change="onFileSelected"
            />
            <input
              v-model="draft"
              type="text"
              placeholder="输入给买家的履约沟通内容"
              :disabled="sending"
            />
            <AppButton class="composer__send" variant="primary" :loading="sending" :disabled="!draft.trim()">
              发送
            </AppButton>
          </form>
        </template>

        <EmptyState v-else image="chat-o" description="选择左侧会话后开始处理买家咨询" />
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showImagePreview, showToast } from 'vant'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import EmptyState from '@/components/EmptyState.vue'
import ImageWithFallback from '@/components/ImageWithFallback.vue'
import AppButton from '@/components/AppButton.vue'
import {
  listChatConversations,
  listChatMessages,
  markChatRead,
  sendChatMessage,
} from '@/api/chats'
import { uploadImage } from '@/api/uploads'
import { useChatPolling } from '@/composables'
import {
  formatAmount,
  formatDateTime,
  getChatCounterpart,
  getChatMessageSummary,
  resolveDisplayImageUrl,
} from '@/utils'
import type { ChatCardPayload, ChatConversationData, ChatMessageData } from '@/types'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const error = ref('')
const messagesLoading = ref(false)
const messagesError = ref('')
const sending = ref(false)
const draft = ref('')
const chats = ref<ChatConversationData[]>([])
const messages = ref<ChatMessageData[]>([])
const messageListRef = ref<HTMLElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const { startPolling, stopPolling } = useChatPolling(5000)

const selectedConversationId = computed(() => route.params.conversationId as string | undefined)
const selectedConversation = computed(() => chats.value.find((item) => String(item.id) === selectedConversationId.value) || null)
const selectedCounterpart = computed(() => selectedConversation.value ? getChatCounterpart(selectedConversation.value) : null)
const unreadTotal = computed(() => chats.value.reduce((sum, item) => sum + item.unreadCount, 0))

function counterpart(chat: ChatConversationData) {
  return getChatCounterpart(chat)
}

function messageSummary(chat: ChatConversationData) {
  return getChatMessageSummary(chat)
}

async function loadChats() {
  loading.value = true
  error.value = ''
  try {
    const data = await listChatConversations({ role: 'leader', page: 1, pageSize: 30 })
    chats.value = data.items
    ensureSelectedConversation()
  } catch (err) {
    error.value = (err as { message?: string }).message || '客服会话加载失败'
  } finally {
    loading.value = false
  }
}

function ensureSelectedConversation() {
  if (chats.value.length === 0) {
    if (selectedConversationId.value) void router.replace('/merchant/chats')
    return
  }
  const exists = chats.value.some((item) => String(item.id) === selectedConversationId.value)
  if (!exists) {
    void router.replace(`/merchant/chats/${chats.value[0].id}`)
  }
}

function selectConversation(conversationId: string) {
  if (conversationId === selectedConversationId.value) return
  void router.push(`/merchant/chats/${conversationId}`)
}

async function loadSelectedMessages() {
  if (!selectedConversationId.value) return
  messagesLoading.value = true
  messagesError.value = ''
  stopPolling()
  try {
    const data = await listChatMessages(selectedConversationId.value, { pageSize: 30 })
    messages.value = data.items
    await markChatRead(selectedConversationId.value)
    chats.value = chats.value.map((item) => (
      String(item.id) === selectedConversationId.value ? { ...item, unreadCount: 0 } : item
    ))
    await nextTick()
    scrollToBottom()
    startPolling(pollNewMessages)
  } catch (err) {
    messagesError.value = (err as { message?: string }).message || '聊天消息加载失败'
  } finally {
    messagesLoading.value = false
  }
}

async function pollNewMessages() {
  if (!selectedConversationId.value) return
  const lastId = messages.value.at(-1)?.id
  if (!lastId) return
  const data = await listChatMessages(selectedConversationId.value, { afterMessageId: lastId, pageSize: 30 })
  const seen = new Set(messages.value.map((item) => item.id))
  messages.value = [...messages.value, ...data.items.filter((item) => !seen.has(item.id))]
  await markChatRead(selectedConversationId.value)
  await nextTick()
  scrollToBottom()
}

async function sendText() {
  if (!selectedConversationId.value) return
  const content = draft.value.trim()
  if (!content) return
  sending.value = true
  try {
    const sent = await sendChatMessage(selectedConversationId.value, {
      messageType: 'text',
      content,
      clientMessageId: createClientMessageId('text'),
    })
    messages.value.push(sent)
    draft.value = ''
    await nextTick()
    scrollToBottom()
  } catch (err) {
    showToast((err as { message?: string }).message || '发送失败')
  } finally {
    sending.value = false
  }
}

function chooseImage() {
  fileInputRef.value?.click()
}

async function onFileSelected(event: Event) {
  if (!selectedConversationId.value) return
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  sending.value = true
  try {
    const uploaded = await uploadImage(file)
    const sent = await sendChatMessage(selectedConversationId.value, {
      messageType: 'image',
      imageAssetId: uploaded.assetId,
      imageUrl: uploaded.url,
      clientMessageId: createClientMessageId('image'),
    })
    messages.value.push(sent)
    await nextTick()
    scrollToBottom()
  } catch (err) {
    showToast((err as { message?: string }).message || '图片发送失败')
  } finally {
    sending.value = false
  }
}

function openCard(message: ChatMessageData) {
  const url = message.cardPayload?.leaderActionUrl
  if (typeof url !== 'string' || !url.startsWith('/')) return
  const merchantUrl = url.replace(/^\/leader\/orders\//, '/merchant/orders/')
  void router.push(merchantUrl)
}

function previewImage(message: ChatMessageData) {
  const imageUrl = resolveDisplayImageUrl(message.imageUrl, '聊天图片', 'cover')
  if (!imageUrl) return
  showImagePreview({ images: [imageUrl], closeable: true })
}

function cardTag(type?: string | null) {
  const map: Record<string, string> = {
    order_created: '下单',
    order_paid: '支付',
    order_shipped: '发货',
    order_completed: '完成',
    prepare_done: '备货',
    order_summary: '订单',
  }
  return type ? map[type] || '订单' : '订单'
}

function cardTitle(message: ChatMessageData) {
  if (message.cardPayload?.title) return message.cardPayload.title
  if (message.content) return message.content
  return message.cardType === 'prepare_done' ? '备货完成' : '订单卡片'
}

function cardAmount(payload?: ChatCardPayload | null) {
  if (typeof payload?.payAmount !== 'number') return ''
  return formatAmount(payload.payAmount)
}

function scrollToBottom() {
  const el = messageListRef.value
  if (el) el.scrollTop = el.scrollHeight
}

function createClientMessageId(prefix: string) {
  return `${prefix}:${Date.now()}:${Math.random().toString(36).slice(2, 10)}`
}

watch(selectedConversationId, () => {
  messages.value = []
  draft.value = ''
  if (selectedConversationId.value) void loadSelectedMessages()
})

onMounted(async () => {
  await loadChats()
  if (selectedConversationId.value) await loadSelectedMessages()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.merchant-page {
  display: grid;
  gap: 16px;
  min-height: calc(100vh - 128px);
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-head p {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
}

.page-head h1 {
  margin: 4px 0 0;
  font-size: 26px;
}

.refresh-button {
  height: 36px;
  padding: 0 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  color: #374151;
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.chat-workbench {
  min-height: 620px;
  display: grid;
  grid-template-columns: minmax(280px, 360px) minmax(0, 1fr);
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  overflow: hidden;
}

.chat-list {
  min-width: 0;
  border-right: 1px solid #e5e7eb;
  background: #f9fafb;
}

.chat-list__head {
  min-height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 16px;
  border-bottom: 1px solid #e5e7eb;
}

.chat-list__head strong {
  color: #111827;
  font-size: 15px;
}

.chat-list__head span {
  color: #6b7280;
  font-size: 13px;
}

.chat-list__body {
  max-height: 680px;
  overflow-y: auto;
}

.conversation-row {
  width: 100%;
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  min-height: 92px;
  padding: 12px 14px;
  border: 0;
  border-bottom: 1px solid #eef2f7;
  background: transparent;
  color: inherit;
  text-align: left;
  font: inherit;
  cursor: pointer;
}

.conversation-row--active {
  background: #fff1ed;
}

.conversation-row__body {
  min-width: 0;
  display: grid;
  gap: 2px;
}

.conversation-row__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.conversation-row__head strong {
  min-width: 0;
  color: #111827;
  font-size: 14px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-row__head small,
.conversation-row__store,
.conversation-row__summary {
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
}

.conversation-row__head small {
  flex-shrink: 0;
}

.conversation-row__store,
.conversation-row__summary {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-row__summary {
  color: #9ca3af;
}

.conversation-row mark {
  min-width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
  border-radius: 999px;
  background: #e9563f;
  color: #ffffff;
  font-size: 12px;
  font-weight: 900;
}

.chat-panel {
  min-width: 0;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
  background: #ffffff;
}

.chat-panel__head {
  min-height: 76px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  border-bottom: 1px solid #e5e7eb;
}

.chat-panel__head p,
.chat-panel__head h2 {
  margin: 0;
}

.chat-panel__head p,
.chat-panel__head span,
.order-context span,
.message-item time {
  color: #6b7280;
  font-size: 13px;
}

.chat-panel__head h2 {
  margin-top: 2px;
  color: #111827;
  font-size: 20px;
  line-height: 1.3;
}

.order-context {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 10px 18px;
  border-bottom: 1px solid #eef2f7;
  background: #f9fafb;
}

.order-context strong {
  flex-shrink: 0;
  color: #374151;
  font-size: 13px;
}

.message-stream {
  min-height: 0;
  padding: 18px;
  overflow-y: auto;
  background: #f5f7fa;
}

.message-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  margin-bottom: 12px;
}

.message-item--mine {
  align-items: flex-end;
}

.message-item--system {
  align-items: center;
}

.message-bubble {
  max-width: min(560px, 76%);
  padding: 10px 12px;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 2px 10px rgba(31, 35, 41, 0.06);
  color: #111827;
}

.message-bubble p {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.message-item--mine .message-bubble {
  background: #07c160;
  color: #ffffff;
}

.message-bubble--media,
.message-item--mine .message-bubble--media {
  padding: 0;
  background: transparent;
  box-shadow: none;
  color: #111827;
}

.message-image,
.message-card {
  border: 0;
  padding: 0;
  background: transparent;
  cursor: pointer;
}

.message-card {
  width: 280px;
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  text-align: left;
}

.message-card span {
  width: fit-content;
  padding: 2px 8px;
  border-radius: 999px;
  background: #fff1ed;
  color: #d63f2b;
  font-size: 12px;
  font-weight: 900;
}

.message-card strong {
  color: #111827;
  font-size: 14px;
}

.message-card b {
  color: #e9563f;
  font-size: 18px;
}

.message-card small {
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
}

.composer {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) 88px;
  gap: 10px;
  align-items: center;
  padding: 12px 18px;
  border-top: 1px solid #e5e7eb;
  background: #ffffff;
}

.composer__icon {
  width: 40px;
  height: 40px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  color: #374151;
  cursor: pointer;
}

.composer__file {
  display: none;
}

.composer input[type='text'] {
  min-width: 0;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
  color: #111827;
  font: inherit;
  font-size: 14px;
}

.composer__send {
  min-width: 88px;
}

@media (max-width: 900px) {
  .chat-workbench {
    grid-template-columns: 1fr;
  }

  .chat-list {
    border-right: 0;
    border-bottom: 1px solid #e5e7eb;
  }
}
</style>
