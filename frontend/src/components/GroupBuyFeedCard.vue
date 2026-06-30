<template>
  <article class="group-buy-feed-card" @click="$emit('click')">
    <div class="group-buy-feed-card__info">
      <div class="group-buy-feed-card__header">
        <div class="group-buy-feed-card__store-row">
          <img
            v-if="item.leader.avatarUrl"
            :src="item.leader.avatarUrl"
            class="group-buy-feed-card__avatar"
            :alt="`${item.leader.displayName}头像`"
          />
          <span v-else class="group-buy-feed-card__avatar group-buy-feed-card__avatar--fallback">
            {{ avatarText }}
          </span>
          <div class="group-buy-feed-card__store-info">
            <span class="group-buy-feed-card__leader-name van-ellipsis">
              {{ item.leader.displayName }}
            </span>
            <span class="group-buy-feed-card__store-name van-ellipsis">{{ item.store.name }}</span>
          </div>
        </div>
        <button
          type="button"
          class="group-buy-feed-card__subscribe"
          @click.stop="$emit('subscribe')"
        >
          +订阅
        </button>
      </div>

      <h3 class="group-buy-feed-card__title">{{ item.title }}</h3>

      <div class="group-buy-feed-card__trust-line">
        <span class="group-buy-feed-card__tag">回复超快</span>
        <span class="group-buy-feed-card__tag group-buy-feed-card__tag--orange">
          回头客多
        </span>
      </div>

      <div class="group-buy-feed-card__tags">
        <span class="group-buy-feed-card__tag">全国可购</span>
        <span class="group-buy-feed-card__hot-badge">
          {{ isEnded ? '已结束' : `${watchCount}人在抢` }}
        </span>
      </div>

      <div class="group-buy-feed-card__price">
        <PriceText :amount="item.minPriceAmount" size="xl" color="var(--color-price)" />
      </div>

      <div class="group-buy-feed-card__image-row" aria-label="商品图">
        <ImageWithFallback
          :src="item.coverImageUrl"
          width="100%"
          height="116px"
          fit="cover"
          radius="7px"
          :alt="item.title"
        />
        <div class="group-buy-feed-card__fake-img" :class="imageTone">
          <span>商品图</span>
        </div>
        <div class="group-buy-feed-card__fake-img" :class="imageTone">
          <span>商品图</span>
        </div>
      </div>

      <div class="group-buy-feed-card__footer">
        <div class="group-buy-feed-card__social">
          <span class="group-buy-feed-card__faces" aria-hidden="true">
            <span>买</span>
            <span>团</span>
            <span>邻</span>
          </span>
          <b>{{ item.soldCount }}人已团｜{{ watchCount }}人看过</b>
        </div>
        <div class="group-buy-feed-card__actions">
          <button
            type="button"
            class="group-buy-feed-card__share"
            @click.stop="$emit('share')"
          >
            <van-icon name="share-o" size="16" />
            分享
          </button>
          <button type="button" class="group-buy-feed-card__cta" @click.stop="$emit('click')">
            去跟团
          </button>
        </div>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PublicGroupBuyItem } from '@/types'
import PriceText from './PriceText.vue'
import ImageWithFallback from './ImageWithFallback.vue'

const props = defineProps<{
  item: PublicGroupBuyItem
}>()

defineEmits<{
  click: []
  share: []
  subscribe: []
}>()

const isEnded = computed(() => props.item.status === 'ended')
const avatarText = computed(() => props.item.store.name.slice(0, 1) || props.item.leader.displayName.slice(0, 1))
const watchCount = computed(() => Math.max(props.item.soldCount + 76, 128))
const imageTone = computed(() => {
  const title = props.item.title
  if (/鞋|脚|喷剂|清爽/.test(title)) return 'group-buy-feed-card__fake-img--foot'
  if (/果|菜|鲜|橙|桃|肉/.test(title)) return 'group-buy-feed-card__fake-img--fresh'
  if (/衣|服|裤|衫/.test(title)) return 'group-buy-feed-card__fake-img--cloth'
  return 'group-buy-feed-card__fake-img--daily'
})
</script>

<style scoped>
.group-buy-feed-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-card);
  overflow: hidden;
  margin-bottom: var(--spacing-md);
  cursor: pointer;
  box-shadow: var(--shadow-card);
}

