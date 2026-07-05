type DemoImageKind = 'product' | 'cover' | 'avatar' | 'store'

const PRODUCT_IMAGES = [
  'https://images.unsplash.com/photo-1567306226416-28f0efdc88ce?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1518843875459-f738682238a6?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1601004890684-d8cbf643f5f2?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1518977676601-b53f82aba655?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=800&q=80',
  'https://images.unsplash.com/photo-1547514701-42782101795e?auto=format&fit=crop&w=800&q=80',
]

const NAMED_DEMO_IMAGES: Array<[RegExp, string]> = [
  [/peach|桃|蜜桃/i, PRODUCT_IMAGES[0]],
  [/wogan|沃柑|橘|橙|柑/i, PRODUCT_IMAGES[5]],
]

const DEMO_IMAGES: Record<DemoImageKind, string> = {
  product: PRODUCT_IMAGES[0],
  cover: PRODUCT_IMAGES[4],
  avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=300&q=80',
  store: 'https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=400&q=80',
}

function hashSeed(seed: string): number {
  return Array.from(seed).reduce((total, char) => total + char.charCodeAt(0), 0)
}

export function getDemoProductImage(seed = '', offset = 0): string {
  const index = (hashSeed(seed) + offset) % PRODUCT_IMAGES.length
  return PRODUCT_IMAGES[index]
}

export function getDemoImage(kind: DemoImageKind, seed = ''): string {
  if (kind === 'product') return getDemoProductImage(seed)
  return DEMO_IMAGES[kind]
}

export function isExampleImageUrl(src?: string | null): boolean {
  if (!src) return false

  try {
    return new URL(src).hostname.endsWith('example.com')
  } catch {
    return false
  }
}

export function normalizeLocalUploadUrl(src?: string | null): string | null {
  if (!src) return null

  try {
    const url = new URL(src)
    if ((url.hostname === 'localhost' || url.hostname === '127.0.0.1') && url.pathname.startsWith('/uploads/')) {
      return `${url.pathname}${url.search}${url.hash}`
    }
  } catch {
    return src
  }

  return src
}

function resolveUploadAssetUrl(src: string): string {
  const apiBaseUrl = import.meta.env.MODE === 'android'
    ? import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '')
    : ''

  if (apiBaseUrl && src.startsWith('/uploads/')) {
    return `${apiBaseUrl}${src}`
  }

  return src
}

function isUsableImageUrl(src: string): boolean {
  if (src.startsWith('/') || src.startsWith('data:') || src.startsWith('blob:')) return true

  try {
    const protocol = new URL(src).protocol
    return protocol === 'http:' || protocol === 'https:'
  } catch {
    return false
  }
}

export function resolveDisplayImageUrl(
  src?: string | null,
  seed = '',
  kind: DemoImageKind = 'product',
): string | null {
  const normalizedSrc = normalizeLocalUploadUrl(src)
  if (!normalizedSrc) return null
  if (!isUsableImageUrl(normalizedSrc)) return getDemoImage(kind, seed)
  if (!isExampleImageUrl(normalizedSrc)) return resolveUploadAssetUrl(normalizedSrc)

  const pathname = new URL(normalizedSrc).pathname.toLowerCase()
  const namedImage = NAMED_DEMO_IMAGES.find(([pattern]) => pattern.test(`${pathname} ${seed}`))
  if (namedImage) return namedImage[1]

  if (pathname.includes('avatar')) return getDemoImage('avatar', seed)
  if (pathname.includes('logo')) return getDemoImage('store', seed)
  if (pathname.includes('cover')) return getDemoImage('cover', seed)
  return getDemoImage(kind, seed)
}
