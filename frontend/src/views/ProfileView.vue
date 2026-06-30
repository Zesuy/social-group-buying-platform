<template>
  <PageLayout show-tab-bar>
    <div class="app-topbar">我的</div>
    <div class="profile-content">
      <!-- 用户信息区 -->
      <div class="profile-header" @click="handleHeaderClick">
        <img
          v-if="isLoggedIn && user?.avatarUrl"
          :src="user.avatarUrl"
          class="profile-avatar"
          alt=""
        />
        <van-icon v-else name="contact" class="profile-avatar profile-avatar--placeholder" />
        <div class="profile-info">
          <div class="profile-name-row">
            <span v-if="isLoggedIn && user" class="profile-name">{{ user.nickname }}</span>
            <span v-else class="profile-name profile-name--guest">点击登录</span>
            <span v-if="isLoggedIn" class="status-chip">
              {{ isLeader ? '创建者' : '买家' }}
            </span>
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

      <!-- 功能卡片 -->
      <div class="profile-feature-card">
        <div class="profile-feature-card__header">
          <h2>{{ isLeader ? '团长功能' : '团员功能' }}</h2>
          <button v-if="isLoggedIn" type="button" @click="onSwitchRoleClick">
            切换{{ isLeader ? '团员' : '团长' }}功能
          </button>
        </div>
        <div class="grid-4">
          <button
            v-for="entry in featureEntries"
            :key="entry.label"
            type="button"
            class="profile-feature"
            @click="handleFeatureClick(entry)"
          >
            <span class="profile-feature__icon">
              <van-icon :name="entry.icon" />
            </span>
            <span>{{ entry.label }}</span>
          </button>
        </div>
      </div>

      <!-- 推荐卡片 -->
      <div class="profile-recommend-card">
        <h2>猜你喜欢</h2>
        <p>更多个性化推荐将在后续版本开放。</p>
      </div>

      <!-- 退出 -->
      <div v-if="isLoggedIn" class="logout-section">
        <van-button type="default" block round @click="handleLogout">退出登录</van-button>
      </div>
    </div>
  </PageLayout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import PageLayout from '@/components/PageLayout.vue'
import { isFeatureDisabled, type NonMvpFeature } from '@/utils/non-mvp'

const router = useRouter()
const authStore = useAuthStore()
const { isLoggedIn, isLeader, user, leader } = storeToRefs(authStore)

interface ProfileFeatureEntry {
  label: string
  icon: string
  to?: string
  disabledFeature?: NonMvpFeature
}

const buyerEntries: ProfileFeatureEntry[] = [
  { label: '地址管理', icon: 'location-o', to: '/addresses' },
  { label: '会员卡', icon: 'card', to: '/member-cards' },
  { label: '优惠券', icon: 'coupon-o', disabledFeature: 'coupon' },
  { label: '我的订单', icon: 'orders-o', to: '/orders' },
  { label: '官方客服', icon: 'service-o', disabledFeature: 'wechatPush' },
  { label: '反馈与建议', icon: 'edit', disabledFeature: 'wechatPush' },
  { label: '收藏与浏览', icon: 'star-o', to: '/subscriptions' },
  { label: '设置', icon: 'setting-o', disabledFeature: 'adminPanel' },
]

const leaderEntries: ProfileFeatureEntry[] = [
  { label: '我的店铺', icon: 'shop-o', to: '/leader/store' },
  { label: '发布团购', icon: 'add-o', to: '/leader/group-buys/new' },
  { label: '我的团购', icon: 'label-o', to: '/leader/group-buys' },
  { label: '团长订单', icon: 'orders-o', to: '/leader/orders' },
  { label: '商品管理', icon: 'bag-o', to: '/leader/products' },
  { label: '订阅团员', icon: 'friends-o', to: '/subscriptions' },
  { label: '数据概览', icon: 'bar-chart-o', disabledFeature: 'adminPanel' },
  { label: '店铺设置', icon: 'setting-o', to: '/leader/store' },
]

const guestEntries: ProfileFeatureEntry[] = [
  { label: '我的订单', icon: 'orders-o', to: '/orders' },
  { label: '地址管理', icon: 'location-o', to: '/addresses' },
  { label: '创建店铺', icon: 'shop-o', to: '/store/create' },
  { label: '会员卡', icon: 'card', disabledFeature: 'memberCards' },
]

const featureEntries = computed(() => {
  if (!isLoggedIn.value) return guestEntries
  return isLeader.value ? leaderEntries : [
    ...buyerEntries,
    { label: '创建店铺', icon: 'shop-o', to: '/store/create' },
  ]
})

function handleHeaderClick() {
  if (!isLoggedIn.value) {
    router.push('/login?redirect=/profile')
  } else if (leader.value?.id) {
    router.push(`/leaders/${leader.value.id}`)
  }
}

function onDisabledFeature(feature: NonMvpFeature) {
  if (isFeatureDisabled(feature)) {
    showToast('即将开放')
  }
}

function handleFeatureClick(entry: ProfileFeatureEntry) {
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
  showToast('功能视图切换将在后续开放')
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

.profile-feature-card,
.profile-recommend-card {
  background: #fff;
  border-radius: var(--radius-card);
  padding: 14px;
  margin-bottom: 12px;
  box-shadow: var(--shadow-card);
}

.profile-feature-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.profile-feature-card__header h2,
.profile-recommend-card h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 900;
}

.profile-feature-card__header button {
  border: 1px solid #e7eaee;
  background: #fff;
  color: #666;
  border-radius: 999px;
  padding: 7px 12px;
  min-height: 36px;
  font-size: 12px;
  font-weight: 800;
}

.profile-feature {
  border: 0;
  background: transparent;
  color: #4f555f;
  text-align: center;
  min-height: 74px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.profile-feature__icon {
  width: 40px;
  height: 40px;
  border: 2px solid #222;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  background: #fff;
  font-size: 22px;
}

.profile-recommend-card {
  text-align: center;
}

.profile-recommend-card p {
  margin: 8px 0 0;
  color: var(--color-text-hint);
  font-size: 13px;
}

.logout-section {
  margin-top: 24px;
  padding: 0 14px;
}

.logout-section :deep(.van-button) {
  height: var(--button-capsule-height);
  font-weight: 900;
}
</style>
