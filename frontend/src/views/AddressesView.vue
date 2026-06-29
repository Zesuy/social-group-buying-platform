<template>
  <PageLayout title="地址管理" show-back @back="goBack">
    <LoadingView v-if="loading" />
    <ErrorView v-if="error && !loading" :message="error" @retry="loadAddresses" />

    <!-- 地址列表 -->
    <div class="addresses-list" v-if="!loading">
      <AddressCard
        v-for="addr in addresses"
        :key="addr.id"
        :address="addr"
        :selectable="isSelectMode"
        :selected="addr.id === selectedId"
        @click="handleAddressClick(addr)"
        @edit="handleEdit"
        @delete="handleDelete"
      />

      <!-- 空态 -->
      <EmptyState v-if="addresses.length === 0" description="还没有收货地址">
        <van-button round type="primary" size="small" @click="goToNew">
          新增地址
        </van-button>
      </EmptyState>
    </div>

    <!-- 新增地址按钮 -->
    <div v-if="!loading && addresses.length > 0" class="addresses-add">
      <van-button round block type="primary" plain icon="plus" @click="goToNew">
        新增地址
      </van-button>
    </div>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import AddressCard from '@/components/AddressCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { useCheckoutStore } from '@/stores'
import { listAddresses, deleteAddress } from '@/api/addresses'
import type { AddressData } from '@/types'

const router = useRouter()
const route = useRoute()
const checkoutStore = useCheckoutStore()

const addresses = ref<AddressData[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

// 是否从 checkout 进入的选择模式
const isSelectMode = computed(() => route.query.from === 'checkout')
const selectedId = computed(() => checkoutStore.selectedAddressId)

async function loadAddresses() {
  loading.value = true
  error.value = null
  try {
    addresses.value = await listAddresses()
  } catch (err) {
    const apiErr = err as { message?: string }
    error.value = apiErr.message || '加载地址失败'
  } finally {
    loading.value = false
  }
}

function handleAddressClick(addr: AddressData) {
  if (isSelectMode.value) {
    // 选择地址并返回 checkout
    checkoutStore.setAddress(addr.id)
    router.back()
  }
}

function handleEdit(id: number) {
  router.push(`/addresses/${id}/edit`)
}

async function handleDelete(id: number) {
  try {
    await showConfirmDialog({
      title: '确认删除',
      message: '确定要删除这个地址吗？',
    })
    await deleteAddress(id)
    addresses.value = addresses.value.filter(a => a.id !== id)
    showToast('删除成功')
  } catch {
    // 用户取消删除不处理
  }
}

function goToNew() {
  const query = route.query.from ? { from: route.query.from as string } : undefined
  router.push({ path: '/addresses/new', query })
}

function goBack() {
  router.back()
}

onMounted(() => {
  loadAddresses()
})
</script>

<style scoped>
.addresses-list {
  padding: var(--spacing-md);
}

.addresses-add {
  padding: var(--spacing-md);
  position: sticky;
  bottom: 0;
  background: var(--color-bg);
}
</style>
