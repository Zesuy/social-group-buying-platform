<template>
  <AppCard clickable :flush="true" @click="$emit('open', notification)">
    <div class="notification-item" :class="{ 'notification-item--read': notification.readStatus === 'read' }">
      <div :class="['notification-item__icon', `notification-item__icon--${tone}`]">
        <van-icon :name="iconName" />
        <span v-if="notification.readStatus === 'unread'" class="notification-item__dot" />
      </div>
      <div class="notification-item__body">
        <div class="notification-item__title-row">
          <strong class="notification-item__title">{{ notification.title }}</strong>
          <span class="notification-item__time">{{ timeText }}</span>
        </div>
        <p class="notification-item__summary">{{ notification.summary }}</p>
      </div>
    </div>
  </AppCard>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AppCard from './AppCard.vue'
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
  display: grid;
  grid-template-columns: 44px 1fr;
  gap: 10px;
  padding: 14px;
  align-items: start;
}

.notification-item--read {
  opacity: 0.72;
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
  background: #fff2e8;
  color: #ff7a2f;
}

.notification-item__icon--blue {
  background: #edf5ff;
  color: #3c85e8;
}

.notification-item__icon--gray {
  background: #f2f4f5;
  color: #6b7280;
}

.notification-item__dot {
  position: absolute;
  right: -2px;
  top: -2px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #f25541;
  border: 2px solid var(--color-bg-card);
}

.notification-item__body {
  min-width: 0;
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
  font-size: var(--font-size-md);
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
  margin: 5px 0 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.45;
  word-break: break-word;
}

@media (max-width: 340px) {
  .notification-item__title-row {
    flex-direction: column;
    gap: 2px;
  }
}
</style>
