<template>
  <PageLayout title="团长订单管理" show-back @back="goBack">
    <LoadingView v-if="firstLoading" />
    <ErrorView v-else-if="showError" :message="error ?? undefined" @retry="load" />

    <div v-else class="orders-root">
      <!-- 状态筛选 chips（同 /leader/products 模式） -->
      <div class="row gap-10 overflow-hidden chips-wrap">
        <span
          v-for="c in chips"
          :key="c.key"
          class="chip"
          :class="{ active: activeChip === c.key }"
          @click="onChipChange(c.key)"
        >{{ c.label }}</span>
      </div>

      <!-- 搜索占位（同 /leader/products 模式） -->
      <div class="marketplace-search">
        <van-icon name="search" size="16" />
        <span>搜索订单号 / 买家 / 商品</span>
      </div>

      <div class="orders-list">
        <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
          <van-list
            v-model:loading="loading"
            :finished="!hasMore"
            finished-text="没有更多了"
            :error="error !== null"
            error-text="加载失败，点击重试"
            :immediate-check="false"
            @load="loadMore"
          >
            <!-- 紧凑订单卡片（同 /leader/products 的列表密度） -->
            <div
              v-for="order in items"
              :key="order.id"
              class="order-row"
              @click="goToDetail(order.id)"
            >
              <div class="order-row__cover">{{ (order.items[0]?.productName || '订').slice(0, 2) }}</div>
              <div class="order-row__info">
                <div class="order-row__name van-multi-ellipsis--l2">
                  {{ order.items[0]?.productName || '团购商品' }}
                </div>
                <div class="order-row__meta">
                  <span v-if="(order as any).buyerName">买家：{{ (order as any).buyerName }}</span>
                  <span>订单号 {{ order.orderNo }}</span>
                </div>
              </div>
              <div class="order-row__right">
                <span :class="['order-row__status', `order-row__status--${order.orderStatus}`]">
                  {{ getOrderStatusText(order.orderStatus) }}
                </span>
                <AppButton variant="primary" size="sm" @click.stop="goToDetail(order.id)">
                  {{ order.orderStatus === 'paid' ? '发货' : '查看' }}
                </AppButton>
              </div>
            </div>

            <EmptyState v-if="isEmpty" description="暂无订单" />
          </van-list>
        </van-pull-refresh>
      </div>
    </div>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import EmptyState from '@/components/EmptyState.vue'
import AppButton from '@/components/AppButton.vue'
import { usePagination, useSmartNavigation } from '@/composables'
import { listLeaderOrders } from '@/api/leaderOrders'
import { getOrderStatusText } from '@/utils/status'

const router = useRouter()
const { goBack } = useSmartNavigation('/leader/dashboard')

const chips = [
  { key: '', label: '全部' },
  { key: 'paid', label: '待发货' },
  { key: 'shipped', label: '已发货' },
  { key: 'completed', label: '已完成' },
  { key: 'canceled', label: '已取消' },
]
const activeChip = ref('')

const {
  items, loading, refreshing, error, hasMore, isEmpty,
  initialized, load, refresh, loadMore, reset,
} = usePagination(
  (page, pageSize) => listLeaderOrders(activeChip.value || undefined, page, pageSize),
)

const firstLoading = computed(() => !initialized.value && loading.value)
const showError = computed(() => !!error.value && items.value.length === 0)

function onChipChange(key: string) {
  activeChip.value = key
  reset()
  load()
}

async function onRefresh() {
  await refresh()
  if (error.value) {
    showToast('刷新失败')
  }
}

function goToDetail(id: string) { router.push(`/leader/orders/${id}`) }

onMounted(() => { load() })
</script>

<style scoped>
.orders-root {
  background: var(--color-bg);
  min-height: 200px;
  padding-bottom: 14px;
}

.chips-wrap {
  padding: 10px 14px;
  overflow-x: auto;
  white-space: nowrap;
}

/* ── chip 与 /leader/products 一致 ── */
.chip {
  display: inline-flex;
  align-items: center;
  min-height: 38px;
  padding: 9px 18px;
  border-radius: 8px;
  background: #f2f3f5;
  color: var(--color-text-secondary);
  font-size: var(--font-size-md);
  font-weight: 700;
  cursor: pointer;
  flex-shrink: 0;
  user-select: none;
}
.chip.active {
  background: var(--color-primary);
  color: #fff;
}

/* ── 搜索占位 ── */
.marketplace-search {
  margin: 0 14px 12px;
}

/* ── 列表 ── */
.orders-list {
  padding: 0 14px;
}

/* ── 紧凑订单行（同 ProductListItem 密度） ── */
.order-row {
  display: flex;
  gap: 12px;
  align-items: center;
  background: var(--color-bg-card);
  border-radius: 12px;
  padding: 13px;
  margin-bottom: 10px;
  box-shadow: var(--shadow-card);
  cursor: pointer;
}
.order-row:active {
  opacity: 0.85;
}

.order-row__cover {
  width: 48px;
  height: 48px;
  min-width: 48px;
  border-radius: 8px;
  background: linear-gradient(145deg, #cfddff, #ec715b);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 900;
  font-size: 14px;
  text-shadow: 0 1px 8px rgb(0 0 0 / 18%);
}

.order-row__info {
  flex: 1;
  min-width: 0;
}

.order-row__name {
  font-size: var(--font-size-md);
  font-weight: 800;
  color: var(--color-text-primary);
  line-height: 1.35;
  margin-bottom: 2px;
}

.order-row__meta {
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.order-row__right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  flex-shrink: 0;
}

.order-row__status {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 999px;
  white-space: nowrap;
  background: #f2f4f5;
  color: var(--color-text-hint);
}
.order-row__status--pendingPay { background: #fff2e8; color: #f36b2a; }
.order-row__status--paid { background: #eafaf1; color: var(--color-primary); }
.order-row__status--shipped { background: #eafaf1; color: var(--color-primary); }
.order-row__status--completed { background: #f2f4f5; color: var(--color-text-hint); }
.order-row__status--canceled { background: #f2f4f5; color: var(--color-text-hint); }
</style>
