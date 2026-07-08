import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createRouter, createWebHashHistory } from 'vue-router'
import { defineComponent } from 'vue'
import PageLayout from '@/components/PageLayout.vue'
import { useSmartNavigation } from '@/composables'

function createTestRouter() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [
      { path: '/', component: { template: '<div>home</div>' } },
      { path: '/fallback', component: { template: '<div>fallback</div>' } },
      { path: '/current', component: { template: '<div>current</div>' }, meta: { backFallback: '/fallback' } },
    ],
  })
}

describe('smart navigation', () => {
  beforeEach(() => {
    window.history.replaceState({ position: 0 }, '', '#/current')
  })

  it('replaces with fallback when there is no browser back target', async () => {
    const router = createTestRouter()
    const replace = vi.spyOn(router, 'replace')
    const TestHost = defineComponent({
      setup() {
        const { goBack } = useSmartNavigation('/fallback')
        return { goBack }
      },
      template: '<button type="button" @click="goBack()">back</button>',
    })

    await router.push('/current')
    await router.isReady()
    const wrapper = mount(TestHost, { global: { plugins: [router] } })
    await wrapper.get('button').trigger('click')

    expect(replace).toHaveBeenCalledWith('/fallback')
  })

  it('uses router.back when browser history has an app back target', async () => {
    const router = createTestRouter()
    const back = vi.spyOn(router, 'back').mockImplementation(() => {})
    const replace = vi.spyOn(router, 'replace')
    const TestHost = defineComponent({
      setup() {
        const { goBack } = useSmartNavigation('/fallback')
        return { goBack }
      },
      template: '<button type="button" @click="goBack()">back</button>',
    })

    await router.push('/current')
    await router.isReady()
    window.history.replaceState({ back: '/', position: 2 }, '', '#/current')
    const wrapper = mount(TestHost, { global: { plugins: [router] } })
    await wrapper.get('button').trigger('click')

    expect(back).toHaveBeenCalled()
    expect(replace).not.toHaveBeenCalled()
  })

  it('PageLayout handles back with its fallback by default', async () => {
    const router = createTestRouter()
    const replace = vi.spyOn(router, 'replace')
    await router.push('/current')
    await router.isReady()

    const wrapper = mount(PageLayout, {
      props: { title: '详情', showBack: true, backFallback: '/fallback' },
      global: { plugins: [router] },
      slots: { default: '<div>content</div>' },
    })
    await wrapper.get('button[aria-label="返回"]').trigger('click')

    expect(replace).toHaveBeenCalledWith('/fallback')
  })
})
