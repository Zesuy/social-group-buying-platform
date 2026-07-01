<template>
  <div :class="['app-notice-strip', `app-notice-strip--${variant}`]">
    <van-icon v-if="showIcon" :name="iconName" size="14" class="app-notice-strip__icon" />
    <span class="app-notice-strip__text">
      <slot>{{ text }}</slot>
    </span>
    <button
      v-if="actionLabel"
      type="button"
      class="app-notice-strip__action"
      @click="$emit('action')"
    >
      {{ actionLabel }}
    </button>
    <button
      v-if="closable"
      type="button"
      class="app-notice-strip__close"
      aria-label="关闭"
      @click="$emit('close')"
    >
      <van-icon name="cross" />
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  text?: string
  variant?: 'info' | 'warning' | 'success'
  showIcon?: boolean
  actionLabel?: string
  closable?: boolean
}>()

defineEmits<{
  action: []
  close: []
}>()

const iconName = computed(() => {
  switch (props.variant) {
    case 'warning': return 'volume-o'
    case 'success': return 'success'
    default: return 'info-o'
  }
})
</script>

<style scoped>
.app-notice-strip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 14px;
  font-size: var(--font-size-sm);
  font-weight: 700;
  line-height: 1.35;
}

.app-notice-strip--info {
  background: #fff5df;
  color: #f26b2c;
}

.app-notice-strip--warning {
  background: #fff7e8;
  color: #e86a2b;
}

.app-notice-strip--success {
  background: #ecfdf3;
  color: #0c9e55;
}

.app-notice-strip__icon {
  flex-shrink: 0;
}

.app-notice-strip__text {
  flex: 1;
  min-width: 0;
}

.app-notice-strip__action {
  border: 0;
  background: var(--color-primary);
  color: #fff;
  border-radius: 999px;
  padding: 5px 12px;
  min-height: 34px;
  font-weight: 800;
  flex-shrink: 0;
  font-size: var(--font-size-sm);
  cursor: pointer;
  font-family: inherit;
}

.app-notice-strip__close {
  width: 32px;
  min-width: 32px;
  min-height: 32px;
  border: 0;
  background: transparent;
  color: inherit;
  opacity: 0.6;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
</style>
