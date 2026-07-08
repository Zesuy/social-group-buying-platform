<template>
  <div class="merchant-shell">
    <aside class="merchant-sidebar">
      <RouterLink class="merchant-brand" to="/merchant/dashboard">
        <span class="merchant-brand__mark">商</span>
        <span>商家管理端</span>
      </RouterLink>
      <nav class="merchant-nav" aria-label="商家管理导航">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="merchant-nav__item"
          active-class="merchant-nav__item--active"
        >
          <van-icon :name="item.icon" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>
    </aside>

    <div class="merchant-main">
      <header class="merchant-topbar">
        <div class="merchant-store">
          <img
            v-if="storeLogo"
            :src="storeLogo"
            :alt="storeName"
            class="merchant-store__logo"
          >
          <div v-else class="merchant-store__logo merchant-store__logo--fallback">
            {{ storeName.slice(0, 1) }}
          </div>
          <div>
            <strong>{{ storeName }}</strong>
            <span>{{ leaderName }} · {{ storeStatusText }}</span>
          </div>
        </div>
        <div class="merchant-topbar__actions">
          <RouterLink v-if="leaderId" :to="`/leaders/${leaderId}`">团长主页</RouterLink>
          <RouterLink to="/leader/dashboard">返回 H5 工作台</RouterLink>
        </div>
      </header>

      <main class="merchant-content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView } from 'vue-router'
import { useAuthStore } from '@/stores'

const authStore = useAuthStore()

const navItems = [
  { label: '工作台', icon: 'dashboard-o', to: '/merchant/dashboard' },
  { label: '订单', icon: 'orders-o', to: '/merchant/orders' },
  { label: '售后', icon: 'after-sale', to: '/merchant/after-sales' },
  { label: '客服', icon: 'chat-o', to: '/merchant/chats' },
  { label: '商品', icon: 'goods-collect-o', to: '/merchant/products' },
  { label: '团购', icon: 'shop-o', to: '/merchant/group-buys' },
  { label: '优惠券', icon: 'coupon-o', to: '/merchant/coupons' },
  { label: '订阅用户', icon: 'friends-o', to: '/merchant/subscribers' },
  { label: '店铺资料', icon: 'setting-o', to: '/merchant/store' },
]

const storeName = computed(() => authStore.store?.name || '我的店铺')
const storeLogo = computed(() => authStore.store?.logoUrl || authStore.leader?.avatarUrl || '')
const leaderName = computed(() => authStore.leader?.displayName || '团长')
const leaderId = computed(() => authStore.leader?.id)
const storeStatusText = computed(() => authStore.store?.status === 'active' ? '营业中' : '待完善')
</script>

<style scoped>
.merchant-shell {
  height: 100vh;
  height: 100dvh;
  display: grid;
  grid-template-columns: 224px minmax(0, 1fr);
  background: #f5f7fa;
  color: #1f2937;
  overflow: hidden;
}

.merchant-sidebar {
  height: 100%;
  min-height: 0;
  padding: 18px 14px;
  background: #ffffff;
  border-right: 1px solid #e5e7eb;
  overflow-y: auto;
}

.merchant-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 44px;
  padding: 0 8px;
  color: #111827;
  font-size: 17px;
  font-weight: 900;
  text-decoration: none;
}

.merchant-brand__mark {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #e9563f;
  color: #ffffff;
}

.merchant-nav {
  margin-top: 18px;
  display: grid;
  gap: 4px;
}

.merchant-nav__item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 42px;
  padding: 0 12px;
  border-radius: 8px;
  color: #4b5563;
  text-decoration: none;
  font-size: 14px;
  font-weight: 700;
}

.merchant-nav__item--active {
  background: #fff1ed;
  color: #d63f2b;
}

.merchant-main {
  min-width: 0;
  min-height: 0;
  display: grid;
  grid-template-rows: 64px minmax(0, 1fr);
}

.merchant-topbar {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 0 28px;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
}

.merchant-store {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.merchant-store__logo {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  object-fit: cover;
}

.merchant-store__logo--fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #eef2f7;
  color: #374151;
  font-weight: 900;
}

.merchant-store strong,
.merchant-store span {
  display: block;
}

.merchant-store strong {
  font-size: 15px;
  line-height: 1.3;
}

.merchant-store span {
  margin-top: 2px;
  color: #6b7280;
  font-size: 12px;
}

.merchant-topbar__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.merchant-topbar__actions a {
  color: #374151;
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
}

.merchant-content {
  min-height: 0;
  padding: 24px 28px 40px;
  overflow-y: auto;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
}

.merchant-content :deep(.merchant-page) {
  min-width: 960px;
}

@media (max-width: 900px) {
  .merchant-shell {
    grid-template-columns: 72px minmax(0, 1fr);
  }

  .merchant-brand span:last-child,
  .merchant-nav__item span {
    display: none;
  }

  .merchant-topbar {
    padding: 0 18px;
  }

  .merchant-content {
    padding: 18px;
  }
}
</style>
