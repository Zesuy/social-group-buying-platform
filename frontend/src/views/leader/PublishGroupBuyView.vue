<template>
  <PageLayout title="发布团购" show-back @back="goBack">
    <div class="publish-content">
      <section class="publish-hero">
        <h2>发布普通团购</h2>
        <p>选择店铺商品，设置团购价和库存，明确配送时间后再发到社群。</p>
        <div class="publish-hero__meta">
          <span>{{ form.items.length }} 个商品</span>
          <span>{{ deliveryText }}</span>
        </div>
      </section>

      <div class="seg" role="tablist" aria-label="发布团购步骤">
        <button
          v-for="(tab, idx) in segTabs"
          :key="tab"
          type="button"
          :class="{ active: activeTab === idx }"
          @click="activeTab = idx"
        >
          {{ tab }}
        </button>
      </div>

      <div v-show="activeTab === 0" class="tab-panel">
        <AppFormCard>
          <template #title>
            <div class="card-title-row">
              <h3>团购信息</h3>
              <button
                type="button"
                class="text-action text-action--ai"
                :disabled="aiPolishing || submitting"
                @click="requestAiPolish"
              >
                <van-icon name="edit" />
                {{ aiPolishing ? '生成中' : 'AI 生成正文' }}
              </button>
            </div>
          </template>

          <div class="form-section">
            <label class="form-field">
              <span>标题</span>
              <input v-model="form.title" class="input" placeholder="团购标题，例如：周末阳山水蜜桃社区团" />
            </label>
            <label class="form-field">
              <span>介绍</span>
              <textarea
                v-model="form.introduction"
                class="textarea"
                placeholder="说明规格、口感、截单时间、发货方式和售后口径"
              />
            </label>
          </div>
        </AppFormCard>

        <AppFormCard title="活动正文">
          <div class="form-section">
            <ContentBlocksEditor v-model="form.contentBlocks" :disabled="submitting" />
          </div>
        </AppFormCard>

        <AppFormCard title="团购封面">
          <div class="form-section">
            <ImageUploader
              v-model="form.coverImageUrl"
              :disabled="submitting"
              :preview-alt="form.title || '团购封面'"
              demo-kind="cover"
              :show-url-input="false"
              :show-hint="false"
              button-label="更换封面"
            />
          </div>
        </AppFormCard>
      </div>

      <div v-show="activeTab === 1" class="tab-panel">
        <AppFormCard>
          <template #title>
            <div class="card-title-row">
              <h3>选择商品</h3>
              <button type="button" class="text-action" @click="goToProducts">商品库</button>
            </div>
          </template>

          <div class="product-picker">
            <label class="search-field">
              <van-icon name="search" />
              <input v-model="productKeyword" placeholder="搜索商品名称" />
            </label>

            <div v-if="productsLoading" class="picker-state">商品库加载中...</div>
            <div v-else-if="productsError" class="picker-state">{{ productsError }}</div>
            <div v-else-if="availableProducts.length === 0" class="picker-state">暂无可选上架商品</div>
            <div v-else class="library-list">
              <button
                v-for="product in availableProducts"
                :key="product.id"
                type="button"
                class="library-item"
                :class="{ selected: selectedProductIds.has(product.id) }"
                @click="addProductFromLibrary(product)"
              >
                <ImageWithFallback
                  :src="product.coverImageUrl"
                  :alt="product.name"
                  demo-kind="product"
                  width="56px"
                  height="56px"
                  radius="8px"
                />
                <span class="library-item__main">
                  <strong>{{ product.name }}</strong>
                  <small>{{ formatAmount(product.basePriceAmount) }} · 库存 {{ product.stock }}</small>
                </span>
                <van-icon :name="selectedProductIds.has(product.id) ? 'success' : 'plus'" />
              </button>
            </div>
          </div>
        </AppFormCard>

        <AppFormCard title="本次团购商品">
          <div class="selected-list">
            <div v-if="form.items.length === 0" class="selected-empty">
              还没有添加商品
            </div>
            <div v-for="(item, index) in form.items" :key="item.localId" class="selected-item">
              <div class="selected-item__head">
                <ImageUploader
                  v-if="!item.productId"
                  v-model="item.coverImageUrl"
                  :disabled="submitting"
                  :preview-alt="item.displayName || '商品封面'"
                  demo-kind="product"
                  :show-url-input="false"
                  :show-hint="false"
                  variant="tile"
                  tile-label="上传"
                />
                <ImageWithFallback
                  v-else
                  :src="item.coverImageUrl"
                  :alt="item.displayName || '团购商品'"
                  demo-kind="product"
                  width="52px"
                  height="52px"
                  radius="8px"
                />
                <label class="selected-item__name">
                  <span>商品名称</span>
                  <input v-model="item.displayName" class="input" placeholder="商品名称" />
                </label>
                <button
                  type="button"
                  class="icon-action"
                  :aria-label="`删除商品${index + 1}`"
                  @click="removeItem(index)"
                >
                  <van-icon name="cross" />
                </button>
              </div>
              <div class="selected-item__fields">
                <label>
                  <span>团购价</span>
                  <input
                    v-model="item.priceText"
                    class="input"
                    placeholder="0.00"
                    type="digit"
                    @input="onItemPriceInput(index, ($event.target as HTMLInputElement).value)"
                  />
                </label>
                <label>
                  <span>团购库存</span>
                  <input v-model.number="item.groupStock" class="input" placeholder="库存数量" type="number" />
                </label>
              </div>
              <label v-if="!item.productId" class="manual-description">
                <span>商品描述</span>
                <textarea
                  v-model="item.description"
                  class="textarea textarea--compact"
                  placeholder="规格、产地、保存方式等"
                />
              </label>
            </div>
            <button type="button" class="add-manual" @click="addManualItem">
              <van-icon name="plus" />
              新增商品
            </button>
          </div>
        </AppFormCard>
      </div>

      <div v-show="activeTab === 2" class="tab-panel">
        <AppFormCard title="履约设置">
          <div class="form-section">
            <div class="form-field">
              <span>配送方式</span>
              <div class="delivery-options">
                <label
                  v-for="option in deliveryOptions"
                  :key="option.value"
                  class="delivery-option"
                  :class="{ active: form.deliveryType === option.value }"
                >
                  <input v-model="form.deliveryType" type="radio" :value="option.value" />
                  <span>{{ option.label }}</span>
                </label>
              </div>
            </div>
            <label class="form-field">
              <span>开始时间</span>
              <input v-model="form.startTime" type="datetime-local" class="input" />
            </label>
            <label class="form-field">
              <span>结束时间</span>
              <input v-model="form.endTime" type="datetime-local" class="input" />
            </label>
            <label class="form-field">
              <span>履约时间</span>
              <input v-model="form.shippingTime" type="datetime-local" class="input" />
            </label>
            <button type="button" class="agreement" @click="form.agreed = !form.agreed">
              <span class="checkbox-circle" :class="{ checked: form.agreed }">
                <van-icon v-if="form.agreed" name="success" />
              </span>
              <span>我已确认商品、价格和履约信息真实有效</span>
            </button>
          </div>
        </AppFormCard>
      </div>
    </div>

    <van-popup
      v-model:show="aiPolishVisible"
      position="bottom"
      round
      class="ai-polish-popup"
      :style="{ maxHeight: '82vh' }"
    >
      <section class="ai-polish-sheet">
        <header class="ai-polish-sheet__header">
          <div>
            <h3>AI 润色建议</h3>
            <p>采用后会更新标题、介绍和活动正文，不会修改价格、库存和配送设置。</p>
          </div>
          <span class="ai-polish-sheet__tag">{{ polishSourceLabel }}</span>
        </header>

        <p v-if="form.items.length === 0" class="ai-polish-note">
          补充商品后再润色，文案会更具体。
        </p>
        <p v-if="polishSuggestion?.fallbackReason" class="ai-polish-note ai-polish-note--fallback">
          {{ polishSuggestion.fallbackReason }}
        </p>

        <div v-if="polishSuggestion" class="ai-polish-preview">
          <section class="ai-polish-preview__section">
            <span>建议标题</span>
            <strong>{{ polishSuggestion.title }}</strong>
          </section>

          <section class="ai-polish-preview__section">
            <span>建议介绍</span>
            <p>{{ polishSuggestion.introduction }}</p>
          </section>

          <section class="ai-polish-preview__section">
            <span>活动正文</span>
            <ContentBlocksPreview :blocks="polishSuggestion.contentBlocks" />
          </section>
        </div>

        <footer class="ai-polish-sheet__actions">
          <AppButton variant="ghost" :disabled="aiPolishing" @click="aiPolishVisible = false">
            取消
          </AppButton>
          <AppButton variant="plain" :loading="aiPolishing" @click="requestAiPolish">
            重新生成
          </AppButton>
          <AppButton :disabled="!polishSuggestion" @click="applyAiPolish">
            采用建议
          </AppButton>
        </footer>
      </section>
    </van-popup>

    <template #action>
      <AppFixedActions single>
        <AppButton variant="primary" :loading="submitting" @click="handleSubmit">
          {{ submitting ? '发布中...' : '发布团购' }}
        </AppButton>
      </AppFixedActions>
    </template>
  </PageLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import AppFormCard from '@/components/AppFormCard.vue'
