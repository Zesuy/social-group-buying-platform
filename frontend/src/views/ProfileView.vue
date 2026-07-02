<template>
  <PageLayout show-tab-bar>
    <div class="app-topbar">我的</div>
    <div class="profile-content">
      <!-- 用户信息区 -->
      <div class="profile-header" @click="handleHeaderClick">
        <img
          v-if="isLoggedIn && profileAvatarUrl"
          :src="profileAvatarUrl"
          class="profile-avatar"
          alt=""
        />
        <van-icon v-else name="contact" class="profile-avatar profile-avatar--placeholder" />
        <div class="profile-info">
          <div class="profile-name-row">
            <span v-if="isLoggedIn && user" class="profile-name">{{ user.nickname }}</span>
            <span v-else class="profile-name profile-name--guest">点击登录</span>
            <AppStatusPill v-if="isLoggedIn" variant="green" size="sm">
              {{ isLeader ? '创建者' : '团员' }}
            </AppStatusPill>
            <button
              v-if="isLoggedIn && isLeader"
              type="button"
              class="profile-role-swap"
              aria-label="切换团员团长功能"
              @click.stop="onSwitchRoleClick"
            >
              <van-icon name="exchange" />
            </button>
          </div>
          <button type="button" class="profile-home-btn" @click.stop="handleHeaderClick">
            主页
            <van-icon name="arrow" />
          </button>
          <div v-if="!isLoggedIn" class="profile-desc">
            登录后可管理订单、收货地址等
          </div>
        </div>
      </div>

      <!-- 统计行 -->
      <AppCard>
        <div class="stat-row">
          <div>
            <div class="num">¥0</div>
            <span>余额</span>
          </div>
          <div>
            <div class="num">0</div>
            <span>团员</span>
          </div>
          <div>
            <div class="num">{{ isLeader ? 1 : 0 }}</div>
            <span>店铺</span>
          </div>
          <div>
            <div class="num">0</div>
            <span>社群</span>
          </div>
        </div>
      </AppCard>

      <!-- 功能卡片 -->
      <ProfileFeatureGrid
        :title="featureTitle"
        :entries="featureEntries"
        :columns="5"
        @item-click="handleFeatureClick"
      >
        <template #header-right>
          <AppButton v-if="isLoggedIn" variant="plain" class="profile-switch-btn" @click="onSwitchRoleClick">
            {{ isLeaderFeatureView ? '切换团员功能' : '切换团长功能' }}
          </AppButton>
        </template>
      </ProfileFeatureGrid>

      <AppCard v-if="isLeaderFeatureView" class="profile-welfare-card">
        <h2 class="recommend-title">福利中心</h2>
        <div class="welfare-entry">
          <van-icon name="bookmark" />
          <span>新人攻略</span>
        </div>
      </AppCard>

      <div class="index-follow-banner profile-follow-banner">
        <span>关注公众号，收到活动和订单、物流通知</span>
        <button type="button" class="index-follow-banner__btn" @click="onWechatNoticeClick">
          关注
        </button>
        <button
          type="button"
          class="index-follow-banner__close"
          aria-label="关闭关注提示"
          @click="showToast('公众号提醒仅作占位展示')"
        >
          <van-icon name="cross" />
        </button>
      </div>

      <!-- 推荐卡片 -->
      <AppCard v-if="!isLeaderFeatureView">
        <h2 class="recommend-title">猜你喜欢</h2>
        <p class="recommend-desc">更多个性化推荐将在后续版本开放。</p>
      </AppCard>

      <!-- 退出 -->
      <div v-if="isLoggedIn" class="logout-section">
        <AppButton variant="danger" block pill @click="handleLogout">退出登录</AppButton>
      </div>
    </div>
  </PageLayout>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { STORAGE_KEYS } from '@/constants'
import PageLayout from '@/components/PageLayout.vue'
import AppCard from '@/components/AppCard.vue'
import AppButton from '@/components/AppButton.vue'
import AppStatusPill from '@/components/AppStatusPill.vue'
import ProfileFeatureGrid from '@/components/ProfileFeatureGrid.vue'
import type { ProfileGridEntry } from '@/components/ProfileFeatureGrid.vue'
import { resolveDisplayImageUrl } from '@/utils'
import { isFeatureDisabled, type NonMvpFeature } from '@/utils/non-mvp'

