<template>
  <van-popup
    v-model:show="visible"
    position="bottom"
    round
    class="cart-sheet h5-constrained-bottom-sheet"
    :style="{ maxHeight: '82vh' }"
  >
    <div class="cart-sheet__handle" aria-hidden="true"></div>

    <header class="cart-sheet__header">
      <div>
        <h2>购物车</h2>
        <p>{{ currentGroupBuyId ? '当前团购内商品' : '选择同一个团购内的商品结算' }}</p>
      </div>
      <button v-if="displayItems.length > 0" type="button" @click="handleClear">清空</button>
    </header>

    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" @retry="loadCart" />

    <template v-else>
      <EmptyState v-if="displayItems.length === 0" description="还没有加购商品" />

      <div v-else class="cart-sheet__list">
        <article
          v-for="item in displayItems"
          :key="item.cartItemId"
          class="cart-sheet__item"
          :class="{ 'cart-sheet__item--checked': selectedIds.includes(item.cartItemId) }"
        >
          <button type="button" class="cart-sheet__check" :aria-label="`选择${item.title || '商品'}`" @click="toggleSelect(item)">
            <span :class="{ on: selectedIds.includes(item.cartItemId) }"></span>
          </button>
          <ImageWithFallback
            :src="item.coverImageUrl"
            width="72px"
            height="72px"
            fit="cover"
            radius="8px"
            :alt="item.title || '购物车商品'"
          />
          <div class="cart-sheet__info">
            <h3>{{ item.title || '团购商品' }}</h3>
            <div class="cart-sheet__meta">
              <PriceText :amount="item.groupPriceAmount || 0" color="var(--color-price)" />
              <span>库存 {{ item.availableStock ?? '-' }}</span>
            </div>
            <div class="cart-sheet__actions">
              <div class="cart-sheet__qty" aria-label="修改数量">
                <button
                  type="button"
                  :disabled="item.quantity <= 1 || updatingId === item.cartItemId"
                  aria-label="减少数量"
                  @click="changeQuantity(item, item.quantity - 1)"
                >
                  -
                </button>
                <span>{{ item.quantity }}</span>
                <button
                  type="button"
                  :disabled="item.quantity >= (item.availableStock || 999) || updatingId === item.cartItemId"
                  aria-label="增加数量"
                  @click="changeQuantity(item, item.quantity + 1)"
                >
                  +
                </button>
              </div>
              <button type="button" @click="handleDelete(item)">删除</button>
            </div>
          </div>
        </article>
      </div>
    </template>

    <footer v-if="displayItems.length > 0" class="cart-sheet__footer">
      <button type="button" class="cart-sheet__select-all" @click="toggleSelectAll">
        <span :class="{ on: allSelected }"></span>
        {{ allSelected ? '取消全选' : '全选' }}
      </button>
      <div class="cart-sheet__total">
        <span>合计</span>
        <PriceText :amount="selectedTotal" size="lg" color="var(--color-price)" />
      </div>
      <button type="button" class="cart-sheet__checkout" :disabled="selectedItems.length === 0" @click="handleCheckout">
        去结算 {{ selectedItems.length }} 件
      </button>
    </footer>
  </van-popup>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import LoadingView from './LoadingView.vue'
import ErrorView from './ErrorView.vue'
import EmptyState from './EmptyState.vue'
import ImageWithFallback from './ImageWithFallback.vue'
import PriceText from './PriceText.vue'
import { clearCartItems, deleteCartItem, listCartItems, updateCartItem } from '@/api/cart'
import { useCheckoutStore } from '@/stores'
import type { CartItemData } from '@/types'

const props = defineProps<{
  modelValue: boolean
  currentGroupBuyId?: string | null
  shareToken?: string | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const router = useRouter()
const checkoutStore = useCheckoutStore()

const items = ref<CartItemData[]>([])
const selectedIds = ref<string[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const updatingId = ref<string | null>(null)

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
})
const displayItems = computed(() => {
  if (!props.currentGroupBuyId) return items.value
  return items.value.filter(item => item.groupBuyId === props.currentGroupBuyId)
})
const selectedItems = computed(() => displayItems.value.filter(item => selectedIds.value.includes(item.cartItemId)))
const allSelected = computed(() => displayItems.value.length > 0 && selectedItems.value.length === displayItems.value.length)
const selectedTotal = computed(() => selectedItems.value.reduce((sum, item) => {
  return sum + (item.groupPriceAmount || 0) * item.quantity
}, 0))

