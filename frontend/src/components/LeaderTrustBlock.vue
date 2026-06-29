<template>
  <div class="leader-trust-block">
    <div class="leader-trust-block__header">
      <img
        :src="leader.avatarUrl || undefined"
        class="leader-trust-block__avatar"
        alt=""
      />
      <div class="leader-trust-block__info">
        <span class="leader-trust-block__name">{{ leader.displayName }}</span>
        <div class="leader-trust-block__stats">
          <span>粉丝 {{ leader.followerCount ?? 0 }}</span>
          <template v-if="'memberCount' in leader">
            <span class="leader-trust-block__dot">|</span>
            <span>会员 {{ (leader as any).memberCount ?? 0 }}</span>
          </template>
        </div>
      </div>
    </div>
    <div class="leader-trust-block__store">
      <van-icon name="shop" size="14" color="var(--color-text-hint)" />
      <span class="leader-trust-block__store-name van-ellipsis">
        {{ store.name }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { LeaderDetail, StoreDetail, LeaderHomepageLeader, LeaderHomepageStore } from '@/types'

defineProps<{
  leader: LeaderDetail | LeaderHomepageLeader
  store: StoreDetail | LeaderHomepageStore
}>()
</script>

<style scoped>
.leader-trust-block {
  padding: var(--spacing-md) var(--spacing-lg);
  background: var(--color-bg-card);
}

.leader-trust-block__header {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.leader-trust-block__avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  flex-shrink: 0;
  object-fit: cover;
}

.leader-trust-block__info {
  flex: 1;
  min-width: 0;
}

.leader-trust-block__name {
  font-size: var(--font-size-lg);
  font-weight: 500;
  color: var(--color-text-primary);
}

.leader-trust-block__stats {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 2px;
  font-size: var(--font-size-xs);
  color: var(--color-text-hint);
}

.leader-trust-block__dot {
  color: var(--color-border);
}

.leader-trust-block__store {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: var(--spacing-xs);
  padding-top: var(--spacing-xs);
  border-top: 1px solid var(--color-border-light);
}

.leader-trust-block__store-name {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}
</style>
