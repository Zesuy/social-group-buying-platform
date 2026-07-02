import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ImageWithFallback from '@/components/ImageWithFallback.vue'

describe('ImageWithFallback', () => {
  it('shows a named gradient placeholder when image url is empty', () => {
    const wrapper = mount(ImageWithFallback, {
      props: {
        src: null,
        alt: '羽毛球训练套装',
        width: '72px',
        height: '72px',
      },
    })

    expect(wrapper.find('.image-with-fallback__placeholder--named').exists()).toBe(true)
    expect(wrapper.text()).toContain('羽毛球训练套装')
  })

  it('keeps icon placeholder when no name is available', () => {
    const wrapper = mount(ImageWithFallback, {
      props: {
        src: null,
        alt: '',
      },
    })

    expect(wrapper.find('.image-with-fallback__placeholder--named').exists()).toBe(false)
    expect(wrapper.find('.van-icon-photo').exists()).toBe(true)
  })
})
