<template>
  <PageLayout title="新增地址" show-back @back="goBack">
    <AddressForm ref="addressFormRef" :loading="submitting" @submit="handleSubmit" />
    <template #action>
      <AppFixedActions single>
        <AppButton variant="primary" :disabled="submitting" @click="handleSave">保存地址</AppButton>
      </AppFixedActions>
    </template>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import AddressForm from '@/components/AddressForm.vue'
import AppFixedActions from '@/components/AppFixedActions.vue'
import AppButton from '@/components/AppButton.vue'
import { useCheckoutStore } from '@/stores'
import { createAddress } from '@/api/addresses'

const router = useRouter()
const route = useRoute()
const checkoutStore = useCheckoutStore()
const submitting = ref(false)
const addressFormRef = ref<InstanceType<typeof AddressForm> | null>(null)

const isFromCheckout = computed(() => route.query.from === 'checkout')

async function handleSubmit(data: {
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault: boolean
}) {
  submitting.value = true
  try {
    const created = await createAddress(data)
    showToast('地址保存成功')

    if (isFromCheckout.value) {
      checkoutStore.setAddress(created.id)
      router.replace('/checkout')
    } else {
      router.push('/addresses')
    }
  } catch (err) {
    const apiErr = err as { message?: string }
    showToast(apiErr.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

function handleSave() {
  addressFormRef.value?.submit()
}

function goBack() {
  router.back()
}
</script>
