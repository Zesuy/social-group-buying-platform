<template>
  <van-action-sheet
    v-model:show="visible"
    closeable
    title="确认商品"
    class="sku-sheet-action"
    :close-on-click-action="false"
  >
    <div class="sku-top">
      <ImageWithFallback
        :src="item?.coverImageUrl || item?.product?.coverImageUrl"
        width="64px"
        height="64px"
        radius="8px"
        :alt="item?.displayName ?? ''"
      />
      <div class="sku-top__info">
        <PriceText class="sku-price" :amount="item?.groupPriceAmount ?? 0" />
        <p class="sku-stock">
          库存 {{ item?.groupStock ?? 0 }}｜已团 {{ item?.soldCount ?? 0 }} 件
        </p>
        <p class="sku-name van-multi-ellipsis--l2">{{ item?.displayName ?? '' }}</p>
      </div>
    </div>

    <div v-if="item" class="sku-product-detail">
      <div class="sku-product-detail__head">
        <span>商品详情</span>
        <small>下单前确认</small>
      </div>
      <p v-if="productDescription">{{ productDescription }}</p>
      <p v-else class="sku-muted">该商品暂未配置独立图文说明，请以团购介绍和团长说明为准。</p>
      <div v-if="detailImages.length > 0" class="sku-product-detail__images">
        <ImageWithFallback
          v-for="url in detailImages"
          :key="url"
          :src="url"
          width="100%"
          height="180px"
          fit="cover"
          radius="8px"
          :alt="item.displayName"
        />
      </div>
    </div>

    <!-- 规格 -->
    <div class="sku-section">
      <div class="sku-section-title">规格</div>
      <div class="sku-chips">
        <span
          v-for="spec in specOptions"
          :key="spec"
          :class="['sku-chip', { active: selectedSpec === spec }]"
          @click="selectedSpec = spec"
        >
          {{ spec }}
        </span>
      </div>
    </div>

    <!-- 配送 -->
    <div class="sku-section">
      <div class="sku-section-title">配送</div>
      <div class="sku-chips">
        <span
          v-for="dlv in deliveryOptions"
          :key="dlv.value"
          :class="['sku-chip', { active: selectedDelivery === dlv.value }]"
          @click="selectedDelivery = dlv.value"
        >
          {{ dlv.label }}
        </span>
      </div>
    </div>

    <!-- 购买数量 -->
    <div class="sku-section sku-qty-row">
      <b>购买数量</b>
      <div class="qty">
        <span class="qty__btn" @click="decrement">－</span>
        <span class="qty__num">{{ quantity }}</span>
        <span class="qty__btn" @click="increment">＋</span>
      </div>
    </div>

    <!-- 底部按钮 -->
    <div class="sku-bottom" :class="{ 'sku-bottom--dual': showCartAction }">
      <button v-if="showCartAction" class="sku-btn sku-btn--cart" @click="emit('add-to-cart', confirmData)">加入购物车</button>
      <button class="sku-btn sku-btn--buy" @click="emit('buy-now', confirmData)">立即购买</button>
    </div>
  </van-action-sheet>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { PublicGroupBuyDetailItem } from '@/types'
import ImageWithFallback from './ImageWithFallback.vue'
import PriceText from './PriceText.vue'

const props = defineProps<{
  modelValue: boolean
  item: PublicGroupBuyDetailItem | null
  defaultDeliveryType?: string
  showCartAction?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'add-to-cart': [payload: { itemId: string; quantity: number; deliveryType: string }]
  'buy-now': [payload: { itemId: string; quantity: number; deliveryType: string }]
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val: boolean) => emit('update:modelValue', val),
})

const selectedSpec = ref('')
const selectedDelivery = ref('express')
const quantity = ref(1)

const specOptions = ref(['默认规格'])
const productDescription = computed(() => props.item?.product?.description?.trim() ?? '')
const detailImages = computed(() => props.item?.product?.detailImageUrls ?? [])

