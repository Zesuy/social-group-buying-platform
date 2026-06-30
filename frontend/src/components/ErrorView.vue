<template>
  <div class="error-view">
    <div class="error-view__illustration">
      <van-icon name="warn-o" :size="iconSize" :color="iconColor" />
    </div>
    <p class="error-view__message">{{ message }}</p>
    <van-button
      v-if="showRetry"
      type="primary"
      size="small"
      round
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
  iconSize: '36px',
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

.error-view__illustration {
  width: 120px;
  height: 100px;
  border-radius: 30px;
  background: #fff7e6;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.error-view__message {
  margin: 0 0 16px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-md);
  text-align: center;
  line-height: 1.5;
}
</style>
