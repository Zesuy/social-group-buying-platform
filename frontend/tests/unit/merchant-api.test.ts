import { beforeEach, describe, expect, it, vi } from 'vitest'
import request from '@/api/request'
import { listLeaderOrdersByParams } from '@/api/leaderOrders'
import { listProductsByParams } from '@/api/products'
import { getStoreWorkbenchSummary } from '@/api/stores'

vi.mock('@/api/request', () => ({
  default: {
    get: vi.fn(),
  },
}))

describe('merchant api wiring', () => {
  beforeEach(() => {
    vi.mocked(request.get).mockReset()
  })

  it('fetches workbench summary', async () => {
    vi.mocked(request.get).mockResolvedValue({
      success: true,
      data: {
        store: { id: '20', name: '王姐鲜果团', logoUrl: null, status: 'active' },
        leader: { id: '10', displayName: '王姐', avatarUrl: null },
        todos: { paidOrders: 3, pendingAfterSales: 1, unreadLeaderChats: 4, publishedGroupBuys: 5 },
        statusCounts: {
          orders: { paid: 3, shipped: 8, completed: 21, afterSale: 1, canceled: 2 },
          afterSales: { pending: 1, approved: 2, rejected: 1, completed: 6 },
          groupBuys: { draft: 2, published: 5, ended: 9 },
        },
      },
      traceId: 'req_1',
    })

    const data = await getStoreWorkbenchSummary()

    expect(request.get).toHaveBeenCalledWith('/my/store/workbench-summary')
    expect(data.todos.paidOrders).toBe(3)
  })

  it('passes order status and keyword filters', async () => {
    vi.mocked(request.get).mockResolvedValue({
      success: true,
      data: { items: [], page: 1, pageSize: 20, total: 0, hasMore: false },
      traceId: 'req_2',
    })

    await listLeaderOrdersByParams({ status: 'paid', keyword: '202607', page: 1, pageSize: 20 })

    expect(request.get).toHaveBeenCalledWith('/my/store/orders', {
      params: { status: 'paid', keyword: '202607', page: 1, pageSize: 20 },
    })
  })

  it('passes product status and keyword filters', async () => {
    vi.mocked(request.get).mockResolvedValue({
      success: true,
      data: { items: [], page: 1, pageSize: 20, total: 0, hasMore: false },
      traceId: 'req_3',
    })

    await listProductsByParams({ status: 'active', keyword: '水蜜桃', page: 1, pageSize: 20 })

    expect(request.get).toHaveBeenCalledWith('/my/store/products', {
      params: { status: 'active', keyword: '水蜜桃', page: 1, pageSize: 20 },
    })
  })
})
