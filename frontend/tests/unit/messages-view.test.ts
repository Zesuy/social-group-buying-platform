import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import MessagesView from '@/views/MessagesView.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/messages', name: 'messages', component: { template: '<div>消息</div>' }, meta: { showTabBar: true } },
  ],
})

describe('MessagesView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should render placeholder messages', () => {
    const wrapper = mount(MessagesView, {
      global: {
        plugins: [router, createPinia()],
      },
    })

    expect(wrapper.text()).toContain('订单待发货提醒')
    expect(wrapper.text()).toContain('物流更新')
  })

  it('should show reminder banner', () => {
    const wrapper = mount(MessagesView, {
      global: {
        plugins: [router, createPinia()],
      },
    })

    expect(wrapper.text()).toContain('不请求消息或推送接口')
  })

  it('should not invoke any API', () => {
    // 通过检查没有外部 API 调用来验证消息页不请求接口
    const fetchSpy = vi.spyOn(globalThis, 'fetch')

    mount(MessagesView, {
      global: {
        plugins: [router, createPinia()],
      },
    })

    // 消息页自身不应发起任何 fetch 请求
    expect(fetchSpy).not.toHaveBeenCalled()
    fetchSpy.mockRestore()
  })
})
