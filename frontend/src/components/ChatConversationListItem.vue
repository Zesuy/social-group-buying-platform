<template>
  <button type="button" class="chat-conversation-item" @click="$emit('open', conversation)">
    <ImageWithFallback
      :src="avatarUrl"
      :alt="avatarAlt"
      width="48px"
      height="48px"
      radius="14px"
      demo-kind="avatar"
    />
    <span class="chat-conversation-item__body">
      <span class="chat-conversation-item__head">
        <strong>{{ counterpart.title }}</strong>
        <span>{{ timeText }}</span>
      </span>
      <span class="chat-conversation-item__meta">
        {{ counterpart.subtitle }}
      </span>
      <span class="chat-conversation-item__summary">
        {{ summaryText }}
      </span>
    </span>
    <span v-if="conversation.unreadCount > 0" class="chat-conversation-item__badge">
      {{ unreadText }}
    </span>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import ImageWithFallback from './ImageWithFallback.vue'
import { formatDateTime, getChatCounterpart, getChatMessageSummary } from '@/utils'
import type { ChatConversationData } from '@/types'

const props = defineProps<{
  conversation: ChatConversationData
}>()

defineEmits<{
  open: [conversation: ChatConversationData]
}>()

const counterpart = computed(() => getChatCounterpart(props.conversation))
const avatarUrl = computed(() => counterpart.value.avatarUrl)
const avatarAlt = computed(() => counterpart.value.title)
const unreadText = computed(() => props.conversation.unreadCount > 99 ? '99+' : String(props.conversation.unreadCount))
const timeText = computed(() => props.conversation.lastMessageAt ? formatDateTime(props.conversation.lastMessageAt) : '')

const summaryText = computed(() => {
  return getChatMessageSummary(props.conversation)
})
</script>

<style scoped>
.chat-conversation-item {
  width: 100%;
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  min-height: 76px;
  padding: 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  background: var(--color-bg-card);
  box-shadow: var(--shadow-card);
  color: inherit;
  text-align: left;
  font-family: inherit;
  cursor: pointer;
}

.chat-conversation-item__body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.chat-conversation-item__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.chat-conversation-item__head strong {
  min-width: 0;
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
  font-weight: 800;
  line-height: 1.35;
  word-break: break-word;
}

.chat-conversation-item__head span {
  flex-shrink: 0;
  color: var(--color-text-hint);
  font-size: var(--font-size-xs);
  line-height: 1.5;
}

.chat-conversation-item__meta,
.chat-conversation-item__summary {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.45;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-conversation-item__summary {
  color: var(--color-text-hint);
}

.chat-conversation-item__badge {
  min-width: 22px;
  height: 22px;
  border-radius: 999px;
  padding: 0 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--color-price);
  color: #fff;
  font-size: var(--font-size-xs);
  font-weight: 800;
}

@media (max-width: 340px) {
  .chat-conversation-item {
    grid-template-columns: 44px minmax(0, 1fr);
  }

  .chat-conversation-item__badge {
    grid-column: 2;
    justify-self: flex-start;
  }

  .chat-conversation-item__head {
    flex-direction: column;
    gap: 1px;
  }
}
</style>
