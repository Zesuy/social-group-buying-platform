<template>
  <PageLayout title="收货地址" show-back @back="goBack">
    <LoadingView v-if="loading" />
    <ErrorView v-if="error && !loading" :message="error" @retry="loadAddresses" />

    <!-- 地址列表 -->
    <div class="addresses-scroll" v-if="!loading">
      <div
        v-for="addr in addresses"
        :key="addr.id"
        class="list-item"
        :class="{ 'list-item--selectable': isSelectMode }"
        @click="handleAddressClick(addr)"
      >
        <div class="grow">
          <b>{{ addr.receiverName }} {{ addr.receiverPhone }}</b>
          <p class="muted" style="margin:2px 0">{{ addr.fullAddress }}</p>
          <span v-if="addr.isDefault" class="status-chip">默认</span>
        </div>
        <div v-if="!isSelectMode" style="display:flex;gap:8px;align-items:center">
          <button class="btn ghost" @click.stop="handleEdit(addr.id)">编辑</button>
        </div>
      </div>

      <!-- 空态 -->
      <EmptyState v-if="addresses.length === 0" description="还没有收货地址">
        <button class="btn primary" @click="goToNew" style="margin-top:10px">新增地址</button>
      </EmptyState>
    </div>

    <!-- 固定新增按钮 -->
    <template v-if="!loading" #action>
      <div class="fixed-actions single">
        <button class="btn primary" @click="goToNew">+ 新增收货地址</button>
      </div>
    </template>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import EmptyState from '@/components/EmptyState.vue'
import { useCheckoutStore } from '@/stores'
import { listAddresses } from '@/api/addresses'
import type { AddressData } from '@/types'

const router = useRouter()
const route = useRoute()
const checkoutStore = useCheckoutStore()

const addresses = ref<AddressData[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

// 是否从 checkout 进入的选择模式
const isSelectMode = computed(() => route.query.from === 'checkout')

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
    checkoutStore.setAddress(addr.id)
    router.back()
  }
}

function handleEdit(id: string) {
  router.push(`/addresses/${id}/edit`)
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
.addresses-scroll {
  padding: 12px 14px 0;
}
</style>
