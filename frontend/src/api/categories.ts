import request from './request'
import type { ApiResponse, ProductCategoryData } from '@/types'

type RawProductCategoryData = Omit<ProductCategoryData, 'id' | 'parentId'> & {
  id: string | number
  parentId: string | number | null
}

export async function listCategories(): Promise<ProductCategoryData[]> {
  const res = await request.get('/categories') as ApiResponse<RawProductCategoryData[]>
  return res.data.map((category) => ({
    ...category,
    id: String(category.id),
    parentId: category.parentId == null ? null : String(category.parentId),
  }))
}
