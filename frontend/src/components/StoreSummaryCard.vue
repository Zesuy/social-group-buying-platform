<template>
  <div class="store-summary-card" @click="$emit('click')">
    <div class="store-summary-card__content">
      <img
        v-if="store.logoUrl"
        :src="store.logoUrl"
        class="store-summary-card__logo"
        alt=""
      />
      <div class="store-summary-card__info">
        <h3 class="store-summary-card__name">{{ store.name }}</h3>
        <p v-if="'description' in store && store.description" class="store-summary-card__desc van-ellipsis">
          {{ store.description }}
        </p>
      </div>
      <van-icon name="arrow" color="var(--color-text-hint)" size="16" class="store-summary-card__arrow" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { StoreDetail, LeaderHomepageStore } from '@/types'

defineProps<{
  store: StoreDetail | LeaderHomepageStore
}>()

defineEmits<{ click: [] }>()
</script>

<style scoped>
.store-summary-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-card);
  padding: var(--spacing-md) var(--spacing-lg);
  cursor: pointer;
}

.store-summary-card__content {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.store-summary-card__logo {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
  object-fit: cover;
}

.store-summary-card__info {
  flex: 1;
  min-width: 0;
}

.store-summary-card__name {
  font-size: var(--font-size-lg);
  font-weight: 500;
  color: var(--color-text-primary);
  margin-bottom: 2px;
}

.store-summary-card__desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
}

.store-summary-card__arrow {
  flex-shrink: 0;
}
</style>
