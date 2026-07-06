<template>
  <PageLayout title="商家工作台">
    <LoadingView v-if="loading" text="正在加载工作台..." />
    <ErrorView v-else-if="error" :message="error" @retry="loadDashboard" />

    <div v-else class="dashboard">
      <section class="store-panel">
        <ImageWithFallback
          :src="storeData?.store.logoUrl || storeData?.leader.avatarUrl"
          :alt="storeData?.store.name || '店铺'"
          width="58px"
          height="58px"
          radius="14px"
          demo-kind="store"
        />
        <div class="store-panel__main">
          <div class="store-panel__title">
            <h1>{{ storeData?.store.name || '我的店铺' }}</h1>
            <AppStatusPill :variant="storeStatusVariant" size="sm">{{ storeStatusText }}</AppStatusPill>
          </div>
          <p>{{ storeData?.leader.displayName || '团长' }} · {{ deliveryText }}</p>
          <div class="store-panel__actions">
            <AppButton variant="primary" size="sm" icon="shop-o" @click="go('/leader/store')">店铺资料</AppButton>
            <AppButton
              v-if="storeData?.leader.id"
              variant="ghost"
              size="sm"
              icon="wap-home-o"
              @click="go(`/leaders/${storeData?.leader.id}`)"
            >
              团长主页
            </AppButton>
          </div>
        </div>
      </section>

      <section class="todo-grid" aria-label="关键待办">
        <button
          v-for="item in todoItems"
          :key="item.label"
          type="button"
          class="todo-card"
          @click="go(item.to)"
        >
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.hint }}</small>
        </button>
      </section>

      <section class="quick-section">
        <div class="section-head">
          <h2>常用操作</h2>
          <span>商品、团购、履约和复购入口</span>
        </div>
        <div class="quick-grid">
          <button
            v-for="entry in quickEntries"
            :key="entry.label"
            type="button"
            class="quick-entry"
            @click="go(entry.to)"
          >
            <van-icon :name="entry.icon" />
            <span>{{ entry.label }}</span>
            <small>{{ entry.desc }}</small>
          </button>
        </div>
      </section>
    </div>
  </PageLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import AppButton from '@/components/AppButton.vue'
import AppStatusPill from '@/components/AppStatusPill.vue'
import ImageWithFallback from '@/components/ImageWithFallback.vue'
import { listChatConversations } from '@/api/chats'
import { listMyGroupBuys } from '@/api/leaderGroupBuys'
import { listLeaderAfterSales } from '@/api/leaderAfterSales'
import { listLeaderOrders } from '@/api/leaderOrders'
import { getMyStore } from '@/api/stores'
import type { MyStoreResponseData } from '@/types'

const router = useRouter()

const loading = ref(true)
const error = ref('')
const storeData = ref<MyStoreResponseData>(null)
const paidOrderCount = ref(0)
const pendingAfterSaleCount = ref(0)
const leaderUnreadCount = ref(0)
const publishedGroupBuyCount = ref(0)

const storeStatusText = computed(() => {
  const status = storeData.value?.store.status
  if (status === 'active') return '营业中'
  if (status === 'disabled') return '已停用'
  return status || '待完善'
})

const storeStatusVariant = computed<'green' | 'orange' | 'gray' | 'red'>(() => {
  const status = storeData.value?.store.status
  if (status === 'active') return 'green'
  if (status === 'disabled') return 'red'
  return 'orange'
})

const deliveryText = computed(() => {
  const deliveryType = storeData.value?.store.defaultDeliveryType
  if (deliveryType === 'pickup') return '默认自提'
  if (deliveryType === 'delivery') return '默认配送'
  return '配送方式待完善'
})

const todoItems = computed(() => [
  { label: '待发货订单', value: paidOrderCount.value, hint: '已支付待履约', to: '/leader/orders' },
  { label: '售后待处理', value: pendingAfterSaleCount.value, hint: '退款申请待审核', to: '/leader/after-sales' },
  { label: '未读客服消息', value: leaderUnreadCount.value, hint: '买家咨询未读', to: '/leader/chats' },
  { label: '进行中团购', value: publishedGroupBuyCount.value, hint: '当前可下单活动', to: '/leader/group-buys' },
])

