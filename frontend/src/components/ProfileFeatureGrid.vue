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

    <div class="profile-feature-grid__grid">
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
        <span class="profile-feature-grid__copy">
          <span class="profile-feature-grid__label">{{ entry.label }}</span>
          <span v-if="entry.description" class="profile-feature-grid__desc">
            {{ entry.description }}
          </span>
        </span>
        <van-icon name="arrow" class="profile-feature-grid__arrow" />
      </button>
    </div>
  </AppCard>
</template>

<script setup lang="ts">
import AppCard from './AppCard.vue'

export interface ProfileGridEntry {
  label: string
  description?: string
  icon: string
  to?: string
  disabledFeature?: string
}

defineProps<{
  title: string
  entries: ProfileGridEntry[]
}>()

defineEmits<{
  'item-click': [entry: ProfileGridEntry]
}>()
</script>

<style scoped>
.profile-feature-grid__title {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.4;
}

.profile-feature-grid__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.profile-feature-grid__item {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  color: var(--color-text-secondary);
  text-align: left;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  background: var(--color-bg-surface);
  cursor: pointer;
  min-height: 82px;
  font-size: var(--font-size-sm);
  font-family: inherit;
  padding: 12px 28px 12px 12px;
  overflow: hidden;
}

.profile-feature-grid__item:active {
  background: var(--color-primary-light);
  border-color: rgba(16, 196, 104, 0.26);
}

.profile-feature-grid__icon {
  width: 34px;
  height: 34px;
  min-width: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  background: var(--color-primary-light);
  font-size: 20px;
}

.profile-feature-grid__copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.profile-feature-grid__label {
  color: var(--color-text-primary);
  font-size: var(--font-size-md);
  font-weight: 700;
  line-height: 1.35;
}

.profile-feature-grid__desc {
  color: var(--color-text-hint);
  font-size: var(--font-size-sm);
  line-height: 1.35;
}

.profile-feature-grid__arrow {
  position: absolute;
  right: 10px;
  top: 12px;
  color: var(--color-text-placeholder);
  font-size: 14px;
}

@media (max-width: 340px) {
  .profile-feature-grid__grid {
    grid-template-columns: 1fr;
  }
}
</style>
