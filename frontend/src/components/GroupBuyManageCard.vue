<template>
  <AppCard :clickable="clickable" @click="$emit('click')">
    <template #header>
      <div class="group-buy-manage-card__header">
        <div class="group-buy-manage-card__title-row">
          <AppStatusPill
            :variant="statusVariant"
            size="sm"
          >
            {{ statusText }}
          </AppStatusPill>
          <span class="group-buy-manage-card__title van-multi-ellipsis--l2">
            {{ groupBuy.title }}
          </span>
        </div>
      </div>
    </template>

    <div class="group-buy-manage-card__body">
      <ImageWithFallback
        v-if="groupBuy.coverImageUrl"
        :src="groupBuy.coverImageUrl"
        class="group-buy-manage-card__cover"
        width="100%"
        height="120px"
        radius="8px"
      />
      <div class="group-buy-manage-card__info">
        <span class="group-buy-manage-card__label">配送方式：{{ deliveryText }}</span>
        <span class="group-buy-manage-card__label">
          结束时间：{{ formatDate(groupBuy.endTime) }}
        </span>
        <span class="group-buy-manage-card__stat">
          <PriceText :amount="minPriceAmount ?? 0" size="sm" suffix=" 起" />
        </span>
      </div>
    </div>

    <template v-if="$slots.actions" #footer>
      <slot name="actions" />
    </template>
  </AppCard>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { GroupBuyManageData } from '@/types'
import AppCard from './AppCard.vue'
import AppStatusPill from './AppStatusPill.vue'
import ImageWithFallback from './ImageWithFallback.vue'
import PriceText from './PriceText.vue'
import { getGroupBuyStatusText, getDeliveryTypeText } from '@/utils/status'
import { formatDate } from '@/utils/format'

const props = defineProps<{
  groupBuy: GroupBuyManageData
  clickable?: boolean
  minPriceAmount?: number | null
}>()

defineEmits<{
  click: []
}>()

const statusText = computed(() => getGroupBuyStatusText(props.groupBuy.status))
const deliveryText = computed(() => getDeliveryTypeText(props.groupBuy.deliveryType))

const statusVariant = computed(() => {
  switch (props.groupBuy.status) {
    case 'published': return 'green'
    case 'ended': return 'gray'
    default: return 'gray'
  }
})
</script>

<style scoped>
.group-buy-manage-card__header {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.group-buy-manage-card__title-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.group-buy-manage-card__title {
  flex: 1;
  min-width: 0;
  font-size: var(--font-size-md);
  font-weight: 800;
  color: var(--color-text-primary);
  line-height: 1.35;
}

.group-buy-manage-card__body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.group-buy-manage-card__cover {
  border-radius: 8px;
  overflow: hidden;
}

.group-buy-manage-card__info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.group-buy-manage-card__label {
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
}

.group-buy-manage-card__stat {
  font-size: var(--font-size-md);
  color: var(--color-price);
}
</style>