import AppButton from '@/components/AppButton.vue'
import AppFixedActions from '@/components/AppFixedActions.vue'
import ContentBlocksEditor from '@/components/ContentBlocksEditor.vue'
import ContentBlocksPreview from '@/components/ContentBlocksPreview.vue'
import ImageUploader from '@/components/ImageUploader.vue'
import ImageWithFallback from '@/components/ImageWithFallback.vue'
import { createGroupBuy, polishGroupBuyCopy } from '@/api/leaderGroupBuys'
import { listProducts } from '@/api/products'
import { useSmartNavigation, useUnsavedChangesGuard } from '@/composables'
import { amountToYuan, formatAmount, getDemoProductImage, normalizeContentBlocks } from '@/utils'
import type { ContentBlockData, GroupBuyAiPolishResponse, ProductData } from '@/types'

const router = useRouter()
const { goBack, goAfterSuccess } = useSmartNavigation('/leader/group-buys')

const segTabs = ['介绍', '商品', '设置']
const activeTab = ref(0)
const submitting = ref(false)
const productsLoading = ref(false)
const productsError = ref('')
const productKeyword = ref('')
const products = ref<ProductData[]>([])
const aiPolishing = ref(false)
const aiPolishVisible = ref(false)
const polishSuggestion = ref<GroupBuyAiPolishResponse | null>(null)
let localItemId = 0