const router = useRouter()
const authStore = useAuthStore()
const { isLoggedIn, isLeader, user, leader } = storeToRefs(authStore)
const profileAvatarUrl = computed(() => resolveDisplayImageUrl(
  user.value?.avatarUrl,
  user.value?.nickname || '',
  'avatar',
))

interface ProfileFeatureEntry extends ProfileGridEntry {
  disabledFeature?: NonMvpFeature
}

const buyerEntries: ProfileFeatureEntry[] = [
  { label: '收货地址', icon: 'location-o', to: '/addresses' },
  { label: '会员卡', icon: 'card', to: '/member-cards' },
  { label: '优惠券', icon: 'coupon-o', disabledFeature: 'coupon' },
  { label: '我的订单', icon: 'orders-o', to: '/orders' },
  { label: '官方客服', icon: 'service-o', disabledFeature: 'wechatPush' },
  { label: '反馈与建议', icon: 'edit', disabledFeature: 'wechatPush' },
  { label: '收藏与浏览', icon: 'star-o', to: '/subscriptions' },
  { label: '设置', icon: 'setting-o', disabledFeature: 'adminPanel' },
]

const leaderEntries: ProfileFeatureEntry[] = [
  { label: '商品库', icon: 'cube-o', to: '/leader/products' },
  { label: '订单管理', icon: 'orders-o', to: '/leader/orders' },
  { label: '团员改地址', icon: 'aim', disabledFeature: 'adminPanel' },
  { label: '商品核销', icon: 'completed-o', disabledFeature: 'adminPanel' },
  { label: '二维码海报', icon: 'photo-o', disabledFeature: 'wechatPush' },
  { label: '自提点管理', icon: 'location-o', disabledFeature: 'adminPanel' },
  { label: '会员管理', icon: 'vip-card-o', disabledFeature: 'memberCards' },
  { label: '积分商城', icon: 'star-o', disabledFeature: 'pointsMall' },
  { label: '团长管理', icon: 'manager-o', to: '/leader/store' },
  { label: '团团学堂', icon: 'description-o', disabledFeature: 'adminPanel' },
  { label: '流量神器', icon: 'friends-o', disabledFeature: 'wechatPush' },
  { label: '直播工具', icon: 'video-o', disabledFeature: 'wechatPush' },
  { label: '输入法', icon: 'like-o', disabledFeature: 'adminPanel' },
  { label: '官方客服', icon: 'service-o', disabledFeature: 'wechatPush' },
  { label: '反馈与建议', icon: 'edit', disabledFeature: 'wechatPush' },
  { label: '收藏与浏览', icon: 'star-o', to: '/subscriptions' },
  { label: '推广链接', icon: 'link-o', disabledFeature: 'distribution' },
  { label: '渠道订阅码', icon: 'bookmark-o', disabledFeature: 'wechatPush' },
  { label: '卡券核销', icon: 'scan', disabledFeature: 'coupon' },
  { label: '设置', icon: 'setting-o', to: '/leader/store' },
  { label: '违规信息', icon: 'warning-o', disabledFeature: 'adminPanel' },
]

const guestEntries: ProfileFeatureEntry[] = [
  { label: '我的订单', icon: 'orders-o', to: '/orders' },
  { label: '地址管理', icon: 'location-o', to: '/addresses' },
  { label: '创建店铺', icon: 'shop-o', to: '/store/create' },
  { label: '会员卡', icon: 'card', disabledFeature: 'memberCards' },
]

type ProfileFeatureRole = 'buyer' | 'leader'

function readSavedFeatureRole(): ProfileFeatureRole {
  if (typeof window === 'undefined') return 'buyer'
  return localStorage.getItem(STORAGE_KEYS.PROFILE_FEATURE_ROLE) === 'leader' ? 'leader' : 'buyer'
}

const activeFeatureRole = ref<ProfileFeatureRole>(readSavedFeatureRole())
const isLeaderFeatureView = computed(() => isLoggedIn.value && activeFeatureRole.value === 'leader')
const featureTitle = computed(() => (isLeaderFeatureView.value ? '团长功能' : '团员功能'))
const featureEntries = computed(() => {
  if (!isLoggedIn.value) return guestEntries
  if (isLeaderFeatureView.value) return leaderEntries
  return buyerEntries
})

function handleHeaderClick() {
  if (!isLoggedIn.value) {
    router.push('/login?redirect=/profile')
  } else if (leader.value?.id) {
    router.push(`/leaders/${leader.value.id}`)
  }
}

function onDisabledFeature(feature: string) {
  if (isFeatureDisabled(feature as NonMvpFeature)) {
    showToast('即将开放')
  }
}

