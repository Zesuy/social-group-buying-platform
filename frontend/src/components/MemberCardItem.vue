<template>
  <div class="member-card-item">
    <!-- 顶部：店铺 Logo + 名称 -->
    <div class="member-card-item__header">
      <div class="member-card-item__store-logo-wrap">
        <van-image
          v-if="storeLogoUrl"
          :src="storeLogoUrl"
          class="member-card-item__store-logo"
          round
          fit="cover"
        />
        <div v-else class="member-card-item__store-logo member-card-item__store-logo--fallback">
          {{ card.store.name.charAt(0) }}
        </div>
      </div>
      <div class="member-card-item__store-info">
        <div class="member-card-item__store-name">{{ card.store.name }}</div>
        <div class="member-card-item__leader">
          <van-image
            v-if="leaderAvatarUrl"
            :src="leaderAvatarUrl"
            class="member-card-item__leader-avatar"
            round
            fit="cover"
          />
          <div v-else class="member-card-item__leader-avatar member-card-item__leader-avatar--fallback">
            {{ card.leader.displayName.charAt(0) }}
          </div>
          <span>{{ card.leader.displayName }}</span>
        </div>
      </div>
      <!-- 等级标签 -->
      <van-tag
        v-if="card.levelName"
        type="primary"
        class="member-card-item__level"
      >
        {{ card.levelName }}
      </van-tag>
    </div>

    <!-- 统计信息 -->
    <div class="member-card-item__stats">
      <div class="member-card-item__stat">
        <div class="member-card-item__stat-label">成长值</div>
        <div class="member-card-item__stat-value">{{ card.growthValue }}</div>
      </div>
      <div class="member-card-item__stat">
        <div class="member-card-item__stat-label">消费金额</div>
        <div class="member-card-item__stat-value member-card-item__stat-value--price">
          {{ formatAmount(card.totalOrderAmount) }}
        </div>
      </div>
      <div class="member-card-item__stat">
        <div class="member-card-item__stat-label">订单数</div>
        <div class="member-card-item__stat-value">{{ card.totalOrders }}</div>
      </div>
    </div>

    <!-- 最近下单时间 -->
    <div class="member-card-item__footer">
      <van-icon name="clock-o" class="member-card-item__footer-icon" />
      <span v-if="card.lastOrderAt">最近下单：{{ formatDate(card.lastOrderAt) }}</span>
      <span v-else class="member-card-item__no-order">暂无下单记录</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { MemberCardData } from '@/types'
import { formatAmount, resolveDisplayImageUrl } from '@/utils'

const props = defineProps<{
  card: MemberCardData
}>()

const storeLogoUrl = computed(() => resolveDisplayImageUrl(
  props.card.store.logoUrl,
  props.card.store.name,
  'store',
))
const leaderAvatarUrl = computed(() => resolveDisplayImageUrl(
  props.card.leader.avatarUrl,
  props.card.leader.displayName,
  'avatar',
))

/**
 * 将 ISO 日期字符串格式化为 "YYYY-MM-DD HH:mm"
 */
function formatDate(dateStr: string | null): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
</script>

<style scoped>
.member-card-item {
  background: var(--color-bg-card);
  border-radius: var(--radius-card);
  overflow: hidden;
  box-shadow: var(--shadow-card);
  margin-bottom: var(--spacing-md);
}

.member-card-item__header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-bottom: 1px solid var(--color-border);
}

.member-card-item__store-logo-wrap {
  flex-shrink: 0;
}

.member-card-item__store-logo {
  width: 44px;
  height: 44px;
  display: block;
}

.member-card-item__store-logo--fallback {
  border-radius: 50%;
  background: var(--color-primary-light);
  color: var(--color-primary);
  font-weight: 900;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.member-card-item__store-info {
  flex: 1;
  min-width: 0;
}

.member-card-item__store-name {
  font-weight: 700;
  font-size: var(--font-size-lg);
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.member-card-item__leader {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.member-card-item__leader-avatar {
  width: 20px;
  height: 20px;
  display: block;
}

.member-card-item__leader-avatar--fallback {
  border-radius: 50%;
  background: var(--color-bg);
  color: var(--color-text-hint);
  font-weight: 700;
  font-size: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.member-card-item__level {
  flex-shrink: 0;
  font-weight: 700;
  padding: 2px 8px;
}

.member-card-item__stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  padding: 14px;
  gap: 8px;
}

.member-card-item__stat {
  text-align: center;
}

.member-card-item__stat-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-hint);
  margin-bottom: 4px;
}

.member-card-item__stat-value {
  font-size: var(--font-size-lg);
  font-weight: 700;
  color: var(--color-text-primary);
}

.member-card-item__stat-value--price {
  color: var(--color-price);
}

.member-card-item__footer {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 10px 14px;
  border-top: 1px solid var(--color-border);
  font-size: var(--font-size-xs);
  color: var(--color-text-hint);
}

.member-card-item__footer-icon {
  font-size: var(--font-size-sm);
}

.member-card-item__no-order {
  color: var(--color-text-hint);
}
</style>
