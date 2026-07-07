/**
 * 商品管理 API
 *
 * 团长管理自己店铺的商品。
 */

import request from './request'
import type {
  ApiResponse,
  PageResponse,
  ProductData,
  ProductListParams,
  CreateProductRequest,
  UpdateProductRequest,
} from '@/types'

/**
 * 获取商品列表
 */
export async function listProducts(
  page = 1,
  pageSize = 20,
): Promise<PageResponse<ProductData>> {
  return listProductsByParams({ page, pageSize })
}

/**
 * 按条件获取商品列表
 */
export async function listProductsByParams(
  params: ProductListParams = {},
): Promise<PageResponse<ProductData>> {
  const res = await request.get('/my/store/products', {
    params,
  }) as ApiResponse<PageResponse<ProductData>>
  return res.data
}

/**
 * 创建商品
 */
export async function createProduct(data: CreateProductRequest): Promise<ProductData> {
  const res = await request.post('/my/store/products', data) as ApiResponse<ProductData>
  return res.data
}

/**
 * 获取商品详情
 */
export async function getProduct(productId: string): Promise<ProductData> {
  const res = await request.get(`/my/store/products/${productId}`) as ApiResponse<ProductData>
  return res.data
}

/**
 * 更新商品
 */
export async function updateProduct(productId: string, data: UpdateProductRequest): Promise<ProductData> {
  const res = await request.patch(`/my/store/products/${productId}`, data) as ApiResponse<ProductData>
  return res.data
}

/**
 * 删除商品
 */
export async function deleteProduct(productId: string): Promise<void> {
  await request.delete(`/my/store/products/${productId}`)
}
