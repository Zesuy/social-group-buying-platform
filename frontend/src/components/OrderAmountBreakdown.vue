<template>
  <div class="order-amount-breakdown">
    <div class="order-amount-breakdown__row">
      <span class="order-amount-breakdown__label">商品金额</span>
      <PriceText :amount="totalAmount" size="sm" color="var(--color-text-secondary)" />
    </div>
    <div v-if="(discountAmount ?? 0) > 0" class="order-amount-breakdown__row">
      <span class="order-amount-breakdown__label">优惠</span>
      <PriceText :amount="-(discountAmount ?? 0)" size="sm" color="var(--color-primary)" />
    </div>
    <div class="order-amount-breakdown__divider" />
    <div class="order-amount-breakdown__row order-amount-breakdown__row--total">
      <span class="order-amount-breakdown__label order-amount-breakdown__label--total">应付</span>
      <PriceText :amount="payAmount" size="xl" color="var(--color-price)" />
    </div>
  </div>
</template>

<script setup lang="ts">
import PriceText from './PriceText.vue'

defineProps<{
  totalAmount: number
  discountAmount?: number
  payAmount: number
}>()
</script>

<style scoped>
.order-amount-breakdown {
  background: var(--color-bg-card);
  border-radius: var(--radius-card);
  padding: 14px;
}

.order-amount-breakdown__row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
}

.order-amount-breakdown__label {
  font-size: var(--font-size-md);
  color: var(--color-text-secondary);
}

.order-amount-breakdown__divider {
  height: 1px;
  background: var(--color-border);
  margin: var(--spacing-sm) 0;
}

.order-amount-breakdown__row--total {
  margin-top: 0;
}

.order-amount-breakdown__label--total {
  font-weight: 900;
  color: var(--color-text-primary);
}
</style>
