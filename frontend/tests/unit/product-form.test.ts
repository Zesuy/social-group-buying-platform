import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ProductForm from '@/components/ProductForm.vue'

describe('ProductForm', () => {
  it('uses a demo product image when cover image is empty', async () => {
    const wrapper = mount(ProductForm)

    await wrapper.find('input[placeholder="请输入商品名称"]').setValue('山东蜜桃')
    await wrapper.find('input[placeholder="0.00"]').setValue('29.90')
    await wrapper.find('input[placeholder="库存数量"]').setValue('100')

    const data = wrapper.vm.getFormData()

    expect(data?.coverImageUrl).toContain('images.unsplash.com')
    expect(data?.name).toBe('山东蜜桃')
  })
})
