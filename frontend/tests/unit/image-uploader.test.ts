import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ImageUploader from '@/components/ImageUploader.vue'
import { uploadImage } from '@/api/uploads'
import { showToast } from 'vant'

vi.mock('@/api/uploads', () => ({
  uploadImage: vi.fn(),
}))

vi.mock('vant', async () => {
  const actual = await vi.importActual<typeof import('vant')>('vant')
  return {
    ...actual,
    showToast: vi.fn(),
  }
})

describe('ImageUploader', () => {
  beforeEach(() => {
    vi.mocked(uploadImage).mockReset()
    vi.mocked(showToast).mockClear()
  })

  it('emits manual url input changes', async () => {
    const wrapper = mount(ImageUploader, {
      props: {
        modelValue: '',
      },
    })

    await wrapper.find('.image-uploader__input').setValue('https://example.com/cover.png')

    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['https://example.com/cover.png'])
  })

  it('uploads a selected image and emits returned url', async () => {
    vi.mocked(uploadImage).mockResolvedValue({
      url: 'http://localhost:8080/uploads/images/cover.png',
      objectKey: 'images/cover.png',
      originalFilename: 'cover.png',
      contentType: 'image/png',
      size: 9,
    })
    const wrapper = mount(ImageUploader)

    const file = new File(['image'], 'cover.png', { type: 'image/png' })
    const input = wrapper.find('input[type="file"]')
    Object.defineProperty(input.element, 'files', {
      value: [file],
      configurable: true,
    })
    await input.trigger('change')
    await flushPromises()

    expect(uploadImage).toHaveBeenCalledWith(file)
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([
      'http://localhost:8080/uploads/images/cover.png',
    ])
    expect(wrapper.emitted('uploaded')?.[0][0]).toMatchObject({ objectKey: 'images/cover.png' })
    expect(showToast).toHaveBeenCalledWith('图片已上传')
  })

  it('keeps current value and shows toast when upload fails', async () => {
    vi.mocked(uploadImage).mockRejectedValue({ message: '仅支持 jpg、png、webp 图片' })
    const wrapper = mount(ImageUploader, {
      props: {
        modelValue: 'https://example.com/old.png',
      },
    })

    const input = wrapper.find('input[type="file"]')
    Object.defineProperty(input.element, 'files', {
      value: [new File(['text'], 'bad.txt', { type: 'text/plain' })],
      configurable: true,
    })
    await input.trigger('change')
    await flushPromises()

    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
    expect(showToast).toHaveBeenCalledWith('仅支持 jpg、png、webp 图片')
  })
})