watch(() => props.modelValue, value => {
  if (value) loadCart()
})

async function loadCart() {
  loading.value = true
  error.value = null
  try {
    items.value = await listCartItems()
    selectedIds.value = displayItems.value.map(item => item.cartItemId)
  } catch (err) {
    error.value = (err as { message?: string }).message || '购物车加载失败'
  } finally {
    loading.value = false
  }
}

function toggleSelect(item: CartItemData) {
  if (selectedIds.value.includes(item.cartItemId)) {
    selectedIds.value = selectedIds.value.filter(id => id !== item.cartItemId)
    return
  }
  selectedIds.value = [...selectedIds.value, item.cartItemId]
}

function toggleSelectAll() {
  selectedIds.value = allSelected.value ? [] : displayItems.value.map(item => item.cartItemId)
}

async function handleQuantityChange(item: CartItemData, value: number | string) {
  const quantity = Number(value)
  if (!Number.isFinite(quantity) || quantity < 1 || quantity === item.quantity) return
  updatingId.value = item.cartItemId
  try {
    const updated = await updateCartItem(item.cartItemId, { quantity })
    items.value = items.value.map(row => row.cartItemId === item.cartItemId ? updated : row)
  } catch (err) {
    showToast((err as { message?: string }).message || '修改数量失败')
    await loadCart()
  } finally {
    updatingId.value = null
  }
}

function changeQuantity(item: CartItemData, quantity: number) {
  handleQuantityChange(item, quantity)
}

async function handleDelete(item: CartItemData) {
  try {
    await showConfirmDialog({ title: '删除商品', message: `确定删除「${item.title || '该商品'}」吗？` })
  } catch {
    return
  }
  try {
    await deleteCartItem(item.cartItemId)
    items.value = items.value.filter(row => row.cartItemId !== item.cartItemId)
    selectedIds.value = selectedIds.value.filter(id => id !== item.cartItemId)
    showToast('已删除')
  } catch (err) {
    showToast((err as { message?: string }).message || '删除失败')
  }
}

async function handleClear() {
  try {
    await showConfirmDialog({ title: '清空购物车', message: props.currentGroupBuyId ? '确定清空当前团购的购物车商品吗？' : '确定清空所有购物车商品吗？' })
  } catch {
    return
  }
  try {
    if (props.currentGroupBuyId && displayItems.value.length !== items.value.length) {
      await Promise.all(displayItems.value.map(item => deleteCartItem(item.cartItemId)))
    } else {
      await clearCartItems()
    }
    items.value = props.currentGroupBuyId
      ? items.value.filter(item => item.groupBuyId !== props.currentGroupBuyId)
      : []
    selectedIds.value = []
    showToast('已清空')
  } catch (err) {
    showToast((err as { message?: string }).message || '清空失败')
    await loadCart()
  }
}

function handleCheckout() {
  if (selectedItems.value.length === 0) return
  const groupBuyIds = new Set(selectedItems.value.map(item => item.groupBuyId))
  if (groupBuyIds.size > 1) {
    showToast('一次只能结算同一个团购内的商品')
    return
  }
  const groupBuyId = selectedItems.value[0]?.groupBuyId
  if (!groupBuyId) return
  checkoutStore.setCartCheckoutContext({
    groupBuyId,
    cartItemIds: selectedItems.value.map(item => item.cartItemId),
    shareToken: props.shareToken ?? null,
  })
  visible.value = false
  router.push('/checkout')
}
</script>

<style scoped>
.cart-sheet {
  overflow: hidden;
}

.cart-sheet__handle {
  width: 38px;
  height: 4px;
  border-radius: 999px;
  background: #d8dde3;
  margin: 8px auto 0;
}

