<template>
  <div class="merchant-page">
    <div class="page-head">
      <div>
        <p>订单详情</p>
        <h1>{{ order?.orderNo || '订单' }}</h1>
      </div>
      <button type="button" class="ghost-link" @click="goBack()">返回</button>
    </div>

    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" @retry="loadOrder" />

    <template v-else-if="order">
      <section class="detail-grid">
        <article class="panel">
          <h2>收货信息</h2>
          <p><strong>{{ order.receiverName }}</strong> {{ order.receiverPhone }}</p>
          <p>{{ order.fullAddress }}</p>
        </article>
        <article class="panel">
          <h2>支付信息</h2>
          <p>{{ formatAmount(order.payAmount) }} · {{ getOrderStatusText(order.orderStatus) }}</p>
          <p>支付时间：{{ formatDateTime(order.paidAt) }}</p>
        </article>
      </section>

      <section class="panel">
        <h2>商品信息</h2>
        <table class="merchant-table">
          <tbody>
            <tr v-for="item in order.items" :key="item.id">
              <td>{{ item.productName }}</td>
              <td>{{ formatAmount(item.unitPriceAmount) }}</td>
              <td>x{{ item.quantity }}</td>
              <td>{{ formatAmount(item.totalAmount) }}</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section v-if="order.orderStatus === 'paid'" class="panel ship-panel">
        <h2>确认发货</h2>
        <form class="ship-form" @submit.prevent="submitShip">
          <label>
            <span>配送方式</span>
            <select v-model="shipForm.deliveryType">
              <option value="express">快递配送</option>
              <option value="pickup">到店自提</option>
              <option value="local_delivery">同城配送</option>
            </select>
          </label>
          <label>
            <span>物流公司</span>
            <input v-model="shipForm.logisticsCompany" placeholder="如 顺丰速运">
          </label>
          <label>
            <span>物流单号</span>
            <input v-model="shipForm.trackingNo" placeholder="可选">
          </label>
          <button type="submit" :disabled="shipLoading">{{ shipLoading ? '提交中...' : '确认发货' }}</button>
        </form>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showToast } from 'vant'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import { getLeaderOrder, shipOrder } from '@/api/leaderOrders'
import { useSmartNavigation } from '@/composables'
import { formatAmount, formatDateTime, getOrderStatusText } from '@/utils'
import type { LeaderOrderData } from '@/types'

const route = useRoute()
const { goBack } = useSmartNavigation('/merchant/orders')
const loading = ref(true)
const error = ref('')
const shipLoading = ref(false)
const order = ref<LeaderOrderData | null>(null)
const shipForm = reactive({
  deliveryType: 'express',
  logisticsCompany: '',
  trackingNo: '',
})

async function loadOrder() {
  loading.value = true
  error.value = ''
  try {
    order.value = await getLeaderOrder(route.params.id as string)
  } catch (err) {
    error.value = (err as { message?: string }).message || '订单详情加载失败'
  } finally {
    loading.value = false
  }
}

async function submitShip() {
  shipLoading.value = true
  try {
    await shipOrder(route.params.id as string, shipForm)
    showToast('发货成功')
    await loadOrder()
  } catch (err) {
    showToast((err as { message?: string }).message || '发货失败')
  } finally {
    shipLoading.value = false
  }
}

onMounted(loadOrder)
</script>

<style scoped>
.merchant-page {
  display: grid;
  gap: 16px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.page-head p {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
}

.page-head h1 {
  margin: 4px 0 0;
  font-size: 26px;
}

.page-head a,
.ghost-link,
.merchant-table a {
  border: 0;
  background: transparent;
  color: #d63f2b;
  font-family: inherit;
  font-weight: 900;
  text-decoration: none;
  cursor: pointer;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.panel {
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.panel h2 {
  margin: 0 0 10px;
  font-size: 17px;
}

.panel p {
  margin: 6px 0;
  color: #374151;
  font-size: 14px;
}

.merchant-table {
  width: 100%;
  border-collapse: collapse;
}

.merchant-table td {
  padding: 11px 4px;
  border-top: 1px solid #eef2f7;
  font-size: 14px;
}

.ship-form {
  display: grid;
  grid-template-columns: 180px 220px 220px auto;
  gap: 12px;
  align-items: end;
}

.ship-form label span {
  display: block;
  margin-bottom: 6px;
  color: #6b7280;
  font-size: 13px;
  font-weight: 800;
}

.ship-form input,
.ship-form select {
  width: 100%;
  height: 36px;
  padding: 0 10px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font: inherit;
}

.ship-form button {
  height: 36px;
  padding: 0 16px;
  border: 0;
  border-radius: 8px;
  background: #e9563f;
  color: #ffffff;
  font: inherit;
  font-weight: 900;
  cursor: pointer;
}
</style>
