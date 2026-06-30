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
  padding: 0 18px;
  height: 54px;
}

.channel-tabs__scroll {
  display: flex;
  gap: 28px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.channel-tabs__item {
  flex-shrink: 0;
  padding: 10px 0 13px;
  font-size: 18px;
  color: var(--color-text-secondary);
  position: relative;
  min-height: var(--touch-size-min);
  display: flex;
  align-items: center;
  cursor: pointer;
}

.channel-tabs__item--active {
  color: var(--color-text-primary);
  font-weight: 900;
}

.channel-tabs__item--active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  left: 6px;
  right: 6px;
  width: auto;
  height: 3px;
  background: var(--color-primary);
  border-radius: 2px;
}
</style>