.cart-sheet__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px 10px;
  border-bottom: 1px solid var(--color-border-light);
}

.cart-sheet__header h2 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 21px;
  line-height: 1.2;
  font-weight: 900;
}

.cart-sheet__header p {
  margin: 4px 0 0;
  color: var(--color-text-secondary);
  font-size: 13px;
}

.cart-sheet__header button,
.cart-sheet__actions > button {
  min-height: 44px;
  border: 0;
  background: transparent;
  color: var(--color-primary);
  font-size: 14px;
  font-weight: 900;
}

.cart-sheet__list {
  max-height: min(54vh, 430px);
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 10px 12px;
  background: #f7f9f8;
}

.cart-sheet__item {
  display: grid;
  grid-template-columns: 30px 72px minmax(0, 1fr);
  gap: 10px;
  align-items: start;
  padding: 12px;
  border: 1px solid var(--color-border-light);
  border-radius: 8px;
  background: #fff;
}

.cart-sheet__item + .cart-sheet__item {
  margin-top: 10px;
}

.cart-sheet__item--checked {
  border-color: rgba(16, 196, 104, 0.42);
  background: #f8fff9;
}

.cart-sheet__check {
  min-width: 30px;
  min-height: 72px;
  border: 0;
  background: transparent;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.cart-sheet__check span,
.cart-sheet__select-all span {
  width: 20px;
  height: 20px;
  border-radius: 999px;
  border: 1px solid #d8dde3;
  background: #fff;
  position: relative;
}

.cart-sheet__check span.on,
.cart-sheet__select-all span.on {
  border-color: var(--color-primary);
  background: var(--color-primary);
}

.cart-sheet__check span.on::after,
.cart-sheet__select-all span.on::after {
  content: '';
  position: absolute;
  left: 6px;
  top: 3px;
  width: 5px;
  height: 10px;
  border: solid #fff;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.cart-sheet__info {
  min-width: 0;
}

.cart-sheet__info h3 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 15px;
  line-height: 1.35;
  font-weight: 900;
}

.cart-sheet__meta {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-top: 6px;
  color: var(--color-text-hint);
  font-size: 12px;
}

.cart-sheet__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
}

.cart-sheet__qty {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.cart-sheet__qty button {
  width: 32px;
  height: 32px;
  min-width: 32px;
  min-height: 32px;
  border-radius: 999px;
  border: 1px solid rgba(16, 196, 104, 0.38);
  background: #fff;
  color: var(--color-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  font-size: 20px;
  line-height: 1;
  font-weight: 500;
}

.cart-sheet__qty button:last-child {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: #fff;
}

.cart-sheet__qty button:disabled {
  border-color: #dfe7e2;
  background: #f7faf8;
  color: #b7c6bd;
}

.cart-sheet__qty span {
  min-width: 20px;
  color: var(--color-text-primary);
  font-size: 15px;
  line-height: 32px;
  font-weight: 800;
  text-align: center;
}

.cart-sheet__footer {
  display: grid;
  grid-template-columns: 84px minmax(0, 1fr) minmax(124px, 42%);
  gap: 8px;
  align-items: center;
  padding: 10px 12px calc(10px + var(--safe-area-bottom));
  border-top: 1px solid var(--color-border-light);
  background: #fff;
}

.cart-sheet__select-all {
  min-height: 44px;
  border: 0;
  background: transparent;
  color: var(--color-text-primary);
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 14px;
  font-weight: 900;
}

.cart-sheet__total {
  min-width: 0;
  text-align: right;
}

.cart-sheet__total > span {
  display: block;
  color: var(--color-text-hint);
  font-size: 12px;
}

.cart-sheet__checkout {
  min-height: 48px;
  border: 0;
  border-radius: 999px;
  background: var(--color-primary);
  color: #fff;
  font-size: 16px;
  font-weight: 900;
}

.cart-sheet__checkout:disabled {
  opacity: 0.48;
}

@media (max-width: 374px) {
  .cart-sheet__footer {
    grid-template-columns: 74px minmax(0, 1fr) 116px;
  }

  .cart-sheet__checkout {
    font-size: 15px;
  }
}
</style>
