<template>
  <div class="merchant-page">
    <div class="page-head">
      <div>
        <p>团购运营</p>
        <h1>新建团购</h1>
      </div>
      <RouterLink class="ghost-link" to="/merchant/group-buys">返回团购列表</RouterLink>
    </div>

    <form class="publish-form" @submit.prevent="handleSubmit">
      <section class="form-panel">
        <div class="panel-title">
          <h2>团购信息</h2>
          <button type="button" class="text-button" :disabled="aiPolishing" @click="requestAiPolish">
            <van-icon name="edit" />
            {{ aiPolishing ? '润色中' : 'AI 润色' }}
          </button>
        </div>
        <label class="field">
          <span>标题</span>
          <input v-model="form.title" placeholder="例如：周末阳山水蜜桃社区团" />
        </label>
        <label class="field">
          <span>介绍</span>
          <textarea v-model="form.introduction" rows="6" placeholder="规格、口感、截单时间、配送方式和售后口径" />
        </label>
        <div class="content-editor-field">
          <span>活动内容块</span>
          <ContentBlocksEditor v-model="form.contentBlocks" :disabled="submitting" />
        </div>
        <div class="field-grid">
          <label class="field">
            <span>开始时间</span>
            <input v-model="form.startTime" type="datetime-local" />
          </label>
          <label class="field">
            <span>结束时间</span>
            <input v-model="form.endTime" type="datetime-local" />
          </label>
        </div>
        <label class="field">
          <span>发货说明</span>
          <input v-model="form.shippingTime" placeholder="例如：截单后 48 小时内发货" />
        </label>
        <div class="delivery-grid">
          <button
            v-for="option in deliveryOptions"
            :key="option.value"
            type="button"
            :class="{ active: form.deliveryType === option.value }"
            @click="form.deliveryType = option.value"
          >
            {{ option.label }}
          </button>
        </div>
      </section>

      <aside class="form-panel">
        <h2>团购封面</h2>
        <ImageUploader
          v-model="form.coverImageUrl"
          :disabled="submitting"
          :preview-alt="form.title || '团购封面'"
          demo-kind="cover"
          :show-url-input="false"
          :show-hint="false"
          button-label="更换封面"
        />
      </aside>

      <section class="form-panel product-library">
        <div class="panel-title">
          <h2>商品库</h2>
          <RouterLink to="/merchant/products">管理商品</RouterLink>
        </div>
        <label class="search-box">
          <van-icon name="search" />
          <input v-model="productKeyword" placeholder="搜索上架商品" />
        </label>
        <div v-if="productsLoading" class="state-text">商品加载中...</div>
        <div v-else-if="productsError" class="state-text">{{ productsError }}</div>
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
              width="42px"
              height="42px"
              radius="8px"
            />
            <span>
              <strong>{{ product.name }}</strong>
              <small>{{ formatAmount(product.basePriceAmount) }} · 库存 {{ product.stock }}</small>
            </span>
            <van-icon :name="selectedProductIds.has(product.id) ? 'success' : 'plus'" />
          </button>
        </div>
      </section>

      <section class="form-panel selected-panel">
        <div class="panel-title">
          <h2>本次团购商品</h2>
          <button type="button" class="text-button" @click="addManualItem">
            <van-icon name="plus" />
            手动新增
          </button>
        </div>
        <div v-if="form.items.length === 0" class="state-text">请从商品库选择，或手动新增商品</div>
        <div v-else class="selected-list">
          <article v-for="(item, index) in form.items" :key="item.localId" class="selected-item">
            <div class="selected-head">
              <ImageWithFallback
                v-if="item.productId"
                :src="item.coverImageUrl"
                :alt="item.displayName"
                demo-kind="product"
                width="46px"
                height="46px"
                radius="8px"
              />
              <ImageUploader
                v-else
                v-model="item.coverImageUrl"
                :disabled="submitting"
                :preview-alt="item.displayName || '商品封面'"
                demo-kind="product"
                :show-url-input="false"
                :show-hint="false"
                variant="tile"
                tile-label="上传"
              />
              <label class="field">
                <span>商品名称</span>
                <input v-model="item.displayName" placeholder="商品名称" />
              </label>
              <button type="button" class="icon-button" aria-label="删除商品" @click="removeItem(index)">
                <van-icon name="cross" />
              </button>
            </div>
            <div class="field-grid">
              <label class="field">
                <span>团购价</span>
                <input v-model="item.priceText" inputmode="decimal" placeholder="0.00" @input="onItemPriceInput(index)" />
              </label>
              <label class="field">
                <span>团购库存</span>
                <input v-model="item.groupStock" inputmode="numeric" placeholder="库存" />
              </label>
            </div>
            <label v-if="!item.productId" class="field">
              <span>商品描述</span>
              <textarea v-model="item.description" rows="3" placeholder="规格、产地、保存方式等" />
            </label>
          </article>
        </div>
      </section>

      <div class="form-actions">
        <button type="submit" class="primary-button" :disabled="submitting">
          {{ submitting ? '发布中...' : '发布团购' }}
        </button>
      </div>
    </form>

    <van-popup v-model:show="aiVisible" position="right" :style="{ width: '420px', height: '100%' }">
      <section class="ai-panel">
        <header>
          <h2>AI 润色建议</h2>
          <span>本地生成</span>
        </header>
        <template v-if="polishSuggestion">
          <h3>{{ polishSuggestion.title }}</h3>
          <p>{{ polishSuggestion.introduction }}</p>
          <article v-for="(block, index) in polishSuggestion.contentBlocks" :key="index">
            <strong>{{ block.title || contentBlockLabel(block.type) }}</strong>
            <p v-if="block.text">{{ block.text }}</p>
            <ul v-if="block.items?.length">
              <li v-for="entry in block.items" :key="entry">{{ entry }}</li>
            </ul>
          </article>
        </template>
        <footer>
          <button type="button" class="ghost-button" @click="aiVisible = false">取消</button>
          <button type="button" class="primary-button" :disabled="!polishSuggestion" @click="applyAiPolish">采用建议</button>
        </footer>
      </section>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { showToast } from 'vant'
