<template>
  <div class="merchant-page">
    <div class="page-head">
      <div>
        <p>履约运营台</p>
        <h1>今日待处理</h1>
      </div>
    </div>

    <LoadingView v-if="loading" text="正在加载商家工作台..." />
    <ErrorView v-else-if="error" :message="error" @retry="loadDashboard" />

    <template v-else>
      <section class="metric-grid" aria-label="关键待办">
        <button v-for="item in metrics" :key="item.label" type="button" class="metric" @click="router.push(item.to)">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.hint }}</small>
        </button>
      </section>

      <section class="panel-grid">
        <article class="panel">
          <div class="panel__head">
            <h2>最近待发货</h2>
            <RouterLink to="/merchant/orders">处理订单</RouterLink>
          </div>
          <table class="dense-table">
            <tbody>
              <tr v-for="order in recentOrders" :key="order.id">
                <td>
                  <strong>{{ order.orderNo }}</strong>
                  <span>{{ order.receiverName }} · {{ order.items[0]?.productName || '团购商品' }}</span>
                </td>
                <td>{{ formatAmount(order.payAmount) }}</td>
                <td><RouterLink :to="`/merchant/orders/${order.id}`">发货</RouterLink></td>
              </tr>
            </tbody>
          </table>
          <EmptyState v-if="recentOrders.length === 0" description="暂无待发货订单" />
        </article>

        <article class="panel">
          <div class="panel__head">
            <h2>最近售后</h2>
            <RouterLink to="/merchant/after-sales">处理售后</RouterLink>
          </div>
          <table class="dense-table">
            <tbody>
              <tr v-for="item in recentAfterSales" :key="item.id">
                <td>
                  <strong>售后单 {{ item.id }}</strong>
                  <span>{{ item.receiverName || item.buyerNickname || '买家' }} · {{ item.reason }}</span>
                </td>
                <td>{{ formatAmount(item.amount) }}</td>
                <td><RouterLink :to="`/merchant/after-sales/${item.id}`">处理</RouterLink></td>
              </tr>
            </tbody>
          </table>
          <EmptyState v-if="recentAfterSales.length === 0" description="暂无待处理售后" />
        </article>

        <article class="panel">
          <div class="panel__head">
            <h2>最近客服</h2>
            <RouterLink to="/merchant/chats">查看客服</RouterLink>
          </div>
          <table class="dense-table">
            <tbody>
              <tr v-for="chat in recentChats" :key="chat.id">
                <td>
                  <strong>{{ chat.buyerName }}</strong>
                  <span>{{ chat.lastMessageText || '暂无消息' }}</span>
                </td>
                <td><mark v-if="chat.unreadCount">{{ chat.unreadCount }}</mark></td>
                <td><RouterLink :to="`/chats/${chat.id}`">进入</RouterLink></td>
              </tr>
            </tbody>
          </table>
          <EmptyState v-if="recentChats.length === 0" description="暂无客服会话" />
        </article>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import EmptyState from '@/components/EmptyState.vue'
import { listChatConversations } from '@/api/chats'
import { listLeaderAfterSales } from '@/api/leaderAfterSales'
import { listLeaderOrdersByParams } from '@/api/leaderOrders'
import { getStoreWorkbenchSummary } from '@/api/stores'
import { formatAmount } from '@/utils'
import type { AfterSaleData, ChatConversationData, LeaderOrderData, StoreWorkbenchSummaryData } from '@/types'

const router = useRouter()
const loading = ref(true)
const error = ref('')
const summary = ref<StoreWorkbenchSummaryData | null>(null)
const recentOrders = ref<LeaderOrderData[]>([])
const recentAfterSales = ref<AfterSaleData[]>([])
const recentChats = ref<ChatConversationData[]>([])

const metrics = computed(() => [
  { label: '待发货订单', value: summary.value?.todos.paidOrders ?? 0, hint: '已支付待履约', to: '/merchant/orders?status=paid' },
  { label: '待处理售后', value: summary.value?.todos.pendingAfterSales ?? 0, hint: '退款申请待审核', to: '/merchant/after-sales?status=pending' },
  { label: '未读客服', value: summary.value?.todos.unreadLeaderChats ?? 0, hint: '买家咨询未读', to: '/merchant/chats' },
  { label: '进行中团购', value: summary.value?.todos.publishedGroupBuys ?? 0, hint: '当前可下单活动', to: '/merchant/group-buys' },
])

async function loadDashboard() {
  loading.value = true
  error.value = ''
  try {
    const [summaryData, orders, afterSales, chats] = await Promise.all([
      getStoreWorkbenchSummary(),
      listLeaderOrdersByParams({ status: 'paid', page: 1, pageSize: 5 }),
      listLeaderAfterSales({ status: 'pending', page: 1, pageSize: 5 }),
      listChatConversations({ role: 'leader', page: 1, pageSize: 5 }),
    ])
    summary.value = summaryData
    recentOrders.value = orders.items
    recentAfterSales.value = afterSales.items
    recentChats.value = chats.items
  } catch (err) {
    error.value = (err as { message?: string }).message || '商家工作台加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<style scoped>
.merchant-page {
  display: grid;
  gap: 18px;
}

.page-head p,
.metric span,
.metric small,
.dense-table span {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
}

.page-head h1 {
  margin: 4px 0 0;
  font-size: 26px;
  line-height: 1.2;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.metric,
.panel {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.metric {
  min-height: 112px;
  padding: 16px;
  text-align: left;
  font-family: inherit;
  cursor: pointer;
}

.metric strong {
  display: block;
  margin: 8px 0;
  color: #111827;
  font-size: 30px;
  line-height: 1;
}

.panel-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.panel:last-child {
  grid-column: 1 / -1;
}

.panel {
  padding: 16px;
}

.panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.panel__head h2 {
  margin: 0;
  font-size: 17px;
}

.panel a,
.dense-table a {
  color: #d63f2b;
  font-weight: 800;
  text-decoration: none;
}

.dense-table {
  width: 100%;
  border-collapse: collapse;
}

.dense-table tr {
  border-top: 1px solid #eef2f7;
}

.dense-table td {
  padding: 12px 4px;
  vertical-align: middle;
  font-size: 14px;
}

.dense-table td:first-child {
  width: 70%;
}

.dense-table strong,
.dense-table span {
  display: block;
}

.dense-table mark {
  display: inline-flex;
  min-width: 22px;
  height: 22px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #e9563f;
  color: #ffffff;
  font-size: 12px;
}

@media (max-width: 1100px) {
  .metric-grid,
  .panel-grid {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
