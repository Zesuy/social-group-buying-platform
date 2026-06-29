<template>
  <div class="channel-tabs">
    <div class="channel-tabs__scroll">
      <div
        v-for="tab in tabs"
        :key="tab.key"
        class="channel-tabs__item"
        :class="{ 'channel-tabs__item--active': tab.key === active }"
        @click="$emit('change', tab.key)"
      >
        {{ tab.label }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  tabs?: Array<{ key: string; label: string }>
  active?: string
}>(), {
  tabs: () => [
    { key: 'recommend', label: '推荐' },
    { key: 'newest', label: '最新' },
    { key: 'popular', label: '热门' },
  ],
  active: 'recommend',
})

defineEmits<{ change: [key: string] }>()
</script>

<style scoped>
.channel-tabs {
  background: var(--color-bg-card);
  padding: 0 var(--spacing-md);
}

.channel-tabs__scroll {
  display: flex;
  gap: 0;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.channel-tabs__item {
  flex-shrink: 0;
  padding: var(--spacing-sm) var(--spacing-md);
  font-size: var(--font-size-md);
  color: var(--color-text-secondary);
  position: relative;
  min-height: var(--touch-size-min);
  display: flex;
  align-items: center;
  cursor: pointer;
}

.channel-tabs__item--active {
  color: var(--color-primary);
  font-weight: 600;
}

.channel-tabs__item--active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 3px;
  background: var(--color-primary);
  border-radius: 2px;
}
</style>