const deliveryOptions = [
  { value: 'express', label: '快递配送' },
  { value: 'pickup', label: '到店自提' },
  { value: 'local_delivery', label: '同城配送' },
]

interface ItemForm {
  localId: number
  productId?: string
  displayName: string
  priceText: string
  groupPriceAmount: number
  groupStock: number
  coverImageUrl: string
  description?: string
}

function createEmptyItem(): ItemForm {
  localItemId += 1
  return {
    localId: localItemId,
    displayName: '',
    priceText: '',
    groupPriceAmount: 0,
    groupStock: 0,
    coverImageUrl: '',
    description: '',
  }
}

const form = reactive({
  title: '',
  introduction: '',
  coverImageUrl: '',
  deliveryType: 'express',
  startTime: '',
  endTime: '',
  shippingTime: '',
  agreed: false,
  contentBlocks: [] as ContentBlockData[],
  items: [] as ItemForm[],
})
const initialSnapshot = JSON.stringify(form)
const unsavedGuard = useUnsavedChangesGuard({
  isDirty: () => JSON.stringify(form) !== initialSnapshot,
})

const selectedProductIds = computed(() => new Set(form.items.map((item) => item.productId).filter(Boolean) as string[]))
const availableProducts = computed(() => {
  const keyword = productKeyword.value.trim().toLowerCase()
  return products.value
    .filter((product) => product.status === 'active')
    .filter((product) => !keyword || product.name.toLowerCase().includes(keyword))
})
const deliveryText = computed(() => {
  return deliveryOptions.find((option) => option.value === form.deliveryType)?.label || '快递配送'
})
const polishSourceLabel = computed(() => polishSuggestion.value?.source === 'openai' ? 'OpenAI 生成' : '本地兜底')

