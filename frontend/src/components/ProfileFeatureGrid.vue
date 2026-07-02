<template>
  <AppCard>
    <template #header>
      <slot name="header">
        <h3 class="profile-feature-grid__title">{{ title }}</h3>
      </slot>
    </template>
    <template #header-right>
      <slot name="header-right" />
    </template>

    <div class="profile-feature-grid__grid" :class="`profile-feature-grid__grid--${columns}`">
      <button
        v-for="entry in entries"
        :key="entry.label"
        type="button"
        class="profile-feature-grid__item"
        @click="$emit('item-click', entry)"
      >
        <span class="profile-feature-grid__icon">
          <van-icon :name="entry.icon" />
        </span>
        <span class="profile-feature-grid__label">{{ entry.label }}</span>
      </button>
    </div>
  </AppCard>
</template>

<script setup lang="ts">
import AppCard from './AppCard.vue'

export interface ProfileGridEntry {
  label: string
  icon: string
  to?: string
  disabledFeature?: string
}

defineProps<{
  title: string
  entries: ProfileGridEntry[]
  columns?: 3 | 4 | 5
}>()

defineEmits<{
  'item-click': [entry: ProfileGridEntry]
}>()
</script>

<style scoped>
.profile-feature-grid__title {
  margin: 0;
  font-size: 18px;
  font-weight: 900;
  color: var(--color-text-primary);
}

.profile-feature-grid__grid {
  display: grid;
  gap: 18px 10px;
}

.profile-feature-grid__grid--4 {
  grid-template-columns: repeat(4, 1fr);
}

.profile-feature-grid__grid--5 {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.profile-feature-grid__grid--3 {
  grid-template-columns: repeat(3, 1fr);
}

.profile-feature-grid__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: var(--color-text-secondary);
  text-align: center;
  border: 0;
  background: transparent;
  cursor: pointer;
  min-height: 74px;
  font-size: 12px;
  font-family: inherit;
  padding: 0;
}

.profile-feature-grid__item:active {
  opacity: 0.7;
}

.profile-feature-grid__icon {
  width: 44px;
  height: 44px;
  min-height: 44px;
  border: 2px solid var(--color-border);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  background: var(--color-bg-card);
  font-size: 22px;
}

.profile-feature-grid__label {
  line-height: 1.2;
}
</style>
