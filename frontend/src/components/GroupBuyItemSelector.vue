<template>
  <div
    :class="['group-buy-item-selector', { 'group-buy-item-selector--selected': selected }]"
    @click="$emit('select')"
  >
    <ImageWithFallback
      :src="item.coverImageUrl"
      class="group-buy-item-selector__image"
      width="64px"
      height="64px"
      radius="8px"
      :alt="item.displayName"
    />
    <div class="group-buy-item-selector__info">
      <div class="group-buy-item-selector__name van-multi-ellipsis--l2">
        {{ item.displayName }}
      </div>
      <div class="group-buy-item-selector__spec" v-if="item.displayName">
        {{ item.displayName }}
      </div>
      <div class="group-buy-item-selector__meta">
        <span class="group-buy-item-selector__price">
          <PriceText :amount="item.groupPriceAmount" size="sm" />
        </span>
        <span v-if="item.groupStock != null" class="group-buy-item-selector__stock">
          库存 {{ item.groupStock }}
        </span>
        <span v-if="item.soldCount != null" class="group-buy-item-selector__sales">
          已售 {{ item.soldCount }}
        </span>
      </div>
    </div>
    <div v-if="selected" class="group-buy-item-selector__check">
      <van-icon name="success" size="20" color="var(--color-primary)" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PublicGroupBuyDetailItem } from '@/types'
import ImageWithFallback from './ImageWithFallback.vue'
import PriceText from './PriceText.vue'

defineProps<{
  item: PublicGroupBuyDetailItem
  selected?: boolean
}>()

defineEmits<{
  select: []
}>()
</script>

<style scoped>
.group-buy-item-selector {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 12px;
  background: var(--color-bg-card);
  border-radius: 12px;
  margin-bottom: 8px;
  box-shadow: var(--shadow-card);
  cursor: pointer;
  border: 2px solid transparent;
  transition: border-color 0.2s;
}

.group-buy-item-selector--selected {
  border-color: var(--color-primary);
}

.group-buy-item-selector:active {
  opacity: 0.85;
}

.group-buy-item-selector__image {
  flex-shrink: 0;
}

.group-buy-item-selector__info {
  flex: 1;
  min-width: 0;
}

.group-buy-item-selector__name {
  font-size: var(--font-size-md);
  font-weight: 800;
  color: var(--color-text-primary);
  line-height: 1.35;
  margin-bottom: 2px;
}

.group-buy-item-selector__spec {
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
  margin-bottom: 2px;
}

.group-buy-item-selector__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.group-buy-item-selector__price {
  color: var(--color-price);
  font-weight: 700;
}

.group-buy-item-selector__stock,
.group-buy-item-selector__sales {
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
}

.group-buy-item-selector__check {
  flex-shrink: 0;
}
</style>
