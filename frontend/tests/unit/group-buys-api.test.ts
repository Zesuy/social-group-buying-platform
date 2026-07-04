import { beforeEach, describe, expect, it, vi } from 'vitest'
import request from '@/api/request'
import { listPublicGroupBuys } from '@/api/groupBuys'

vi.mock('@/api/request', () => ({
  default: {
    get: vi.fn(),
  },
}))

describe('group buys api', () => {
  beforeEach(() => {
    vi.mocked(request.get).mockReset()
  })

  it('lists public group buys with keyword and location params', async () => {
    vi.mocked(request.get).mockResolvedValue({
      success: true,
      data: { items: [], page: 1, pageSize: 20, total: 0, hasMore: false },
      traceId: 'req_1',
    })

    await listPublicGroupBuys({
      page: 1,
      pageSize: 20,
      keyword: '蜜桃',
      latitude: 30.2741,
      longitude: 120.1551,
      maxDistanceMeters: 5000,
      sort: 'distance',
    })

    expect(request.get).toHaveBeenCalledWith('/group-buys', {
      params: {
        page: 1,
        pageSize: 20,
        keyword: '蜜桃',
        latitude: 30.2741,
        longitude: 120.1551,
        maxDistanceMeters: 5000,
        sort: 'distance',
      },
    })
  })
})
