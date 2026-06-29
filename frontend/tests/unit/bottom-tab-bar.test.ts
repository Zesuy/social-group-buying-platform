import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHashHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import BottomTabBar from '@/components/BottomTabBar.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', name: 'index', component: { template: '<div>首页</div>' }, meta: { showTabBar: true } },
    { path: '/orders', name: 'orders', component: { template: '<div>订单</div>' }, meta: { showTabBar: true } },
    { path: '/open-group', name: 'openGroup', component: { template: '<div>一键开团</div>' }, meta: { showTabBar: true } },
    { path: '/messages', name: 'messages', component: { template: '<div>消息</div>' }, meta: { showTabBar: true } },
    { path: '/profile', name: 'profile', component: { template: '<div>我的</div>' }, meta: { showTabBar: true } },
  ],
})

describe('BottomTabBar', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should render 5 tabs', () => {
    const wrapper = mount(BottomTabBar, {
      global: {
        plugins: [router, createPinia()],
      },
    })

    const items = wrapper.findAllComponents({ name: 'VanTabbarItem' })
    expect(items.length).toBe(5)
  })

  it('should have correct tab labels', () => {
    const wrapper = mount(BottomTabBar, {
      global: {
        plugins: [router, createPinia()],
      },
    })

    const labels = wrapper.findAllComponents({ name: 'VanTabbarItem' })
    expect(labels[0].text()).toContain('首页')
    expect(labels[1].text()).toContain('订单')
    expect(labels[2].text()).toContain('一键开团')
    expect(labels[3].text()).toContain('消息')
    expect(labels[4].text()).toContain('我的')
  })
})
