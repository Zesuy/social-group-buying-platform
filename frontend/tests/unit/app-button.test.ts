import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import AppButton from '@/components/AppButton.vue'

describe('AppButton', () => {
  it('renders label text', () => {
    const wrapper = mount(AppButton, {
      props: { label: '提交' },
    })
    expect(wrapper.text()).toContain('提交')
  })

  it('renders default slot content', () => {
    const wrapper = mount(AppButton, {
      slots: { default: '确认支付' },
    })
    expect(wrapper.text()).toContain('确认支付')
  })

  it('applies primary variant', () => {
    const wrapper = mount(AppButton, {
      props: { variant: 'primary' },
    })
    expect(wrapper.classes()).toContain('app-button--primary')
  })

  it('applies ghost variant', () => {
    const wrapper = mount(AppButton, {
      props: { variant: 'ghost' },
    })
    expect(wrapper.classes()).toContain('app-button--ghost')
  })

  it('applies danger variant', () => {
    const wrapper = mount(AppButton, {
      props: { variant: 'danger' },
    })
    expect(wrapper.classes()).toContain('app-button--danger')
  })

  it('applies plain variant', () => {
    const wrapper = mount(AppButton, {
      props: { variant: 'plain' },
    })
    expect(wrapper.classes()).toContain('app-button--plain')
  })

  it('applies block class', () => {
    const wrapper = mount(AppButton, {
      props: { block: true },
    })
    expect(wrapper.classes()).toContain('app-button--block')
  })

  it('applies disabled state', () => {
    const wrapper = mount(AppButton, {
      props: { disabled: true },
    })
    expect(wrapper.classes()).toContain('app-button--disabled')
    expect(wrapper.attributes('disabled')).toBeDefined()
  })

  it('applies pill class', () => {
    const wrapper = mount(AppButton, {
      props: { pill: true },
    })
    expect(wrapper.classes()).toContain('app-button--pill')
  })

  it('emits click event', async () => {
    const wrapper = mount(AppButton, {
      props: { label: '点击' },
    })
    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeTruthy()
  })

  it('does not emit click when disabled', async () => {
    const wrapper = mount(AppButton, {
      props: { disabled: true, label: '禁用' },
    })
    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeFalsy()
  })

  it('does not emit click when loading', async () => {
    const wrapper = mount(AppButton, {
      props: { loading: true },
    })
    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeFalsy()
  })
})
