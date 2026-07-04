import { describe, expect, it } from 'vitest'
import {
  NEARBY_DISTANCE_METERS,
  isNearbyGroupBuy,
  matchesGroupBuyCategory,
} from '@/utils'
import type { PublicGroupBuyItem } from '@/types'

type GroupBuyOverrides = Partial<Omit<PublicGroupBuyItem, 'store'>> & {
  store?: Partial<PublicGroupBuyItem['store']>
}

function groupBuy(overrides: GroupBuyOverrides = {}): PublicGroupBuyItem {
  return {
    id: '1',
    title: '周末阳山水蜜桃社区团',
    coverImageUrl: null,
    status: 'published',
    endTime: null,
    minPriceAmount: 3990,
    soldCount: 12,
    leader: {
      id: '1',
      displayName: '王姐',
      avatarUrl: null,
    },
    ...overrides,
    store: {
      id: '1',
      name: '王姐鲜果团',
      latitude: 30.2741,
      longitude: 120.1551,
      distanceMeters: null,
      distanceText: null,
      ...overrides.store,
    },
  }
}

describe('group buy filters', () => {
  it('keeps nearby group buys within 5km only', () => {
    expect(isNearbyGroupBuy(groupBuy({
      store: { distanceMeters: 4999 },
    }))).toBe(true)
    expect(isNearbyGroupBuy(groupBuy({
      store: { distanceMeters: NEARBY_DISTANCE_METERS },
    }))).toBe(true)
    expect(isNearbyGroupBuy(groupBuy({
      store: { distanceMeters: 5001, distanceText: '5.1km' },
    }))).toBe(false)
    expect(isNearbyGroupBuy(groupBuy({
      store: { distanceMeters: null, distanceText: '附近' },
    }))).toBe(false)
  })

  it('matches keyword categories without affecting all group buys', () => {
    expect(matchesGroupBuyCategory(groupBuy(), 'all')).toBe(true)
    expect(matchesGroupBuyCategory(groupBuy(), 'fresh')).toBe(true)
    expect(matchesGroupBuyCategory(groupBuy({ title: '办公室常备早餐团' }), 'repeat')).toBe(true)
    expect(matchesGroupBuyCategory(groupBuy({ title: '手作毛巾团', store: { name: '邻里好物' } }), 'fresh')).toBe(false)
  })
})
