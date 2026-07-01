<template>
  <PageLayout show-tab-bar>
    <div class="app-topbar">
      我购买的订单
    </div>

    <div class="notice-strip orders-notice">
      <span>关注公众号，收到活动和订单、物流通知</span>
      <button type="button" @click="onWechatNoticeClick">关注</button>
    </div>

    <!-- 搜索占位 -->
    <div class="orders-search marketplace-search" @click="onSearchClick">
      <van-icon name="search" size="16" />
      <span>搜索商品名 / 订单号 / 团长</span>
    </div>

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
      <div class="page-note">
        订单列表主态：顶部搜索 + 状态 Tab；订单卡片露出团长、商品快照、金额、状态和可操作按钮。
      </div>
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
            :class="['order-card', `order-card--${order.orderStatus}`]"
            @click="goToDetail(order.id)"
          >
            <!-- 头部：店铺信息 + 状态标签 -->
            <div class="order-head">
              <b>
                <span :class="['state-dot', getStateDotClass(order.orderStatus)]" />
                {{ getStoreText(order) }}
              </b>
              <span :class="['tiny-pill', getTinyPillClass(order.orderStatus)]">
                {{ getOrderStatusText(order.orderStatus) }}
              </span>
            </div>

            <!-- 商品快照 -->
            <div class="order-body">
              <div class="order-card__snapshot">
                <div class="order-card__item-cover">
                  <span>{{ getCoverText(order) }}</span>
                </div>
                <div class="order-card__item-info">
                  <span class="order-card__item-name van-multi-ellipsis--l2">
                    {{ order.items[0]?.productName || '团购商品' }}
                  </span>
                  <span class="order-card__item-meta">
                    <template v-if="order.items[0]?.skuName">规格：{{ order.items[0]?.skuName }}｜</template>
                    数量 x{{ order.items[0]?.quantity || 0 }}
                  </span>
                  <span class="order-card__item-meta order-card__state-meta">
                    {{ getOrderHint(order.orderStatus) }}
                  </span>
                  <div class="amount-line">
                    <span class="order-card__order-no">订单号 {{ order.orderNo }}</span>
                    <span class="order-card__pay">
                      实付
                      <PriceText :amount="order.payAmount" size="md" color="var(--color-price)" />
                    </span>
                  </div>
                </div>
              </div>
              <div v-if="order.items.length > 1" class="order-card__more">
                等{{ order.items.length }}件商品
              </div>
            </div>

            <!-- 操作按钮 -->
            <div v-if="getActionButtons(order.orderStatus).length > 0" class="order-actions">
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
import PriceText from '@/components/PriceText.vue'
import { usePagination } from '@/composables/usePagination'
import { listMyOrders, cancelOrder } from '@/api/orders'
import { getOrderStatusText } from '@/utils/status'
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

// ── 订单卡片辅助 ──
function getStoreText(order: OrderData): string {
  return `店铺 #${order.storeId}`
}

function getOrderHint(status: string): string {
  switch (status) {
    case 'pendingPay':
      return '待完成模拟支付，超时后订单将关闭'
    case 'paid':
      return '已支付，等待团长发货'
    case 'shipped':
      return '团长已发货，收到后可确认收货'
    case 'completed':
      return '交易已完成'
    case 'canceled':
      return '订单已取消'
    default:
      return '订单处理中'
  }
}

function getCoverText(order: OrderData): string {
  const name = order.items[0]?.productName?.trim()
  return name ? name.slice(0, 2) : '商品'
}

/** 获取 state-dot 颜色类 */
function getStateDotClass(status: string): string {
  switch (status) {
    case 'pendingPay': return 'orange'
    case 'completed': return 'gray'
    case 'canceled': return 'gray'
    default: return ''
  }
}

/** 获取 tiny-pill 颜色类 */
function getTinyPillClass(status: string): string {
  switch (status) {
    case 'pendingPay': return 'orange'
    case 'paid':
    case 'shipped': return 'green'
    case 'canceled':
    case 'completed': return ''
    default: return ''
  }
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
    case 'paid':
      return [
        { text: '联系团长', type: 'default', action: 'contact' },
      ]
    case 'shipped':
      return [
        { text: '确认收货', type: 'success', action: 'complete' },
      ]
    default:
      return []
  }
}

const actionLoadingId = ref<string | null>(null)

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
        router.push(`/orders/${order.id}`)
        break
      case 'contact':
        showToast('联系团长将在后续开放')
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
  padding: 0 14px;
}

.orders-search {
  margin: 14px 14px 12px;
}

.orders-notice button {
  border: 0;
  background: var(--color-primary);
  color: #fff;
  border-radius: 999px;
  padding: 5px 12px;
  min-height: 34px;
  font-weight: 900;
  flex-shrink: 0;
}

:deep(.van-tabs__wrap) {
  background: #fff;
  height: 54px;
}

:deep(.van-tab) {
  font-size: 16px;
  font-weight: 800;
}

.order-card__snapshot {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.order-card__item-cover {
  width: 72px;
  height: 72px;
  background: linear-gradient(145deg, #cfddff, #ec715b);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
  font-weight: 900;
  text-align: center;
  line-height: 1.2;
  text-shadow: 0 1px 8px rgb(0 0 0 / 18%);
}

.order-card--paid .order-card__item-cover {
  background: linear-gradient(145deg, #ffd273, #55aa5d);
}

.order-card--shipped .order-card__item-cover {
  background: linear-gradient(145deg, #ddd, #383d46);
}

.order-card--completed .order-card__item-cover {
  background: linear-gradient(145deg, #dcefe0, #8fbf97);
}

.order-card--canceled .order-card__item-cover {
  background: linear-gradient(145deg, #e9edf2, #aeb6c2);
}

.order-card__item-info {
  flex: 1;
  min-width: 0;
}

.order-card__item-name {
  font-size: var(--font-size-md);
  color: var(--color-text-primary);
  font-weight: 800;
  display: block;
  line-height: 1.35;
}

.order-card__item-meta {
  display: block;
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
  margin-top: 4px;
}

.order-card__state-meta {
  color: #7a808a;
  background: #f5f6f7;
  border-radius: 999px;
  display: inline-flex;
  width: fit-content;
  max-width: 100%;
  padding: 4px 8px;
}

.order-card__more {
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
  padding-top: 8px;
}

.order-card__order-no {
  color: var(--color-text-hint);
  font-size: 12px;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-card__pay {
  color: var(--color-text-primary);
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
}

.orders-empty {
  min-height: 280px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.order-actions :deep(.van-button--small) {
  min-width: 84px;
  height: 36px;
  border-radius: 999px !important;
  font-weight: 800;
}
</style>
