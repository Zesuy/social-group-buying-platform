import { describe, expect, it } from 'vitest'
import { createContentBlock, normalizeContentBlocks } from '@/utils'
import type { ContentBlockData } from '@/types'

describe('content block helpers', () => {
  it('creates editable defaults for supported block types', () => {
    expect(createContentBlock('paragraph')).toMatchObject({ type: 'paragraph', text: '' })
    expect(createContentBlock('section')).toMatchObject({ type: 'section', title: '', text: '' })
    expect(createContentBlock('image')).toMatchObject({ type: 'image', url: '' })
    expect(createContentBlock('list')).toMatchObject({ type: 'list', items: [''] })
    expect(createContentBlock('deliveryNote')).toMatchObject({ type: 'deliveryNote', text: '' })
  })

  it('normalizes supported blocks and removes empty blocks', () => {
    const blocks: ContentBlockData[] = [
      { type: 'paragraph', text: '  新鲜采摘  ' },
      { type: 'section', title: '  产地  ', text: '  阳山  ' },
      { type: 'image', url: '  /uploads/cover.png  ', caption: '  实拍  ' },
      { type: 'list', title: '', items: [' 当季 ', '', ' 次日发货 '] },
      { type: 'deliveryNote', text: '  截单后 48 小时内发货  ' },
      { type: 'paragraph', text: '   ' },
      { type: 'image', url: '' },
      { type: 'list', items: [''] },
    ]

    expect(normalizeContentBlocks(blocks)).toEqual([
      { type: 'paragraph', title: null, text: '新鲜采摘', url: null, caption: null, items: null },
      { type: 'section', title: '产地', text: '阳山', url: null, caption: null, items: null },
      { type: 'image', title: null, text: null, url: '/uploads/cover.png', caption: '实拍', items: null },
      { type: 'list', title: '活动要点', text: null, url: null, caption: null, items: ['当季', '次日发货'] },
      { type: 'deliveryNote', title: null, text: '截单后 48 小时内发货', url: null, caption: null, items: null },
    ])
  })

  it('limits content blocks to 20 entries', () => {
    const blocks = Array.from({ length: 24 }, (_, index) => ({ type: 'paragraph', text: `内容 ${index}` }))

    expect(normalizeContentBlocks(blocks).length).toBe(20)
  })
})
