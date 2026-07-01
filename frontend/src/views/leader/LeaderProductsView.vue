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
      <!-- 分类 + 新建（行内） -->
      <div class="row between" style="margin-bottom:12px">
        <div class="row" style="gap:10px;overflow:hidden">
          <span
            v-for="c in chips"
            :key="c.key"
            class="chip"
            :class="{ active: selectedChip === c.key }"
            @click="selectedChip = c.key"
          >{{ c.label }}</span>
        </div>
      </div>

      <!-- 搜索栏（demo .search） -->
      <div class="marketplace-search" style="margin-bottom:14px">
        <van-icon name="search" size="16" />
        <span>搜索商品名称</span>
      </div>

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
            class="list-item"
          >
            <div class="product-cover">
              <img
                v-if="product.coverImageUrl"
                :src="product.coverImageUrl"
                :alt="product.name"
                class="product-cover__img"
              />
              <van-icon v-else name="photo" :size="24" color="var(--color-text-hint)" />
            </div>
            <div class="product-info">
              <div class="product-info__name">{{ product.name }}</div>
              <div class="product-info__desc muted">
                库存 {{ product.stock }}｜{{ product.status === 'active' ? '已上架' : '已下架' }}
              </div>
              <div class="product-info__price">{{ formatAmount(product.basePriceAmount) }}</div>
            </div>
            <div class="product-actions">
              <button class="btn ghost" @click="goToEdit(product.id)">编辑</button>
              <button class="btn danger" @click="handleDelete(product)">删除</button>
            </div>
          </div>

          <EmptyState
            v-if="isEmpty"
            description="暂无商品"
          />
        </van-list>
      </van-pull-refresh>
    </div>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
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

const chips = [
  { key: 'all', label: '全部' },
  { key: 'active', label: '上架' },
  { key: 'inactive', label: '下架' },
]
const selectedChip = ref('all')

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

function goBack() { router.back() }

function goToNew() { router.push('/leader/products/new') }

function goToEdit(id: string) { router.push(`/leader/products/${id}/edit`) }

async function handleDelete(product: ProductData) {
  try {
    await showConfirmDialog({
      title: '确认删除',
      message: `确定要删除商品「${product.name}」吗？此操作不可恢复。`,
    })
  } catch { return }
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
  if (error.value) showToast('刷新失败')
}

onMounted(() => { load() })
</script>

<style scoped>
.page-actions { padding: 8px 14px; }

.products-content { padding: 14px; background: var(--color-bg); min-height: 200px; }

/* chips 筛选 */
.chip {
  background: #fff;
  border-radius: 8px;
  padding: 9px 18px;
  white-space: nowrap;
  color: var(--color-text-secondary);
  cursor: pointer;
  flex-shrink: 0;
  font-weight: 500;
}
.chip.active {
  background: #dff8eb;
  color: var(--color-primary);
  font-weight: 800;
}

.product-cover {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  background: var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.product-cover__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.product-info__name {
  font-size: var(--font-size-md);
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.35;
}

.product-info__desc {
  font-size: var(--font-size-sm);
}

.product-info__price {
  font-size: var(--font-size-lg);
  color: var(--color-price);
  font-weight: 900;
}

.product-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex-shrink: 0;
}
</style>
