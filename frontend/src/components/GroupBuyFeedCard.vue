<template>
  <article class="group-buy-feed-card" @click="$emit('click')">
    <div v-if="showStoreHeader" class="group-buy-feed-card__leader" @click.stop="$emit('leader')">
      <img
        v-if="leaderAvatarUrl"
        :src="leaderAvatarUrl"
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
      <button
        type="button"
        class="group-buy-feed-card__subscribe"
        :class="{ 'group-buy-feed-card__subscribe--active': subscribed }"
        :disabled="subscribeLoading"
        @click.stop="$emit('subscribe')"
      >
        {{ subscribeText }}
      </button>
    </div>

    <div v-if="locationBadgeText" class="group-buy-feed-card__location">
      <van-icon name="location-o" size="14" />
      <span>{{ locationBadgeText }}</span>
    </div>

    <div class="group-buy-feed-card__body">
      <div class="group-buy-feed-card__image-wrap">
        <ImageWithFallback
          v-if="item.coverImageUrl"
          :src="item.coverImageUrl"
          width="108px"
          height="108px"
          fit="cover"
          radius="12px"
          :alt="item.title"
        />
        <div v-else class="group-buy-feed-card__image-fallback" aria-hidden="true">
          <van-icon name="photo-o" size="26" />
          <span>{{ fallbackImageText }}</span>
        </div>
      </div>

      <div class="group-buy-feed-card__content">
        <div class="group-buy-feed-card__status-row">
          <span class="group-buy-feed-card__status" :class="{ 'group-buy-feed-card__status--ended': isEnded }">
            {{ statusText }}
          </span>
          <span v-if="endTimeText" class="group-buy-feed-card__time">
            {{ endTimeText }}
          </span>
        </div>

        <h3 class="group-buy-feed-card__title">{{ item.title }}</h3>

        <p class="group-buy-feed-card__reason">{{ activityReasonText }}</p>

        <div class="group-buy-feed-card__signals">
          <span>{{ item.soldCount }}人已团</span>
          <span>{{ fulfillmentSignalText }}</span>
        </div>

        <div class="group-buy-feed-card__meta">
          <span>{{ watchCount }}人看过</span>
        </div>

        <div class="group-buy-feed-card__deal">
          <PriceText :amount="item.minPriceAmount" size="xl" color="var(--color-price)" />
          <button type="button" class="group-buy-feed-card__cta" @click.stop="$emit('click')">
            去跟团
          </button>
        </div>
      </div>
    </div>

    <div class="group-buy-feed-card__footer">
      <span>{{ footerText }}</span>
      <button type="button" class="group-buy-feed-card__share" @click.stop="$emit('share')">
        <van-icon name="share-o" size="15" />
        分享
      </button>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PublicGroupBuyItem } from '@/types'
import { resolveDisplayImageUrl } from '@/utils/demo-images'
import PriceText from './PriceText.vue'
import ImageWithFallback from './ImageWithFallback.vue'

const props = withDefaults(defineProps<{
  item: PublicGroupBuyItem
  subscribed?: boolean
  subscribeLoading?: boolean
  showLocationSignals?: boolean
  showStoreHeader?: boolean
}>(), {
  showLocationSignals: true,
  showStoreHeader: true,
})

defineEmits<{
  click: []
  share: []
  subscribe: []
  leader: []
}>()

const isEnded = computed(() => props.item.status === 'ended')
const avatarText = computed(() => props.item.store.name.slice(0, 1) || props.item.leader.displayName.slice(0, 1))
const watchCount = computed(() => Math.max(props.item.soldCount + 76, 128))
const leaderAvatarUrl = computed(() => resolveDisplayImageUrl(
  props.item.leader.avatarUrl,
  props.item.leader.displayName,
  'avatar',
))
const statusText = computed(() => {
  if (isEnded.value) return '已结束'
  if (props.item.status === 'published') return '正在开团'
  return props.item.status
})
const subscribeText = computed(() => {
  if (props.subscribeLoading) return '处理中'
  return props.subscribed ? '已订阅' : '订阅'
})
const locationBadgeText = computed(() => {
  if (props.showLocationSignals === false) return ''
  if (props.item.store.distanceText) return `距你 ${props.item.store.distanceText}`
  return ''
})
const fulfillmentSignalText = computed(() => {
  if (props.showLocationSignals !== false && props.item.store.distanceText) return '附近可履约'
  return '集中履约'
})
const showStoreHeader = computed(() => props.showStoreHeader !== false)
const fallbackImageText = computed(() => {
  if (/果|桃|鲜|菜|梨|莓|橙|瓜/.test(props.item.title)) return '生鲜团'
  if (/米|粮|油|蛋|肉|海鲜/.test(props.item.title)) return '食材团'
  return '社区团'
})
const activityReasonText = computed(() => {
  const title = props.item.title.replace(/团购|社区团|拼团/g, '').trim()
  if (title) return `${props.item.store.name}组织 ${title}，集中收单按约履约。`
  return '买的是商品，也是团长的服务承诺。'
})
const footerText = computed(() => (
  showStoreHeader.value ? '订阅团长，方便下次复购' : '当前团购由本店团长集中履约'
))
const endTimeText = computed(() => {
  if (!props.item.endTime) return ''
  const end = new Date(props.item.endTime)
  if (Number.isNaN(end.getTime())) return ''
  const now = Date.now()
  const diff = end.getTime() - now
  if (diff <= 0) return '已到结束时间'
  const hours = Math.ceil(diff / 1000 / 60 / 60)
  if (hours < 24) return `${hours}小时后结束`
  return `${Math.ceil(hours / 24)}天后结束`
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
  border: 1px solid rgba(237, 240, 242, 0.9);
}

.group-buy-feed-card__leader {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 12px 0;
  min-height: 54px;
}

.group-buy-feed-card__location {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: calc(100% - 24px);
  min-height: 26px;
  margin: 8px 12px 0;
  border: 1px solid rgba(16, 196, 104, 0.22);
  border-radius: var(--radius-pill);
  background: var(--color-primary-light);
  color: var(--color-primary-dark);
  padding: 0 9px;
  font-size: var(--font-size-xs);
  font-weight: 800;
  line-height: 1.2;
}

.group-buy-feed-card__location span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-buy-feed-card__body {
  display: flex;
  gap: 12px;
  padding: 12px;
}

.group-buy-feed-card__content {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
}

.group-buy-feed-card__status-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  min-width: 0;
}

