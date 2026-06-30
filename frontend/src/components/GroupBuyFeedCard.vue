<template>
  <div class="group-buy-feed-card" @click="$emit('click')">
    <!-- Photo-first 封面区 -->
    <div class="group-buy-feed-card__cover">
      <ImageWithFallback
        :src="item.coverImageUrl"
        width="100%"
        height="160px"
        fit="cover"
        radius="var(--radius-card)"
      />
      <span v-if="isEnded" class="group-buy-feed-card__ended-tag">已结束</span>
      <span v-if="item.soldCount > 0" class="group-buy-feed-card__sold-badge">
        {{ item.soldCount }}人已团
      </span>
    </div>

    <div class="group-buy-feed-card__info">
      <!-- 团长/店铺信任行 -->
      <div class="group-buy-feed-card__store-row">
        <img
          v-if="item.leader.avatarUrl"
          :src="item.leader.avatarUrl"
          class="group-buy-feed-card__avatar"
          alt=""
        />
        <span v-else class="group-buy-feed-card__avatar group-buy-feed-card__avatar--fallback">
          {{ item.leader.displayName.slice(0, 1) }}
        </span>
        <div class="group-buy-feed-card__store-info">
          <span class="group-buy-feed-card__leader-name van-ellipsis">
            {{ item.leader.displayName }}
          </span>
          <span class="group-buy-feed-card__store-name van-ellipsis">
            {{ item.store.name }}
          </span>
        </div>
      </div>

      <!-- 商品标题 -->
      <h3 class="group-buy-feed-card__title van-multi-ellipsis--l2">{{ item.title }}</h3>

      <!-- 信任标签 -->
      <div class="group-buy-feed-card__tags">
        <span class="group-buy-feed-card__tag">回复超快</span>
        <span class="group-buy-feed-card__tag group-buy-feed-card__tag--orange">
          回头客多
        </span>
        <span class="group-buy-feed-card__tag">全国可购</span>
      </div>

      <!-- 价格 + 主 CTA -->
      <div class="group-buy-feed-card__price-row">
        <div class="group-buy-feed-card__price">
          <PriceText
            :amount="item.minPriceAmount"
            size="xl"
            color="var(--color-price)"
          />
        </div>
        <button class="group-buy-feed-card__cta">
          去跟团
        </button>
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
  box-shadow: var(--shadow-card);
  border: 1px solid rgba(237, 240, 242, 0.72);
}

/* ── 封面 ── */
.group-buy-feed-card__cover {
  position: relative;
  line-height: 0;
  padding: 6px 6px 0;
}

.group-buy-feed-card__ended-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 3px 10px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: var(--font-size-xs);
  border-radius: 99px;
  font-weight: 600;
}

.group-buy-feed-card__sold-badge {
  position: absolute;
  bottom: 8px;
  right: 8px;
  padding: 3px 10px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: var(--font-size-xs);
  border-radius: 99px;
  font-weight: 600;
}

/* ── 信息区 ── */
.group-buy-feed-card__info {
  padding: 12px 14px 14px;
}

/* ── 店铺行 ── */
.group-buy-feed-card__store-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: 8px;
}

.group-buy-feed-card__avatar {
  width: 46px;
  height: 46px;
  border-radius: 10px;
  flex-shrink: 0;
  object-fit: cover;
}

.group-buy-feed-card__avatar--fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ff9827, #d87016);
  color: #fff;
  font-size: 18px;
  font-weight: 900;
}

.group-buy-feed-card__store-info {
  display: grid;
  gap: 3px;
  min-width: 0;
  flex: 1;
}

.group-buy-feed-card__leader-name {
  font-size: 17px;
  font-weight: 900;
  color: var(--color-text-primary);
  max-width: 240px;
}

.group-buy-feed-card__store-name {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  max-width: 240px;
}

/* ── 标题 ── */
.group-buy-feed-card__title {
  font-size: 19px;
  font-weight: 900;
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-xs);
  line-height: 1.4;
}

/* ── 信任标签 ── */
.group-buy-feed-card__tags {
  display: flex;
  gap: 6px;
  margin-bottom: var(--spacing-sm);
  flex-wrap: wrap;
}

.group-buy-feed-card__tag {
  font-size: var(--font-size-xs);
  color: var(--color-primary);
  background: #fff;
  border: 1px solid #b7edcf;
  padding: 2px 8px;
  border-radius: 4px;
  white-space: nowrap;
}

.group-buy-feed-card__tag--orange {
  color: #f36b2a;
  background: #fff5df;
  border-color: #ffc49b;
}

/* ── 价格行 ── */
.group-buy-feed-card__price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--spacing-sm);
}

.group-buy-feed-card__price {
  display: flex;
  align-items: baseline;
}

.group-buy-feed-card__cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: var(--touch-size-min);
  padding: 0 22px;
  background: var(--color-primary);
  color: #fff;
  font-size: var(--font-size-md);
  font-weight: 600;
  border: none;
  border-radius: var(--button-capsule-radius);
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.group-buy-feed-card__cta:active {
  transform: scale(0.95);
  opacity: 0.9;
}
</style>
