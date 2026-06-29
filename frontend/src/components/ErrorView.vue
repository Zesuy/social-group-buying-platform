<template>
  <div class="error-view">
    <van-icon name="fail" :size="iconSize" :color="iconColor" />
    <p class="error-view__message">{{ message }}</p>
    <van-button
      v-if="showRetry"
      type="primary"
      size="small"
      plain
      @click="$emit('retry')"
    >
      重新加载
    </van-button>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  message?: string
  showRetry?: boolean
  iconSize?: string
  iconColor?: string
}>(), {
  message: '加载失败，请稍后重试',
  showRetry: true,
  iconSize: '56px',
  iconColor: 'var(--color-text-hint)',
})

defineEmits<{
  retry: []
}>()
</script>

<style scoped>
.error-view {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 16px;
  min-height: 200px;
}

.error-view__message {
  margin: 16px 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-md);
  text-align: center;
}
</style>
