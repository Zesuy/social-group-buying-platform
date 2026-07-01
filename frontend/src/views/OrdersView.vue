<template>
  <PageLayout show-tab-bar>
    <div class="app-topbar">
      我购买的订单
    </div>

    <AppNoticeStrip
      text="关注公众号，收到活动和订单、物流通知"
      action-label="关注"
      @action="onWechatNoticeClick"
    />

    <!-- 搜索占位 -->
    <AppCard class="orders-search" :clickable="true" @click="onSearchClick">
      <van-icon name="search" size="16" />
      <span>搜索商品名 / 订单号 / 团长</span>
    </AppCard>

    <!-- 状态 Tab（保持 Vant tabs，E2E 兼容） -->
    <van-tabs v-model:active="activeTab" @change="onTabChange">
      <van-tab
        v-for="t in statusTabs"
        :key="t.name"
        :title="t.title"
        :name="t.name"
      />
    </van-tabs>

    <!-- 首次加载 -->
    <LoadingView v-if="firstLoading" />

    <!-- 错误重试 -->
    <ErrorView
      v-else-if="showError"
      :message="error ?? undefined"
      @retry="retry"
    />

    <!-- 订单列表 -->
    <div v-else class="orders-content">
      <AppPageNote text="订单列表主态：顶部搜索 + 状态 Tab；订单卡片露出团长、商品快照、金额、状态和可操作按钮。" />

      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <van-list
          :loading="loading"
          :finished="!hasMore"
          finished-text="没有更多了"
          :error="error !== null"
          error-text="加载失败，点击重试"
          :immediate-check="false"
          @load="onLoadMore"
        >
          <!-- 订单卡片 -->
          <OrderListCard
            v-for="order in items"
            :key="order.id"
            :order="order"
            mode="buyer"
            :clickable="true"
            :action-buttons="getActionButtons(order)"
            @click="goToDetail(order.id)"
          />

          <!-- 空态 -->
          <div v-if="isEmpty" class="orders-empty">
            <EmptyState description="暂无订单" />
            <button type="button" class="btn primary" @click="goHome">去首页逛逛</button>
          </div>
        </van-list>
      </van-pull-refresh>
    </div>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { showConfirmDialog } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import EmptyState from '@/components/EmptyState.vue'
import AppNoticeStrip from '@/components/AppNoticeStrip.vue'
import AppPageNote from '@/components/AppPageNote.vue'
import AppCard from '@/components/AppCard.vue'
import OrderListCard from '@/components/OrderListCard.vue'
import { usePagination } from '@/composables/usePagination'
import { listMyOrders, cancelOrder } from '@/api/orders'
import { isFeatureDisabled } from '@/utils/non-mvp'
import type { OrderData } from '@/types'

const router = useRouter()

// ── 状态 Tab ──
const statusTabs = [
  { name: '', title: '全部' },
  { name: 'pendingPay', title: '待支付' },
  { name: 'paid', title: '已支付' },
  { name: 'shipped', title: '已发货' },
  { name: 'completed', title: '已完成' },
  { name: 'canceled', title: '已取消' },
]
const activeTab = ref('')

// ── 分页 ──
const {
  items,
  loading,
  refreshing,
  error,
  hasMore,
  isEmpty,
  initialized,
  load,
  refresh,
  loadMore,
  retry,
  reset,
} = usePagination<OrderData>(
  (page, pageSize) => listMyOrders(activeTab.value || undefined, page, pageSize),
)

const firstLoading = computed(() => !initialized.value && loading.value)
const showError = computed(() => !!error.value && items.value.length === 0)

// ── Tab 切换 ──
function onTabChange() {
  reset()
  load()
}

// ── 下拉刷新 ──
async function onRefresh() {
  await refresh()
  if (error.value) {
    showToast('刷新失败')
  }
}

// ── 触底加载 ──
async function onLoadMore() {
  await loadMore()
}

// ── 导航 ──
function goToDetail(id: string) {
  router.push(`/orders/${id}`)
}

function onSearchClick() {
  showToast('订单搜索将在后续开放')
}

function onWechatNoticeClick() {
  if (isFeatureDisabled('wechatPush')) {
    showToast('公众号推送将在后续开放')
  }
}

function goHome() {
  router.push('/')
}

// ── 操作按钮定义 ──
function getActionButtons(order: OrderData): Array<{
  text: string
  variant: 'primary' | 'ghost' | 'danger' | 'success'
  onClick: () => void
}> {
  switch (order.orderStatus) {
    case 'pendingPay':
      return [
        {
          text: '取消订单',
          variant: 'ghost',
          onClick: async () => {
            try {
              await showConfirmDialog({ title: '提示', message: '确认取消该订单？' })
            } catch {
              return
            }
            try {
              await cancelOrder(order.id)
              showToast('已取消')
              reset()
              load()
            } catch (err) {
              const apiErr = err as { message?: string; code?: string }
              if (apiErr.code === 'ORDER_NOT_CANCELABLE') {
                showToast('订单不可取消')
              } else if (apiErr.message) {
                showToast(apiErr.message)
              }
            }
          },
        },
        {
          text: '去支付',
          variant: 'primary',
          onClick: () => router.push(`/orders/${order.id}`),
        },
      ]
    case 'paid':
      return [
        {
          text: '联系团长',
          variant: 'ghost',
          onClick: () => showToast('联系团长将在后续开放'),
        },
      ]
    case 'shipped':
      return [
        {
          text: '确认收货',
          variant: 'success',
          onClick: () => router.push(`/orders/${order.id}`),
        },
      ]
    default:
      return []
  }
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.orders-content {
  background: var(--color-bg);
  min-height: 200px;
  padding: 0 14px;
}

.orders-search {
  margin: 14px 14px 12px;
}

:deep(.van-tabs__wrap) {
  background: #fff;
  height: 54px;
}

:deep(.van-tab) {
  font-size: 16px;
  font-weight: 800;
}

.orders-empty {
  min-height: 280px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
}
</style>
