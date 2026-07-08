import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import GroupBuyFeedCard from '@/components/GroupBuyFeedCard.vue'
import type { PublicGroupBuyItem } from '@/types'

const mockItem: PublicGroupBuyItem = {
  id: '100',
  title: '周末阳山水蜜桃社区团',
  coverImageUrl: 'https://example.com/cover.png',
  status: 'published',
  endTime: '2026-07-01T12:00:00',
  minPriceAmount: 2990,
  soldCount: 12,
  leader: { id: '10', displayName: '王姐鲜果团', avatarUrl: 'https://example.com/avatar.png' },
  store: {
    id: '20',
    name: '王姐社区鲜果店',
    latitude: null,
    longitude: null,
    distanceMeters: null,
    distanceText: null,
  },
}

describe('GroupBuyFeedCard', () => {
  it('renders title and price', () => {
    const wrapper = mount(GroupBuyFeedCard, { props: { item: mockItem } })
    expect(wrapper.text()).toContain('周末阳山水蜜桃社区团')
    expect(wrapper.text()).toContain('¥29.90')
  })

  it('renders leader name and store name', () => {
    const wrapper = mount(GroupBuyFeedCard, { props: { item: mockItem } })
    expect(wrapper.text()).toContain('王姐鲜果团')
    expect(wrapper.text()).toContain('王姐社区鲜果店')
  })

  it('renders sold count', () => {
    const wrapper = mount(GroupBuyFeedCard, { props: { item: mockItem } })
    expect(wrapper.text()).toContain('12人已团')
  })

  it('renders activity reason, fulfillment signal and primary action', () => {
    const wrapper = mount(GroupBuyFeedCard, { props: { item: mockItem } })
    expect(wrapper.text()).toContain('王姐社区鲜果店组织 周末阳山水蜜桃，集中收单按约履约。')
    expect(wrapper.text()).toContain('集中履约')
    expect(wrapper.get('button.group-buy-feed-card__cta').text()).toBe('去跟团')
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

  it('renders subscribed and loading subscription states', async () => {
    const wrapper = mount(GroupBuyFeedCard, {
      props: { item: mockItem, subscribed: true },
    })
    expect(wrapper.text()).toContain('已订阅')

    await wrapper.setProps({ subscribed: false, subscribeLoading: true })
    expect(wrapper.text()).toContain('处理中')
    expect(wrapper.find('.group-buy-feed-card__subscribe').attributes('disabled')).toBeDefined()
  })

  it('renders distance marker when distance text exists', () => {
    const wrapper = mount(GroupBuyFeedCard, {
      props: {
        item: {
          ...mockItem,
          store: { ...mockItem.store, distanceMeters: 860, distanceText: '860m' },
        },
      },
    })

    expect(wrapper.text()).toContain('距你 860m')
    expect(wrapper.text()).toContain('附近可履约')
  })

  it('renders credible fallback image text when cover image is missing', () => {
    const wrapper = mount(GroupBuyFeedCard, {
      props: {
        item: {
          ...mockItem,
          coverImageUrl: null,
        },
      },
    })

    expect(wrapper.find('.group-buy-feed-card__image-fallback').exists()).toBe(true)
    expect(wrapper.text()).toContain('生鲜团')
  })

  it('hides duplicated store header in leader homepage context but keeps activity information', () => {
    const wrapper = mount(GroupBuyFeedCard, {
      props: {
        item: mockItem,
        showStoreHeader: false,
        showLocationSignals: false,
      },
    })

    expect(wrapper.find('.group-buy-feed-card__leader').exists()).toBe(false)
    expect(wrapper.text()).toContain('周末阳山水蜜桃社区团')
    expect(wrapper.text()).toContain('¥29.90')
    expect(wrapper.text()).toContain('正在开团')
    expect(wrapper.text()).toContain('12人已团')
    expect(wrapper.text()).toContain('去跟团')
  })
})