const quickEntries = [
  { label: '商品', desc: '维护商品库', icon: 'cube-o', to: '/leader/products' },
  { label: '团购', desc: '发布和分享活动', icon: 'shop-o', to: '/leader/group-buys' },
  { label: '订单', desc: '发货履约', icon: 'orders-o', to: '/leader/orders' },
  { label: '售后', desc: '审核退款', icon: 'after-sale', to: '/leader/after-sales' },
  { label: '客服', desc: '买家会话', icon: 'chat-o', to: '/leader/chats' },
  { label: '优惠券', desc: '订阅新人券', icon: 'coupon-o', to: '/leader/coupons' },
  { label: '团员', desc: '订阅用户', icon: 'friends-o', to: '/leader/subscribers' },
  { label: '店铺', desc: '资料和定位', icon: 'setting-o', to: '/leader/store' },
]

async function loadDashboard() {
  loading.value = true
  error.value = ''
  try {
    storeData.value = await getMyStore()
    const [orders, afterSales, chats, groupBuys] = await Promise.allSettled([
      listLeaderOrders('paid', 1, 1),
      listLeaderAfterSales({ page: 1, pageSize: 50 }),
      listChatConversations({ role: 'leader', page: 1, pageSize: 20 }),
      listMyGroupBuys('published', 1, 1),
    ])
    paidOrderCount.value = orders.status === 'fulfilled' ? orders.value.total : 0
    pendingAfterSaleCount.value = afterSales.status === 'fulfilled'
      ? afterSales.value.items.filter((item) => item.status === 'pending').length
      : 0
    leaderUnreadCount.value = chats.status === 'fulfilled'
      ? chats.value.items.reduce((sum, item) => sum + item.unreadCount, 0)
      : 0
    publishedGroupBuyCount.value = groupBuys.status === 'fulfilled' ? groupBuys.value.total : 0
  } catch (err) {
    error.value = (err as { message?: string }).message || '商家工作台加载失败'
  } finally {
    loading.value = false
  }
}

function go(path: string) {
  router.push(path)
}

onMounted(() => {
  loadDashboard()
})
</script>

<style scoped>
.dashboard {
  padding: 12px 14px 24px;
}

.store-panel {
  display: flex;
  gap: 12px;
  padding: 16px;
  border-radius: var(--radius-card);
  background: var(--color-bg-card);
  box-shadow: var(--shadow-card);
}

.store-panel__main {
  flex: 1;
  min-width: 0;
}

.store-panel__title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.store-panel__title h1 {
  margin: 0;
  min-width: 0;
  color: var(--color-text-primary);
  font-size: var(--font-size-xl);
  font-weight: 900;
  line-height: 1.35;
  word-break: break-word;
}

.store-panel__main p {
  margin: 4px 0 10px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.store-panel__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.store-panel__actions :deep(.app-button) {
  min-height: 34px;
  padding: 0 12px;
  font-size: var(--font-size-sm);
}

.todo-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.todo-card {
  min-height: 104px;
  padding: 13px;
  border: 0;
  border-radius: var(--radius-card);
  background: var(--color-bg-card);
  box-shadow: var(--shadow-card);
  text-align: left;
  font-family: inherit;
}

.todo-card span,
.section-head span,
.quick-entry small {
  color: var(--color-text-hint);
  font-size: var(--font-size-sm);
}

.todo-card strong {
  display: block;
  margin: 8px 0 4px;
  color: var(--color-text-primary);
  font-size: 26px;
  font-weight: 900;
  line-height: 1;
}

.todo-card small {
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
}

.quick-section {
  margin-top: 16px;
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.section-head h2 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
  font-weight: 900;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.quick-entry {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-height: 82px;
  padding: 10px 6px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  background: var(--color-bg-card);
  color: var(--color-text-primary);
  font-family: inherit;
  text-align: center;
}

.quick-entry .van-icon {
  font-size: 21px;
  color: var(--color-primary);
}

.quick-entry span {
  font-size: var(--font-size-sm);
  font-weight: 800;
}

@media (max-width: 360px) {
  .quick-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
