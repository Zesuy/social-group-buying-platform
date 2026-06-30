<template>
  <div class="loading-view">
    <div class="loading-view__spinner">
      <van-loading
        :size="size"
        :type="type"
        :color="color"
      />
    </div>
    <p class="loading-view__text">
      <slot>{{ text }}</slot>
    </p>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  size?: string
  type?: 'circular' | 'spinner'
  color?: string
  text?: string
}>(), {
  size: '32px',
  type: 'circular',
  color: 'var(--color-primary)',
  text: '加载中...',
})
</script>

<style scoped>
.loading-view {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 16px;
  min-height: 200px;
}

.loading-view__spinner {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--color-primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.loading-view__text {
  color: var(--color-text-hint);
  font-size: var(--font-size-md);
}
</style>
