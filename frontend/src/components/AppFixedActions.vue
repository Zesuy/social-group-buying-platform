<template>
  <div
    class="app-fixed-actions"
    :class="{
      'app-fixed-actions--single': single,
      'app-fixed-actions--h5-constrained': h5Constrained,
    }"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

defineProps<{
  single?: boolean
}>()

const route = useRoute()
const h5Constrained = computed(() => !(route.path ?? '').startsWith('/merchant'))
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

.app-fixed-actions--h5-constrained {
  right: 50%;
  left: auto;
  width: 100%;
  max-width: 480px;
  transform: translateX(50%);
}

.app-fixed-actions :deep(.van-button) {
  height: 50px;
  font-size: 18px;
  border-radius: 8px;
}
</style>
