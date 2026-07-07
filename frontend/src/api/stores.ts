/**
 * 店铺 API
 *
 * 创建店铺、获取我的店铺、更新我的店铺。
 */

import request from './request'
import type {
  ApiResponse,
  CreateStoreRequest,
  UpdateStoreRequest,
  StoreResponseData,
  MyStoreResponseData,
  StoreWorkbenchSummaryData,
} from '@/types'

/**
 * 创建店铺
 *
 * 当前用户必须未创建过店铺，否则返回 STORE_ALREADY_EXISTS (409)。
 */
export async function createStore(data: CreateStoreRequest): Promise<StoreResponseData> {
  const res = await request.post('/stores', data) as ApiResponse<StoreResponseData>
  return res.data
}

/**
 * 获取当前用户自己的店铺信息
 *
 * 未创建店铺时返回 200，但 data 为 null。
 */
export async function getMyStore(): Promise<MyStoreResponseData> {
  const res = await request.get('/my/store') as ApiResponse<MyStoreResponseData>
  return res.data ?? null
}

export async function getStoreWorkbenchSummary(): Promise<StoreWorkbenchSummaryData> {
  const res = await request.get('/my/store/workbench-summary') as ApiResponse<StoreWorkbenchSummaryData>
  return res.data
}

/**
 * 更新当前用户自己的店铺信息
 *
 * PATCH 语义，仅更新传入字段。未创建店铺时返回 LEADER_REQUIRED (403)。
 */
export async function updateMyStore(data: UpdateStoreRequest): Promise<StoreResponseData> {
  const res = await request.patch('/my/store', data) as ApiResponse<StoreResponseData>
  return res.data
}
