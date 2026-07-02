<template>
  <van-action-sheet
    v-model:show="visible"
    closeable
    class="cart-sheet-action"
    :close-on-click-action="false"
  >
    <!-- 自定义表头 -->
    <div class="cart-sheet-header">
      <span class="cart-sheet-header__left">已选 {{ checkedCount }} 件</span>
      <span class="cart-sheet-header__title">购物车</span>
    </div>

    <!-- 商品列表 -->
    <div class="cart-items">
      <div
        v-for="(row, index) in localItems"
        :key="row._key"
        class="cart-row"
      >
        <span
          :class="['round-check', { on: row.checked }]"
          @click="toggleCheck(index)"
        ></span>
        <ImageWithFallback
          :src="row.coverImage"
          width="74px"
          height="74px"
          radius="8px"
          :alt="row.productName"
        />
        <div class="cart-row__info">
          <p class="cart-row__name van-multi-ellipsis--l2">{{ row.productName }}</p>
          <span class="sku-line">{{ row.skuName }}</span>
          <div class="cart-row__bottom">
            <span class="cart-row__price">¥{{ row.unitPriceAmount }}</span>
            <div class="qty">
              <span class="qty__btn" @click="decrement(index)">－</span>
              <span class="qty__num">{{ row.quantity }}</span>
              <span class="qty__btn" @click="increment(index)">＋</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="localItems.length === 0" class="cart-empty-box">
      还没有加购商品，去逛逛
    </div>

    <!-- 底部栏 -->
    <div class="cart-footer">
      <div class="cart-footer__select-all" @click="toggleSelectAll">
        <span :class="['round-check', { on: allChecked }]"></span>
        <b>全选</b>
      </div>
      <div class="cart-total">
        合计
        <template v-if="checkedItems.length > 0">
          <b>¥{{ checkedTotal }}</b>
        </template>
        <template v-else>
          <span class="muted">¥0</span>
        </template>
        <br />
        <span class="muted small">不含运费/优惠</span>
      </div>
      <button class="cart-checkout-btn" :disabled="checkedItems.length === 0" @click="handleCheckout">
        去结算
      </button>
    </div>
  </van-action-sheet>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import ImageWithFallback from './ImageWithFallback.vue'

export interface CartSheetItem {
  itemId?: string
  productName: string
  skuName: string
  quantity: number
  unitPriceAmount: number
  coverImage?: string
}

interface LocalCartItem extends CartSheetItem {
  _key: number
  checked: boolean
}

const props = defineProps<{
  modelValue: boolean
  items: CartSheetItem[]
  totalAmount: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'checkout': [items: CartSheetItem[]]
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val: boolean) => emit('update:modelValue', val),
})

let keyCounter = 0
const localItems = ref<LocalCartItem[]>([])

function syncItems() {
  localItems.value = props.items.map((item) => ({
    ...item,
    _key: keyCounter++,
    checked: true,
  }))
}

watch(() => props.items, () => {
  syncItems()
}, { deep: true, immediate: true })

watch(() => props.modelValue, (val) => {
  if (val) syncItems()
})

const allChecked = computed(() => {
  return localItems.value.length > 0 && localItems.value.every((row) => row.checked)
})

const checkedCount = computed(() => {
  return localItems.value.filter((row) => row.checked).length
})

const checkedItems = computed(() => {
  return localItems.value.filter((row) => row.checked)
})

const checkedTotal = computed(() => {
  return checkedItems.value.reduce((sum, row) => sum + row.unitPriceAmount * row.quantity, 0)
})

function toggleCheck(index: number) {
  const row = localItems.value[index]
  if (row) row.checked = !row.checked
}

function toggleSelectAll() {
  const newVal = !allChecked.value
  localItems.value.forEach((row) => {
    row.checked = newVal
  })
}

function decrement(index: number) {
  const row = localItems.value[index]
  if (row && row.quantity > 1) row.quantity--
}

function increment(index: number) {
  const row = localItems.value[index]
  if (row) row.quantity++
}

function handleCheckout() {
  if (checkedItems.value.length === 0) return
  emit('checkout', checkedItems.value.map(({ _key, checked: _checked, ...rest }) => rest))
}
</script>

<style scoped>
.cart-sheet-header {
  height: 54px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #edf0f2;
  font-weight: 900;
  font-size: 17px;
  position: relative;
  color: #16181d;
}

.cart-sheet-header__left {
  position: absolute;
  left: 16px;
  color: #6b7280;
  font-size: 13px;
  font-weight: 400;
}

.cart-items {
  max-height: 340px;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.cart-row {
  display: grid;
  grid-template-columns: 26px 74px 1fr;
  gap: 10px;
  padding: 14px;
  border-bottom: 1px solid #edf0f2;
  align-items: start;
}

.cart-row__info {
  min-width: 0;
}

.cart-row__name {
  font-size: 14px;
  font-weight: 700;
  color: #16181d;
  line-height: 1.35;
  margin: 0 0 4px;
}

.sku-line {
  font-size: 12px;
  color: #999;
  background: #f5f6f7;
  border-radius: 99px;
  padding: 3px 8px;
  display: inline-block;
  margin-bottom: 6px;
}

.cart-row__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cart-row__price {
  font-size: 17px;
  color: #ff602a;
  font-weight: 900;
}

.round-check {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 1px solid #d8dde3;
  display: inline-block;
  margin-top: 27px;
  flex-shrink: 0;
  cursor: pointer;
  box-sizing: border-box;
  transition: all 0.2s;
}

.round-check.on {
  background: #10c468;
  border-color: #10c468;
  position: relative;
}

.round-check.on::after {
  content: '✓';
  color: #fff;
  position: absolute;
  font-size: 13px;
  left: 3px;
  top: 0;
  font-weight: 900;
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
  min-width: 30px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  user-select: none;
  font-size: 14px;
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
  min-width: 34px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-right: 1px solid #e1e5e9;
  font-weight: 900;
  font-size: 14px;
  color: #16181d;
}

.qty__num:last-child {
  border-right: 0;
}

.cart-empty-box {
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #aaa;
  border: 1px dashed #e2e6ea;
  border-radius: 12px;
  margin: 14px;
  background: #fbfcfd;
  font-size: 14px;
}

.cart-footer {
  display: grid;
  grid-template-columns: 88px 1fr 130px;
  gap: 8px;
  align-items: center;
  padding: 12px 14px 20px;
  border-top: 1px solid #edf0f2;
}

.cart-footer__select-all {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  user-select: none;
}

.cart-footer__select-all .round-check {
  margin-top: 0;
}

.cart-footer__select-all b {
  font-size: 14px;
  color: #16181d;
}

.cart-total {
  font-size: 13px;
  color: #6b7280;
  text-align: right;
}

.cart-total b {
  font-size: 20px;
  color: #ff602a;
}

.cart-total .muted {
  color: #969ca5;
}

.cart-total .small {
  font-size: 11px;
}

.cart-checkout-btn {
  height: 42px;
  border: 0;
  border-radius: 8px;
  font-weight: 900;
  font-size: 16px;
  cursor: pointer;
  background: #10c468;
  color: #fff;
  transition: opacity 0.2s;
}

.cart-checkout-btn:active {
  opacity: 0.85;
}

.cart-checkout-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.muted {
  color: #969ca5;
}
</style>
