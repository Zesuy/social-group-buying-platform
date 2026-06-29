import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PriceText from '@/components/PriceText.vue'

describe('PriceText', () => {
  it('renders formatted amount from integer fen', () => {
    const wrapper = mount(PriceText, { props: { amount: 2990 } })
    expect(wrapper.text()).toContain('¥29.90')
  })

  it('renders ¥0.00 for 0', () => {
    const wrapper = mount(PriceText, { props: { amount: 0 } })
    expect(wrapper.text()).toContain('¥0.00')
  })

  it('renders ¥0.00 for null', () => {
    const wrapper = mount(PriceText, { props: { amount: null } })
    expect(wrapper.text()).toContain('¥0.00')
  })

  it('renders ¥0.00 for undefined', () => {
    const wrapper = mount(PriceText, { props: { amount: undefined } })
    expect(wrapper.text()).toContain('¥0.00')
  })

  it('applies size class correctly', () => {
    const wrapper = mount(PriceText, { props: { amount: 100, size: 'lg' } })
    expect(wrapper.classes()).toContain('price-text--lg')
  })

  it('applies custom color', () => {
    const wrapper = mount(PriceText, { props: { amount: 100, color: 'red' } })
    expect(wrapper.attributes('style')).toContain('red')
  })

  it('renders prefix and suffix slots', () => {
    const wrapper = mount(PriceText, {
      props: { amount: 2990 },
      slots: { prefix: '起', suffix: '/份' },
    })
    expect(wrapper.text()).toContain('起')
    expect(wrapper.text()).toContain('/份')
  })
})
