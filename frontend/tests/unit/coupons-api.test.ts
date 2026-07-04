import { beforeEach, describe, expect, it, vi } from 'vitest'
import request from '@/api/request'
import {
  claimCoupon,
  createStoreCoupon,
  disableStoreCoupon,
  listLeaderHomepageCoupons,
  listStoreCoupons,
  updateStoreCoupon,
} from '@/api/coupons'

vi.mock('@/api/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
  },
}))

describe('coupons api', () => {
  beforeEach(() => {
    vi.mocked(request.get).mockReset()
    vi.mocked(request.post).mockReset()
    vi.mocked(request.patch).mockReset()
  })

  it('lists store coupons', async () => {
    vi.mocked(request.get).mockResolvedValue({ success: true, data: [], traceId: 'req_1' })

    await listStoreCoupons()

    expect(request.get).toHaveBeenCalledWith('/my/store/coupons')
  })

  it('creates and updates new subscriber coupons', async () => {
    const payload = {
      name: '新客订阅立减券',
      couponType: 'amount',
      claimCondition: 'new_subscriber',
      amount: 1000,
      thresholdAmount: 0,
      totalQuantity: 100,
      perUserLimit: 1,
      startTime: '2026-07-04T10:00:00',
      endTime: '2026-08-04T10:00:00',
    }
    vi.mocked(request.post).mockResolvedValue({ success: true, data: { id: '1' }, traceId: 'req_1' })
    vi.mocked(request.patch).mockResolvedValue({ success: true, data: { id: '1' }, traceId: 'req_2' })

    await createStoreCoupon(payload)
    await updateStoreCoupon('1', payload)

    expect(request.post).toHaveBeenCalledWith('/my/store/coupons', payload)
    expect(request.patch).toHaveBeenCalledWith('/my/store/coupons/1', payload)
  })

  it('disables and claims coupons', async () => {
    vi.mocked(request.post).mockResolvedValue({ success: true, data: null, traceId: 'req_1' })

    await disableStoreCoupon('1')
    await claimCoupon('2')

    expect(request.post).toHaveBeenCalledWith('/my/store/coupons/1/disable')
    expect(request.post).toHaveBeenCalledWith('/coupons/2/claim')
  })

  it('lists leader homepage coupon offers with homepage scene', async () => {
    vi.mocked(request.get).mockResolvedValue({ success: true, data: [], traceId: 'req_1' })

    await listLeaderHomepageCoupons('9')

    expect(request.get).toHaveBeenCalledWith('/leaders/9/coupons', {
      params: { scene: 'homepage' },
    })
  })
})