const deliveryOptions = [
  { value: 'express', label: '全国包邮' },
  { value: 'pickup', label: '同城自提' },
  { value: 'local_delivery', label: '同城配送' },
]

function decrement() {
  if (quantity.value > 1) quantity.value--
}

const confirmData = computed(() => ({
  itemId: props.item?.id ?? '',
  quantity: quantity.value,
  deliveryType: selectedDelivery.value,
}))

function increment() {
  const max = props.item?.groupStock ?? 0
  if (max <= 0 || quantity.value < max) quantity.value++
}

watch(() => props.modelValue, (val) => {
  if (val) {
    quantity.value = 1
    selectedSpec.value = specOptions.value[0] || ''
    selectedDelivery.value = props.defaultDeliveryType || 'express'
  }
})
</script>

<style scoped>
.sku-sheet-action :deep(.van-action-sheet__content) {
  max-height: calc(90vh - 56px);
  overflow-y: auto;
}

.sku-top {
  display: grid;
  grid-template-columns: 64px 1fr;
  gap: 12px;
  padding: 16px 16px 0;
  align-items: start;
}

.sku-top__info {
  min-width: 0;
}

.sku-price {
  font-size: 24px;
  color: #ff602a;
  font-weight: 900;
  line-height: 1.2;
}

.sku-stock {
  margin: 4px 0;
  color: #969ca5;
  font-size: 12px;
  line-height: 1.4;
}

.sku-name {
  display: block;
  font-size: 14px;
  line-height: 1.4;
  color: #16181d;
  margin-top: 2px;
}

.sku-section {
  padding: 14px 16px 0;
}

.sku-product-detail {
  margin: 14px 16px 0;
  padding: 12px;
  border-radius: 8px;
  background: #f7fbf8;
}

.sku-product-detail__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.sku-product-detail__head span {
  color: #16181d;
  font-size: 15px;
  font-weight: 900;
}

.sku-product-detail__head small,
.sku-muted {
  color: #7c858f;
}

.sku-product-detail p {
  margin: 0;
  color: #333;
  font-size: 14px;
  line-height: 1.65;
  white-space: pre-line;
}

.sku-product-detail__images {
  display: grid;
  gap: 10px;
  margin-top: 10px;
}

.sku-section:last-of-type {
  padding-bottom: 0;
}

.sku-section-title {
  font-weight: 900;
  font-size: 15px;
  margin-bottom: 10px;
  color: #16181d;
}

.sku-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.sku-chip {
  border: 1px solid #e4e7eb;
  background: #f7f8f9;
  border-radius: 8px;
  padding: 8px 14px;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.sku-chip:active {
  opacity: 0.7;
}

.sku-chip.active {
  border-color: #8ee5b8;
  background: #eafff3;
  color: #10c468;
  font-weight: 900;
}

.sku-qty-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.qty {
  display: inline-flex;
  align-items: center;
  border: 1px solid #e1e5e9;
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
}

.qty__btn {
  min-width: 32px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  user-select: none;
  font-size: 16px;
  color: #333;
  border-right: 1px solid #e1e5e9;
}

.qty__btn:last-child {
  border-right: 0;
}

.qty__btn:active {
  background: #f5f6f7;
}

.qty__num {
  min-width: 38px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-right: 1px solid #e1e5e9;
  font-weight: 900;
  font-size: 15px;
  color: #16181d;
}

.qty__num:last-child {
  border-right: 0;
}

.sku-bottom {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
  padding: 16px 16px 20px;
  position: sticky;
  bottom: 0;
  z-index: 2;
  background: #fff;
  box-shadow: 0 -8px 20px rgba(18, 34, 25, 0.06);
}

.sku-bottom--dual {
  grid-template-columns: 1fr 1fr;
}

.sku-btn {
  height: 44px;
  border: 0;
  border-radius: 8px;
  font-weight: 900;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: opacity 0.2s;
}

.sku-btn:active {
  opacity: 0.85;
}

.sku-btn--cart {
  background: #ff7a2f;
  color: #fff;
}

.sku-btn--buy {
  background: #10c468;
  color: #fff;
}
</style>
