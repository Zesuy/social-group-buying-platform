import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import AddressForm from '@/components/AddressForm.vue'

vi.mock('vant', async () => {
  const actual = await vi.importActual<typeof import('vant')>('vant')
  return {
    ...actual,
    showToast: vi.fn(),
  }
})

describe('AddressForm', () => {
  it('renders form-card with 5 fields', () => {
    const wrapper = mount(AddressForm)
    // form-card 内应包含 5 个 field 行
    const labels = wrapper.findAll('.field label')
    expect(labels).toHaveLength(5)
    expect(labels[0].text()).toContain('收货人')
    expect(labels[1].text()).toContain('手机号')
    expect(labels[2].text()).toContain('省市区')
    expect(labels[3].text()).toContain('详细地址')
    expect(labels[4].text()).toContain('设为默认')
  })

  it('shows placeholder text when form is empty', () => {
    const wrapper = mount(AddressForm)
    expect(wrapper.text()).toContain('请输入姓名')
    expect(wrapper.text()).toContain('请输入手机号码')
    expect(wrapper.text()).toContain('请选择省市区')
    expect(wrapper.text()).toContain('街道门牌')
  })

  it('shows address data when provided', () => {
    const wrapper = mount(AddressForm, {
      props: {
        address: {
          id: '1',
          receiverName: '张三',
          receiverPhone: '13800000000',
          province: '浙江省',
          city: '杭州市',
          district: '西湖区',
          fullAddress: '浙江省杭州市西湖区某某路 1 号',
          detail: '某某路 1 号',
          isDefault: true,
        },
      },
    })
    expect(wrapper.text()).toContain('张三')
    expect(wrapper.text()).toContain('13800000000')
    expect(wrapper.text()).toContain('浙江省')
  })

  it('has default switch that toggles on click', async () => {
    const wrapper = mount(AddressForm)
    const toggle = wrapper.find('.switch')
    expect(toggle.exists()).toBe(true)
    // 初始应为 off（未勾选）
    expect(toggle.classes()).not.toContain('on')
    await toggle.trigger('click')
    expect(toggle.classes()).toContain('on')
  })

  it('submits empty form shows validation error', async () => {
    const wrapper = mount(AddressForm)
    // 直接调用 expose 的 submit 方法应因验证失败而不 emit
    wrapper.vm.submit()
    // 验证表单未提交成功（无 submit 事件）
    expect(wrapper.emitted('submit')).toBeFalsy()
  })

  it('selects province city district and submits payload', async () => {
    const wrapper = mount(AddressForm)

    await wrapper.find('.field').trigger('click')
    await wrapper.find('input[name="receiverName"]').setValue('李四')
    await wrapper.find('input[name="receiverName"]').trigger('blur')

    await wrapper.findAll('.field')[1].trigger('click')
    await wrapper.find('input[name="receiverPhone"]').setValue('13900000001')
    await wrapper.find('input[name="receiverPhone"]').trigger('blur')

    wrapper.vm.onAreaConfirm({
      selectedOptions: [
        { code: '440000', name: '广东省' },
        { code: '440100', name: '广州市' },
        { code: '440106', name: '天河区' },
      ],
    })

    await wrapper.findAll('.field')[3].trigger('click')
    await wrapper.find('textarea[name="detail"]').setValue('某某路 88 号')
    await wrapper.find('textarea[name="detail"]').trigger('blur')

    wrapper.vm.submit()

    expect(wrapper.emitted('submit')?.[0]?.[0]).toEqual({
      receiverName: '李四',
      receiverPhone: '13900000001',
      province: '广东省',
      city: '广州市',
      district: '天河区',
      detail: '某某路 88 号',
      isDefault: false,
    })
  })
})
