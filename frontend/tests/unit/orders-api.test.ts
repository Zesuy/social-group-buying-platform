import { beforeEach, describe, expect, it, vi } from 'vitest'
import request from '@/api/request'
import { payOrder, simulatePay } from '@/api/orders'

vi.mock('@/api/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('orders api', () => {
  beforeEach(() => {
    vi.mocked(request.post).mockReset()
  })

  it('starts payment through unified pay endpoint', async () => {
    vi.mocked(request.post).mockResolvedValue({
      success: true,
      data: { mode: 'sandboxAlipay', formHtml: '<form id="alipaysubmit"></form>' },
      traceId: 'req_1',
    })

    const result = await payOrder('9001')

    expect(request.post).toHaveBeenCalledWith('/orders/9001/pay')
    expect(result.mode).toBe('sandboxAlipay')
    expect(result.formHtml).toContain('alipaysubmit')
  })

  it('keeps simulate-pay helper for internal tests', async () => {
    vi.mocked(request.post).mockResolvedValue({
      success: true,
      data: { id: '9001', payStatus: 'paid', orderStatus: 'paid' },
      traceId: 'req_1',
    })

    const result = await simulatePay('9001')

    expect(request.post).toHaveBeenCalledWith('/orders/9001/simulate-pay')
    expect(result.payStatus).toBe('paid')
  })
})
