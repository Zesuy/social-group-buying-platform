import { describe, it, expect } from 'vitest'
import { usePagination } from '@/composables/usePagination'
import type { PageResponse } from '@/types'

function makeFetcher(results: Array<{ items: number[]; hasMore: boolean }>) {
  return async (page: number): Promise<PageResponse<number>> => {
    const r = results[page - 1] || { items: [], hasMore: false }
    return { items: r.items, page, pageSize: 20, total: r.items.length, hasMore: r.hasMore }
  }
}

function makeFailingFetcher(): (page: number, pageSize: number) => Promise<PageResponse<number>> {
  return async () => { throw new Error('Network error') }
}

describe('usePagination', () => {
  it('initial state is empty and not initialized', () => {
    const pagination = usePagination<number>(makeFetcher([]))
    expect(pagination.items.value).toEqual([])
    expect(pagination.loading.value).toBe(false)
    expect(pagination.hasMore.value).toBe(true)
    expect(pagination.initialized.value).toBe(false)
    expect(pagination.isEmpty.value).toBe(false)
  })

  it('load fetches first page', async () => {
    const pagination = usePagination(makeFetcher([
      { items: [1, 2, 3], hasMore: true },
    ]))
    await pagination.load()
    expect(pagination.items.value).toEqual([1, 2, 3])
    expect(pagination.loading.value).toBe(false)
    expect(pagination.hasMore.value).toBe(true)
    expect(pagination.initialized.value).toBe(true)
  })

  it('loadMore appends items', async () => {
    const pagination = usePagination(makeFetcher([
      { items: [1, 2], hasMore: true },
      { items: [3, 4], hasMore: false },
    ]))
    await pagination.load()
    expect(pagination.items.value).toEqual([1, 2])
    await pagination.loadMore()
    expect(pagination.items.value).toEqual([1, 2, 3, 4])
    expect(pagination.hasMore.value).toBe(false)
  })

  it('refresh resets to first page', async () => {
    let callIndex = 0
    const fetcher = async (_page: number): Promise<PageResponse<number>> => {
      callIndex++
      if (callIndex === 1) return { items: [1, 2], page: 1, pageSize: 20, total: 2, hasMore: false }
      // refresh
      return { items: [5, 6], page: 1, pageSize: 20, total: 2, hasMore: false }
    }

    const pagination = usePagination(fetcher, 20)
    await pagination.load()
    expect(pagination.items.value).toEqual([1, 2])
    expect(pagination.hasMore.value).toBe(false)
    // Call refresh
    await pagination.refresh()
    expect(pagination.items.value).toEqual([5, 6])
    expect(pagination.page.value).toBe(1)
  })

  it('error sets error message and initialized remains false on first load failure', async () => {
    const pagination = usePagination(makeFailingFetcher())
    await pagination.load()
    expect(pagination.error.value).toBe('Network error')
    expect(pagination.initialized.value).toBe(false)
    expect(pagination.items.value).toEqual([])
  })

  it('retry calls load when no items', async () => {
    let fail = true
    const fetcher = async (_page: number): Promise<PageResponse<number>> => {
      if (fail) { fail = false; throw new Error('Error') }
      return { items: [1], page: 1, pageSize: 20, total: 1, hasMore: false }
    }
    const pagination = usePagination(fetcher, 20)
    await pagination.load()
    expect(pagination.error.value).toBe('Error')
    await pagination.retry()
    expect(pagination.items.value).toEqual([1])
  })

  it('isEmpty is true when initialized with empty data', async () => {
    const pagination = usePagination(makeFetcher([{ items: [], hasMore: false }]))
    expect(pagination.isEmpty.value).toBe(false) // not initialized
    await pagination.load()
    expect(pagination.isEmpty.value).toBe(true)
  })

  it('reset clears all state', async () => {
    const pagination = usePagination(makeFetcher([{ items: [1, 2, 3], hasMore: true }]))
    await pagination.load()
    expect(pagination.initialized.value).toBe(true)
    expect(pagination.page.value).toBe(1)
    pagination.reset()
    expect(pagination.items.value).toEqual([])
    expect(pagination.page.value).toBe(1)
    expect(pagination.initialized.value).toBe(false)
    expect(pagination.hasMore.value).toBe(true)
  })

  it('retry calls loadMore when items exist and error occurs on loadMore', async () => {
    let loadMoreFail = false
    const fetcher = async (page: number): Promise<PageResponse<number>> => {
      if (page === 2 && loadMoreFail) throw new Error('LoadMore error')
      return {
        items: page === 1 ? [1] : [2],
        page,
        pageSize: 20,
        total: 2,
        hasMore: page === 1, // Only page 1 has more
      }
    }
    const pagination = usePagination(fetcher, 20)
    await pagination.load()
    expect(pagination.items.value).toEqual([1])
    expect(pagination.hasMore.value).toBe(true)

    // Trigger loadMore error
    loadMoreFail = true
    await pagination.loadMore()
    expect(pagination.error.value).toBe('LoadMore error')

    // Fix and retry
    loadMoreFail = false
    await pagination.retry()
    expect(pagination.items.value).toEqual([1, 2])
  })
})
