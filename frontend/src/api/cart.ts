/**
 * 购物车 API
 *
 * P1 购物车：加购、列表、改数量、删除、清空、结算预览。
 */

import request from './request'
import type {
  AddCartItemRequest,
  ApiResponse,
  CartCheckoutPreviewRequest,
  CartItemData,
  OrderPreviewData,
  UpdateCartItemRequest,
} from '@/types'

export async function listCartItems(): Promise<CartItemData[]> {
  const res = await request.get('/cart/items') as ApiResponse<CartItemData[]>
  return res.data
}

export async function addCartItem(data: AddCartItemRequest): Promise<CartItemData> {
  const res = await request.post('/cart/items', data) as ApiResponse<CartItemData>
  return res.data
}

export async function updateCartItem(cartItemId: string, data: UpdateCartItemRequest): Promise<CartItemData> {
  const res = await request.patch(`/cart/items/${cartItemId}`, data) as ApiResponse<CartItemData>
  return res.data
}

export async function deleteCartItem(cartItemId: string): Promise<void> {
  await request.delete(`/cart/items/${cartItemId}`)
}

export async function clearCartItems(): Promise<void> {
  await request.delete('/cart/items')
}

export async function previewCartCheckout(data: CartCheckoutPreviewRequest): Promise<OrderPreviewData> {
  const res = await request.post('/cart/checkout-preview', data) as ApiResponse<OrderPreviewData>
  return res.data
}
