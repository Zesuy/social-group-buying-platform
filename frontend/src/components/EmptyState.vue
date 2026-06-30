<template>
  <div class="empty-state">
    <div class="empty-state__illustration">
      <van-icon :name="image === 'default' ? 'info-o' : image" :size="56" color="var(--color-primary)" />
    </div>
    <p class="empty-state__description">{{ description }}</p>
    <div v-if="$slots.default" class="empty-state__action">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  description?: string
  image?: string
}>(), {
  description: '暂无内容',
  image: 'default',
})
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 16px;
  min-height: 240px;
}

.empty-state__illustration {
  width: 120px;
  height: 100px;
  border-radius: 30px;
  background: linear-gradient(145deg, #e9eef2, #fff);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.empty-state__description {
  color: #a0a6ae;
  font-size: var(--font-size-md);
  margin-bottom: var(--spacing-lg);
  text-align: center;
}

.empty-state__action {
  display: flex;
  justify-content: center;
}
</style>
