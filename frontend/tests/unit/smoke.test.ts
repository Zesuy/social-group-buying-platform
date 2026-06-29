import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'
import { createPinia } from 'pinia'
import App from '@/App.vue'

// 创建一个带占位路由的 router 实例用于测试
const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      name: 'index',
      component: { template: '<div>首页占位</div>' },
    },
  ],
})

describe('App smoke test', () => {
  it('should mount the app', async () => {
    const wrapper = mount(App, {
      global: {
        plugins: [router, createPinia()],
      },
    })

    // 验证根组件可以挂载
    expect(wrapper.exists()).toBe(true)
    expect(wrapper.findComponent(App).exists()).toBe(true)
  })

  it('should have a router-view', () => {
    const wrapper = mount(App, {
      global: {
        plugins: [router, createPinia()],
      },
    })

    expect(wrapper.findComponent({ name: 'RouterView' }).exists()).toBe(true)
  })
})
