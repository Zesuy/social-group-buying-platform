<template>
  <div class="leader-trust-block" @click="$emit('click')">
    <div class="leader-trust-block__header">
      <img
        :src="leader.avatarUrl || undefined"
        class="leader-trust-block__avatar"
        alt=""
      />
      <div class="leader-trust-block__info">
        <div class="leader-trust-block__name-row">
          <span class="leader-trust-block__name">{{ leader.displayName }}</span>
          <span class="leader-trust-block__badge">金牌团长</span>
        </div>
        <div class="leader-trust-block__stats">
          <span class="leader-trust-block__stat">
            <van-icon name="friends-o" size="12" />
            粉丝 {{ leader.followerCount ?? 0 }}
          </span>
          <span class="leader-trust-block__dot">|</span>
          <span class="leader-trust-block__stat">
            <van-icon name="medal-o" size="12" />
            会员 {{ (leader as any).memberCount ?? 0 }}
          </span>
        </div>
        <div class="leader-trust-block__trust-tags">
          <span class="trust-tag">10万+人跟TA买</span>
          <span class="trust-tag">10万+条好评</span>
        </div>
      </div>
    </div>
    <div class="leader-trust-block__store">
      <van-icon name="shop" size="14" color="var(--color-primary)" />
      <span class="leader-trust-block__store-name van-ellipsis">
        {{ store.name }}
      </span>
      <van-icon name="arrow" size="12" color="var(--color-text-hint)" class="leader-trust-block__store-arrow" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { LeaderDetail, StoreDetail, LeaderHomepageLeader, LeaderHomepageStore } from '@/types'

defineProps<{
  leader: LeaderDetail | LeaderHomepageLeader
  store: StoreDetail | LeaderHomepageStore
}>()

defineEmits<{ click: [] }>()
</script>

<style scoped>
.leader-trust-block {
  margin: -24px 14px 12px;
  padding: 14px;
  background: var(--color-bg-card);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  cursor: pointer;
  position: relative;
  z-index: 2;
}

.leader-trust-block__header {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-md);
}

.leader-trust-block__avatar {
  width: 46px;
  height: 46px;
  border-radius: 10px;
  flex-shrink: 0;
  object-fit: cover;
}

.leader-trust-block__info {
  flex: 1;
  min-width: 0;
}

.leader-trust-block__name-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 2px;
}

.leader-trust-block__name {
  font-size: 19px;
  font-weight: 900;
  color: var(--color-text-primary);
}

.leader-trust-block__badge {
  font-size: var(--font-size-xs);
  color: #f36b2a;
  background: #fff5df;
  border: 1px solid #ffc49b;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 800;
}

.leader-trust-block__stats {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.leader-trust-block__stat {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.leader-trust-block__dot {
  color: var(--color-border);
}

.leader-trust-block__trust-tags {
  display: flex;
  gap: 6px;
  margin-top: var(--spacing-xs);
  flex-wrap: wrap;
}

.trust-tag {
  font-size: 11px;
  color: #f36b2a;
  background: #fff5df;
  border: 1px solid #ffc49b;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 800;
}

.leader-trust-block__store {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: var(--spacing-sm);
  padding-top: var(--spacing-sm);
  border-top: 1px solid var(--color-border-light);
}

.leader-trust-block__store-name {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  flex: 1;
}

.leader-trust-block__store-arrow {
  flex-shrink: 0;
}
</style>
