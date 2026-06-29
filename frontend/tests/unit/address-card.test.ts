import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import AddressCard from '@/components/AddressCard.vue'
import type { AddressData } from '@/types'

const mockAddress: AddressData = {
  id: 1,
  receiverName: '张三',
  receiverPhone: '13800000000',
  province: '浙江省',
  city: '杭州市',
  district: '西湖区',
  detail: '某某路 1 号',
  fullAddress: '浙江省杭州市西湖区某某路 1 号',
  isDefault: true,
}

const mockNonDefault: AddressData = { ...mockAddress, id: 2, isDefault: false }

describe('AddressCard', () => {
  it('renders receiver name and phone', () => {
    const wrapper = mount(AddressCard, { props: { address: mockAddress } })
    expect(wrapper.text()).toContain('张三')
    expect(wrapper.text()).toContain('13800000000')
  })

  it('renders full address', () => {
    const wrapper = mount(AddressCard, { props: { address: mockAddress } })
    expect(wrapper.text()).toContain('浙江省杭州市西湖区某某路 1 号')
  })

  it('shows default tag for default address', () => {
    const wrapper = mount(AddressCard, { props: { address: mockAddress } })
    expect(wrapper.text()).toContain('默认')
  })

  it('does not show default tag for non-default address', () => {
    const wrapper = mount(AddressCard, { props: { address: mockNonDefault } })
    expect(wrapper.text()).not.toContain('默认')
  })

  it('emits click with address when clicked in selectable mode', async () => {
    const wrapper = mount(AddressCard, {
      props: { address: mockAddress, selectable: true },
    })
    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeTruthy()
    const emitted = wrapper.emitted('click')![0][0] as AddressData
    expect(emitted.id).toBe(1)
  })

  it('emits edit event', async () => {
    const wrapper = mount(AddressCard, { props: { address: mockAddress } })
    const editBtn = wrapper.find('.van-button--default')
    await editBtn.trigger('click')
    expect(wrapper.emitted('edit')).toBeTruthy()
    expect(wrapper.emitted('edit')![0][0]).toBe(1)
  })

  it('emits delete event', async () => {
    const wrapper = mount(AddressCard, { props: { address: mockAddress } })
    const deleteBtn = wrapper.find('.van-button--danger')
    await deleteBtn.trigger('click')
    expect(wrapper.emitted('delete')).toBeTruthy()
    expect(wrapper.emitted('delete')![0][0]).toBe(1)
  })

  it('applies selected class when selected and selectable', () => {
    const wrapper = mount(AddressCard, {
      props: { address: mockAddress, selected: true, selectable: true },
    })
    expect(wrapper.classes()).toContain('address-card--selected')
  })
})
