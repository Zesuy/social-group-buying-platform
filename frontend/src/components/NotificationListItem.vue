<template>
  <button
    type="button"
    class="notification-item"
    :class="{ 'notification-item--read': notification.readStatus === 'read' }"
    @click="$emit('open', notification)"
  >
    <span :class="['notification-item__icon', `notification-item__icon--${tone}`]">
      <van-icon :name="iconName" />
      <span v-if="notification.readStatus === 'unread'" class="notification-item__dot" />
    </span>
    <span class="notification-item__body">
      <span class="notification-item__title-row">
        <strong class="notification-item__title">{{ notification.title }}</strong>
        <span class="notification-item__time">{{ timeText }}</span>
      </span>
      <span class="notification-item__summary">{{ notification.summary }}</span>
      <span class="notification-item__footer">
        <span v-if="notification.readStatus === 'unread'" class="notification-item__status">
          未读
        </span>
        <span v-else class="notification-item__status notification-item__status--read">
          已读
        </span>
        <van-icon
          v-if="notification.actionUrl"
          name="arrow"
          class="notification-item__arrow"
        />
      </span>
    </span>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { NotificationData } from '@/types'
import { formatDateTime } from '@/utils'

const props = defineProps<{
  notification: NotificationData
}>()

defineEmits<{
  open: [notification: NotificationData]
}>()

const tone = computed(() => {
  if (props.notification.type === 'order_shipped') return 'blue'
  if (props.notification.type === 'order_paid') return 'orange'
  if (props.notification.type === 'group_buy_published') return 'green'
  return 'gray'
})

const iconName = computed(() => {
  if (props.notification.type === 'order_shipped') return 'logistics'
  if (props.notification.type === 'order_paid') return 'paid'
  if (props.notification.type === 'group_buy_published') return 'shop-o'
  if (props.notification.type === 'subscription_created') return 'friends-o'
  return 'notes-o'
})

const timeText = computed(() => formatDateTime(props.notification.createdAt))
</script>

<style scoped>
.notification-item {
  width: 100%;
  display: grid;
  grid-template-columns: 44px 1fr;
  gap: 12px;
  padding: 14px;
  align-items: start;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  background: var(--color-bg-card);
  box-shadow: var(--shadow-card);
  color: inherit;
  text-align: left;
  font-family: inherit;
  cursor: pointer;
}

.notification-item--read {
  background: rgba(255, 255, 255, 0.72);
}

.notification-item__icon {
  position: relative;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.notification-item__icon--green {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.notification-item__icon--orange {
  background: var(--color-price-light);
  color: var(--color-price);
}

.notification-item__icon--blue {
  background: var(--color-bg-surface);
  color: var(--color-primary-deep);
}

.notification-item__icon--gray {
  background: var(--color-bg-surface);
  color: var(--color-text-secondary);
}

.notification-item__dot {
  position: absolute;
  right: -2px;
  top: -2px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--color-price);
  border: 2px solid var(--color-bg-card);
}

.notification-item__body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.notification-item__title-row {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  justify-content: space-between;
  min-width: 0;
}

.notification-item__title {
  min-width: 0;
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
  font-weight: 700;
  line-height: 1.35;
  word-break: break-word;
}

.notification-item__time {
  flex-shrink: 0;
  color: var(--color-text-hint);
  font-size: var(--font-size-xs);
  line-height: 1.4;
}

.notification-item__summary {
  color: var(--color-text-secondary);
  font-size: var(--font-size-md);
  line-height: 1.5;
  word-break: break-word;
}

.notification-item__footer {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--color-text-hint);
  font-size: var(--font-size-sm);
}

.notification-item__status {
  color: var(--color-primary);
  font-weight: 700;
}

.notification-item__status--read {
  color: var(--color-text-hint);
  font-weight: 400;
}

.notification-item__arrow {
  margin-left: auto;
  color: var(--color-text-placeholder);
}

@media (max-width: 340px) {
  .notification-item__title-row {
    flex-direction: column;
    gap: 2px;
  }
}
</style>
