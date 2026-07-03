<template>
  <div class="form-card">
    <div class="form-title">商品信息</div>
    <label class="field">
      <span class="field-label">名称</span>
      <input v-model="form.name" class="input" placeholder="请输入商品名称" />
    </label>
    <label class="field">
      <span class="field-label">描述</span>
      <textarea
        v-model="form.description"
        class="input textarea"
        rows="5"
        placeholder="写清楚规格、口感、保存方式、配送注意事项等"
      />
    </label>
    <section class="field">
      <div class="field-head">
        <span class="field-label">封面图</span>
        <span class="field-hint">用于商品库和团购商品卡片</span>
      </div>
      <ImageUploader
        v-model="form.coverImageUrl"
        :disabled="submitting"
        :preview-alt="form.name || '商品封面'"
        demo-kind="product"
        :show-url-input="false"
        :show-hint="false"
        button-label="更换封面"
      />
    </section>
    <section class="field">
      <div class="field-head">
        <span class="field-label">详情图</span>
        <span class="field-hint">最多 9 张，用于展示商品细节</span>
      </div>
      <div class="detail-image-grid">
        <div
          v-for="(_, index) in form.detailImageUrls"
          :key="index"
          class="detail-image-cell"
        >
          <ImageUploader
            v-model="form.detailImageUrls[index]"
            :disabled="submitting"
            :preview-alt="`${form.name || '商品'}详情图${index + 1}`"
            demo-kind="product"
            :show-url-input="false"
            :show-hint="false"
            variant="tile"
            :tile-label="form.detailImageUrls[index] ? '更换' : '上传'"
          />
          <button
            type="button"
            class="detail-image-remove"
            :aria-label="`移除详情图${index + 1}`"
            :disabled="submitting"
            @click="removeDetailImage(index)"
          >
            <van-icon name="cross" />
          </button>
        </div>
        <button
          v-if="form.detailImageUrls.length < 9"
          type="button"
          class="detail-image-add"
          :disabled="submitting"
          @click="addDetailImage"
        >
          <van-icon name="plus" />
          <span>添加</span>
        </button>
      </div>
    </section>
    <div class="field-grid">
      <label class="field">
        <span class="field-label">基础价</span>
        <input v-model="form.basePriceYuan" class="input" placeholder="0.00" type="digit" />
      </label>
      <label class="field">
        <span class="field-label">库存</span>
        <input v-model="form.stock" class="input" placeholder="库存数量" type="number" />
      </label>
    </div>
    <div v-if="isEdit" class="status-field">
      <div>
        <span class="field-label">状态</span>
        <p class="status-desc">{{ form.status === 'active' ? '当前商品可用于开团' : '下架后不会作为可售商品展示' }}</p>
      </div>
      <button
        type="button"
        class="switch"
        :class="{ on: form.status === 'active' }"
        :aria-pressed="form.status === 'active'"
        @click="form.status = form.status === 'active' ? 'inactive' : 'active'"
      >
        <span>{{ form.status === 'active' ? '上架' : '下架' }}</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, computed, watch } from 'vue'
import { amountToYuan, getDemoProductImage } from '@/utils'
import ImageUploader from './ImageUploader.vue'
import type { ProductData } from '@/types'

const props = withDefaults(defineProps<{
  product?: ProductData | null
  submitting?: boolean
}>(), {
  product: null,
  submitting: false,
})

const isEdit = computed(() => !!props.product)

const form = reactive({
  name: '',
  description: '',
  coverImageUrl: '',
  detailImageUrls: [] as string[],
  basePriceYuan: '',
  stock: '',
  status: 'active' as string,
})

watch(() => props.product, (p) => {
  if (p) {
    form.name = p.name
    form.description = p.description || ''
    form.coverImageUrl = p.coverImageUrl || ''
    form.detailImageUrls = [...(p.detailImageUrls || [])]
    form.basePriceYuan = String(amountToYuan(p.basePriceAmount))
    form.stock = String(p.stock)
    form.status = p.status
  }
}, { immediate: true })

