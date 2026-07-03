import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import LeaderSubscribersView from '@/views/leader/LeaderSubscribersView.vue'
import { listMySubscribers } from '@/api/subscriptions'

vi.mock('@/api/subscriptions', () => ({
  listMySubscribers: vi.fn(),
}))

function createTestRouter() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [
      {
        path: '/leader/subscribers',
        name: 'leaderSubscribers',
        component: LeaderSubscribersView,
      },
    ],
  })
}

describe('LeaderSubscribersView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(listMySubscribers).mockReset()
    vi.mocked(listMySubscribers).mockResolvedValue({
      items: [
        {
          subscriptionId: 's1',
          userId: '100001',
          nickname: '小李',
          avatarUrl: null,
          phone: '13800000001',
          source: 'homepage',
          subscribedAt: '2026-07-03T10:00:00',
        },
        {
          subscriptionId: 's2',
          userId: '100002',
          nickname: '小王',
          avatarUrl: null,
          phone: '13800000002',
          source: 'indexFeed',
          subscribedAt: '2026-07-03T11:00:00',
        },
      ],
      total: 2,
    })
  })

  it('puts the notification subscriber first when subscriptionId query exists', async () => {
    const router = createTestRouter()
    await router.push('/leader/subscribers?subscriptionId=s2')
    await router.isReady()

    const wrapper = mount(LeaderSubscribersView, {
      global: {
        plugins: [router, createPinia()],
      },
    })
    await flushPromises()

    const cards = wrapper.findAll('.subscriber-card')
    expect(cards).toHaveLength(2)
    expect(cards[0].text()).toContain('小王')
    expect(cards[0].text()).toContain('本次新增订阅')
    expect(cards[0].classes()).toContain('subscriber-card--focused')
  })
})
