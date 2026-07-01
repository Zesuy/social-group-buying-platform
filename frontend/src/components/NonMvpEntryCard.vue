<template>
  <div
    :class="['non-mvp-entry-card', `non-mvp-entry-card--${variant}`]"
    @click="handleClick"
  >
    <div class="non-mvp-entry-card__icon">
      <van-icon :name="icon" :size="iconSize" />
    </div>
    <div class="non-mvp-entry-card__info">
      <div class="non-mvp-entry-card__title">{{ title }}</div>
      <div v-if="description" class="non-mvp-entry-card__desc">{{ description }}</div>
    </div>
    <div class="non-mvp-entry-card__arrow">
      <van-icon name="arrow" size="14" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { showToast } from 'vant'
import { isFeatureDisabled } from '@/utils/non-mvp'
import type { NonMvpFeature } from '@/utils/non-mvp'

const props = defineProps<{
  title: string
  icon?: string
  iconSize?: string
  description?: string
  variant?: 'card' | 'list' | 'pill'
  disabledFeature?: NonMvpFeature
  disabled?: boolean
  toast?: string
}>()

const isDisabled = computed(() => {
  if (props.disabled) return true
  if (props.disabledFeature && isFeatureDisabled(props.disabledFeature)) return true
  return false
})

function handleClick() {
  if (isDisabled.value) {
    showToast(props.toast || '即将开放，敬请期待')
    return
  }
  // Let parent handle navigation if needed
}
</script>

<style scoped>
.non-mvp-entry-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--color-bg-card);
  border-radius: var(--radius-card);
  padding: 14px;
  box-shadow: var(--shadow-card);
}

.non-mvp-entry-card--list {
  border-radius: 0;
  box-shadow: none;
  border-bottom: 1px solid var(--color-border);
}

.non-mvp-entry-card--pill {
  display: inline-flex;
  border-radius: 999px;
  padding: 6px 12px;
  box-shadow: none;
  background: #f2f4f5;
  font-size: var(--font-size-sm);
}

.non-mvp-entry-card__icon {
  flex-shrink: 0;
  opacity: 0.4;
}

.non-mvp-entry-card__info {
  flex: 1;
  min-width: 0;
}

.non-mvp-entry-card__title {
  font-size: var(--font-size-md);
  color: var(--color-text-hint);
  font-weight: 500;
}

.non-mvp-entry-card__desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-placeholder);
  margin-top: 2px;
}

.non-mvp-entry-card__arrow {
  color: var(--color-text-placeholder);
  flex-shrink: 0;
}
</style>