function onItemPriceInput(index: number, val: string) {
  const num = parseFloat(val) || 0
  form.items[index].groupPriceAmount = Math.round(num * 100)
  form.items[index].priceText = val
}

function addManualItem() {
  form.items.push(createEmptyItem())
}

function addProductFromLibrary(product: ProductData) {
  if (selectedProductIds.value.has(product.id)) {
    showToast('该商品已在本次团购中')
    return
  }

  const item = {
    localId: ++localItemId,
    productId: product.id,
    displayName: product.name,
    priceText: String(amountToYuan(product.basePriceAmount)),
    groupPriceAmount: product.basePriceAmount,
    groupStock: product.stock,
    coverImageUrl: product.coverImageUrl || '',
    description: product.description || '',
  }

  form.items.push(item)
}

function removeItem(index: number) {
  form.items.splice(index, 1)
}

async function requestAiPolish() {
  if (aiPolishing.value) return
  aiPolishing.value = true
  try {
    polishSuggestion.value = await polishGroupBuyCopy({
      title: form.title,
      introduction: form.introduction,
      deliveryType: form.deliveryType,
      startTime: toISOWithTZ(form.startTime),
      endTime: toISOWithTZ(form.endTime),
      shippingTime: toISOWithTZ(form.shippingTime),
      items: form.items.map((item) => ({
        productId: item.productId,
        displayName: item.displayName,
        groupPriceAmount: item.groupPriceAmount,
        groupStock: item.groupStock,
        description: item.description || null,
      })),
    })
    aiPolishVisible.value = true
  } catch (err) {
    const apiErr = err as { message?: string }
    showToast(apiErr.message || 'AI 润色失败')
  } finally {
    aiPolishing.value = false
  }
}

function applyAiPolish() {
  if (!polishSuggestion.value) return
  form.title = polishSuggestion.value.title
  form.introduction = polishSuggestion.value.introduction
  form.contentBlocks = normalizeContentBlocks(polishSuggestion.value.contentBlocks)
  aiPolishVisible.value = false
  showToast('已采用润色文案')
}

function toISOWithTZ(dt: string): string | null {
  if (!dt) return null
  if (Number.isNaN(new Date(dt).getTime())) return null
  let normalized = dt
  if (normalized.length === 16) normalized += ':00'
  return `${normalized}+08:00`
}

function validate(): string | null {
  if (!form.title.trim()) return '请输入团购标题'
  if (form.items.length === 0) return '至少需要添加一个商品'
  for (const item of form.items) {
    if (!item.displayName.trim()) return '请填写所有商品的名称'
    if (item.groupPriceAmount <= 0) return '团购价格必须大于 0'
    if (item.groupStock <= 0) return '团购库存必须大于 0'
  }
  if (form.startTime && form.endTime && new Date(form.endTime) <= new Date(form.startTime)) {
    return '结束时间必须晚于开始时间'
  }
  if (!form.agreed) return '请确认团购信息真实有效'
  return null
}

function goToProducts() {
  router.push('/leader/products')
}

async function loadProducts() {
  productsLoading.value = true
  productsError.value = ''
  try {
    const data = await listProducts(1, 50)
    products.value = data.items
  } catch (err) {
    const apiErr = err as { message?: string }
    productsError.value = apiErr.message || '商品库加载失败'
  } finally {
    productsLoading.value = false
  }
}

