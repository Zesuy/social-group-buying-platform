<template>
  <PageLayout title="编辑地址" show-back @back="goBack">
    <LoadingView v-if="loading" />
    <ErrorView v-if="error && !loading" :message="error" @retry="fetchAddress" />
    <AddressForm
      v-if="address && !loading"
      :address="address"
      :loading="submitting"
      @submit="handleSubmit"
    />
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import AddressForm from '@/components/AddressForm.vue'
import { listAddresses, updateAddress } from '@/api/addresses'
import type { AddressData } from '@/types'

const route = useRoute()
const router = useRouter()

const address = ref<AddressData | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const submitting = ref(false)

async function fetchAddress() {
  loading.value = true
  error.value = null
  try {
    const id = Number(route.params.id)
    const all = await listAddresses()
    const found = all.find(a => a.id === id)
    if (found) {
      address.value = found
    } else {
      error.value = '地址不存在'
    }
  } catch (err) {
    const apiErr = err as { message?: string }
    error.value = apiErr.message || '加载地址失败'
  } finally {
    loading.value = false
  }
}

async function handleSubmit(data: {
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault: boolean
}) {
  if (!address.value) return
  submitting.value = true
  try {
    await updateAddress(address.value.id, data)
    showToast('地址更新成功')
    router.push('/addresses')
  } catch (err) {
    const apiErr = err as { message?: string }
    showToast(apiErr.message || '更新失败')
  } finally {
    submitting.value = false
  }
}

function goBack() {
  router.back()
}

onMounted(() => {
  fetchAddress()
})
</script>
