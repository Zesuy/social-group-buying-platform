<template>
  <PageLayout title="收货地址" show-back @back="goBack">
    <LoadingView v-if="loading" />
    <ErrorView v-if="error && !loading" :message="error" @retry="loadAddresses" />

    <!-- 地址列表 -->
    <div class="addresses-scroll" v-if="!loading">
      <AppCard
        v-for="addr in addresses"
        :key="addr.id"
        :clickable="isSelectMode"
        @click="handleAddressClick(addr)"
      >
        <div class="address-card__row">
          <div class="address-card__info">
            <b>{{ addr.receiverName }} {{ addr.receiverPhone }}</b>
            <p class="address-card__detail">{{ addr.fullAddress }}</p>
            <AppStatusPill v-if="addr.isDefault" label="默认" variant="green" />
          </div>
          <AppButton v-if="!isSelectMode" variant="ghost" @click.stop="handleEdit(addr.id)">
            编辑
          </AppButton>
        </div>
      </AppCard>

      <!-- 空态 -->
      <EmptyState v-if="addresses.length === 0" description="还没有收货地址">
        <AppButton variant="primary" class="addresses-empty__btn" @click="goToNew">新增地址</AppButton>
      </EmptyState>
    </div>

    <!-- 固定新增按钮 -->
    <template v-if="!loading" #action>
      <AppFixedActions single>
        <AppButton variant="primary" @click="goToNew">+ 新增收货地址</AppButton>
      </AppFixedActions>
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
import AppCard from '@/components/AppCard.vue'
import AppButton from '@/components/AppButton.vue'
import AppFixedActions from '@/components/AppFixedActions.vue'
import AppStatusPill from '@/components/AppStatusPill.vue'
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

.address-card__row {
  display: flex;
  gap: 8px;
}

.address-card__info {
  flex: 1;
  min-width: 0;
}

.address-card__detail {
  margin: 2px 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.addresses-empty__btn {
  margin-top: 10px;
}
</style>
