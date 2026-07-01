<template>
  <AppCard>
    <template #header>
      <slot name="header">
        <span>商品信息</span>
      </slot>
    </template>

    <div class="order-snapshot-card__items">
      <div
        v-for="(item, idx) in items"
        :key="idx"
        class="order-snapshot-card__item"
      >
        <div class="order-snapshot-card__cover order-snapshot-card__cover--placeholder">
          {{ (item.productName || '商品').slice(0, 2) }}
        </div>
        <div class="order-snapshot-card__info">
          <div class="order-snapshot-card__name van-multi-ellipsis--l2">
            {{ item.productName }}
          </div>
          <div v-if="item.skuName" class="order-snapshot-card__spec">
            {{ item.skuName }}
          </div>
        </div>
        <div class="order-snapshot-card__amount">
          <PriceText :amount="item.unitPriceAmount" size="sm" />
          <span class="order-snapshot-card__qty">x{{ item.quantity }}</span>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="order-snapshot-card__total">
        <span class="order-snapshot-card__total-label">
          <slot name="total-label">合计</slot>
        </span>
        <PriceText :amount="totalAmount" size="md" color="var(--color-price)" />
      </div>
    </template>
  </AppCard>
</template>

<script setup lang="ts">
import type { OrderItemData } from '@/types'
import AppCard from './AppCard.vue'
import PriceText from './PriceText.vue'

defineProps<{
  items: OrderItemData[]
  totalAmount: number
}>()
</script>

<style scoped>
.order-snapshot-card__items {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.order-snapshot-card__item {
  display: flex;
  gap: 10px;
  align-items: center;
}

.order-snapshot-card__cover {
  flex-shrink: 0;
}

.order-snapshot-card__cover--placeholder {
  width: 64px;
  height: 64px;
  background: linear-gradient(145deg, #cfddff, #ec715b);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 900;
}

.order-snapshot-card__info {
  flex: 1;
  min-width: 0;
}

.order-snapshot-card__name {
  font-size: var(--font-size-md);
  font-weight: 800;
  color: var(--color-text-primary);
  line-height: 1.35;
}

.order-snapshot-card__spec {
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
  margin-top: 4px;
}

.order-snapshot-card__amount {
  text-align: right;
  flex-shrink: 0;
}

.order-snapshot-card__qty {
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
  display: block;
  margin-top: 2px;
}

.order-snapshot-card__total {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.order-snapshot-card__total-label {
  font-size: var(--font-size-md);
  color: var(--color-text-primary);
  font-weight: 700;
}
</style>
