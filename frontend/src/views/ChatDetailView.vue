<template>
  <PageLayout :title="pageTitle" show-back @back="goBack">
    <LoadingView v-if="loading && messages.length === 0" text="正在加载聊天..." />
    <ErrorView v-else-if="error" :message="error" @retry="reload" />

    <template v-else>
      <div ref="messageListRef" class="chat-detail">
        <div v-if="conversation" class="chat-detail__context">
          <ImageWithFallback
            :src="conversation.storeLogoUrl || conversation.leaderAvatarUrl"
            :alt="conversation.storeName"
            width="42px"
            height="42px"
            radius="12px"
            demo-kind="store"
          />
          <div>
            <strong>{{ conversation.storeName }}</strong>
            <p>{{ counterpartText }}</p>
          </div>
        </div>

        <div class="chat-detail__messages">
          <div
            v-for="message in messages"
            :key="message.id"
            class="chat-message"
            :class="{
              'chat-message--mine': message.mine,
              'chat-message--system': message.senderRole === 'system',
            }"
          >
            <div
              class="chat-message__bubble"
              :class="{
                'chat-message__bubble--media': message.messageType === 'image' || message.messageType === 'card',
              }"
            >
              <p v-if="message.messageType === 'text'" class="chat-message__text">
                {{ message.content }}
              </p>

              <button
                v-else-if="message.messageType === 'image'"
                type="button"
                class="chat-image-preview"
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
                <span>点击查看大图</span>
              </button>

              <button
                v-else-if="message.messageType === 'card'"
                type="button"
                class="chat-card"
                @click="openCard(message)"
              >
                <span class="chat-card__tag">{{ cardTag(message.cardType) }}</span>
                <span class="chat-card__main">
                  <strong>{{ cardTitle(message) }}</strong>
                  <b v-if="cardAmount(message.cardPayload)">{{ cardAmount(message.cardPayload) }}</b>
                </span>
                <span class="chat-card__summary">
                  {{ message.cardPayload?.summary || '正在咨询这笔订单的商品与履约信息' }}
                </span>
                <span
                  v-if="message.cardPayload?.orderNo || message.cardPayload?.orderStatus"
                  class="chat-card__meta"
                >
                  <small v-if="message.cardPayload?.orderNo">订单号：{{ message.cardPayload.orderNo }}</small>
                  <small v-if="message.cardPayload?.orderStatus">
                    {{ cardStatusText(message.cardPayload.orderStatus) }}
                  </small>
                </span>
                <span class="chat-card__action">
                  查看订单
                  <van-icon name="arrow" />
                </span>
              </button>
            </div>
            <span class="chat-message__time">{{ formatDateTime(message.createdAt) }}</span>
          </div>
        </div>
      </div>
    </template>

    <template #action>
      <div class="chat-composer">
        <button type="button" class="chat-composer__icon" aria-label="发送图片" @click="chooseImage">
          <van-icon name="photo-o" />
        </button>
        <input
          ref="fileInputRef"
          class="chat-composer__file"
          type="file"
          accept="image/jpeg,image/png,image/webp"
          @change="onFileSelected"
        />
        <input
          v-model="draft"
          class="chat-composer__input"
          type="text"
          placeholder="输入履约沟通内容"
          :disabled="sending"
          @keyup.enter="sendText"
        />
        <AppButton
          class="chat-composer__send"
          variant="primary"
          :loading="sending"
          :disabled="!draft.trim()"
          @click="sendText"
        >
          发送
        </AppButton>
      </div>
    </template>
  </PageLayout>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showImagePreview, showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import AppButton from '@/components/AppButton.vue'
import ImageWithFallback from '@/components/ImageWithFallback.vue'
import {
  listChatConversations,
  listChatMessages,
  markChatRead,
  sendChatMessage,
} from '@/api/chats'
import { uploadImage } from '@/api/uploads'
import { useChatPolling } from '@/composables'
import { formatAmount, formatDateTime, resolveDisplayImageUrl } from '@/utils'
import type { ChatCardPayload, ChatConversationData, ChatMessageData } from '@/types'

const route = useRoute()
const router = useRouter()
const conversationId = computed(() => route.params.conversationId as string)

const loading = ref(true)
const error = ref('')
const sending = ref(false)
const draft = ref('')
const messages = ref<ChatMessageData[]>([])
const conversation = ref<ChatConversationData | null>(null)
const messageListRef = ref<HTMLElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const { startPolling, stopPolling } = useChatPolling(5000)

const pageTitle = computed(() => conversation.value?.storeName || '联系团长')
const counterpartText = computed(() => {
  if (!conversation.value) return ''
  if (conversation.value.currentUserRole === 'leader') return `买家 ${conversation.value.buyerName}`
  return `团长 ${conversation.value.leaderName}`
})

async function loadConversation() {
  const data = await listChatConversations({ page: 1, pageSize: 50 })
  conversation.value = data.items.find((item) => item.id === conversationId.value) || null
}

async function loadMessages(afterMessageId?: string) {
  const data = await listChatMessages(conversationId.value, { afterMessageId, pageSize: 30 })
  if (afterMessageId) {
    const seen = new Set(messages.value.map((item) => item.id))
    messages.value = [...messages.value, ...data.items.filter((item) => !seen.has(item.id))]
  } else {
    messages.value = data.items
  }
  await nextTick()
  scrollToBottom()
}

async function pollNewMessages() {
  const lastId = messages.value.at(-1)?.id
  if (!lastId) return
  await loadMessages(lastId)
  await markChatRead(conversationId.value)
}

