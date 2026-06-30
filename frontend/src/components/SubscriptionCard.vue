<template>
  <div class="subscription-card">
    <div class="subscription-card__header">
      <!-- 团长头像 -->
      <div class="subscription-card__avatar-wrap">
        <van-image
          v-if="leader?.avatarUrl"
          :src="leader.avatarUrl"
          class="subscription-card__avatar"
          round
          fit="cover"
        />
        <div v-else class="subscription-card__avatar subscription-card__avatar--fallback">
          {{ displayName.charAt(0) || '?' }}
        </div>
      </div>

      <div class="subscription-card__info">
        <!-- 团长名称 -->
        <div class="subscription-card__name">
          {{ displayName }}
        </div>
        <!-- 店铺名称 -->
        <div class="subscription-card__store">
          <van-icon name="shop-o" class="subscription-card__store-icon" />
          {{ storeName }}
        </div>
        <!-- 订阅时间 -->
        <div class="subscription-card__time">
          <van-icon name="clock-o" class="subscription-card__time-icon" />
          {{ formatDate(subscription.subscribedAt) }}
        </div>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="subscription-card__actions">
      <van-button
        size="small"
        round
        plain
        type="primary"
        :loading="loading"
        @click="$emit('visit')"
      >
        访问主页
      </van-button>
      <van-button
        size="small"
        round
        plain
        :disabled="loading"
        class="subscription-card__unsubscribe"
        @click="$emit('unsubscribe')"
      >
        取消订阅
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { SubscriptionListItem } from '@/types'

const props = defineProps<{
  subscription: SubscriptionListItem
  loading?: boolean
}>()

defineEmits<{
  visit: []
  unsubscribe: []
}>()

const leader = computed(() => props.subscription.leader)
const displayName = computed(() => {
  return leader.value?.displayName || `团长 ${props.subscription.leaderId.slice(-6)}`
})
const storeName = computed(() => {
  return props.subscription.store?.name || `店铺 ${props.subscription.storeId.slice(-6)}`
})

/**
 * 将 ISO 日期字符串格式化为 "YYYY-MM-DD HH:mm"
 */
function formatDate(dateStr: string | null): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
</script>

<style scoped>
.subscription-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-card);
  padding: 14px;
  margin-bottom: var(--spacing-md);
  box-shadow: var(--shadow-card);
}

.subscription-card__header {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.subscription-card__avatar-wrap {
  flex-shrink: 0;
}

.subscription-card__avatar {
  width: 48px;
  height: 48px;
  display: block;
}

.subscription-card__avatar--fallback {
  border-radius: 50%;
  background: var(--color-primary-light);
  color: var(--color-primary);
  font-weight: 900;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.subscription-card__info {
  flex: 1;
  min-width: 0;
}

.subscription-card__name {
  font-weight: 700;
  font-size: var(--font-size-lg);
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.subscription-card__store {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.subscription-card__store-icon {
  font-size: var(--font-size-md);
}

.subscription-card__time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--font-size-xs);
  color: var(--color-text-hint);
}

.subscription-card__time-icon {
  font-size: var(--font-size-sm);
}

.subscription-card__actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  justify-content: flex-end;
}

.subscription-card__unsubscribe {
  --van-button-plain-border-color: var(--color-border);
  color: var(--color-text-secondary) !important;
  border-color: var(--color-border) !important;
}
</style>