function handleFeatureClick(entry: ProfileGridEntry) {
  if (!isLoggedIn.value && entry.to !== undefined) {
    router.push(`/login?redirect=${entry.to}`)
    return
  }
  if (entry.disabledFeature) {
    onDisabledFeature(entry.disabledFeature)
    return
  }
  if (entry.to) {
    router.push(entry.to)
  }
}

function onSwitchRoleClick() {
  if (!isLoggedIn.value) {
    router.push('/login?redirect=/profile')
    return
  }
  if (!isLeader.value && activeFeatureRole.value === 'buyer') {
    showToast('创建店铺后可切换团长功能')
    return
  }
  activeFeatureRole.value = activeFeatureRole.value === 'buyer' ? 'leader' : 'buyer'
  localStorage.setItem(STORAGE_KEYS.PROFILE_FEATURE_ROLE, activeFeatureRole.value)
}

watch([isLoggedIn, isLeader], ([loggedIn, leaderReady]) => {
  if (!loggedIn || !leaderReady) {
    activeFeatureRole.value = 'buyer'
    return
  }

  activeFeatureRole.value = readSavedFeatureRole()
}, { immediate: true })

function onWechatNoticeClick() {
  if (isFeatureDisabled('wechatPush')) {
    showToast('公众号推送将在后续开放')
  }
}

async function handleLogout() {
  try {
    await showConfirmDialog({
      title: '提示',
      message: '确认退出登录？',
    })
    authStore.logout()
  } catch {
    // 用户取消操作
  }
}
</script>

<style scoped>
.profile-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 22px 8px 16px;
  color: var(--color-text-primary);
}

.profile-content {
  padding: 0 14px 24px;
}

.profile-avatar {
  width: 74px;
  height: 74px;
  border-radius: 10px;
  object-fit: cover;
  flex-shrink: 0;
}

.profile-avatar--placeholder {
  font-size: 40px;
  color: var(--color-primary);
  background: #fff49b;
  display: flex;
  align-items: center;
  justify-content: center;
}

.profile-info {
  flex: 1;
  min-width: 0;
}

.profile-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.profile-name {
  font-size: 22px;
  font-weight: 900;
  color: var(--color-text-primary);
}

.profile-name--guest {
  opacity: 0.9;
}

.profile-desc {
  font-size: var(--font-size-sm);
  opacity: 0.8;
  margin-top: 4px;
}

.profile-home-btn {
  margin-top: 6px;
  border: 1px solid #e7eaee;
  background: #fff;
  color: #666;
  border-radius: 999px;
  padding: 7px 14px;
  min-height: 36px;
  font-weight: 800;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  text-align: center;
}

.stat-row .num {
  font-size: 20px;
  font-weight: 900;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.stat-row span {
  font-size: 12px;
  color: var(--color-text-hint);
}

.profile-switch-btn {
  font-size: var(--font-size-sm);
  min-height: 34px;
  padding: 0 10px;
  border-color: var(--color-border);
  color: var(--color-text-secondary);
  background: var(--color-bg-card);
}

.profile-role-swap {
  width: 30px;
  height: 30px;
  border: 0;
  background: transparent;
  color: var(--color-text-placeholder);
  font-size: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.profile-welfare-card {
  margin-top: 12px;
}

.welfare-entry {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-top: 18px;
  color: var(--color-text-secondary);
  font-size: 14px;
}

.welfare-entry .van-icon {
  font-size: 42px;
  color: var(--color-primary);
}

.profile-follow-banner {
  margin: 14px 0 12px;
  border-radius: 12px;
}

.index-follow-banner {
  min-height: 54px;
  background: #fff7e6;
  color: #e96c2b;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  font-weight: 800;
}

.index-follow-banner span {
  flex: 1;
  min-width: 0;
}

.index-follow-banner__btn {
  border: 0;
  border-radius: 9px;
  background: var(--color-primary);
  color: #fff;
  min-height: 36px;
  padding: 0 18px;
  font-weight: 900;
}

.index-follow-banner__close {
  border: 0;
  background: transparent;
  color: #b8a995;
  font-size: 18px;
  width: 28px;
  height: 28px;
}

.recommend-title {
  margin: 0;
  font-size: 20px;
  font-weight: 900;
  color: var(--color-text-primary);
}

.recommend-desc {
  margin: 8px 0 0;
  color: var(--color-text-hint);
  font-size: 13px;
}

.logout-section {
  margin-top: 24px;
}
</style>
