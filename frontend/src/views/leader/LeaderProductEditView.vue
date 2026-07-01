<template>
  <PageLayout title="编辑商品" show-back @back="goBack">
    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" @retry="fetchProduct" />
    <template v-else-if="product">
      <div class="content-area">
        <AppCard>
          <ProductForm ref="formRef" :product="product" :submitting="saving" />
        </AppCard>
        <AppButton variant="danger" block :disabled="saving" @click="handleDelete">删除商品</AppButton>
      </div>
    </template>

    <template #action>
      <AppFixedActions v-if="product">
        <AppButton variant="ghost" :disabled="saving" @click="handleToggleStatus">{{ product?.status === 'active' ? '下架' : '上架' }}</AppButton>
        <AppButton variant="primary" :disabled="saving" @click="handleSave">{{ saving ? '保存中...' : '保存修改' }}</AppButton>
      </AppFixedActions>
    </template>
  </PageLayout>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import ProductForm from '@/components/ProductForm.vue'
import AppFixedActions from '@/components/AppFixedActions.vue'
import AppButton from '@/components/AppButton.vue'
import AppCard from '@/components/AppCard.vue'
import { getProduct, updateProduct, deleteProduct } from '@/api/products'
import type { ProductData } from '@/types'
const route = useRoute()
const router = useRouter()
const loading = ref(true)
const error = ref<string | null>(null)
const product = ref<ProductData | null>(null)
const saving = ref(false)
const formRef = ref<InstanceType<typeof ProductForm> | null>(null)
async function fetchProduct() {
  loading.value = true; error.value = null
  try { product.value = await getProduct(route.params.id as string) }
  catch (err) { error.value = (err as { message?: string }).message || '加载失败' }
  finally { loading.value = false }
}
function goBack() { router.back() }
async function handleSave() {
  if (!formRef.value || !product.value) return
  const err = formRef.value.validate?.()
  if (err) { showToast(err); return }
  const data = formRef.value.getFormData?.()
  if (!data) { showToast('请填写完整信息'); return }
  saving.value = true
  try { await updateProduct(product.value.id, data); showToast('保存成功'); router.push('/leader/products') }
  catch (err) { showToast((err as { message?: string }).message || '保存失败') }
  finally { saving.value = false }
}
async function handleToggleStatus() {
  if (!product.value) return
  const ns = product.value.status === 'active' ? 'inactive' : 'active'
  saving.value = true
  try { await updateProduct(product.value.id, { status: ns }); showToast(ns === 'active' ? '已上架' : '已下架'); product.value.status = ns }
  catch (err) { showToast((err as { message?: string }).message || '操作失败') }
  finally { saving.value = false }
}
async function handleDelete() {
  if (!product.value) return
  try { await showConfirmDialog({ title: '确认删除', message: `确定删除「${product.value.name}」？` }) } catch { return }
  saving.value = true
  try { await deleteProduct(product.value.id); showToast('删除成功'); router.push('/leader/products') }
  catch (err) { showToast((err as { message?: string }).message || '删除失败') }
  finally { saving.value = false }
}
onMounted(() => { fetchProduct() })
</script>
<style scoped>
.content-area { padding: 16px 14px calc(var(--actionbar-height) + var(--safe-area-bottom) + 14px); }
.content-area .app-button--danger { margin-top: 12px; }
</style>
