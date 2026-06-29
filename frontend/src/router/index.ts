import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'index',
    component: () => import('@/views/IndexView.vue'),
    meta: { title: '首页', showTabBar: true },
  },
  {
    path: '/orders',
    name: 'orders',
    component: () => import('@/views/OrdersView.vue'),
    meta: { title: '订单', showTabBar: true, requiresAuth: true },
  },
  {
    path: '/open-group',
    name: 'openGroup',
    component: () => import('@/views/OpenGroupView.vue'),
    meta: { title: '一键开团', showTabBar: true, requiresAuth: true, requiresLeader: true },
  },
  {
    path: '/messages',
    name: 'messages',
    component: () => import('@/views/MessagesView.vue'),
    meta: { title: '消息', showTabBar: true },
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('@/views/ProfileView.vue'),
    meta: { title: '我的', showTabBar: true, requiresAuth: true },
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/group-buys/:id',
    name: 'groupBuyDetail',
    component: () => import('@/views/GroupBuyDetailView.vue'),
    meta: { title: '团购详情' },
  },
  {
    path: '/leaders/:id',
    name: 'leaderDetail',
    component: () => import('@/views/LeaderDetailView.vue'),
    meta: { title: '团长主页' },
  },
  {
    path: '/checkout',
    name: 'checkout',
    component: () => import('@/views/CheckoutView.vue'),
    meta: { title: '确认订单', requiresAuth: true },
  },
  {
    path: '/addresses',
    name: 'addresses',
    component: () => import('@/views/AddressesView.vue'),
    meta: { title: '地址管理', requiresAuth: true },
  },
  {
    path: '/store/create',
    name: 'createStore',
    component: () => import('@/views/CreateStoreView.vue'),
    meta: { title: '创建店铺', requiresAuth: true },
  },
  {
    path: '/leader',
    name: 'leaderLayout',
    component: () => import('@/views/leader/LeaderLayout.vue'),
    meta: { title: '团长管理', requiresAuth: true, requiresLeader: true },
    children: [
      {
        path: '',
        redirect: { name: 'leaderDashboard' },
      },
      {
        path: 'dashboard',
        name: 'leaderDashboard',
        component: () => import('@/views/leader/LeaderDashboardView.vue'),
        meta: { title: '团长仪表盘' },
      },
      {
        path: 'orders',
        name: 'leaderOrders',
        component: () => import('@/views/leader/LeaderOrdersView.vue'),
        meta: { title: '团长订单' },
      },
      {
        path: 'products',
        name: 'leaderProducts',
        component: () => import('@/views/leader/LeaderProductsView.vue'),
        meta: { title: '商品管理' },
      },
      {
        path: 'group-buys',
        name: 'leaderGroupBuys',
        component: () => import('@/views/leader/LeaderGroupBuysView.vue'),
        meta: { title: '团购管理' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'notFound',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '404' },
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

// ── 路由守卫：登录拦截 + 团长身份拦截 + 登录后重定向 ──
router.beforeEach((to, _from, next) => {
  // 1. 设置页面标题
  document.title = (to.meta.title as string) || '团购商城'

  const authStore = useAuthStore()

  // 2. requiresAuth：未登录时跳转登录页，保留完整路径作为 redirect
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return next({ name: 'login', query: { redirect: to.fullPath } })
  }

  // 3. requiresLeader：已登录但非团长，跳转到创建店铺页
  if (to.meta.requiresLeader && !authStore.isLeader) {
    return next({ name: 'createStore', query: { redirect: to.fullPath } })
  }

  // 4. 已登录用户访问 /login，重定向到 redirect 查询参数或 /profile
  if (to.name === 'login' && authStore.isLoggedIn) {
    return next({ path: (to.query.redirect as string) || '/profile' })
  }

  next()
})

export default router
