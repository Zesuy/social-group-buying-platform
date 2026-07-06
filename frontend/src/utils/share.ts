import { shareByNative } from './native'

const PUBLIC_SHARE_ORIGIN = 'https://shop.zesuy.top'

export type SystemShareResult = 'shared' | 'unsupported' | 'aborted' | 'failed'

export function buildHashRouteUrl(path: string): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  const base = import.meta.env.BASE_URL === '/' ? '/' : import.meta.env.BASE_URL
  const normalizedBase = base.endsWith('/') ? base : `${base}/`
  return `${PUBLIC_SHARE_ORIGIN}${normalizedBase}#${normalizedPath}`
}

export function buildGroupBuyShareUrl(groupBuyId: string): string {
  return buildHashRouteUrl(`/group-buys/${groupBuyId}`)
}

export function buildShareTokenUrl(shareToken: string): string {
  return buildHashRouteUrl(`/share/group-buys/${shareToken}`)
}

export async function shareBySystem(data: ShareData): Promise<SystemShareResult> {
  const nativeResult = await shareByNative(data)
  if (nativeResult) return nativeResult

  if (typeof navigator === 'undefined' || typeof navigator.share !== 'function') {
    return 'unsupported'
  }

  try {
    await navigator.share(data)
    return 'shared'
  } catch (err) {
    if ((err as { name?: string }).name === 'AbortError') return 'aborted'
    return 'failed'
  }
}

export async function copyTextToClipboard(text: string): Promise<void> {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
    return
  }
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', 'true')
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  textarea.select()
  document.execCommand('copy')
  document.body.removeChild(textarea)
}