.group-buy-feed-card__avatar {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  flex-shrink: 0;
  object-fit: cover;
}

.group-buy-feed-card__avatar--fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ff9827, #d87016);
  color: #fff;
  font-size: 17px;
  font-weight: 900;
}

.group-buy-feed-card__store-info {
  display: grid;
  gap: 3px;
  min-width: 0;
  flex: 1;
}

.group-buy-feed-card__leader-name {
  font-size: var(--font-size-lg);
  font-weight: 800;
  color: var(--color-text-primary);
  max-width: 240px;
  line-height: 1.25;
}

.group-buy-feed-card__store-name {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  max-width: 240px;
  line-height: 1.25;
}

.group-buy-feed-card__title {
  display: -webkit-box;
  margin: 7px 0 0;
  overflow: hidden;
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
  font-weight: 900;
  line-height: 1.38;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.group-buy-feed-card__status {
  flex-shrink: 0;
  border-radius: var(--radius-pill);
  background: var(--color-primary-light);
  color: var(--color-primary-dark);
  padding: 3px 8px;
  font-size: var(--font-size-xs);
  font-weight: 900;
  line-height: 1.2;
}

.group-buy-feed-card__status--ended {
  background: #f1f2f4;
  color: var(--color-text-secondary);
}

.group-buy-feed-card__time {
  min-width: 0;
  color: #f08a24;
  font-size: var(--font-size-xs);
  font-weight: 800;
  line-height: 1.2;
}

.group-buy-feed-card__signals,
.group-buy-feed-card__meta,
.group-buy-feed-card__deal,
.group-buy-feed-card__footer {
  display: flex;
  align-items: center;
}

.group-buy-feed-card__image-wrap {
  flex: 0 0 108px;
  width: 108px;
  height: 108px;
}

.group-buy-feed-card__image-fallback {
  width: 108px;
  height: 108px;
  border-radius: 12px;
  border: 1px solid rgba(16, 196, 104, 0.18);
  background:
    linear-gradient(135deg, rgba(232, 255, 242, 0.92), rgba(255, 245, 223, 0.76)),
    #fff;
  color: var(--color-primary-dark);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 7px;
  font-size: var(--font-size-sm);
  font-weight: 900;
}

.group-buy-feed-card__reason {
  display: -webkit-box;
  margin: 7px 0 0;
  overflow: hidden;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.38;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.group-buy-feed-card__signals {
  gap: 5px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.group-buy-feed-card__signals span {
  border-radius: 6px;
  background: #f7f8fa;
  color: var(--color-text-secondary);
  padding: 3px 6px;
  font-size: var(--font-size-xs);
  line-height: 1.2;
  font-weight: 700;
}

.group-buy-feed-card__meta {
  gap: 8px;
  margin-top: auto;
  padding-top: 8px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  line-height: 1.2;
}

.group-buy-feed-card__deal {
  justify-content: space-between;
  gap: 8px;
  margin-top: 6px;
}

.group-buy-feed-card__footer {
  justify-content: space-between;
  gap: 10px;
  border-top: 1px solid var(--color-border);
  padding: 9px 12px 10px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.35;
}

.group-buy-feed-card__footer span {
  min-width: 0;
}

.group-buy-feed-card__subscribe {
  border: 1px solid rgba(16, 196, 104, 0.28);
  background: #fff;
  color: var(--color-primary-dark);
  border-radius: var(--radius-pill);
  padding: 0 12px;
  min-height: 34px;
  font-size: var(--font-size-sm);
  font-weight: 800;
  white-space: nowrap;
  cursor: pointer;
}

.group-buy-feed-card__subscribe--active {
  background: var(--color-primary-light);
  color: var(--color-primary-dark);
}

.group-buy-feed-card__subscribe:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.group-buy-feed-card__share,
.group-buy-feed-card__cta {
  border-radius: var(--radius-pill);
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
  min-height: 32px;
  border: 1px solid var(--color-border);
  background: #fff;
  color: var(--color-text-secondary);
  padding: 0 10px;
  font-size: var(--font-size-sm);
  flex-shrink: 0;
}

.group-buy-feed-card__cta {
  min-height: 38px;
  background: var(--color-primary);
  color: #fff;
  border: 1px solid var(--color-primary);
  padding: 0 14px;
  font-size: var(--font-size-md);
}

.group-buy-feed-card__subscribe:active,
.group-buy-feed-card__share:active,
.group-buy-feed-card__cta:active {
  transform: scale(0.95);
  opacity: 0.9;
}

@media (max-width: 360px) {
  .group-buy-feed-card__body {
    gap: 10px;
  }
}
</style>