import ContentBlocksEditor from '@/components/ContentBlocksEditor.vue'
import ImageUploader from '@/components/ImageUploader.vue'
import ImageWithFallback from '@/components/ImageWithFallback.vue'
import { createGroupBuy, polishGroupBuyCopy } from '@/api/leaderGroupBuys'
import { listProductsByParams } from '@/api/products'
import { amountToYuan, formatAmount, getDemoProductImage, normalizeContentBlocks } from '@/utils'
import type { ContentBlockData, GroupBuyAiPolishResponse, ProductData } from '@/types'

interface ItemForm {
  localId: number
  productId?: string
  displayName: string
  priceText: string
  groupPriceAmount: number
  groupStock: string
  coverImageUrl: string
  description: string
}

const router = useRouter()
let localItemId = 0

const deliveryOptions = [
  { value: 'express', label: '快递配送' },
  { value: 'pickup', label: '到店自提' },
  { value: 'local_delivery', label: '同城配送' },
]

const products = ref<ProductData[]>([])
const productsLoading = ref(true)
const productsError = ref('')
const productKeyword = ref('')
const submitting = ref(false)
const aiPolishing = ref(false)
const aiVisible = ref(false)
const polishSuggestion = ref<GroupBuyAiPolishResponse | null>(null)

const form = reactive({
  title: '',
  introduction: '',
  coverImageUrl: '',
  deliveryType: 'express',
  shippingTime: '',
  startTime: '',
  endTime: '',
  contentBlocks: [] as ContentBlockData[],
  items: [] as ItemForm[],
})

const selectedProductIds = computed(() => new Set(form.items.map((item) => item.productId).filter(Boolean) as string[]))
const availableProducts = computed(() => {
  const keyword = productKeyword.value.trim().toLowerCase()
  return products.value.filter((product) => !keyword || product.name.toLowerCase().includes(keyword))
})

function createEmptyItem(): ItemForm {
  localItemId += 1
  return {
    localId: localItemId,
    displayName: '',
    priceText: '',
    groupPriceAmount: 0,
    groupStock: '',
    coverImageUrl: '',
    description: '',
  }
}

function addManualItem() {
  form.items.push(createEmptyItem())
}

function addProductFromLibrary(product: ProductData) {
  if (selectedProductIds.value.has(product.id)) {
    showToast('该商品已在本次团购中')
    return
  }
  localItemId += 1
  form.items.push({
    localId: localItemId,
    productId: product.id,
    displayName: product.name,
    priceText: String(amountToYuan(product.basePriceAmount)),
    groupPriceAmount: product.basePriceAmount,
    groupStock: String(product.stock),
    coverImageUrl: product.coverImageUrl || '',
    description: product.description || '',
  })
}

function removeItem(index: number) {
  form.items.splice(index, 1)
}

function onItemPriceInput(index: number) {
  form.items[index].groupPriceAmount = Math.round(Number(form.items[index].priceText || 0) * 100)
}

function toISOWithTZ(value: string): string | null {
  if (!value) return null
  return `${value.length === 16 ? `${value}:00` : value}+08:00`
}

function contentBlockLabel(type: string): string {
  if (type === 'list') return '要点'
  if (type === 'deliveryNote') return '履约'
  return '说明'
}

function buildContentBlocks(): ContentBlockData[] | undefined {
  const blocks = normalizeContentBlocks(form.contentBlocks)
  return blocks.length > 0 ? blocks : undefined
}

function validate(): string | null {
  if (!form.title.trim()) return '请输入团购标题'
  if (form.items.length === 0) return '至少添加一个商品'
  for (const item of form.items) {
    if (!item.displayName.trim()) return '请填写所有商品名称'
    if (item.groupPriceAmount <= 0) return '团购价必须大于 0'
    if (!Number.isInteger(Number(item.groupStock)) || Number(item.groupStock) <= 0) return '团购库存必须大于 0'
  }
  if (form.startTime && form.endTime && new Date(form.endTime) <= new Date(form.startTime)) return '结束时间必须晚于开始时间'
  return null
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
      shippingTime: form.shippingTime || null,
      items: form.items.map((item) => ({
        productId: item.productId,
        displayName: item.displayName,
        groupPriceAmount: item.groupPriceAmount,
        groupStock: Number(item.groupStock || 0),
        description: item.description || null,
      })),
    })
    aiVisible.value = true
  } catch (err) {
    showToast((err as { message?: string }).message || 'AI 润色失败')
  } finally {
    aiPolishing.value = false
  }
}

