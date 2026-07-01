<template>
  <div class="app-fixed-actions" :class="{ 'app-fixed-actions--single': single }">
    <slot />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  single?: boolean
}>()
</script>

<style scoped>
.app-fixed-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: var(--color-bg-card);
  border-top: 1px solid var(--color-border);
  padding: 10px 14px;
  padding-bottom: calc(16px + var(--safe-area-bottom));
  z-index: 100;
}

.app-fixed-actions--single {
  grid-template-columns: 1fr;
}

.app-fixed-actions :deep(.van-button) {
  height: 50px;
  font-size: 18px;
  border-radius: 8px;
}
</style>
