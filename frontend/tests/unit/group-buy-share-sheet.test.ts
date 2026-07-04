import { describe, expect, it, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import GroupBuyShareSheet from '@/components/GroupBuyShareSheet.vue'
import QRCode from 'qrcode'
import { showToast } from 'vant'

vi.mock('qrcode', () => ({
  default: {
    toDataURL: vi.fn(),
  },
}))

vi.mock('vant', () => ({
  showToast: vi.fn(),
}))

const payload = {
  title: '周末阳山水蜜桃社区团',
  coverImageUrl: null,
  minPriceAmount: 3990,
  maxPriceAmount: 4990,
  storeName: '王姐鲜果团',
  leaderName: '王姐',
  deliveryType: 'express',
  shippingTime: '2026-07-10T18:00:00',
}

function mountSheet() {
  return mount(GroupBuyShareSheet, {
    props: {
      modelValue: true,
      payload,
      shareUrl: 'https://demo.example/#/share/group-buys/token-1',
    },
    global: {
      stubs: {
        'van-popup': {
          props: ['show'],
          template: '<div v-if="show" class="van-popup"><slot /></div>',
        },
        'van-icon': true,
        'van-loading': true,
        ImageWithFallback: true,
      },
    },
  })
}

describe('GroupBuyShareSheet', () => {
  beforeEach(() => {
    vi.mocked(QRCode.toDataURL).mockResolvedValue('data:image/png;base64,qr')
    vi.mocked(showToast).mockReset()
    Object.assign(navigator, {
      clipboard: {
        writeText: vi.fn().mockResolvedValue(undefined),
      },
      share: undefined,
    })
  })

  it('renders share card and generated qr code', async () => {
    const wrapper = mountSheet()
    await flushPromises()

    expect(QRCode.toDataURL).toHaveBeenCalledWith(
      'https://demo.example/#/share/group-buys/token-1',
      expect.objectContaining({ width: 216 }),
    )
    expect(wrapper.text()).toContain('周末阳山水蜜桃社区团')
    expect(wrapper.text()).toContain('王姐鲜果团')
    expect(wrapper.find('.qr-panel__code img').attributes('src')).toBe('data:image/png;base64,qr')
  })

  it('copies link when system share is unavailable', async () => {
    const wrapper = mountSheet()
    await flushPromises()

    await wrapper.find('.share-sheet__actions .app-button--primary').trigger('click')
    await flushPromises()

    expect(navigator.clipboard.writeText).toHaveBeenCalledWith('https://demo.example/#/share/group-buys/token-1')
    expect(showToast).toHaveBeenCalledWith('当前浏览器不支持系统分享，已复制链接')
  })
})
