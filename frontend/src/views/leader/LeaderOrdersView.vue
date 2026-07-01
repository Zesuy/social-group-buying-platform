<template>
  <PageLayout title="团长订单列表" show-back @back="goBack">
    <div class="orders-content">
      <van-tabs v-model:active="activeTab" @change="onTabChange">
        <van-tab v-for="tab in tabs" :key="tab.key" :title="tab.label" />
      </van-tabs>

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
          <div v-for="o in items" :key="o.id" class="order-card" @click="goToDetail(o.id)">
            <div class="order-head">
              <b>{{ o.orderNo }}</b>
              <span class="status-chip">{{ getOrderStatusText(o.orderStatus) }}</span>
            </div>
            <div class="order-body">
              <div class="row" style="gap:10px">
                <div class="fake-img-sm">{{ o.items?.[0]?.productName?.charAt(0) || '?' }}</div>
                <div class="grow">
                  <b>{{ o.buyerNickname || o.receiverName }}</b>
                  <p class="muted" style="margin:4px 0;font-size:13px">{{ o.items?.[0]?.productName }}{{ o.items?.length > 1 ? ` 等${o.items.length}件` : '' }} x{{ o.items?.[0]?.quantity }}</p>
                  <b style="color:#ff602a;font-size:18px">{{ formatAmount(o.payAmount) }}</b>
                </div>
                <button v-if="o.orderStatus === 'paid'" class="btn primary" @click.stop="goToDetail(o.id)">去发货</button>
              </div>
            </div>
          </div>

          <EmptyState v-if="isEmpty" description="暂无订单" />
        </van-list>
      </van-pull-refresh>
    </div>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import PageLayout from '@/components/PageLayout.vue'
import EmptyState from '@/components/EmptyState.vue'
import { usePagination } from '@/composables/usePagination'
import { listLeaderOrders } from '@/api/leaderOrders'
import { getOrderStatusText, formatAmount } from '@/utils'

const router = useRouter()
const activeTab = ref(0)
const tabs = [
  { key: '', label: '全部' },
  { key: 'paid', label: '待发货' },
  { key: 'shipped', label: '已发货' },
  { key: 'completed', label: '已完成' },
  { key: 'canceled', label: '已取消' },
]

const { items, loading, refreshing, error, hasMore, isEmpty, load, refresh, loadMore } = usePagination(
  (page, pageSize) => listLeaderOrders(tabs[activeTab.value]?.key || undefined, page, pageSize),
)

function goBack() { router.back() }
function onTabChange() { load() }
function onRefresh() { refresh() }
function goToDetail(id: string) { router.push(`/leader/orders/${id}`) }
watch(activeTab, () => { load() })
</script>

<style scoped>
.orders-content { background: var(--color-bg); min-height: 200px; padding: 14px; }

/* ── Tab 样式覆写 — demo .tabs 视觉 ── */
.orders-content :deep(.van-tabs__wrap) { background:#fff; height:54px; }
.orders-content :deep(.van-tab) { font-size:18px; color:#60646c; padding-bottom:13px; }
.orders-content :deep(.van-tab--active) { color:#111; font-weight:900; }
.orders-content :deep(.van-tabs__line) { background:var(--color-primary); height:3px; border-radius:6px; bottom:9px; }

/* ── 订单卡片 ── */
.order-card { background:#fff; border-radius:14px; margin-bottom:12px; overflow:hidden; box-shadow:0 1px 0 rgba(0,0,0,.03); }
.order-head { padding:12px 14px; border-bottom:1px solid #edf0f2; display:flex; align-items:center; justify-content:space-between; font-size:13px; }
.order-body { padding:12px 14px; }
.fake-img-sm { width:72px; height:72px; border-radius:8px; background:linear-gradient(145deg,#dcefe0,#a1c49f); display:flex; align-items:center; justify-content:center; color:#fff; font-weight:900; flex:none; font-size:20px; }
.status-chip { background:#eafaf1; color:var(--color-primary); padding:4px 8px; border-radius:99px; font-size:12px; font-weight:900; white-space:nowrap; }
</style>
