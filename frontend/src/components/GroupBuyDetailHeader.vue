<template>
  <div class="group-buy-detail-header">
    <!-- 封面 -->
    <ImageWithFallback
      :src="groupBuy.coverImageUrl"
      class="group-buy-detail-header__cover"
      width="100%"
      height="200px"
      fit="cover"
    />

    <!-- 信息区 -->
    <div class="group-buy-detail-header__info">
      <h1 class="group-buy-detail-header__title">{{ groupBuy.title }}</h1>

      <div class="group-buy-detail-header__tags">
        <AppStatusPill
          :variant="groupBuy.status === 'published' ? 'green' : 'gray'"
          size="sm"
        >
          {{ statusText }}
        </AppStatusPill>
        <AppStatusPill variant="gray" size="sm">
          {{ deliveryText }}
        </AppStatusPill>
      </div>

      <div class="group-buy-detail-header__meta">
        <span v-if="minPrice != null" class="group-buy-detail-header__price">
          <PriceText :amount="minPrice" size="lg" suffix=" 起" />
        </span>
      </div>

      <div class="group-buy-detail-header__stats">
        <span v-if="soldCount != null">已售 {{ soldCount }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { GroupBuyDetail, PublicGroupBuyDetailItem } from '@/types'
import ImageWithFallback from './ImageWithFallback.vue'
import PriceText from './PriceText.vue'
import AppStatusPill from './AppStatusPill.vue'
import { getGroupBuyStatusText, getDeliveryTypeText } from '@/utils/status'

const props = defineProps<{
  groupBuy: GroupBuyDetail
  items?: PublicGroupBuyDetailItem[]
  minPrice?: number | null
  soldCount?: number | null
}>()

const statusText = computed(() => getGroupBuyStatusText(props.groupBuy.status))
const deliveryText = computed(() => getDeliveryTypeText(props.groupBuy.deliveryType))
</script>

<style scoped>
.group-buy-detail-header__cover {
  border-radius: 0;
  overflow: hidden;
}

.group-buy-detail-header__info {
  padding: 14px;
  background: var(--color-bg-card);
}

.group-buy-detail-header__title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 900;
  color: var(--color-text-primary);
  line-height: 1.3;
}

.group-buy-detail-header__tags {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
}

.group-buy-detail-header__meta {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 6px;
}

.group-buy-detail-header__price {
  color: var(--color-price);
}

.group-buy-detail-header__price-range {
  color: var(--color-text-hint);
  font-size: var(--font-size-sm);
}

.group-buy-detail-header__stats {
  display: flex;
  gap: 12px;
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
}
</style>
