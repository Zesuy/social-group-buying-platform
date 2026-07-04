import type { PublicGroupBuyItem } from '@/types'

export const NEARBY_DISTANCE_METERS = 5000

export const groupBuyCategoryKeywords: Record<string, string[]> = {
  fresh: ['生鲜', '水果', '鲜', '桃', '瓜', '梨', '莓', '橙', '柑', '苹果', '荔枝'],
  seasonal: ['节令', '特产', '应季', '当季', '产地', '年货', '端午', '中秋'],
  repeat: ['复购', '常备', '家庭', '办公室', '每日', '每周', '回购'],
}

export function isNearbyGroupBuy(
  item: PublicGroupBuyItem,
  maxDistanceMeters = NEARBY_DISTANCE_METERS,
): boolean {
  return typeof item.store.distanceMeters === 'number'
    && item.store.distanceMeters >= 0
    && item.store.distanceMeters <= maxDistanceMeters
}

export function matchesGroupBuyCategory(
  item: PublicGroupBuyItem,
  category: string,
): boolean {
  if (category === 'all') return true
  if (category === 'nearby') return isNearbyGroupBuy(item)

  const keywords = groupBuyCategoryKeywords[category] ?? []
  const haystack = `${item.title} ${item.store.name}`.toLowerCase()
  return keywords.some(keyword => haystack.includes(keyword.toLowerCase()))
}
