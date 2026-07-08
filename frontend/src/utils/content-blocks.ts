import type { ContentBlockData } from '@/types'

const MAX_CONTENT_BLOCKS = 20

export function contentBlockTypeText(type: string): string {
  const map: Record<string, string> = {
    paragraph: '文字说明',
    section: '小标题',
    image: '图片',
    list: '要点列表',
    deliveryNote: '发货说明',
  }
  return map[type] || '内容块'
}

export function createContentBlock(type: ContentBlockData['type']): ContentBlockData {
  if (type === 'section') return { type, title: '', text: '', url: null, caption: null, items: null }
  if (type === 'image') return { type, title: null, text: null, url: '', caption: '', items: null }
  if (type === 'list') return { type, title: '推荐理由', text: null, url: null, caption: null, items: [''] }
  if (type === 'deliveryNote') return { type, title: null, text: '', url: null, caption: null, items: null }
  return { type: 'paragraph', title: null, text: '', url: null, caption: null, items: null }
}

export function normalizeContentBlocks(blocks: ContentBlockData[] | undefined): ContentBlockData[] {
  return (blocks || [])
    .slice(0, MAX_CONTENT_BLOCKS)
    .map(normalizeContentBlock)
    .filter((block): block is ContentBlockData => block !== null)
}

function normalizeContentBlock(block: ContentBlockData): ContentBlockData | null {
  if (block.type === 'section') {
    const title = clean(block.title)
    const text = clean(block.text)
    if (!title && !text) return null
    return { type: 'section', title, text, url: null, caption: null, items: null }
  }
  if (block.type === 'image') {
    const url = clean(block.url)
    if (!url) return null
    return { type: 'image', title: null, text: null, url, caption: clean(block.caption), items: null }
  }
  if (block.type === 'list') {
    const items = (block.items || []).map((item) => item.trim()).filter(Boolean)
    if (items.length === 0) return null
    return { type: 'list', title: clean(block.title) || '活动要点', text: null, url: null, caption: null, items }
  }
  if (block.type === 'deliveryNote') {
    const text = clean(block.text)
    if (!text) return null
    return { type: 'deliveryNote', title: null, text, url: null, caption: null, items: null }
  }
  const text = clean(block.text)
  if (!text) return null
  return { type: 'paragraph', title: null, text, url: null, caption: null, items: null }
}

function clean(value?: string | null): string | null {
  const text = value?.trim() || ''
  return text || null
}
