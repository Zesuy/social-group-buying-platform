<template>
  <PageLayout title="购物车" show-back @back="goBack">
    <LoadingView v-if="loading" />
    <ErrorView v-else-if="error" :message="error" @retry="loadCart" />

    <template v-else>
      <div class="cart-page">
        <div class="cart-page__head">
          <div>
            <h1>购物车</h1>
            <p>同一次结算需要选择同一个团购内的商品</p>
          </div>
          <button v-if="items.length > 0" type="button" @click="handleClear">清空</button>
        </div>

        <EmptyState v-if="items.length === 0" description="还没有加购商品" />

        <div v-else class="cart-list">
          <div
            v-for="item in items"
            :key="item.cartItemId"
            class="cart-card"
            :class="{ 'cart-card--checked': selectedIds.includes(item.cartItemId) }"
          >
            <button type="button" class="cart-check" :aria-label="`选择${item.title}`" @click="toggleSelect(item)">
              <span :class="{ on: selectedIds.includes(item.cartItemId) }"></span>
            </button>
            <ImageWithFallback
              :src="item.coverImageUrl"
              width="78px"
              height="78px"
              fit="cover"
              radius="8px"
              :alt="item.title || '购物车商品'"
            />
            <div class="cart-card__info">
              <div class="cart-card__title">{{ item.title || '团购商品' }}</div>
              <div class="cart-card__meta">
                <PriceText :amount="item.groupPriceAmount || 0" color="var(--color-price)" />
                <span>库存 {{ item.availableStock ?? '-' }}</span>
              </div>
              <div class="cart-card__actions">
                <van-stepper
                  :model-value="item.quantity"
                  :min="1"
                  :max="item.availableStock || 999"
                  integer
                  theme="round"
                  button-size="24"
                  :disabled="updatingId === item.cartItemId"
                  @change="val => handleQuantityChange(item, val)"
                />
                <button type="button" @click="handleDelete(item)">删除</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <template v-if="items.length > 0" #action>
      <AppFixedActions>
        <button type="button" class="cart-page__select" @click="toggleSelectAll">
          <span :class="{ on: allSelected }"></span>
          {{ allSelected ? '取消全选' : '全选' }}
        </button>
        <AppButton variant="primary" :disabled="selectedItems.length === 0" @click="handleCheckout">
          去结算 {{ selectedItems.length }} 件
        </AppButton>
      </AppFixedActions>
    </template>
  </PageLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import LoadingView from '@/components/LoadingView.vue'
import ErrorView from '@/components/ErrorView.vue'
import EmptyState from '@/components/EmptyState.vue'
import ImageWithFallback from '@/components/ImageWithFallback.vue'
import PriceText from '@/components/PriceText.vue'
import AppButton from '@/components/AppButton.vue'
import AppFixedActions from '@/components/AppFixedActions.vue'
import { listCartItems, updateCartItem, deleteCartItem, clearCartItems } from '@/api/cart'
import { useSmartNavigation } from '@/composables'
import { useCheckoutStore } from '@/stores'
import type { CartItemData } from '@/types'

const router = useRouter()
const { goBack } = useSmartNavigation('/')
const checkoutStore = useCheckoutStore()

const items = ref<CartItemData[]>([])
const selectedIds = ref<string[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const updatingId = ref<string | null>(null)

const selectedItems = computed(() => items.value.filter(item => selectedIds.value.includes(item.cartItemId)))
const allSelected = computed(() => items.value.length > 0 && selectedIds.value.length === items.value.length)

async function loadCart() {
  loading.value = true
  error.value = null
  try {
    items.value = await listCartItems()
    selectedIds.value = items.value.map(item => item.cartItemId)
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
  selectedIds.value = allSelected.value ? [] : items.value.map(item => item.cartItemId)
}

async function handleQuantityChange(item: CartItemData, val: number | string) {
  const quantity = Number(val)
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
    await showConfirmDialog({ title: '清空购物车', message: '确定清空所有购物车商品吗？' })
  } catch {
    return
  }
  try {
    await clearCartItems()
    items.value = []
    selectedIds.value = []
    showToast('已清空')
  } catch (err) {
    showToast((err as { message?: string }).message || '清空失败')
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
  })
  router.push('/checkout')
}

onMounted(() => {
  loadCart()
})
</script>

<style scoped>
.cart-page {
  min-height: 100%;
  padding: 14px 14px calc(var(--actionbar-height) + var(--safe-area-bottom) + 16px);
  background: var(--color-bg);
}

.cart-page__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.cart-page__head h1 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 24px;
  line-height: 1.2;
  font-weight: 900;
}

.cart-page__head p {
  margin: 6px 0 0;
  color: var(--color-text-secondary);
  font-size: 13px;
  line-height: 1.45;
}

.cart-page__head button,
.cart-card__actions button {
  border: 0;
  background: transparent;
  color: var(--color-primary);
  font-weight: 800;
}

.cart-list {
  display: grid;
  gap: 10px;
}

.cart-card {
  display: grid;
  grid-template-columns: 26px 78px minmax(0, 1fr);
  gap: 10px;
  align-items: start;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid var(--color-border-light);
  background: #fff;
}

.cart-card--checked {
  border-color: rgba(16, 196, 104, 0.42);
  background: #f8fff9;
}

.cart-check {
  min-width: 26px;
  min-height: 78px;
  border: 0;
  background: transparent;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cart-check span,
.cart-page__select span {
  width: 20px;
  height: 20px;
  border-radius: 999px;
  border: 1px solid #d8dde3;
  background: #fff;
}

.cart-check span.on,
.cart-page__select span.on {
  border-color: var(--color-primary);
  background: var(--color-primary);
  position: relative;
}

.cart-check span.on::after,
.cart-page__select span.on::after {
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

.cart-card__info {
  min-width: 0;
}

.cart-card__title {
  color: var(--color-text-primary);
  font-size: 15px;
  line-height: 1.35;
  font-weight: 900;
}

.cart-card__meta {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-top: 6px;
  color: var(--color-text-hint);
  font-size: 12px;
}

.cart-card__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 10px;
}

.cart-page__select {
  min-height: 44px;
  border: 0;
  background: transparent;
  color: var(--color-text-primary);
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 900;
}
</style>
