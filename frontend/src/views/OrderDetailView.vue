<template>
  <PageLayout title="订单详情" show-back @back="goBack">
    <!-- 加载态 -->
    <LoadingView v-if="loading" />

    <!-- 订单创建成功视图（最小占位） -->
    <div v-else class="order-detail">
      <div class="order-detail__success">
        <van-icon name="success" :size="56" color="var(--color-primary)" />
        <h2 class="order-detail__title">订单提交成功</h2>
        <p class="order-detail__hint">等待支付功能开发</p>
      </div>

      <div class="order-card">
        <div class="order-card__row">
          <span class="order-card__label">订单编号</span>
          <span class="order-card__value">{{ orderNo }}</span>
        </div>
        <div class="order-card__row">
          <span class="order-card__label">订单状态</span>
          <span class="order-card__value order-card__value--primary">待支付</span>
        </div>
      </div>

      <div class="order-actions">
        <van-button round block type="primary" disabled @click="handlePay">
          立即支付
        </van-button>
        <p class="order-actions__tip">支付功能将在后续版本开放</p>
      </div>
    </div>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const orderNo = ref('')

function goBack() {
  router.push('/orders')
}

function handlePay() {
  showToast('支付功能开发中')
}

onMounted(() => {
  const id = route.params.id
  // 最小占位，不调用后端 API
  orderNo.value = `OR${String(id).padStart(8, '0')}`
  loading.value = false
})
</script>

<style scoped>
.order-detail {
  padding: var(--spacing-lg);
}

.order-detail__success {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--spacing-xl) 0;
}

.order-detail__title {
  font-size: var(--font-size-xl);
  font-weight: 600;
  color: var(--color-text-primary);
  margin-top: var(--spacing-md);
}

.order-detail__hint {
  margin-top: var(--spacing-xs);
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
}

.order-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-card);
  padding: var(--spacing-lg);
  margin-top: var(--spacing-md);
}

.order-card__row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.order-card__label {
  font-size: var(--font-size-md);
  color: var(--color-text-secondary);
}

.order-card__value {
  font-size: var(--font-size-md);
  color: var(--color-text-primary);
}

.order-card__value--primary {
  color: var(--color-primary);
  font-weight: 500;
}

.order-actions {
  margin-top: var(--spacing-xl);
}

.order-actions__tip {
  text-align: center;
  margin-top: var(--spacing-sm);
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
}
</style>
