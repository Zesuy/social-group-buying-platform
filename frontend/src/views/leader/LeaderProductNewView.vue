<template>
  <PageLayout title="新建商品" show-back @back="goBack">
    <LoadingView v-if="saving" />
    <div v-else class="content-area">
      <ProductForm ref="formRef" :product="null" :submitting="saving" />
    </div>

    <template #action>
      <div class="fixed-actions single">
        <button class="btn primary" :disabled="saving" @click="handleSave">
          {{ saving ? '保存中...' : '保存商品' }}
        </button>
      </div>
    </template>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ProductForm from '@/components/ProductForm.vue'
import { createProduct } from '@/api/products'

const router = useRouter()
const saving = ref(false)
const formRef = ref<InstanceType<typeof ProductForm> | null>(null)

function goBack() {
  router.back()
}

async function handleSave() {
  if (!formRef.value) return
  const err = formRef.value.validate?.()
  if (err) {
    showToast(err)
    return
  }
  const data = formRef.value.getFormData?.()
  if (!data) {
    showToast('请填写完整信息')
    return
  }

  saving.value = true
  try {
    await createProduct(data)
    showToast('创建成功')
    router.push('/leader/products')
  } catch (err) {
    const apiErr = err as { message?: string }
    showToast(apiErr.message || '创建失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.content-area {
  padding: 16px 0;
}
.fixed-actions {
  background: #fff;
  border-top: 1px solid #eee;
  padding: 10px 14px calc(10px + var(--safe-area-bottom, 0px));
  display: grid;
  gap: 12px;
}
.fixed-actions.single {
  grid-template-columns: 1fr;
}
.fixed-actions .btn {
  height: 50px;
  font-size: 18px;
  border-radius: 8px;
}
.btn {
  border: 0;
  border-radius: 9px;
  padding: 8px 14px;
  font-weight: 800;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 14px;
}
.btn.primary {
  background: var(--color-primary);
  color: #fff;
}
.btn:disabled {
  opacity: 0.5;
}
</style>
