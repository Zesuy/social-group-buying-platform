<template>
  <div class="merchant-page">
    <div class="page-head">
      <div>
        <p>商品库</p>
        <h1>商品管理</h1>
      </div>
      <RouterLink class="primary-link" to="/merchant/products/new">
        <van-icon name="plus" />
        新建商品
      </RouterLink>
    </div>

    <div class="toolbar">
      <div class="segmented">
        <button
          v-for="tab in statusTabs"
          :key="tab.value"
          type="button"
          :class="{ active: filters.status === tab.value }"
          @click="setStatus(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>
      <label class="search-box">
        <van-icon name="search" />
        <input v-model="filters.keyword" placeholder="搜索商品名称" @keyup.enter="loadProducts" />
      </label>
      <button type="button" class="ghost-button" @click="loadProducts">查询</button>
    </div>

    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" show-retry @retry="loadProducts" />

    <div v-else class="table-panel">
      <table class="merchant-table">
        <thead>
          <tr>
            <th>商品</th>
            <th>基础价</th>
            <th>库存</th>
            <th>状态</th>
            <th>更新时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="product in products" :key="product.id">
            <td>
              <div class="product-cell">
                <ImageWithFallback
                  :src="product.coverImageUrl"
                  :alt="product.name"
                  demo-kind="product"
                  width="48px"
                  height="48px"
                  radius="8px"
                />
                <div>
                  <strong>{{ product.name }}</strong>
                  <span>{{ product.description || '暂无描述' }}</span>
                </div>
              </div>
            </td>
            <td>{{ formatAmount(product.basePriceAmount) }}</td>
            <td>{{ product.stock }}</td>
            <td>
              <span class="status-pill" :class="product.status === 'active' ? 'green' : 'gray'">
                {{ product.status === 'active' ? '上架' : '下架' }}
              </span>
            </td>
            <td>{{ formatDateTime(product.updatedAt || product.createdAt) }}</td>
            <td>
              <div class="row-actions">
                <RouterLink :to="`/merchant/products/${product.id}/edit`">编辑</RouterLink>
                <button type="button" @click="handleDelete(product)">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <EmptyState v-if="products.length === 0" description="暂无商品" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import EmptyState from '@/components/EmptyState.vue'
import ErrorView from '@/components/ErrorView.vue'
import ImageWithFallback from '@/components/ImageWithFallback.vue'
import LoadingView from '@/components/LoadingView.vue'
import { deleteProduct, listProductsByParams } from '@/api/products'
import { formatAmount, formatDateTime } from '@/utils'
import type { ProductData } from '@/types'

const statusTabs = [
  { label: '全部', value: '' },
  { label: '上架', value: 'active' },
  { label: '下架', value: 'inactive' },
]

const loading = ref(true)
const error = ref('')
const products = ref<ProductData[]>([])
const filters = reactive({
  status: '',
  keyword: '',
})

function setStatus(status: string) {
  filters.status = status
  void loadProducts()
}

async function loadProducts() {
  loading.value = true
  error.value = ''
  try {
    const data = await listProductsByParams({
      page: 1,
      pageSize: 50,
      status: filters.status || undefined,
      keyword: filters.keyword.trim() || undefined,
    })
    products.value = data.items
  } catch (err) {
    error.value = (err as { message?: string }).message || '商品列表加载失败'
  } finally {
    loading.value = false
  }
}

async function handleDelete(product: ProductData) {
  try {
    await showConfirmDialog({
      title: '删除商品',
      message: `确定删除「${product.name}」吗？已被团购使用的商品可能无法删除。`,
    })
  } catch {
    return
  }

  try {
    await deleteProduct(product.id)
    showToast('删除成功')
    await loadProducts()
  } catch (err) {
    showToast((err as { message?: string }).message || '删除失败')
  }
}

onMounted(loadProducts)
</script>

<style scoped>
.merchant-page {
  display: grid;
  gap: 16px;
}

.page-head,
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
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

.primary-link,
.ghost-button {
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 900;
  text-decoration: none;
}

.primary-link {
  padding: 0 14px;
  background: #e9563f;
  color: #fff;
}

.ghost-button {
  padding: 0 13px;
  border: 1px solid #d1d5db;
  background: #fff;
  color: #374151;
}

.toolbar {
  justify-content: flex-start;
  flex-wrap: wrap;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.segmented {
  display: inline-flex;
  padding: 3px;
  border-radius: 8px;
  background: #f3f4f6;
}

.segmented button {
  min-width: 68px;
  height: 30px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #4b5563;
  font-weight: 800;
}

.segmented button.active {
  background: #fff;
  color: #d63f2b;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.08);
}

.search-box {
  width: min(360px, 100%);
  height: 36px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #fff;
}

.search-box input {
  min-width: 0;
  flex: 1;
  border: 0;
  outline: 0;
  font-size: 13px;
}

.table-panel {
  overflow-x: auto;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.merchant-table {
  width: 100%;
  min-width: 860px;
  border-collapse: collapse;
}

.merchant-table th,
.merchant-table td {
  padding: 13px 14px;
  border-bottom: 1px solid #eef2f7;
  text-align: left;
  font-size: 13px;
  vertical-align: middle;
}

.merchant-table th {
  background: #f9fafb;
  color: #6b7280;
  font-weight: 900;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 260px;
}

.product-cell strong,
.product-cell span {
  display: block;
}

.product-cell strong {
  color: #111827;
  font-size: 14px;
}

.product-cell span {
  max-width: 360px;
  margin-top: 3px;
  color: #6b7280;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  font-weight: 900;
}

.status-pill.green {
  background: #e8f8ef;
  color: #087a3f;
}

.status-pill.gray {
  background: #eef2f7;
  color: #4b5563;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.row-actions a,
.row-actions button {
  border: 0;
  background: transparent;
  color: #d63f2b;
  font-size: 13px;
  font-weight: 900;
  text-decoration: none;
}
</style>
