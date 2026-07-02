<template>
  <div class="leader-store-header">
    <!-- 头像区 -->
    <div class="leader-store-header__avatar-row">
      <ImageWithFallback
        :src="leader.avatarUrl"
        class="leader-store-header__avatar"
        width="56px"
        height="56px"
        radius="50%"
        demo-kind="avatar"
      />
      <div class="leader-store-header__info">
        <div class="leader-store-header__name-row">
          <span class="leader-store-header__name">{{ leader.displayName }}</span>
          <AppStatusPill variant="orange" size="sm">
            金牌团长
          </AppStatusPill>
        </div>
        <div class="leader-store-header__store-name" v-if="store">
          {{ store.name }}
        </div>
      </div>
    </div>

    <!-- 信任徽章 -->
    <div class="leader-store-header__badges">
      <span class="leader-store-header__badge">
        <van-icon name="medal-o" size="12" /> 已认证
      </span>
      <span class="leader-store-header__badge leader-store-header__badge--warm">
        10万+人跟TA买
      </span>
    </div>

    <!-- 入口按钮 -->
    <div class="leader-store-header__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
import ImageWithFallback from './ImageWithFallback.vue'
import AppStatusPill from './AppStatusPill.vue'
import type { LeaderDetail, LeaderHomepageLeader, LeaderHomepageStore } from '@/types'

defineProps<{
  leader: LeaderDetail | LeaderHomepageLeader
  store?: LeaderHomepageStore | null
}>()
</script>

<style scoped>
.leader-store-header {
  background: var(--color-bg-card);
  border-radius: var(--radius-card);
  padding: 14px;
  box-shadow: var(--shadow-card);
}

.leader-store-header__avatar-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.leader-store-header__avatar {
  flex-shrink: 0;
}

.leader-store-header__info {
  flex: 1;
  min-width: 0;
}

.leader-store-header__name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.leader-store-header__name {
  font-size: 18px;
  font-weight: 900;
  color: var(--color-text-primary);
}

.leader-store-header__store-name {
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
  margin-top: 2px;
}

.leader-store-header__badges {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.leader-store-header__badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  min-height: 24px;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid #b7edcf;
  background: #fff;
  color: var(--color-primary);
  font-size: 11px;
  font-weight: 800;
  white-space: nowrap;
}

.leader-store-header__badge--warm {
  border-color: #ffc49b;
  background: #fff5df;
  color: #f36b2a;
}

.leader-store-header__actions {
  display: flex;
  gap: 8px;
}
</style>
