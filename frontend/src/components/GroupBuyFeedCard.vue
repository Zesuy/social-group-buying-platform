<template>
  <div class="group-buy-feed-card" @click="$emit('click')">
    <div class="group-buy-feed-card__cover">
      <ImageWithFallback
        :src="item.coverImageUrl"
        width="100%"
        height="140px"
        fit="cover"
      />
      <span v-if="isEnded" class="group-buy-feed-card__ended-tag">已结束</span>
    </div>
    <div class="group-buy-feed-card__info">
      <h3 class="group-buy-feed-card__title van-ellipsis">{{ item.title }}</h3>
      <div class="group-buy-feed-card__price-row">
        <PriceText
          :amount="item.minPriceAmount"
          size="lg"
          color="var(--color-price)"
        />
        <span class="group-buy-feed-card__sold">
          已售 {{ item.soldCount }}
        </span>
      </div>
      <div class="group-buy-feed-card__store-row">
        <img
          v-if="item.leader.avatarUrl"
          :src="item.leader.avatarUrl"
          class="group-buy-feed-card__avatar"
          alt=""
        />
        <span class="group-buy-feed-card__leader-name van-ellipsis">
          {{ item.leader.displayName }}
        </span>
        <span class="group-buy-feed-card__store-name van-ellipsis">
          {{ item.store.name }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PublicGroupBuyItem } from '@/types'
import PriceText from './PriceText.vue'
import ImageWithFallback from './ImageWithFallback.vue'

const props = defineProps<{
  item: PublicGroupBuyItem
}>()

defineEmits<{ click: [] }>()

const isEnded = computed(() => props.item.status === 'ended')
</script>

<style scoped>
.group-buy-feed-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-card);
  overflow: hidden;
  margin-bottom: var(--spacing-md);
  cursor: pointer;
}

.group-buy-feed-card__cover {
  position: relative;
}

.group-buy-feed-card__ended-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 2px 8px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: var(--font-size-xs);
  border-radius: var(--radius-sm);
}

.group-buy-feed-card__info {
  padding: var(--spacing-sm) var(--spacing-md) var(--spacing-md);
}

.group-buy-feed-card__title {
  font-size: var(--font-size-lg);
  font-weight: 500;
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-xs);
  line-height: 1.4;
}

.group-buy-feed-card__price-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: var(--spacing-xs);
}

.group-buy-feed-card__sold {
  font-size: var(--font-size-xs);
  color: var(--color-text-hint);
}

.group-buy-feed-card__store-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.group-buy-feed-card__avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  flex-shrink: 0;
}

.group-buy-feed-card__leader-name {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  max-width: 80px;
}

.group-buy-feed-card__store-name {
  font-size: var(--font-size-xs);
  color: var(--color-text-hint);
  max-width: 100px;
}
</style>
