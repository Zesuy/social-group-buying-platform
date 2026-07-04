import { beforeEach, describe, expect, it, vi } from 'vitest'
import { buildGroupBuyShareUrl, buildShareTokenUrl, shareBySystem } from '@/utils/share'

describe('share url helpers', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('builds hash route url for public group buy detail', () => {
    expect(buildGroupBuyShareUrl('100')).toBe('https://shop.zesuy.top/#/group-buys/100')
  })

  it('builds hash route url for share token landing page', () => {
    expect(buildShareTokenUrl('token-abc')).toBe('https://shop.zesuy.top/#/share/group-buys/token-abc')
  })

  it('calls Web Share API when available', async () => {
    const share = vi.fn().mockResolvedValue(undefined)
    Object.assign(navigator, { share })

    const result = await shareBySystem({
      title: '周末阳山水蜜桃社区团',
      text: '王姐鲜果团的团购正在进行',
      url: 'https://shop.zesuy.top/#/group-buys/100',
    })

    expect(result).toBe('shared')
    expect(share).toHaveBeenCalledWith({
      title: '周末阳山水蜜桃社区团',
      text: '王姐鲜果团的团购正在进行',
      url: 'https://shop.zesuy.top/#/group-buys/100',
    })
  })

  it('reports unsupported when Web Share API is missing', async () => {
    Object.assign(navigator, { share: undefined })

    await expect(shareBySystem({
      title: '周末阳山水蜜桃社区团',
      url: 'https://shop.zesuy.top/#/group-buys/100',
    })).resolves.toBe('unsupported')
  })
})
