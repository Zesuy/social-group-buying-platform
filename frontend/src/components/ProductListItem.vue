<template>
  <div class="product-list-item" @click="$emit('click')">
    <ImageWithFallback
      :src="item.coverImageUrl"
      class="product-list-item__cover"
      width="72px"
      height="72px"
      radius="8px"
      :alt="item.name"
    />
    <div class="product-list-item__info">
      <div class="product-list-item__name van-multi-ellipsis--l2">
        {{ item.name }}
      </div>
      <div class="product-list-item__meta">
        <template v-if="item.stock != null">
          库存 {{ item.stock }}
        </template>
        <AppStatusPill
          v-if="item.status != null"
          :variant="item.status === 'active' ? 'green' : 'gray'"
          size="sm"
        >
          {{ item.status === 'active' ? '已上架' : '已下架' }}
        </AppStatusPill>
      </div>
      <div class="product-list-item__price">
        <PriceText :amount="item.basePriceAmount" size="md" />
      </div>
    </div>
    <div v-if="$slots.actions" class="product-list-item__actions" @click.stop>
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ProductData } from '@/types'
import ImageWithFallback from './ImageWithFallback.vue'
import PriceText from './PriceText.vue'
import AppStatusPill from './AppStatusPill.vue'

defineProps<{
  item: ProductData
}>()

defineEmits<{
  click: []
}>()
</script>

<style scoped>
.product-list-item {
  display: flex;
  gap: 12px;
  align-items: center;
  background: var(--color-bg-card);
  border-radius: 12px;
  padding: 13px;
  margin-bottom: 10px;
  box-shadow: var(--shadow-card);
  cursor: pointer;
}

.product-list-item:active {
  opacity: 0.85;
}

.product-list-item__cover {
  flex-shrink: 0;
}

.product-list-item__info {
  flex: 1;
  min-width: 0;
}

.product-list-item__name {
  font-size: var(--font-size-md);
  font-weight: 800;
  color: var(--color-text-primary);
  line-height: 1.35;
  margin-bottom: 4px;
}

.product-list-item__meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
  margin-bottom: 4px;
}

.product-list-item__price {
  font-weight: 700;
}

.product-list-item__actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
</style>
