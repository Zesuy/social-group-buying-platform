<template>
  <div class="reminder-banner" :class="`reminder-banner--${type}`">
    <van-icon :name="iconName" class="reminder-banner__icon" />
    <span class="reminder-banner__text">
      <slot>{{ text }}</slot>
    </span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  type?: 'info' | 'warning' | 'success'
  text?: string
}>(), {
  type: 'info',
  text: '',
})

const iconName = computed(() => {
  const map = { info: 'info-o', warning: 'warning-o', success: 'success' }
  return map[props.type]
})
</script>

<style scoped>
.reminder-banner {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  margin: 0 14px 12px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.5;
  font-weight: 700;
}

.reminder-banner--info {
  background-color: var(--color-primary-light);
  color: var(--color-primary-dark);
}

.reminder-banner--warning {
  background-color: #fff5df;
  color: #f26b2c;
}

.reminder-banner--success {
  background-color: #f0faf0;
  color: #1a7b3a;
}

.reminder-banner__icon {
  margin-right: 8px;
  font-size: 16px;
  flex-shrink: 0;
}

.reminder-banner__text {
  flex: 1;
}
</style>
