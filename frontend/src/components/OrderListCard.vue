<template>
  <AppCard :clickable="clickable" @click="$emit('click')">
    <template #header>
      <div class="order-list-card__head">
        <span class="order-list-card__store">
          <span v-if="mode === 'leader' && buyerName" class="order-list-card__buyer">
            {{ buyerName }}
          </span>
          <span v-else class="order-list-card__order-no">订单号 {{ order.orderNo }}</span>
        </span>
        <AppStatusPill :variant="pillVariant" size="sm">
          {{ statusText }}
        </AppStatusPill>
      </div>
    </template>

    <div class="order-list-card__body">
      <div class="order-list-card__items">
        <div
          v-for="(item, idx) in displayItems"
          :key="idx"
          class="order-list-card__item"
        >
          <div class="order-list-card__item-cover order-list-card__item-cover--placeholder">
            {{ (item.productName || '商品').slice(0, 2) }}
          </div>
          <div class="order-list-card__item-info">
            <div class="order-list-card__item-name van-multi-ellipsis--l2">
              {{ item.productName || '团购商品' }}
            </div>
            <div class="order-list-card__item-meta">
              <span v-if="item.skuName">规格：{{ item.skuName }}｜</span>
              数量 x{{ item.quantity || 0 }}
            </div>
            <div class="order-list-card__item-price">
              <span>小计</span>
              <PriceText :amount="item.totalAmount || order.payAmount" size="sm" />
            </div>
          </div>
        </div>
      </div>

      <div v-if="order.items && order.items.length > 1" class="order-list-card__more">
        等{{ order.items.length }}件商品
      </div>

      <div class="order-list-card__amount-line">
        <span v-if="hasDiscount" class="order-list-card__discount">
          已优惠 <PriceText :amount="order.discountAmount" size="sm" color="var(--color-primary)" />
        </span>
        <span v-else class="order-list-card__discount">商品金额</span>
        <span class="order-list-card__pay">
          实付 <PriceText :amount="order.payAmount" size="md" color="var(--color-price)" />
        </span>
      </div>
    </div>

    <template v-if="actionButtons && actionButtons.length > 0" #footer>
      <div class="order-list-card__actions">
        <AppButton
          v-for="btn in actionButtons"
          :key="btn.text"
          :variant="btn.variant"
          :loading="btn.loading"
          @click.stop="btn.onClick"
        >
          {{ btn.text }}
        </AppButton>
      </div>
    </template>
  </AppCard>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { OrderData } from '@/types'
import AppCard from './AppCard.vue'
import AppStatusPill from './AppStatusPill.vue'
import AppButton from './AppButton.vue'
import PriceText from './PriceText.vue'
import { getOrderStatusText } from '@/utils/status'

const props = defineProps<{
  order: OrderData
  mode?: 'buyer' | 'leader'
  clickable?: boolean
  buyerName?: string
  actionButtons?: Array<{
    text: string
    variant: 'primary' | 'ghost' | 'danger' | 'success'
    onClick: () => void
    loading?: boolean
  }>
}>()

defineEmits<{
  click: []
}>()

const statusText = computed(() => getOrderStatusText(props.order.orderStatus))

const pillVariant = computed(() => {
  switch (props.order.orderStatus) {
    case 'pendingPay': return 'orange'
    case 'paid':
    case 'shipped': return 'green'
    case 'completed':
    case 'canceled': return 'gray'
    default: return 'gray'
  }
})

const displayItems = computed(() => {
  return props.order.items?.slice(0, 1) || []
})
const hasDiscount = computed(() => (props.order.discountAmount ?? 0) > 0)
</script>

<style scoped>
.order-list-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.order-list-card__store {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
}

.order-list-card__buyer {
  color: var(--color-text-hint);
  font-weight: 400;
}

.order-list-card__order-no {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  font-weight: 700;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-list-card__body {
  padding: 0;
}

.order-list-card__items {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.order-list-card__item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.order-list-card__item-cover {
  flex-shrink: 0;
}

.order-list-card__item-cover--placeholder {
  width: 72px;
  height: 72px;
  background: var(--color-primary-light);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary-dark);
  font-weight: 900;
  text-align: center;
  line-height: 1.2;
}

.order-list-card__item-info {
  flex: 1;
  min-width: 0;
}

.order-list-card__item-name {
  font-size: var(--font-size-md);
  font-weight: 800;
  color: var(--color-text-primary);
  line-height: 1.35;
}

.order-list-card__item-meta {
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
  margin-top: 4px;
}

.order-list-card__item-price {
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
}

.order-list-card__more {
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
  padding: 8px 0 0;
}

.order-list-card__amount-line {
  border-top: 1px solid var(--color-border);
  margin-top: 10px;
  padding-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.order-list-card__discount {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-list-card__pay {
  color: var(--color-text-primary);
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
}

.order-list-card__actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
</style>
