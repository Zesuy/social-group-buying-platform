<template>
  <div class="category-chips">
    <div class="category-chips__scroll">
      <span
        v-for="chip in chips"
        :key="chip.key"
        class="category-chips__item"
        :class="{ 'category-chips__item--active': chip.key === active }"
        @click="handleClick(chip.key)"
      >
        {{ chip.label }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  chips?: Array<{ key: string; label: string }>
  active?: string
}>(), {
  chips: () => [
    { key: 'all', label: '全部' },
    { key: 'fruit', label: '水果' },
    { key: 'veg', label: '蔬菜' },
    { key: 'meat', label: '肉禽' },
    { key: 'seafood', label: '海鲜' },
    { key: 'snack', label: '零食' },
    { key: 'drink', label: '饮品' },
    { key: 'daily', label: '日用' },
  ],
  active: 'all',
})

const emit = defineEmits<{ change: [key: string] }>()

function handleClick(key: string) {
  // MVP 阶段分类筛选仅做占位，不触发真实请求
  emit('change', key)
}
</script>

<style scoped>
.category-chips {
  background: var(--color-bg-card);
  padding: var(--spacing-xs) var(--spacing-md) var(--spacing-sm);
}

.category-chips__scroll {
  display: flex;
  gap: var(--spacing-sm);
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  white-space: nowrap;
}

.category-chips__item {
  flex-shrink: 0;
  padding: 4px 14px;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  background: var(--color-bg);
  border-radius: 16px;
  min-height: 30px;
  display: inline-flex;
  align-items: center;
  cursor: pointer;
}

.category-chips__item--active {
  color: var(--color-primary);
  background: var(--color-primary-light);
  font-weight: 500;
}
</style>
