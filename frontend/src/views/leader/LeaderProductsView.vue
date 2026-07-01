<template>
  <PageLayout title="商品管理" show-back @back="goBack">
    <template #action>
      <AppFixedActions single>
        <AppButton variant="primary" @click="goToNew">新建商品</AppButton>
      </AppFixedActions>
    </template>

    <LoadingView v-if="firstLoading" />
    <ErrorView v-else-if="showError" :message="error ?? undefined" @retry="load" />

    <div v-else class="products-content">
      <!-- 分类 + 新建（行内） -->
      <div class="row between mb-12">
        <div class="row gap-10 overflow-hidden">
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
      <div class="marketplace-search mb-14">
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
          <ProductListItem
            v-for="product in items"
            :key="product.id"
            :item="product"
            @click="goToDetail(product.id)"
          >
            <template #actions>
              <AppButton variant="ghost" @click.stop="handleEdit(product)">编辑</AppButton>
              <AppButton variant="danger" @click.stop="handleDelete(product)">删除</AppButton>
            </template>
          </ProductListItem>

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
import ProductListItem from '@/components/ProductListItem.vue'
import AppButton from '@/components/AppButton.vue'
import AppFixedActions from '@/components/AppFixedActions.vue'
import { usePagination } from '@/composables/usePagination'
import { listProducts, deleteProduct } from '@/api/products'
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

function goToDetail(id: string) { router.push(`/leader/products/${id}`) }

function goToNew() { router.push('/leader/products/new') }

function handleEdit(product: ProductData) { router.push(`/leader/products/${product.id}/edit`) }

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

.mb-12 { margin-bottom: 12px; }
.gap-10 { gap: 10px; }
.overflow-hidden { overflow: hidden; }
.mb-14 { margin-bottom: 14px; }
</style>
