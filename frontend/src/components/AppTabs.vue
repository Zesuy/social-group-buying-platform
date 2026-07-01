<template>
  <div class="app-tabs" :class="{ 'app-tabs--scrollable': scrollable }">
    <button
      v-for="t in tabs"
      :key="t.key"
      :class="['app-tabs__item', { 'app-tabs__item--active': t.key === active }]"
      @click="$emit('change', t.key)"
    >
      {{ t.label }}
    </button>
  </div>
</template>

<script setup lang="ts">
export interface AppTab {
  key: string
  label: string
}

defineProps<{
  tabs: AppTab[]
  active?: string
  scrollable?: boolean
}>()

defineEmits<{
  change: [key: string]
}>()
</script>

<style scoped>
.app-tabs {
  display: flex;
  gap: 0;
  background: var(--color-bg-card);
  min-height: 54px;
  border-bottom: 1px solid var(--color-border);
}

.app-tabs__item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 54px;
  padding: 0 12px;
  border: 0;
  background: transparent;
  font-size: var(--font-size-md);
  font-weight: 700;
  color: var(--color-text-secondary);
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
  font-family: inherit;
}

.app-tabs__item--active {
  color: var(--color-text-primary);
  font-weight: 900;
}

.app-tabs__item--active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 3px;
  background: var(--color-primary);
  border-radius: 3px 3px 0 0;
}

.app-tabs--scrollable {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  gap: 0;
}

.app-tabs--scrollable .app-tabs__item {
  flex: none;
  padding: 0 20px;
}
</style>
