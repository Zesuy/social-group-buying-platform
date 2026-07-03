/**
 * 文件上传 API
 *
 * 当前仅支持公开图片上传，返回可直接展示的 URL。
 */

import request from './request'
import type { ApiResponse, ImageUploadData } from '@/types'

export async function uploadImage(file: File): Promise<ImageUploadData> {
  const formData = new FormData()
  formData.append('file', file)

  const res = await request.post('/my/uploads/images', formData) as ApiResponse<ImageUploadData>
  return res.data
}
