<template>
  <PageLayout title="商品管理" show-back @back="goBack">
    <template #action>
      <div class="page-actions">
        <van-button type="primary" round @click="goToNew">新建商品</van-button>
      </div>
    </template>

    <LoadingView v-if="firstLoading" />
    <ErrorView v-else-if="showError" :message="error ?? undefined" @retry="load" />

    <div v-else class="products-content">
      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <van-list
          :loading="loading"
          :finished="!hasMore"
          finished-text="没有更多了"
          :error="error !== null"
          error-text="加载失败，点击重试"
          :immediate-check="false"
          @load="loadMore"
        >
          <div
            v-for="product in items"
            :key="product.id"
            class="product-card"
          >
            <div class="product-card__cover">
              <img
                v-if="product.coverImageUrl"
                :src="product.coverImageUrl"
                :alt="product.name"
                class="product-card__cover-img"
              />
              <van-icon v-else name="photo" :size="40" color="var(--color-text-hint)" />
            </div>
            <div class="product-card__info">
              <div class="product-card__name">{{ product.name }}</div>
              <div class="product-card__price">{{ formatAmount(product.basePriceAmount) }}</div>
              <div class="product-card__meta">
                <span>库存：{{ product.stock }}</span>
                <van-tag :type="product.status === 'active' ? 'success' : 'default'">
                  {{ product.status === 'active' ? '上架' : '下架' }}
                </van-tag>
              </div>
            </div>
            <div class="product-card__actions">
              <van-button round @click="goToEdit(product.id)">编辑</van-button>
              <van-button round type="danger" @click="handleDelete(product)">删除</van-button>
            </div>
          </div>

          <EmptyState v-if="isEmpty" description="暂无商品" />
        </van-list>
      </van-pull-refresh>
    </div>
  </PageLayout>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import EmptyState from '@/components/EmptyState.vue'
import { usePagination } from '@/composables/usePagination'
import { listProducts, deleteProduct } from '@/api/products'
import { formatAmount } from '@/utils'
import type { ProductData } from '@/types'

const router = useRouter()

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
} = usePagination<ProductData>(listProducts)

const firstLoading = computed(() => !initialized.value && loading.value)
const showError = computed(() => !!error.value && items.value.length === 0)

function goBack() {
  router.back()
}

function goToNew() {
  router.push('/leader/products/new')
}

function goToEdit(id: string) {
  router.push(`/leader/products/${id}/edit`)
}

async function handleDelete(product: ProductData) {
  try {
    await showConfirmDialog({
      title: '确认删除',
      message: `确定要删除商品「${product.name}」吗？此操作不可恢复。`,
    })
  } catch {
    return
  }
  try {
    await deleteProduct(product.id)
    showToast('删除成功')
    load()
  } catch (err) {
    const apiErr = err as { message?: string }
    showToast(apiErr.message || '删除失败')
  }
}

async function onRefresh() {
  await refresh()
  if (error.value) {
    showToast('刷新失败')
  }
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.page-actions {
  padding: 8px 14px;
}

.products-content {
  padding: 0 14px;
  background: var(--color-bg);
  min-height: 200px;
}

.product-card {
  background: var(--color-bg-white);
  border-radius: 10px;
  padding: 14px;
  margin-bottom: 10px;
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.product-card__cover {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  background: var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.product-card__cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-card__info {
  flex: 1;
  min-width: 0;
}

.product-card__name {
  font-size: var(--font-size-md);
  font-weight: 900;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.product-card__price {
  font-size: var(--font-size-sm);
  color: var(--color-price);
  font-weight: 900;
  margin-bottom: 4px;
}

.product-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
}

.product-card__actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex-shrink: 0;
}

.product-card__actions :deep(.van-button--small) {
  min-width: 56px;
  height: 32px;
  border-radius: 999px !important;
  font-size: 12px;
}
</style>
