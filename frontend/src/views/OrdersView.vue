<template>
  <PageLayout show-tab-bar>
    <!-- 搜索占位 -->
    <van-search
      v-model="searchKeyword"
      shape="round"
      disabled
      placeholder="搜索订单"
    />

    <!-- 状态 Tab -->
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

    <!-- 错误重试（仅首次加载失败） -->
    <ErrorView
      v-else-if="showError"
      :message="error ?? undefined"
      @retry="retry"
    />

    <!-- 订单列表 -->
    <div v-else class="orders-content">
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
          <div
            v-for="order in items"
            :key="order.id"
            class="order-card"
            @click="goToDetail(order.id)"
          >
            <!-- 头部：店铺信息 + 状态标签 -->
            <div class="order-card__header">
              <span class="order-card__store">
                <van-icon name="shop-o" size="16" />
                {{ getStoreText(order) }}
              </span>
              <van-tag
                :color="statusTagColor(order.orderStatus)"
              >
                {{ getOrderStatusText(order.orderStatus) }}
              </van-tag>
            </div>

            <!-- 商品预览（最多 3 项） -->
            <div class="order-card__items">
              <div
                v-for="orderItem in order.items.slice(0, 3)"
                :key="orderItem.id"
                class="order-card__item"
              >
                <div class="order-card__item-icon">
                  <van-icon name="photo-o" size="36" />
                </div>
                <div class="order-card__item-info">
                  <span class="order-card__item-name">{{ orderItem.productName }}</span>
                  <span class="order-card__item-qty">x{{ orderItem.quantity }}</span>
                </div>
              </div>
              <div v-if="order.items.length > 3" class="order-card__more">
                等{{ order.items.length }}件商品
              </div>
            </div>

            <!-- 合计金额 -->
            <div class="order-card__amount">
              合计：
              <PriceText
                :amount="order.payAmount"
                size="md"
                color="var(--color-price)"
              />
            </div>

            <!-- 操作按钮 -->
            <div v-if="getActionButtons(order.orderStatus).length > 0" class="order-card__actions">
              <van-button
                v-for="btn in getActionButtons(order.orderStatus)"
                :key="btn.text"
                :type="btn.type"
                size="small"
                round
                :loading="actionLoadingId === order.id"
                @click.stop="handleOrderAction(order, btn.action)"
              >
                {{ btn.text }}
              </van-button>
            </div>
          </div>

          <!-- 空态 -->
          <EmptyState v-if="isEmpty" description="暂无订单" />
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
import PriceText from '@/components/PriceText.vue'
import { usePagination } from '@/composables/usePagination'
import { listMyOrders, cancelOrder } from '@/api/orders'
import { getOrderStatusText } from '@/utils/status'
import type { OrderData } from '@/types'

const router = useRouter()

// ── 搜索 ──
const searchKeyword = ref('')

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
  // 刷新失败时提示
  if (error.value) {
    showToast('刷新失败')
  }
}

// ── 触底加载 ──
async function onLoadMore() {
  await loadMore()
}

// ── 导航 ──
function goToDetail(id: number) {
  router.push(`/orders/${id}`)
}

// ── 订单卡片辅助 ──
function getStoreText(order: OrderData): string {
  return `店铺 #${order.storeId}`
}

function statusTagColor(status: string): string {
  const colorMap: Record<string, string> = {
    pendingPay: 'var(--color-primary)',
    paid: '#1989fa',
    shipped: '#07c160',
    completed: '#969799',
    canceled: '#969799',
  }
  return colorMap[status] ?? '#969799'
}

// ── 操作按钮定义 ──
interface ActionBtn {
  text: string
  type: 'primary' | 'default' | 'success' | 'danger'
  action: string
}

function getActionButtons(status: string): ActionBtn[] {
  switch (status) {
    case 'pendingPay':
      return [
        { text: '取消订单', type: 'default', action: 'cancel' },
        { text: '去支付', type: 'primary', action: 'pay' },
      ]
    case 'shipped':
      return [
        { text: '确认收货', type: 'success', action: 'complete' },
      ]
    default:
      return []
  }
}

const actionLoadingId = ref<number | null>(null)

async function handleOrderAction(order: OrderData, action: string) {
  actionLoadingId.value = order.id

  try {
    switch (action) {
      case 'cancel': {
        try {
          await showConfirmDialog({
            title: '提示',
            message: '确认取消该订单？',
          })
        } catch {
          return
        }
        await cancelOrder(order.id)
        showToast('已取消')
        reset()
        load()
        break
      }
      case 'pay':
      case 'complete':
        // 跳转详情页执行操作
        router.push(`/orders/${order.id}`)
        break
    }
  } catch (err) {
    const apiErr = err as { message?: string; code?: string }
    if (apiErr.code === 'ORDER_NOT_CANCELABLE') {
      showToast('订单不可取消')
    } else if (apiErr.message) {
      showToast(apiErr.message)
    }
  } finally {
    actionLoadingId.value = null
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
}

.order-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-card);
  margin: var(--spacing-sm) var(--spacing-md);
  padding: var(--spacing-md);
  cursor: pointer;
}

.order-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-sm);
  padding-bottom: var(--spacing-sm);
  border-bottom: 1px solid var(--color-border-light);
  min-height: 44px;
}

.order-card__store {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  gap: 4px;
}

.order-card__items {
  padding: var(--spacing-xs) 0;
}

.order-card__item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: 4px 0;
  min-height: 44px;
}

.order-card__item-icon {
  width: 40px;
  height: 40px;
  background: var(--color-bg);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.order-card__item-info {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-card__item-name {
  font-size: var(--font-size-md);
  color: var(--color-text-primary);
}

.order-card__item-qty {
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
}

.order-card__more {
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
  padding: 4px 0;
  min-height: 44px;
  display: flex;
  align-items: center;
}

.order-card__amount {
  text-align: right;
  font-size: var(--font-size-md);
  color: var(--color-text-primary);
  padding: var(--spacing-xs) 0;
  min-height: 44px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.order-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-sm);
  padding-top: var(--spacing-sm);
  border-top: 1px solid var(--color-border-light);
  min-height: 44px;
  align-items: center;
}
</style>
