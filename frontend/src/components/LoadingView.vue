<template>
  <div class="loading-view">
    <van-loading
      :size="size"
      :type="type"
      :color="color"
    >
      <template v-if="$slots.default">
        <slot />
      </template>
      <template v-else>
        <span class="loading-view__text">加载中...</span>
      </template>
    </van-loading>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  size?: string
  type?: 'circular' | 'spinner'
  color?: string
}>(), {
  size: '32px',
  type: 'circular',
  color: 'var(--color-primary)',
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

.loading-view__text {
  margin-top: 12px;
  color: var(--color-text-hint);
  font-size: var(--font-size-md);
}
</style>
