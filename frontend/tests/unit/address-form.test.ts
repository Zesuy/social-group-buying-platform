import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import AddressForm from '@/components/AddressForm.vue'

describe('AddressForm', () => {
  it('renders form fields', () => {
    const wrapper = mount(AddressForm)
    expect(wrapper.find('input').exists()).toBe(true)
  })

  it('renders save button', () => {
    const wrapper = mount(AddressForm)
    expect(wrapper.text()).toContain('保存地址')
  })

  it('shows loading state on button', () => {
    const wrapper = mount(AddressForm, { props: { loading: true } })
    expect(wrapper.text()).toContain('保存中...')
  })

  it('emits submit event', async () => {
    const wrapper = mount(AddressForm)
    const button = wrapper.find('.van-button--primary')
    expect(button.exists()).toBe(true)
    // Verify button type is submit
    expect(button.attributes('type')).toBe('submit')
  })
})