async function handleSubmit() {
  const errMsg = validate()
  if (errMsg) {
    showToast(errMsg)
    return
  }

  submitting.value = true
  try {
    const contentBlocks = normalizeContentBlocks(form.contentBlocks)
    await createGroupBuy({
      title: form.title.trim(),
      introduction: form.introduction.trim() || null,
      coverImageUrl: form.coverImageUrl || getDemoProductImage(form.title),
      deliveryType: form.deliveryType,
      startTime: toISOWithTZ(form.startTime),
      endTime: toISOWithTZ(form.endTime),
      shippingTime: toISOWithTZ(form.shippingTime),
      contentBlocks: contentBlocks.length > 0 ? contentBlocks : undefined,
      items: form.items.map((item, index) => ({
        ...(item.productId
          ? { productId: item.productId }
          : {
              product: {
                name: item.displayName.trim(),
                description: item.description?.trim() || null,
                coverImageUrl: item.coverImageUrl || getDemoProductImage(item.displayName),
                basePriceAmount: item.groupPriceAmount,
                stock: item.groupStock,
              },
            }),
        displayName: item.displayName.trim(),
        groupPriceAmount: item.groupPriceAmount,
        groupStock: item.groupStock,
        sortOrder: index + 1,
      })),
    })
    showToast('发布成功')
    unsavedGuard.allowNextNavigation()
    await goAfterSuccess('/leader/group-buys')
  } catch (err) {
    const apiErr = err as { message?: string }
    showToast(apiErr.message || '发布失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadProducts()
})
</script>

<style scoped>
.publish-content {
  padding: 14px 14px calc(var(--actionbar-height) + var(--safe-area-bottom) + 20px);
}

.publish-hero {
  padding: 14px;
  margin-bottom: 12px;
  border-radius: var(--radius-card);
  background: var(--color-bg-card);
  box-shadow: var(--shadow-card);
}

.publish-hero h2 {
  margin: 0 0 6px;
  color: var(--color-text-primary);
  font-size: var(--font-size-xl);
  font-weight: 800;
}

.publish-hero p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-md);
  line-height: 1.55;
}

.publish-hero__meta {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.publish-hero__meta span {
  padding: 4px 8px;
  border-radius: 999px;
  background: var(--color-primary-light);
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.seg {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
  padding: 4px;
  margin-bottom: 12px;
  border-radius: 10px;
  background: var(--color-bg-card);
}

.seg button {
  min-height: 38px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--color-text-secondary);
  font-family: inherit;
  font-size: var(--font-size-md);
  font-weight: 700;
}

.seg button.active {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.tab-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-section,
.product-picker,
.selected-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: var(--color-text-primary);
  font-size: var(--font-size-md);
  font-weight: 700;
}

.input,
.textarea,
.search-field input {
  width: 100%;
  min-height: 44px;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  outline: none;
  background: var(--color-bg-surface);
  color: var(--color-text-primary);
  font-family: inherit;
  font-size: var(--font-size-md);
}

.input {
  padding: 0 12px;
}

.textarea {
  min-height: 128px;
  padding: 12px;
  line-height: 1.55;
  resize: vertical;
}

.input:focus,
.textarea:focus,
.search-field input:focus {
  border-color: var(--color-primary);
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.card-title-row h3 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 18px;
  font-weight: 800;
}

.text-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-height: 36px;
  padding: 0 10px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: var(--color-bg-card);
  color: var(--color-primary);
  font-family: inherit;
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.text-action:disabled {
  opacity: 0.55;
}

.text-action--ai {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.ai-polish-popup {
  left: 50%;
  width: 100%;
  max-width: 480px;
  transform: translateX(-50%);
  overflow: hidden;
}

.ai-polish-sheet {
  display: flex;
  max-height: 82vh;
  flex-direction: column;
  background: var(--color-bg-page);
}

.ai-polish-sheet__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 16px 12px;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-card);
}

.ai-polish-sheet__header h3 {
  margin: 0 0 6px;
  color: var(--color-text-primary);
  font-size: 18px;
  font-weight: 900;
}

.ai-polish-sheet__header p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.55;
}

.ai-polish-sheet__tag {
  flex: 0 0 auto;
  padding: 4px 8px;
  border-radius: 999px;
  background: var(--color-primary-light);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: 800;
}

.ai-polish-note {
  margin: 12px 14px 0;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--color-warning-light, #fff7e8);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.5;
}

.ai-polish-note--fallback {
  background: var(--color-bg-surface);
  color: var(--color-text-secondary);
}

.ai-polish-preview {
  display: flex;
  overflow-y: auto;
  flex: 1;
  flex-direction: column;
  gap: 10px;
  padding: 12px 14px;
}

.ai-polish-preview__section {
  padding: 12px;
  border-radius: 10px;
  background: var(--color-bg-card);
  box-shadow: var(--shadow-card);
}