.group-buy-feed-card__info {
  padding: 14px;
}

.group-buy-feed-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.group-buy-feed-card__store-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  min-width: 0;
  flex: 1;
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
  gap: 5px;
  min-width: 0;
  flex: 1;
}

.group-buy-feed-card__leader-name {
  font-size: 19px;
  font-weight: 900;
  color: var(--color-text-primary);
  max-width: 240px;
}

.group-buy-feed-card__store-name {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  max-width: 240px;
}

.group-buy-feed-card__trust-line {
  display: flex;
  gap: 4px;
  overflow: hidden;
  margin: 0 0 6px;
}

.group-buy-feed-card__title {
  font-size: 19px;
  font-weight: 900;
  color: var(--color-text-primary);
  margin: 12px 0 6px;
  line-height: 1.35;
}

.group-buy-feed-card__tags {
  display: flex;
  gap: 6px;
  margin-bottom: 6px;
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

.group-buy-feed-card__hot-badge {
  font-size: var(--font-size-xs);
  color: #f06b2e;
  background: #fff0e8;
  border: 1px solid #ffc7ad;
  padding: 2px 6px;
  border-radius: 5px;
  font-weight: 800;
  white-space: nowrap;
}

.group-buy-feed-card__subscribe {
  border: 1px solid #aeeccd;
  background: #fff;
  color: var(--color-primary);
  border-radius: 9px;
  padding: 0 12px;
  min-height: 36px;
  font-weight: 800;
  white-space: nowrap;
  cursor: pointer;
}

.group-buy-feed-card__price {
  display: flex;
  align-items: center;
  min-height: 38px;
}

.group-buy-feed-card__image-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
  margin: 8px 0 12px;
}

.group-buy-feed-card__fake-img {
  min-height: 116px;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 900;
  overflow: hidden;
}

.group-buy-feed-card__fake-img span {
  background: rgba(0, 0, 0, 0.22);
  border-radius: 99px;
  padding: 4px 8px;
}

.group-buy-feed-card__fake-img--foot {
  background: linear-gradient(145deg, #cfddff, #ec715b);
}

.group-buy-feed-card__fake-img--fresh {
  background: linear-gradient(145deg, #ffd273, #55aa5d);
}

.group-buy-feed-card__fake-img--cloth {
  background: linear-gradient(145deg, #f8cadc, #9f89df);
}

.group-buy-feed-card__fake-img--daily {
  background: linear-gradient(145deg, #dcefe0, #a1c49f);
}

.group-buy-feed-card__footer,
.group-buy-feed-card__actions,
.group-buy-feed-card__social {
  display: flex;
  align-items: center;
}

.group-buy-feed-card__footer {
  justify-content: space-between;
  gap: 10px;
}

.group-buy-feed-card__social {
  min-width: 0;
  color: var(--color-primary);
  font-size: var(--font-size-sm);
}

.group-buy-feed-card__social b {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-buy-feed-card__faces {
  display: flex;
  margin-right: 8px;
}

.group-buy-feed-card__faces span {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #f8d6cd;
  border: 2px solid #fff;
  margin-left: -5px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-xs);
  color: #9b4e34;
  font-weight: 900;
}

.group-buy-feed-card__faces span:first-child {
  margin-left: 0;
}

.group-buy-feed-card__actions {
  gap: 6px;
  flex-shrink: 0;
}

.group-buy-feed-card__share,
.group-buy-feed-card__cta {
  border-radius: 9px;
  padding: 0 12px;
  min-height: 38px;
  font-size: var(--font-size-md);
  font-weight: 800;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
  transition: transform 0.18s ease, opacity 0.18s ease;
  white-space: nowrap;
}

.group-buy-feed-card__share {
  background: #fff;
  color: var(--color-primary);
  border: 1px solid #aeeccd;
}

.group-buy-feed-card__cta {
  background: var(--color-primary);
  color: #fff;
  border: 1px solid var(--color-primary);
}

.group-buy-feed-card__subscribe:active,
.group-buy-feed-card__share:active,
.group-buy-feed-card__cta:active {
  transform: scale(0.95);
  opacity: 0.9;
}

@media (max-width: 374px) {
  .group-buy-feed-card__footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .group-buy-feed-card__actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
