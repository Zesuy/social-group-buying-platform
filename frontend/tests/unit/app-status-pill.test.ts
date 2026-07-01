import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import AppStatusPill from '@/components/AppStatusPill.vue'

describe('AppStatusPill', () => {
  it('renders label text', () => {
    const wrapper = mount(AppStatusPill, {
      props: { label: '已支付' },
    })
    expect(wrapper.text()).toContain('已支付')
  })

  it('renders slot content', () => {
    const wrapper = mount(AppStatusPill, {
      slots: { default: '待处理' },
    })
    expect(wrapper.text()).toContain('待处理')
  })

  it('applies green variant', () => {
    const wrapper = mount(AppStatusPill, {
      props: { variant: 'green', label: '进行中' },
    })
    expect(wrapper.classes()).toContain('app-status-pill--green')
  })

  it('applies orange variant', () => {
    const wrapper = mount(AppStatusPill, {
      props: { variant: 'orange', label: '待支付' },
    })
    expect(wrapper.classes()).toContain('app-status-pill--orange')
  })

  it('applies gray variant', () => {
    const wrapper = mount(AppStatusPill, {
      props: { variant: 'gray', label: '已结束' },
    })
    expect(wrapper.classes()).toContain('app-status-pill--gray')
  })

  it('applies red variant', () => {
    const wrapper = mount(AppStatusPill, {
      props: { variant: 'red', label: '已取消' },
    })
    expect(wrapper.classes()).toContain('app-status-pill--red')
  })

  it('renders dot mode', () => {
    const wrapper = mount(AppStatusPill, {
      props: { dot: true, label: '待支付' },
    })
    expect(wrapper.classes()).toContain('app-status-pill--dot')
    expect(wrapper.find('.app-status-pill__dot').exists()).toBe(true)
  })

  it('applies small size', () => {
    const wrapper = mount(AppStatusPill, {
      props: { size: 'sm', label: '标签' },
    })
    expect(wrapper.classes()).toContain('app-status-pill--sm')
  })
})
