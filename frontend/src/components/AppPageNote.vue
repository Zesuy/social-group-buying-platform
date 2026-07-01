<template>
  <div :class="['app-page-note', `app-page-note--${variant}`]">
    <van-icon v-if="icon" :name="icon" size="14" class="app-page-note__icon" />
    <slot>{{ text }}</slot>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  text?: string
  variant?: 'info' | 'warning' | 'success'
  icon?: string
}>()
</script>

<style scoped>
.app-page-note {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  background: #ecfdf3;
  color: #0c9e55;
  border: 1px solid #b8efd0;
  border-radius: 12px;
  padding: 10px 12px;
  font-size: var(--font-size-sm);
  line-height: 1.45;
  margin-bottom: 12px;
}

.app-page-note--warning {
  background: #fff7e8;
  color: #e86a2b;
  border-color: #f5ead8;
}

.app-page-note--success {
  background: #ecfdf3;
  color: #0c9e55;
  border-color: #b8efd0;
}

.app-page-note__icon {
  flex-shrink: 0;
  margin-top: 2px;
}
</style>