function applyAiPolish() {
  if (!polishSuggestion.value) return
  form.title = polishSuggestion.value.title
  form.introduction = polishSuggestion.value.introduction
  form.contentBlocks = normalizeContentBlocks(polishSuggestion.value.contentBlocks)
  aiVisible.value = false
  showToast('已采用润色文案')
}

async function loadProducts() {
  productsLoading.value = true
  productsError.value = ''
  try {
    const data = await listProductsByParams({ page: 1, pageSize: 80, status: 'active' })
    products.value = data.items
  } catch (err) {
    productsError.value = (err as { message?: string }).message || '商品库加载失败'
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
    await createGroupBuy({
      title: form.title.trim(),
      introduction: form.introduction.trim() || null,
      coverImageUrl: form.coverImageUrl || getDemoProductImage(form.title),
      deliveryType: form.deliveryType,
      shippingTime: form.shippingTime.trim() || null,
      startTime: toISOWithTZ(form.startTime),
      endTime: toISOWithTZ(form.endTime),
      contentBlocks: buildContentBlocks(),
      items: form.items.map((item, index) => ({
        ...(item.productId
          ? { productId: item.productId }
          : {
              product: {
                name: item.displayName.trim(),
                description: item.description.trim() || null,
                coverImageUrl: item.coverImageUrl || getDemoProductImage(item.displayName),
                basePriceAmount: item.groupPriceAmount,
                stock: Number(item.groupStock),
              },
            }),
        displayName: item.displayName.trim(),
        groupPriceAmount: item.groupPriceAmount,
        groupStock: Number(item.groupStock),
        sortOrder: index + 1,
      })),
    })
    showToast('发布成功')
    router.push('/merchant/group-buys')
  } catch (err) {
    showToast((err as { message?: string }).message || '发布失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadProducts)
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

.ghost-link,
.text-button,
.ghost-button,
.primary-button {
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 900;
}

.ghost-link,
.ghost-button {
  padding: 0 14px;
  border: 1px solid #d1d5db;
  background: #fff;
  color: #374151;
  text-decoration: none;
}

.text-button {
  border: 0;
  background: transparent;
  color: #d63f2b;
}

.publish-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
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

.form-panel h2,
.panel-title h2 {
  margin: 0;
  color: #111827;
  font-size: 16px;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-title a {
  color: #d63f2b;
  font-weight: 900;
  text-decoration: none;
}

.field,
.content-editor-field {
  display: grid;
  gap: 8px;
}

.field span,
.content-editor-field > span {
  color: #374151;
  font-size: 13px;
  font-weight: 900;
}

.field input,
.field textarea,
.search-box input {
  width: 100%;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 10px 12px;
  color: #111827;
  font-size: 14px;
  outline: 0;
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

.delivery-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.delivery-grid button {
  min-height: 38px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #fff;
  color: #4b5563;
  font-weight: 900;
}

.delivery-grid button.active {
  border-color: #e9563f;
  background: #fff1ed;
  color: #d63f2b;
}

.product-library,
.selected-panel,
.form-actions {
  grid-column: 1 / -1;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-box input {
  min-width: 0;
  flex: 1;
  border: 0;
  padding: 0;
}

.library-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 10px;
}

.library-item {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) 20px;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  text-align: left;
}

.library-item.selected {
  border-color: #e9563f;
  background: #fff7f4;
}

.library-item strong,
.library-item small {
  display: block;
}

.library-item small {
  margin-top: 2px;
  color: #6b7280;
}

.selected-list {
  display: grid;
  gap: 12px;
}

.selected-item {
  display: grid;
  gap: 12px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
}

.selected-head {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr) 32px;
  align-items: start;
  gap: 12px;
}

.icon-button {
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 8px;
  background: #eef2f7;
  color: #4b5563;
}

.state-text {
  padding: 16px;
  border-radius: 8px;
  background: #f9fafb;
  color: #6b7280;
  font-size: 13px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
}

.primary-button {
  padding: 0 18px;
  border: 0;
  background: #e9563f;
  color: #fff;
}

.primary-button:disabled,
.text-button:disabled {
  opacity: 0.62;
}

.ai-panel {
  height: 100%;
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: 14px;
  padding: 20px;
  overflow-y: auto;
}

.ai-panel header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.ai-panel h2,
.ai-panel h3,
.ai-panel p {
  margin: 0;
}

.ai-panel span {
  color: #d63f2b;
  font-size: 12px;
  font-weight: 900;
}

.ai-panel article {
  padding: 12px;
  border-radius: 8px;
  background: #f9fafb;
}

.ai-panel footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 1100px) {
  .publish-form {
    grid-template-columns: 1fr;
  }
}
</style>
