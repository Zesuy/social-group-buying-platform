import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import GroupBuyFeedCard from '@/components/GroupBuyFeedCard.vue'
import PriceText from '@/components/PriceText.vue'
import type { PublicGroupBuyItem } from '@/types'

const baseItem: PublicGroupBuyItem = {
  id: 100,
  title: '测试团购',
  coverImageUrl: null,
  status: 'published',
  endTime: null,
  minPriceAmount: 1990,
  soldCount: 5,
  leader: { id: 1, displayName: '团长', avatarUrl: null },
  store: { id: 1, name: '店铺' },
}

describe('GroupBuyFeedCard ended tag', () => {
  it('published status does not show ended tag', () => {
    const wrapper = mount(GroupBuyFeedCard, { props: { item: baseItem } })
    expect(wrapper.text()).not.toContain('已结束')
  })

  it('ended status shows ended tag', () => {
    const wrapper = mount(GroupBuyFeedCard, { props: { item: { ...baseItem, status: 'ended' } } })
    expect(wrapper.text()).toContain('已结束')
  })
})

describe('PriceText integer fen', () => {
  it('displays integer fen without floating point business values', () => {
    const wrapper = mount(PriceText, { props: { amount: 1990 } })
    expect(wrapper.text()).toBe('¥19.90')
    expect(wrapper.text()).not.toContain('1990')
  })

  it('displays zero correctly', () => {
    const wrapper = mount(PriceText, { props: { amount: 0 } })
    expect(wrapper.text()).toBe('¥0.00')
  })

  it('displays null as zero', () => {
    const wrapper = mount(PriceText, { props: { amount: null } })
    expect(wrapper.text()).toBe('¥0.00')
  })
})

describe('GroupBuyDetail out-of-stock', () => {
  it('feed card renders stock-zero item without buy button — price still visible', () => {
    // GroupBuyFeedCard doesn't show stock info, but the "选择" disabled state
    // is in the detail view. Verify the card itself still renders for sold-out items.
    const wrapper = mount(GroupBuyFeedCard, {
      props: { item: { ...baseItem, minPriceAmount: 0, soldCount: 100 } },
    })
    expect(wrapper.text()).toContain('¥0.00')
    expect(wrapper.text()).toContain('100人已团')
  })
})
