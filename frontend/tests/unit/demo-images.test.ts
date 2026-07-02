import { describe, expect, it } from 'vitest'
import {
  getDemoProductImage,
  isExampleImageUrl,
  resolveDisplayImageUrl,
} from '@/utils/demo-images'

describe('demo image helpers', () => {
  it('detects example.com placeholder image URLs', () => {
    expect(isExampleImageUrl('https://example.com/product.png')).toBe(true)
    expect(isExampleImageUrl('https://img.example.com/cover-peach.jpg')).toBe(true)
    expect(isExampleImageUrl('https://images.unsplash.com/photo.jpg')).toBe(false)
  })

  it('replaces placeholder product URLs with public demo images', () => {
    const resolved = resolveDisplayImageUrl('https://example.com/product.png', '山东蜜桃')

    expect(resolved).toContain('images.unsplash.com')
    expect(resolved).not.toContain('example.com')
  })

  it('replaces named img.example.com placeholders with matching public images', () => {
    const peach = resolveDisplayImageUrl('https://img.example.com/cover-peach.jpg', '白玉蜜桃')
    const wogan = resolveDisplayImageUrl('https://img.example.com/cover-wogan.jpg', '沃柑')

    expect(peach).toContain('images.unsplash.com')
    expect(wogan).toContain('images.unsplash.com')
    expect(peach).not.toBe(wogan)
  })

  it('replaces invalid ad-hoc image values with kind-aware demo images', () => {
    const resolved = resolveDisplayImageUrl('123', '店铺 Logo', 'store')

    expect(resolved).toContain('images.unsplash.com')
    expect(resolved).not.toBe('123')
  })

  it('keeps real image URLs unchanged', () => {
    const image = 'https://images.unsplash.com/photo-1567306226416-28f0efdc88ce'

    expect(resolveDisplayImageUrl(image, '苹果')).toBe(image)
  })

  it('returns deterministic extra product images', () => {
    expect(getDemoProductImage('山东蜜桃', 1)).toBe(getDemoProductImage('山东蜜桃', 1))
    expect(getDemoProductImage('山东蜜桃', 1)).not.toBe(getDemoProductImage('山东蜜桃', 2))
  })
})