async function reload() {
  loading.value = true
  error.value = ''
  try {
    await Promise.all([loadConversation(), loadMessages()])
    await markChatRead(conversationId.value)
    startPolling(pollNewMessages)
  } catch (err) {
    error.value = (err as { message?: string }).message || '聊天加载失败'
  } finally {
    loading.value = false
  }
}

async function sendText() {
  const content = draft.value.trim()
  if (!content) return
  sending.value = true
  try {
    const sent = await sendChatMessage(conversationId.value, {
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
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  sending.value = true
  try {
    const uploaded = await uploadImage(file)
    const sent = await sendChatMessage(conversationId.value, {
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
  const payload = message.cardPayload
  if (!payload) return
  const url = conversation.value?.currentUserRole === 'leader'
    ? payload.leaderActionUrl
    : payload.buyerActionUrl
  if (typeof url === 'string' && url.startsWith('/')) {
    router.push(url)
  }
}

function previewImage(message: ChatMessageData) {
  const imageUrl = resolveDisplayImageUrl(message.imageUrl, '聊天图片', 'cover')
  if (!imageUrl) return
  showImagePreview({
    images: [imageUrl],
    closeable: true,
  })
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

function cardStatusText(status?: string) {
  const map: Record<string, string> = {
    pendingPay: '待支付',
    paid: '已支付',
    shipped: '已发货',
    completed: '已完成',
    canceled: '已取消',
  }
  return status ? map[status] || status : ''
}

function scrollToBottom() {
  const el = messageListRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

function createClientMessageId(prefix: string) {
  return `${prefix}:${Date.now()}:${Math.random().toString(36).slice(2, 10)}`
}

function goBack() {
  router.back()
}

onMounted(() => {
  void reload()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.chat-detail {
  min-height: 100%;
  padding: 12px 14px 88px;
  overflow-y: auto;
}

.chat-detail__context {
  display: grid;
  grid-template-columns: 42px 1fr;
  gap: 10px;
  align-items: center;
  padding: 12px;
  margin-bottom: 14px;
  border-radius: var(--radius-card);
  background: var(--color-primary-soft);
}

.chat-detail__context strong {
  display: block;
  color: var(--color-text-primary);
  font-size: var(--font-size-md);
  font-weight: 800;
}

.chat-detail__context p {
  margin: 2px 0 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.chat-detail__messages {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-message {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.chat-message--mine {
  align-items: flex-end;
}

.chat-message--system {
  align-items: center;
}

.chat-message__bubble {
  max-width: min(78vw, 360px);
  border-radius: 12px;
  padding: 10px 12px;
  background: var(--color-bg-card);
  box-shadow: var(--shadow-card);
  color: var(--color-text-primary);
}

.chat-message__bubble--media {
  padding: 0;
  background: transparent;
  box-shadow: none;
}

.chat-message--mine .chat-message__bubble {
  background: var(--color-primary);
  color: #fff;
}

.chat-message--mine .chat-message__bubble--media {
  background: transparent;
  color: var(--color-text-primary);
}

.chat-message--system .chat-message__bubble {
  max-width: 92%;
  padding: 0;
  background: transparent;
  box-shadow: none;
}

.chat-message__text {
  margin: 0;
  font-size: var(--font-size-md);
  line-height: 1.55;
  word-break: break-word;
}

.chat-message__time {
  color: var(--color-text-hint);
  font-size: var(--font-size-xs);
}

.chat-image-preview {
  position: relative;
  display: block;
  min-height: 180px;
  padding: 0;
  border: none;
  border-radius: 10px;
  background: transparent;
  overflow: hidden;
  cursor: pointer;
}

.chat-image-preview span {
  position: absolute;
  right: 8px;
  bottom: 8px;
  padding: 3px 8px;
  border-radius: 999px;
  background: rgba(17, 24, 39, 0.72);
  color: #fff;
  font-size: var(--font-size-xs);
  line-height: 1.4;
}

.chat-card {
  width: min(82vw, 360px);
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 120px;
  padding: 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  background: var(--color-bg-card);
  color: inherit;
  text-align: left;
  font-family: inherit;
  cursor: pointer;
}

.chat-card:active {
  transform: scale(0.98);
}

.chat-card__tag {
  align-self: flex-start;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--color-warning-soft);
  color: var(--color-warning);
  font-size: var(--font-size-xs);
  font-weight: 800;
}

.chat-card__main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.chat-card strong {
  min-width: 0;
  color: var(--color-text-primary);
  font-size: var(--font-size-md);
  font-weight: 800;
  line-height: 1.35;
}

.chat-card b {
  flex-shrink: 0;
  color: var(--color-price);
  font-size: var(--font-size-md);
  font-weight: 900;
}

.chat-card__summary,
.chat-card small {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.45;
}

.chat-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.chat-card__meta small {
  padding: 2px 7px;
  border-radius: 999px;
  background: var(--color-bg);
  color: var(--color-text-hint);
}

.chat-card__action {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 32px;
  padding-top: 6px;
  border-top: 1px solid var(--color-border);
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  font-weight: 800;
}

.chat-composer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 100;
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) 72px;
  gap: 8px;
  align-items: center;
  padding: 10px 14px;
  padding-bottom: calc(12px + var(--safe-area-bottom));
  border-top: 1px solid var(--color-border);
  background: var(--color-bg-card);
}

.chat-composer__icon {
  width: 44px;
  height: 44px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: #fff;
  color: var(--color-text-secondary);
  font-size: 20px;
}

.chat-composer__file {
  display: none;
}

.chat-composer__input {
  min-width: 0;
  height: 44px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  padding: 0 14px;
  background: var(--color-bg);
  color: var(--color-text-primary);
  font-size: var(--font-size-md);
  font-family: inherit;
}

.chat-composer__send {
  min-height: 44px;
  padding: 0 14px;
  border-radius: 999px;
}
</style>
