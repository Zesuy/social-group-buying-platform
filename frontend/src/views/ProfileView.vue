<template>
  <PageLayout show-tab-bar>
    <div class="app-topbar">我的</div>
    <div class="profile-content">
      <section class="profile-header" @click="handleHeaderClick">
        <img
          v-if="isLoggedIn && profileAvatarUrl"
          :src="profileAvatarUrl"
          class="profile-avatar"
          alt="用户头像"
        />
        <van-icon v-else name="contact" class="profile-avatar profile-avatar--placeholder" />
        <div class="profile-info">
          <div class="profile-name-row">
            <span v-if="isLoggedIn && user" class="profile-name">{{ user.nickname }}</span>
            <span v-else class="profile-name profile-name--guest">点击登录</span>
            <AppStatusPill v-if="isLoggedIn" variant="green" size="sm">
              {{ isLeader ? '团长' : '团员' }}
            </AppStatusPill>
          </div>
          <p class="profile-desc">{{ profileDescription }}</p>
          <div class="profile-actions">
            <button
              v-if="isLoggedIn && isLeader"
              type="button"
              class="profile-link-btn"
              @click.stop="router.push('/leader/store')"
            >
              我的店铺
              <van-icon name="arrow" />
            </button>
            <button
              v-else-if="isLoggedIn"
              type="button"
              class="profile-link-btn profile-link-btn--primary"
              @click.stop="router.push('/store/create')"
            >
              创建店铺
              <van-icon name="arrow" />
            </button>
          </div>
        </div>
      </section>

      <ProfileFeatureGrid
        title="我的团购关系"
        :entries="buyerEntries"
        @item-click="handleFeatureClick"
      />

      <ProfileFeatureGrid
        v-if="isLeader"
        title="团长经营"
        :entries="leaderEntries"
        @item-click="handleFeatureClick"
      />

      <ProfileFeatureGrid
        v-else
        title="开店经营"
        :entries="storeEntries"
        @item-click="handleFeatureClick"
      />

      <div v-if="isLoggedIn" class="logout-section">
        <AppButton variant="danger" block pill @click="handleLogout">退出登录</AppButton>
      </div>
    </div>
  </PageLayout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog } from 'vant'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import PageLayout from '@/components/PageLayout.vue'
import AppButton from '@/components/AppButton.vue'
import AppStatusPill from '@/components/AppStatusPill.vue'
import ProfileFeatureGrid from '@/components/ProfileFeatureGrid.vue'
import type { ProfileGridEntry } from '@/components/ProfileFeatureGrid.vue'
import { resolveDisplayImageUrl } from '@/utils'

const router = useRouter()
const authStore = useAuthStore()
const { isLoggedIn, isLeader, user, leader, store } = storeToRefs(authStore)
const profileAvatarUrl = computed(() => {
  const avatarUrl = isLeader.value
    ? (leader.value?.avatarUrl || store.value?.logoUrl || user.value?.avatarUrl)
    : user.value?.avatarUrl
  const avatarSeed = isLeader.value
    ? (leader.value?.displayName || store.value?.name || user.value?.nickname || '')
    : (user.value?.nickname || '')

  return resolveDisplayImageUrl(avatarUrl, avatarSeed, 'avatar')
})

const profileDescription = computed(() => {
  if (!isLoggedIn.value) return '登录后查看订单、地址、订阅和会员关系'
  if (isLeader.value) {
    return store.value?.name
      ? `${store.value.name} · 管理团购活动和订单履约`
      : '管理店铺、团购活动和订单履约'
  }
  return '查看履约进度、常用地址、订阅团长和会员权益'
})

const buyerEntries: ProfileGridEntry[] = [
  { label: '我的订单', description: '支付、发货和完成状态', icon: 'orders-o', to: '/orders' },
  { label: '收货地址', description: '下单前维护配送信息', icon: 'location-o', to: '/addresses' },
  { label: '我的订阅', description: '关注的团长和店铺', icon: 'star-o', to: '/subscriptions' },
  { label: '会员卡', description: '复购关系和成长值', icon: 'card', to: '/member-cards' },
]

const leaderEntries: ProfileGridEntry[] = [
  { label: '发布团购', description: '创建新的活动', icon: 'add-o', to: '/leader/group-buys/new' },
  { label: '团购管理', description: '查看活动状态', icon: 'shop-o', to: '/leader/group-buys' },
  { label: '订单管理', description: '处理发货履约', icon: 'orders-o', to: '/leader/orders' },
  { label: '订阅用户', description: '查看关注你的团员', icon: 'friends-o', to: '/leader/subscribers' },
  { label: '商品库', description: '复用商品资料', icon: 'cube-o', to: '/leader/products' },
]

const storeEntries: ProfileGridEntry[] = [
  { label: '创建店铺', description: '成为团长后发布团购', icon: 'shop-o', to: '/store/create' },
]

function handleHeaderClick() {
  if (!isLoggedIn.value) {
    router.push('/login?redirect=/profile')
  } else if (leader.value?.id) {
    router.push(`/leaders/${leader.value.id}`)
  }
}

function handleFeatureClick(entry: ProfileGridEntry) {
  if (!isLoggedIn.value && entry.to !== undefined) {
    router.push(`/login?redirect=${entry.to}`)
    return
  }
  if (entry.to) {
    router.push(entry.to)
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
.profile-content {
  padding: 12px 14px 24px;
}

.profile-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  margin-bottom: 12px;
  border-radius: var(--radius-card);
  background: var(--color-bg-card);
  box-shadow: var(--shadow-card);
  color: var(--color-text-primary);
  cursor: pointer;
}

.profile-header:active {
  opacity: 0.9;
}

.profile-avatar {
  width: 58px;
  height: 58px;
  border-radius: 14px;
  object-fit: cover;
  flex-shrink: 0;
}

.profile-avatar--placeholder {
  font-size: 30px;
  color: var(--color-primary);
  background: var(--color-primary-light);
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
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.35;
}

.profile-name--guest {
  color: var(--color-text-primary);
}

.profile-desc {
  margin: 6px 0 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-md);
  line-height: 1.5;
}

.profile-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.profile-link-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 36px;
  padding: 0 12px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: var(--color-bg-card);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  font-weight: 700;
  font-family: inherit;
}

.profile-link-btn--primary {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.logout-section {
  margin-top: 16px;
}
</style>
