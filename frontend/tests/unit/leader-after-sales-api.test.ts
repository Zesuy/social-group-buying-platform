import { beforeEach, describe, expect, it, vi } from 'vitest'
import request from '@/api/request'
import {
  approveLeaderAfterSale,
  completeLeaderAfterSaleRefund,
  getLeaderAfterSale,
  listLeaderAfterSales,
  rejectLeaderAfterSale,
} from '@/api/leaderAfterSales'

vi.mock('@/api/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

describe('leader after-sales api', () => {
  beforeEach(() => {
    vi.mocked(request.get).mockReset()
    vi.mocked(request.post).mockReset()
  })

  it('lists store after-sales without sending unsupported status param', async () => {
    vi.mocked(request.get).mockResolvedValue({
      success: true,
      data: { items: [], page: 1, pageSize: 20, total: 0, hasMore: false },
      traceId: 'req_1',
    })

    await listLeaderAfterSales({ status: 'pending', page: 1, pageSize: 20 })

    expect(request.get).toHaveBeenCalledWith('/my/store/after-sales', {
      params: { page: 1, pageSize: 20 },
    })
  })

  it('gets detail and handles approve reject complete-refund actions', async () => {
    vi.mocked(request.get).mockResolvedValue({
      success: true,
      data: { id: '1001', status: 'pending' },
      traceId: 'req_1',
    })
    vi.mocked(request.post).mockResolvedValue({
      success: true,
      data: { id: '1001', status: 'approved' },
      traceId: 'req_2',
    })

    await getLeaderAfterSale('1001')
    await approveLeaderAfterSale('1001')
    await rejectLeaderAfterSale('1001', { rejectReason: '商品完好，不予退款' })
    await completeLeaderAfterSaleRefund('1001')

    expect(request.get).toHaveBeenCalledWith('/my/store/after-sales/1001')
    expect(request.post).toHaveBeenNthCalledWith(1, '/my/store/after-sales/1001/approve')
    expect(request.post).toHaveBeenNthCalledWith(2, '/my/store/after-sales/1001/reject', {
      rejectReason: '商品完好，不予退款',
    })
    expect(request.post).toHaveBeenNthCalledWith(3, '/my/store/after-sales/1001/complete-refund')
  })
})
