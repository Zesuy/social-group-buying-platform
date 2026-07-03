import { describe, expect, it, vi, beforeEach } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ProductForm from '@/components/ProductForm.vue'
import { uploadImage } from '@/api/uploads'

vi.mock('@/api/uploads', () => ({
  uploadImage: vi.fn(),
}))

describe('ProductForm', () => {
  beforeEach(() => {
    vi.mocked(uploadImage).mockReset()
  })

  it('uses a demo product image when cover image is empty', async () => {
    const wrapper = mount(ProductForm)

    await wrapper.find('input[placeholder="请输入商品名称"]').setValue('山东蜜桃')
    await wrapper.find('input[placeholder="0.00"]').setValue('29.90')
    await wrapper.find('input[placeholder="库存数量"]').setValue('100')

    const data = wrapper.vm.getFormData()

    expect(data?.coverImageUrl).toContain('images.unsplash.com')
    expect(data?.name).toBe('山东蜜桃')
  })

  it('uses uploaded cover image url in form data', async () => {
    vi.mocked(uploadImage).mockResolvedValue({
      url: 'http://localhost:8080/uploads/images/cover.png',
      objectKey: 'images/cover.png',
      originalFilename: 'cover.png',
      contentType: 'image/png',
      size: 9,
    })
    const wrapper = mount(ProductForm)

    await wrapper.find('input[placeholder="请输入商品名称"]').setValue('山东蜜桃')
    await wrapper.find('input[placeholder="0.00"]').setValue('29.90')
    await wrapper.find('input[placeholder="库存数量"]').setValue('100')

    const fileInput = wrapper.find('input[type="file"]')
    Object.defineProperty(fileInput.element, 'files', {
      value: [new File(['image'], 'cover.png', { type: 'image/png' })],
      configurable: true,
    })
    await fileInput.trigger('change')
    await flushPromises()

    expect(wrapper.vm.getFormData()?.coverImageUrl).toBe('http://localhost:8080/uploads/images/cover.png')
  })
})
