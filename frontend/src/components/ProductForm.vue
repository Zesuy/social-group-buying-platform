<template>
  <div class="form-card">
    <div class="form-title">商品信息</div>
    <div class="field">
      <label>名称</label>
      <input v-model="form.name" class="input" placeholder="请输入商品名称" />
      <span></span>
    </div>
    <div class="field">
      <label>描述</label>
      <input v-model="form.description" class="input" placeholder="可选，输入商品描述" />
      <span></span>
    </div>
    <div class="field">
      <label>封面图</label>
      <ImageUploader
        v-model="form.coverImageUrl"
        :disabled="submitting"
        :preview-alt="form.name || '商品封面'"
        demo-kind="product"
        placeholder="可选，输入或上传商品封面"
      />
      <span></span>
    </div>
    <div class="field">
      <label>基础价</label>
      <input v-model="form.basePriceYuan" class="input" placeholder="0.00" type="digit" />
      <span></span>
    </div>
    <div class="field">
      <label>库存</label>
      <input v-model="form.stock" class="input" placeholder="库存数量" type="number" />
      <span></span>
    </div>
    <!-- 编辑模式显示状态开关（demo .switch） -->
    <div v-if="isEdit" class="field">
      <label>状态</label>
      <span class="value">{{ form.status === 'active' ? '上架' : '下架' }}</span>
      <span class="switch" :class="{ on: form.status === 'active' }" @click="form.status = form.status === 'active' ? 'inactive' : 'active'"></span>
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
  basePriceYuan: '',
  stock: '',
  status: 'active' as string,
})

watch(() => props.product, (p) => {
  if (p) {
    form.name = p.name
    form.description = p.description || ''
    form.coverImageUrl = p.coverImageUrl || ''
    form.basePriceYuan = String(amountToYuan(p.basePriceAmount))
    form.stock = String(p.stock)
    form.status = p.status
  }
}, { immediate: true })

function getFormData(): {
  name: string
  description?: string | null
  coverImageUrl?: string | null
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
  font-weight: 900;
  font-size: 18px;
  padding: 14px;
  border-bottom: 1px solid #edf0f2;
}
.field {
  display: grid;
  grid-template-columns: 94px 1fr auto;
  gap: 8px;
  align-items: center;
  padding: 13px 14px;
  border-bottom: 1px solid #edf0f2;
  min-height: 52px;
}
.field label {
  color: #262b32;
  font-weight: 700;
  font-size: 14px;
}
.field .value {
  color: #9aa0a6;
  font-size: 14px;
}
.input {
  height: 42px;
  background: #f7f8fa;
  border-radius: 8px;
  border: 1px solid #eef0f3;
  padding: 0 10px;
  color: #555;
  width: 100%;
  font-size: 14px;
  outline: none;
}
.input:focus {
  border-color: var(--color-primary);
}
/* demo switch */
.switch {
  width: 48px;
  height: 26px;
  border-radius: 99px;
  background: #d8dde3;
  position: relative;
  cursor: pointer;
  flex-shrink: 0;
}
.switch.on {
  background: var(--color-primary);
}
.switch::after {
  content: "";
  width: 22px;
  height: 22px;
  background: #fff;
  border-radius: 50%;
  position: absolute;
  top: 2px;
  left: 2px;
  box-shadow: 0 1px 4px rgba(0,0,0,.18);
  transition: left 0.2s;
}
.switch.on::after {
  left: 24px;
}
</style>
