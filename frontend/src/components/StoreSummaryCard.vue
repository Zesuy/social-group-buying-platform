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
        <div class="store-summary-card__trust">
          <span class="store-summary-card__trust-item">金牌店铺</span>
          <span class="store-summary-card__trust-item store-summary-card__trust-item--warm">已认证</span>
        </div>
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
  padding: 14px;
  cursor: pointer;
  box-shadow: var(--shadow-card);
  border: 1px solid rgba(237, 240, 242, 0.72);
}

.store-summary-card__content {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.store-summary-card__logo {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  flex-shrink: 0;
  object-fit: cover;
}

.store-summary-card__info {
  flex: 1;
  min-width: 0;
}

.store-summary-card__name {
  font-size: 18px;
  font-weight: 900;
  color: var(--color-text-primary);
  margin-bottom: 2px;
}

.store-summary-card__trust {
  display: flex;
  gap: 6px;
  margin-bottom: 4px;
}

.store-summary-card__trust-item {
  font-size: var(--font-size-xs);
  color: var(--color-primary);
  background: #fff;
  border: 1px solid #b7edcf;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 800;
}

.store-summary-card__trust-item--warm {
  color: #f36b2a;
  background: #fff5df;
  border-color: #ffc49b;
}

.store-summary-card__desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
}

.store-summary-card__arrow {
  flex-shrink: 0;
}
</style>