.ai-polish-preview__section > span,
.ai-block b {
  display: block;
  margin-bottom: 6px;
  color: var(--color-text-hint);
  font-size: var(--font-size-xs);
  font-weight: 800;
}

.ai-polish-preview__section > strong,
.ai-block strong {
  display: block;
  color: var(--color-text-primary);
  font-size: var(--font-size-md);
  font-weight: 900;
  line-height: 1.45;
}

.ai-polish-preview__section p,
.ai-block p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-md);
  line-height: 1.65;
}

.ai-block-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ai-block {
  padding: 10px;
  border: 1px solid var(--color-border);
  border-radius: 9px;
  background: var(--color-bg-surface);
}

.ai-block ul {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-left: 18px;
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-md);
  line-height: 1.55;
}

.ai-polish-sheet__actions {
  display: grid;
  grid-template-columns: 0.85fr 1fr 1fr;
  gap: 8px;
  padding: 10px 14px calc(var(--safe-area-bottom) + 12px);
  border-top: 1px solid var(--color-border);
  background: var(--color-bg-card);
}

.ai-polish-sheet__actions :deep(.app-button) {
  min-width: 0;
  padding: 0 10px;
}

.search-field {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  padding: 0 12px;
  border-radius: 10px;
  background: var(--color-bg-surface);
  color: var(--color-text-hint);
}

.search-field input {
  min-height: 42px;
  padding: 0;
  border: 0;
  background: transparent;
}

.picker-state {
  padding: 14px;
  border-radius: 10px;
  background: var(--color-bg-surface);
  color: var(--color-text-hint);
  font-size: var(--font-size-md);
  text-align: center;
}

.library-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.library-item {
  display: grid;
  grid-template-columns: 56px 1fr auto;
  gap: 10px;
  align-items: center;
  min-height: 76px;
  padding: 10px;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  background: var(--color-bg-card);
  color: var(--color-text-primary);
  font-family: inherit;
  text-align: left;
}

.library-item.selected {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.library-item__main {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.library-item__main strong {
  overflow: hidden;
  font-size: var(--font-size-md);
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.library-item__main small {
  color: var(--color-text-hint);
  font-size: var(--font-size-sm);
}

.selected-item {
  padding: 12px;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  background: var(--color-bg-card);
}

.selected-empty {
  padding: 18px 12px;
  border: 1px dashed var(--color-border);
  border-radius: 10px;
  background: var(--color-bg-surface);
  color: var(--color-text-hint);
  font-size: var(--font-size-md);
  text-align: center;
}

.selected-item__head {
  display: grid;
  grid-template-columns: 52px 1fr auto;
  gap: 10px;
  align-items: center;
}

.selected-item__head :deep(.image-uploader--tile) {
  width: 52px;
  height: 52px;
}

.selected-item__name,
.selected-item__fields label {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 6px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.selected-item__fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 10px;
}

.manual-description {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 10px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.textarea--compact {
  min-height: 76px;
  font-size: var(--font-size-md);
}

.icon-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  min-width: 32px;
  min-height: 32px;
  padding: 0;
  border: 1px solid var(--color-border);
  border-radius: 50%;
  background: var(--color-bg-card);
  color: var(--color-text-secondary);
  font-size: 14px;
}

.add-manual {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 44px;
  border: 1px dashed var(--color-primary);
  border-radius: 10px;
  background: var(--color-primary-light);
  color: var(--color-primary);
  font-family: inherit;
  font-size: var(--font-size-md);
  font-weight: 800;
}

.delivery-options {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.delivery-option {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  background: var(--color-bg-card);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.delivery-option.active {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.delivery-option input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.agreement {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--color-text-secondary);
  font-family: inherit;
  font-size: var(--font-size-md);
  text-align: left;
}

.checkbox-circle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  min-width: 20px;
  min-height: 20px;
  border: 1px solid var(--color-border);
  border-radius: 50%;
  background: var(--color-bg-card);
  color: transparent;
  font-size: 12px;
}

.checkbox-circle.checked {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: #fff;
}

@media (max-width: 340px) {
  .delivery-options,
  .selected-item__fields {
    grid-template-columns: 1fr;
  }
}
</style>
