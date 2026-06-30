<template>
  <PageLayout show-tab-bar>
    <!-- User Info Section -->
    <div class="profile-header" @click="handleHeaderClick">
      <div class="profile-avatar-wrap">
        <img
          v-if="isLoggedIn && user?.avatarUrl"
          :src="user.avatarUrl"
          class="profile-avatar"
          alt=""
        />
        <van-icon v-else name="contact" class="profile-avatar profile-avatar--placeholder" />
      </div>
      <div class="profile-info">
        <div class="profile-name-row">
          <span v-if="isLoggedIn && user" class="profile-name">{{ user.nickname }}</span>
          <span v-else class="profile-name profile-name--guest">点击登录</span>
          <van-tag
            v-if="isLoggedIn"
            :type="isLeader ? 'danger' : 'primary'"
            class="profile-tag"
          >
            {{ isLeader ? '团长' : '买家' }}
          </van-tag>
        </div>
        <div v-if="!isLoggedIn" class="profile-desc">
          登录后可管理订单、收货地址等
        </div>
      </div>
    </div>

    <!-- Logged in: Menu List -->
    <template v-if="isLoggedIn">
      <!-- Common Menu -->
      <div class="menu-section">
        <van-cell-group inset>
          <van-cell title="我的订单" is-link to="/orders" icon="orders-o" />
          <van-cell title="会员卡" is-link @click="onComingSoon" icon="card" />
          <van-cell title="地址管理" is-link to="/addresses" icon="location-o" />
          <van-cell
            v-if="!isLeader"
            title="创建店铺 / 成为团长"
            is-link
            to="/store/create"
            icon="shop-o"
          />
        </van-cell-group>
      </div>

      <!-- Leader Menu -->
      <div v-if="isLeader" class="menu-section">
        <van-cell-group inset>
          <van-cell title="我的店铺" is-link to="/leader/store" icon="shop-o" />
          <van-cell title="发布团购" is-link @click="onComingSoon" icon="add-o" />
          <van-cell title="我的团购" is-link to="/leader/group-buys" icon="label-o" />
          <van-cell title="团长订单" is-link to="/leader/orders" icon="orders-o" />
        </van-cell-group>
      </div>

      <!-- Logout -->
      <div class="logout-section">
        <van-button type="default" block round @click="handleLogout">退出登录</van-button>
      </div>
    </template>
  </PageLayout>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import PageLayout from '@/components/PageLayout.vue'

const router = useRouter()
const authStore = useAuthStore()
const { isLoggedIn, isLeader, user } = storeToRefs(authStore)

function handleHeaderClick() {
  if (!isLoggedIn.value) {
    router.push('/login?redirect=/profile')
  }
}

function onComingSoon() {
  showToast('即将开放')
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
  padding: var(--spacing-lg);
  padding-top: calc(var(--spacing-lg) + env(safe-area-inset-top, 0px));
  background: var(--color-primary);
  color: #fff;
}

.profile-avatar-wrap {
  flex-shrink: 0;
}

.profile-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.profile-avatar--placeholder {
  font-size: 48px;
  color: rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.profile-info {
  flex: 1;
  margin-left: var(--spacing-md);
  min-width: 0;
}

.profile-name-row {
  display: flex;
  align-items: center;
}

.profile-name {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: #fff;
}

.profile-name--guest {
  opacity: 0.9;
}

.profile-tag {
  margin-left: 8px;
}

.profile-desc {
  font-size: var(--font-size-sm);
  opacity: 0.8;
  margin-top: 4px;
}

.menu-section {
  margin-top: 12px;
}

.logout-section {
  margin-top: 24px;
  padding: 0 16px;
}
</style>
