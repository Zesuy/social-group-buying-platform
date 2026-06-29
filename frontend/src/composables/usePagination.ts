/**
 * 通用分页组合式
 *
 * 管理分页加载、下拉刷新、触底加载和错误重试。
 *
 * @example
 * ```ts
 * const { items, loading, hasMore, load, refresh, loadMore, retry } = usePagination(
 *   (page, pageSize) => listPublicGroupBuys(page, pageSize)
 * )
 * await load() // 首次加载
 * ```
 */

import { ref, computed } from 'vue'
import type { PageResponse } from '@/types'
import { DEFAULT_PAGE_SIZE } from '@/constants'

type FetchFn<T> = (page: number, pageSize: number) => Promise<PageResponse<T>>

export function usePagination<T>(fetchFn: FetchFn<T>, pageSize = DEFAULT_PAGE_SIZE) {
  const items = ref<T[]>([])
  const page = ref(1)
  const loading = ref(false)
  const refreshing = ref(false)
  const error = ref<string | null>(null)
  const hasMore = ref(true)
  const total = ref(0)
  const initialized = ref(false)

  const isEmpty = computed(() => initialized.value && !loading.value && !error.value && items.value.length === 0)

  /**
   * 首次加载
   */
  async function load(): Promise<void> {
    if (loading.value) return
    loading.value = true
    error.value = null
    page.value = 1

    try {
      const data = await fetchFn(1, pageSize)
      items.value = data.items
      total.value = data.total
      hasMore.value = data.hasMore
      initialized.value = true
    } catch (err) {
      const apiErr = err as { message?: string }
      error.value = apiErr.message || '加载失败'
    } finally {
      loading.value = false
    }
  }

  /**
   * 下拉刷新（重置到第一页）
   */
  async function refresh(): Promise<void> {
    if (refreshing.value) return
    refreshing.value = true
    error.value = null
    page.value = 1

    try {
      const data = await fetchFn(1, pageSize)
      items.value = data.items
      total.value = data.total
      hasMore.value = data.hasMore
      initialized.value = true
    } catch (err) {
      const apiErr = err as { message?: string }
      error.value = apiErr.message || '刷新失败'
    } finally {
      refreshing.value = false
    }
  }

  /**
   * 触底加载更多
   */
  async function loadMore(): Promise<void> {
    if (loading.value || !hasMore.value) return
    loading.value = true
    error.value = null

    const nextPage = page.value + 1

    try {
      const data = await fetchFn(nextPage, pageSize)
      items.value = [...items.value, ...data.items] as T[]
      total.value = data.total
      hasMore.value = data.hasMore
      page.value = nextPage
    } catch (err) {
      const apiErr = err as { message?: string }
      error.value = apiErr.message || '加载更多失败'
    } finally {
      loading.value = false
    }
  }

  /**
   * 错误重试（加载和加载更多都用 load）
   */
  async function retry(): Promise<void> {
    if (initialized.value && items.value.length > 0) {
      // 已加载过数据但加载更多失败，重试加载更多
      return loadMore()
    }
    return load()
  }

  /**
   * 重置状态
   */
  function reset(): void {
    items.value = []
    page.value = 1
    loading.value = false
    refreshing.value = false
    error.value = null
    hasMore.value = true
    total.value = 0
    initialized.value = false
  }

  return {
    items,
    loading,
    refreshing,
    error,
    hasMore,
    total,
    isEmpty,
    initialized,
    load,
    page,
    refresh,
    loadMore,
    retry,
    reset,
  }
}
