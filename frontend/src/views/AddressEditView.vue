<template>
  <PageLayout title="编辑地址" show-back @back="goBack">
    <LoadingView v-if="loading" />
    <ErrorView v-if="error && !loading" :message="error" @retry="fetchAddress" />
    <div v-if="address && !loading">
      <AddressForm
        ref="addressFormRef"
        :address="address"
        :loading="submitting"
        @submit="handleSubmit"
      />
      <!-- 删除地址 -->
      <div class="address-edit__danger">
        <AppButton variant="danger" block :disabled="submitting" @click="handleDelete">
          删除地址
        </AppButton>
      </div>
    </div>
    <template v-if="address && !loading && !submitting" #action>
      <AppFixedActions single>
        <AppButton variant="primary" @click="handleSave">保存修改</AppButton>
      </AppFixedActions>
    </template>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import AddressForm from '@/components/AddressForm.vue'
import AppFixedActions from '@/components/AppFixedActions.vue'
import AppButton from '@/components/AppButton.vue'
import { deleteAddress, listAddresses, updateAddress } from '@/api/addresses'
import type { AddressData } from '@/types'

const route = useRoute()
const router = useRouter()

const address = ref<AddressData | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const submitting = ref(false)
const addressFormRef = ref<InstanceType<typeof AddressForm> | null>(null)

async function fetchAddress() {
  loading.value = true
  error.value = null
  try {
    const id = route.params.id as string
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

/** 底部保存按钮 → 触发 AddressForm 内部表单验证和提交 */
function handleSave() {
  addressFormRef.value?.submit()
}

async function handleDelete() {
  if (!address.value) return
  try {
    await showConfirmDialog({
      title: '确认删除',
      message: '确定要删除这个地址吗？',
    })
  } catch {
    return
  }

  submitting.value = true
  try {
    await deleteAddress(address.value.id)
    showToast('删除成功')
    router.replace('/addresses')
  } catch (err) {
    const apiErr = err as { message?: string }
    showToast(apiErr.message || '删除失败')
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

<style scoped>
.address-edit__danger {
  padding: 0 14px 12px;
  margin-top: -4px;
}
</style>
