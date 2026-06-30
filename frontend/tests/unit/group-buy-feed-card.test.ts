import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import GroupBuyFeedCard from '@/components/GroupBuyFeedCard.vue'
import type { PublicGroupBuyItem } from '@/types'

const mockItem: PublicGroupBuyItem = {
  id: 100,
  title: '山东蜜桃团购',
  coverImageUrl: 'https://example.com/cover.png',
  status: 'published',
  endTime: '2026-07-01T12:00:00',
  minPriceAmount: 2990,
  soldCount: 12,
  leader: { id: 10, displayName: '某某团长', avatarUrl: 'https://example.com/avatar.png' },
  store: { id: 20, name: '某某的小店' },
}

describe('GroupBuyFeedCard', () => {
  it('renders title and price', () => {
    const wrapper = mount(GroupBuyFeedCard, { props: { item: mockItem } })
    expect(wrapper.text()).toContain('山东蜜桃团购')
    expect(wrapper.text()).toContain('¥29.90')
  })

  it('renders leader name and store name', () => {
    const wrapper = mount(GroupBuyFeedCard, { props: { item: mockItem } })
    expect(wrapper.text()).toContain('某某团长')
    expect(wrapper.text()).toContain('某某的小店')
  })

  it('renders sold count', () => {
    const wrapper = mount(GroupBuyFeedCard, { props: { item: mockItem } })
    expect(wrapper.text()).toContain('12人已团')
  })

  it('emits click event', () => {
    const wrapper = mount(GroupBuyFeedCard, { props: { item: mockItem } })
    wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeTruthy()
  })

  it('shows ended tag when status is ended', () => {
    const endedItem = { ...mockItem, status: 'ended' }
    const wrapper = mount(GroupBuyFeedCard, { props: { item: endedItem } })
    expect(wrapper.text()).toContain('已结束')
  })

  it('does not show ended tag when status is published', () => {
    const wrapper = mount(GroupBuyFeedCard, { props: { item: mockItem } })
    expect(wrapper.text()).not.toContain('已结束')
  })
})