function getFormData(): {
  name: string
  description?: string | null
  coverImageUrl?: string | null
  detailImageUrls?: string[]
  basePriceAmount: number
  stock: number
  status?: string
} | null {
  if (!form.name.trim()) return null
  const yuan = parseFloat(form.basePriceYuan)
  if (isNaN(yuan) || yuan < 0) return null
  const stockNum = parseInt(form.stock, 10)
  if (isNaN(stockNum) || stockNum < 0) return null

  const data: {
    name: string
    description?: string | null
    coverImageUrl?: string | null
    detailImageUrls?: string[]
    basePriceAmount: number
    stock: number
    status?: string
  } = {
    name: form.name.trim(),
    basePriceAmount: Math.round(yuan * 100),
    stock: stockNum,
  }
  if (form.description) data.description = form.description.trim() || null
  data.coverImageUrl = form.coverImageUrl.trim() || getDemoProductImage(form.name)
  data.detailImageUrls = form.detailImageUrls.map((url) => url.trim()).filter(Boolean)
  if (isEdit.value) data.status = form.status
  return data
}

function validate(): string | null {
  if (!form.name.trim()) return '请输入商品名称'
  const yuan = parseFloat(form.basePriceYuan)
  if (isNaN(yuan) || yuan < 0) return '请输入有效的价格'
  const stockNum = parseInt(form.stock, 10)
  if (isNaN(stockNum) || stockNum < 0) return '请输入有效的库存'
  return null
}

function addDetailImage() {
  if (form.detailImageUrls.length >= 9) return
  form.detailImageUrls.push('')
}

function removeDetailImage(index: number) {
  form.detailImageUrls.splice(index, 1)
}

defineExpose({ getFormData, validate })
</script>

<style scoped>
.form-card {
  background: #fff;
  border-radius: 14px;
  margin-bottom: 12px;
  overflow: hidden;
}
.form-title {
  font-weight: 700;
  font-size: var(--font-size-xl);
  padding: 16px 14px;
  border-bottom: 1px solid var(--color-border);
}
.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
  border-bottom: 1px solid var(--color-border);
}

.field-label {
  display: block;
  align-self: flex-start;
  color: var(--color-text-primary);
  font-weight: 700;
  font-size: var(--font-size-md);
  line-height: 1.4;
  text-align: left;
}

.field-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.field-hint,
.status-desc {
  color: var(--color-text-hint);
  font-size: var(--font-size-sm);
  line-height: 1.4;
}

.status-desc {
  margin: 4px 0 0;
}
.input {
  min-height: 44px;
  background: var(--color-bg-surface);
  border-radius: 10px;
  border: 1px solid var(--color-border);
  padding: 0 12px;
  color: var(--color-text-primary);
  width: 100%;
  font-size: var(--font-size-lg);
  outline: none;
  font-family: inherit;
}
.input:focus {
  border-color: var(--color-primary);
}

.textarea {
  min-height: 132px;
  padding: 12px;
  resize: vertical;
  line-height: 1.55;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  border-bottom: 1px solid var(--color-border);
}

.field-grid .field {
  border-bottom: 0;
}

.field-grid .field + .field {
  border-left: 1px solid var(--color-border);
}

.detail-image-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.detail-image-cell {
  position: relative;
  min-width: 0;
}

.detail-image-remove {
  position: absolute;
  top: -7px;
  right: -7px;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  min-width: 28px;
  min-height: 28px;
  max-width: 28px;
  max-height: 28px;
  padding: 0;
  border: 1px solid var(--color-border);
  border-radius: 50%;
  background: var(--color-bg-card);
  color: var(--color-text-secondary);
  font-size: 14px;
  line-height: 1;
  appearance: none;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.12);
}

.detail-image-add {
  display: flex;
  aspect-ratio: 1;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 6px;
  border: 1px dashed var(--color-primary);
  border-radius: 10px;
  background: var(--color-primary-light);
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  font-weight: 700;
  font-family: inherit;
}

.detail-image-add .van-icon {
  font-size: 22px;
}

.status-field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
}
/* demo switch */
.switch {
  width: 64px;
  height: 32px;
  min-width: 64px;
  min-height: 32px;
  max-width: 64px;
  max-height: 32px;
  padding: 0;
  border-radius: 99px;
  background: #d8dde3;
  position: relative;
  cursor: pointer;
  flex-shrink: 0;
  border: 0;
  color: transparent;
  font-family: inherit;
  line-height: 1;
  appearance: none;
}
.switch.on {
  background: var(--color-primary);
}
.switch::after {
  content: "";
  width: 28px;
  height: 28px;
  background: #fff;
  border-radius: 50%;
  position: absolute;
  top: 2px;
  left: 2px;
  box-shadow: 0 1px 4px rgba(0,0,0,.18);
  transition: left 0.2s;
}
.switch.on::after {
  left: 34px;
}

@media (max-width: 360px) {
  .field-grid {
    grid-template-columns: 1fr;
  }

  .field-grid .field + .field {
    border-left: 0;
    border-top: 1px solid var(--color-border);
  }
}
</style>
