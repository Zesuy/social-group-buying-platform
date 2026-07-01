import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import AppCard from '@/components/AppCard.vue'

describe('AppCard', () => {
  it('renders default slot content', () => {
    const wrapper = mount(AppCard, {
      slots: { default: '卡片内容' },
    })
    expect(wrapper.text()).toContain('卡片内容')
  })

  it('renders header slot', () => {
    const wrapper = mount(AppCard, {
      slots: {
        header: '卡片标题',
        default: '内容',
      },
    })
    expect(wrapper.find('.app-card__header').text()).toContain('卡片标题')
  })

  it('renders header-right slot', () => {
    const wrapper = mount(AppCard, {
      slots: {
        header: '标题',
        'header-right': '右侧',
        default: '内容',
      },
    })
    expect(wrapper.find('.app-card__header').text()).toContain('右侧')
  })

  it('renders footer slot', () => {
    const wrapper = mount(AppCard, {
      slots: {
        default: '内容',
        footer: '底部',
      },
    })
    expect(wrapper.find('.app-card__footer').text()).toContain('底部')
  })

  it('renders title prop', () => {
    const wrapper = mount(AppCard, {
      props: { title: '卡片标题' },
    })
    expect(wrapper.find('.app-card__title').text()).toBe('卡片标题')
  })

  it('applies clickable class', () => {
    const wrapper = mount(AppCard, {
      props: { clickable: true },
    })
    expect(wrapper.classes()).toContain('app-card--clickable')
  })

  it('emits click when clickable', async () => {
    const wrapper = mount(AppCard, {
      props: { clickable: true },
    })
    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeTruthy()
  })
})
