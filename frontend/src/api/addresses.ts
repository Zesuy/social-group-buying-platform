/**
 * 地址管理 API
 *
 * 当前用户收货地址的增删改查（需要登录）。
 */

import request from './request'
import type {
  ApiResponse,
  EmptySuccessResponse,
  AddressData,
  CreateAddressRequest,
  UpdateAddressRequest,
} from '@/types'

/**
 * 获取当前用户的所有地址
 */
export async function listAddresses(): Promise<AddressData[]> {
  const res = await request.get('/my/addresses') as ApiResponse<AddressData[]>
  return res.data
}

/**
 * 创建新地址
 *
 * @param data 地址数据
 */
export async function createAddress(data: CreateAddressRequest): Promise<AddressData> {
  const res = await request.post('/my/addresses', data) as ApiResponse<AddressData>
  return res.data
}

/**
 * 更新地址
 *
 * @param addressId 地址 ID
 * @param data 要更新的字段
 */
export async function updateAddress(
  addressId: string,
  data: UpdateAddressRequest,
): Promise<AddressData> {
  const res = await request.patch(`/my/addresses/${addressId}`, data) as ApiResponse<AddressData>
  return res.data
}

/**
 * 删除地址
 *
 * @param addressId 地址 ID
 */
export async function deleteAddress(addressId: string): Promise<void> {
  await request.delete(`/my/addresses/${addressId}`) as EmptySuccessResponse
}
