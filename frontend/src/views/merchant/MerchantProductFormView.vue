<template>
  <div class="merchant-page">
    <div class="page-head">
      <div>
        <p>商品库</p>
        <h1>{{ isEdit ? '编辑商品' : '新建商品' }}</h1>
      </div>
      <button type="button" class="ghost-link" @click="handleBack">返回</button>
    </div>

    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" show-retry @retry="loadProduct" />

    <form v-else class="product-form" @submit.prevent="handleSave">
      <section class="form-panel">
        <h2>基础信息</h2>
        <label class="field">
          <span>商品名称</span>
          <input v-model="form.name" placeholder="例如：临安山核桃仁" />
        </label>
        <label class="field">
          <span>商品描述</span>
          <textarea v-model="form.description" rows="7" placeholder="规格、产地、口感、保存方式和配送注意事项" />
        </label>
        <label class="field">
          <span>商品分类</span>
          <select v-model="form.categoryId" :disabled="saving || categoriesLoading">
            <option value="" disabled>{{ categoriesLoading ? '分类加载中...' : '请选择商品分类' }}</option>
            <option v-for="category in categories" :key="category.id" :value="category.id">
              {{ category.name }}
            </option>
          </select>
          <small v-if="categoriesError" class="field-error">{{ categoriesError }}</small>
        </label>
        <div class="field-grid">
          <label class="field">
            <span>基础价</span>
            <input v-model="form.basePriceYuan" inputmode="decimal" placeholder="0.00" />
          </label>
          <label class="field">
            <span>库存</span>
            <input v-model="form.stock" inputmode="numeric" placeholder="库存数量" />
          </label>
        </div>
        <label v-if="isEdit" class="switch-row">
          <span>
            <strong>商品状态</strong>
            <small>{{ form.status === 'active' ? '上架商品可被选入团购' : '下架后不作为可售商品展示' }}</small>
          </span>
          <input v-model="form.status" type="checkbox" true-value="active" false-value="inactive" />
        </label>
      </section>

      <aside class="form-panel">
        <h2>图片素材</h2>
        <div class="upload-block">
          <span>封面图</span>
          <ImageUploader
            v-model="form.coverImageUrl"
            :disabled="saving"
            :preview-alt="form.name || '商品封面'"
            demo-kind="product"
            :show-url-input="false"
            :show-hint="false"
            button-label="更换封面"
          />
        </div>
        <div class="upload-block">
          <span>详情图</span>
          <div class="detail-grid">
            <div v-for="(_, index) in form.detailImageUrls" :key="index" class="detail-cell">
              <ImageUploader
                v-model="form.detailImageUrls[index]"
                :disabled="saving"
                :preview-alt="`${form.name || '商品'}详情图${index + 1}`"
                demo-kind="product"
                :show-url-input="false"
                :show-hint="false"
                variant="tile"
                tile-label="上传"
              />
              <button type="button" aria-label="移除详情图" @click="removeDetailImage(index)">
                <van-icon name="cross" />
              </button>
            </div>
            <button v-if="form.detailImageUrls.length < 9" type="button" class="add-image" @click="addDetailImage">
              <van-icon name="plus" />
              添加
            </button>
          </div>
        </div>
      </aside>

      <div class="form-actions">
        <button v-if="isEdit" type="button" class="danger-button" :disabled="saving" @click="handleDelete">删除商品</button>
        <button type="submit" class="primary-button" :disabled="saving">
          {{ saving ? '保存中...' : '保存商品' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import ErrorView from '@/components/ErrorView.vue'
import ImageUploader from '@/components/ImageUploader.vue'
import LoadingView from '@/components/LoadingView.vue'
import { listCategories } from '@/api/categories'
import { createProduct, deleteProduct, getProduct, updateProduct } from '@/api/products'
import { useSmartNavigation, useUnsavedChangesGuard } from '@/composables'
import { amountToYuan, getDemoProductImage } from '@/utils'
import type { ProductCategoryData, ProductData } from '@/types'

const route = useRoute()
const { goAfterSuccess } = useSmartNavigation('/merchant/products')

const product = ref<ProductData | null>(null)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const categories = ref<ProductCategoryData[]>([])
const categoriesLoading = ref(false)
const categoriesError = ref('')
const initialSnapshot = ref('')

const isEdit = computed(() => typeof route.params.id === 'string')
const form = reactive({
  name: '',
  description: '',
  categoryId: '',
  coverImageUrl: '',
  detailImageUrls: [] as string[],
  basePriceYuan: '',
  stock: '',
  status: 'active',
})
const unsavedGuard = useUnsavedChangesGuard({
  isDirty: () => !loading.value && JSON.stringify(form) !== initialSnapshot.value,
})

function markClean() {
  initialSnapshot.value = JSON.stringify(form)
}

function fillForm(data: ProductData) {
  form.name = data.name
  form.description = data.description || ''
  form.categoryId = data.categoryId ? String(data.categoryId) : ''
  form.coverImageUrl = data.coverImageUrl || ''
  form.detailImageUrls = [...(data.detailImageUrls || [])]
  form.basePriceYuan = String(amountToYuan(data.basePriceAmount))
  form.stock = String(data.stock)
  form.status = data.status
  markClean()
}

function validate(): string | null {
  if (!form.name.trim()) return '请输入商品名称'
  if (categoriesLoading.value) return '商品分类加载中，请稍后再试'
  if (categoriesError.value) return categoriesError.value
  if (!form.categoryId) return '请选择商品分类'
  if (Number.isNaN(Number(form.basePriceYuan)) || Number(form.basePriceYuan) < 0) return '请输入有效价格'
  if (!Number.isInteger(Number(form.stock)) || Number(form.stock) < 0) return '请输入有效库存'
  return null
}

function buildPayload() {
  return {
    name: form.name.trim(),
    description: form.description.trim() || null,
    coverImageUrl: form.coverImageUrl.trim() || getDemoProductImage(form.name),
    detailImageUrls: form.detailImageUrls.map((url) => url.trim()).filter(Boolean),
    basePriceAmount: Math.round(Number(form.basePriceYuan || 0) * 100),
    stock: Number(form.stock),
    categoryId: form.categoryId,
    ...(isEdit.value ? { status: form.status } : {}),
  }
}

function applyDefaultCategory() {
  if (form.categoryId || categories.value.length === 0) return
  form.categoryId = categories.value[0].id
  if (!isEdit.value) markClean()
}

async function loadCategories() {
  categoriesLoading.value = true
  categoriesError.value = ''
  try {
    categories.value = await listCategories()
    applyDefaultCategory()
  } catch (err) {
    categoriesError.value = (err as { message?: string }).message || '商品分类加载失败'
  } finally {
    categoriesLoading.value = false
  }
}

function addDetailImage() {
  if (form.detailImageUrls.length < 9) form.detailImageUrls.push('')
}

function removeDetailImage(index: number) {
  form.detailImageUrls.splice(index, 1)
}

async function loadProduct() {
  if (!isEdit.value) return
  loading.value = true
  error.value = ''
  try {
    product.value = await getProduct(route.params.id as string)
    fillForm(product.value)
  } catch (err) {
    error.value = (err as { message?: string }).message || '商品详情加载失败'
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  const errMsg = validate()
  if (errMsg) {
    showToast(errMsg)
    return
  }

  saving.value = true
  try {
    if (isEdit.value) {
      await updateProduct(route.params.id as string, buildPayload())
      showToast('保存成功')
    } else {
      await createProduct(buildPayload())
      showToast('创建成功')
    }
    markClean()
    unsavedGuard.allowNextNavigation()
    await goAfterSuccess('/merchant/products')
  } catch (err) {
    showToast((err as { message?: string }).message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleBack() {
  const canLeave = await unsavedGuard.confirmLeave()
  if (!canLeave) return
  unsavedGuard.allowNextNavigation()
  await goAfterSuccess('/merchant/products')
}

async function handleDelete() {
  if (!product.value) return
  try {
    await showConfirmDialog({ title: '删除商品', message: `确定删除「${product.value.name}」吗？` })
  } catch {
    return
  }
  saving.value = true
  try {
    await deleteProduct(product.value.id)
    showToast('删除成功')
    markClean()
    unsavedGuard.allowNextNavigation()
    await goAfterSuccess('/merchant/products')
  } catch (err) {
    showToast((err as { message?: string }).message || '删除失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  markClean()
  void loadCategories()
  void loadProduct()
})
</script>

<style scoped>
.merchant-page {
  display: grid;
  gap: 16px;
}

.page-head {
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

.ghost-link {
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  padding: 0 14px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #fff;
  color: #374151;
  font-size: 13px;
  font-weight: 900;
  font-family: inherit;
  text-decoration: none;
  cursor: pointer;
}

.product-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 16px;
  align-items: start;
}

.form-panel {
  display: grid;
  gap: 14px;
  padding: 18px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.form-panel h2 {
  margin: 0;
  color: #111827;
  font-size: 16px;
}

.field,
.upload-block {
  display: grid;
  gap: 8px;
}

.field span,
.upload-block > span {
  color: #374151;
  font-size: 13px;
  font-weight: 900;
}

.field input,
.field select,
.field textarea {
  width: 100%;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 10px 12px;
  background: #fff;
  color: #111827;
  font-size: 14px;
  outline: 0;
}

.field-error {
  color: #b42318;
  font-size: 12px;
  line-height: 1.4;
}

.field textarea {
  resize: vertical;
  line-height: 1.55;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
}

.switch-row strong,
.switch-row small {
  display: block;
}

.switch-row strong {
  font-size: 13px;
}

.switch-row small {
  margin-top: 3px;
  color: #6b7280;
}

.switch-row input {
  width: 20px;
  height: 20px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.detail-cell {
  position: relative;
}

.detail-cell > button {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 24px;
  height: 24px;
  border: 0;
  border-radius: 999px;
  background: #111827;
  color: #fff;
}

.add-image {
  aspect-ratio: 1;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: #f9fafb;
  color: #4b5563;
  font-weight: 900;
}

.form-actions {
  grid-column: 1 / -1;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.primary-button,
.danger-button {
  min-height: 40px;
  padding: 0 18px;
  border: 0;
  border-radius: 8px;
  color: #fff;
  font-weight: 900;
}

.primary-button {
  background: #e9563f;
}

.danger-button {
  background: #b42318;
}

.primary-button:disabled,
.danger-button:disabled {
  opacity: 0.62;
}

@media (max-width: 1100px) {
  .product-form {
    grid-template-columns: 1fr;
  }
}
</style>
