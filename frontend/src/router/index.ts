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
    meta: { title: '开团', showTabBar: true },
  },
  {
    path: '/messages',
    name: 'messages',
    component: () => import('@/views/MessagesView.vue'),
    meta: { title: '消息', showTabBar: true, requiresAuth: true },
  },
  {
    path: '/chats/:conversationId',
    name: 'chatDetail',
    component: () => import('@/views/ChatDetailView.vue'),
    meta: { title: '联系团长', requiresAuth: true },
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('@/views/ProfileView.vue'),
    meta: { title: '我的', showTabBar: true },
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
    path: '/cart',
    name: 'cart',
    component: () => import('@/views/CartView.vue'),
    meta: { title: '购物车', requiresAuth: true },
  },
  {
    path: '/addresses',
    name: 'addresses',
    component: () => import('@/views/AddressesView.vue'),
    meta: { title: '地址管理', requiresAuth: true },
  },
  {
    path: '/addresses/new',
    name: 'addressNew',
    component: () => import('@/views/AddressNewView.vue'),
    meta: { title: '新增地址', requiresAuth: true },
  },
  {
    path: '/addresses/:id/edit',
    name: 'addressEdit',
    component: () => import('@/views/AddressEditView.vue'),
    meta: { title: '编辑地址', requiresAuth: true },
  },
  {
    path: '/orders/:id',
    name: 'orderDetail',
    component: () => import('@/views/OrderDetailView.vue'),
    meta: { title: '订单详情', requiresAuth: true },
  },
  {
    path: '/store/create',
    name: 'createStore',
    component: () => import('@/views/CreateStoreView.vue'),
    meta: { title: '创建店铺', requiresAuth: true },
  },
  {
    path: '/subscriptions',
    name: 'subscriptions',
    component: () => import('@/views/SubscriptionsView.vue'),
    meta: { title: '订阅列表', requiresAuth: true },
  },
  {
    path: '/member-cards',
    name: 'memberCards',
    component: () => import('@/views/MemberCardsView.vue'),
    meta: { title: '会员卡', requiresAuth: true },
  },
  {
    path: '/leader',
    component: () => import('@/views/leader/LeaderLayout.vue'),
    meta: { title: '团长管理', requiresAuth: true, requiresLeader: true },
    children: [
      {
        path: '',
        name: 'leaderIndex',
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
        path: 'subscribers',
        name: 'leaderSubscribers',
        component: () => import('@/views/leader/LeaderSubscribersView.vue'),
        meta: { title: '订阅用户' },
      },
      {
        path: 'orders/:id',
        name: 'leaderOrderDetail',
        component: () => import('@/views/leader/LeaderOrderDetailView.vue'),
        meta: { title: '订单详情' },
      },
      {
        path: 'products',
        name: 'leaderProducts',
        component: () => import('@/views/leader/LeaderProductsView.vue'),
        meta: { title: '商品管理' },
      },
      {
        path: 'products/new',
        name: 'leaderProductNew',
        component: () => import('@/views/leader/LeaderProductNewView.vue'),
        meta: { title: '新建商品' },
      },
      {
        path: 'products/:id',
        name: 'leaderProductDetailRedirect',
        redirect: (to) => ({
          name: 'leaderProductEdit',
          params: { id: to.params.id },
        }),
      },
      {
        path: 'products/:id/edit',
        name: 'leaderProductEdit',
        component: () => import('@/views/leader/LeaderProductEditView.vue'),
        meta: { title: '编辑商品' },
      },
      {
        path: 'group-buys/new',
        name: 'leaderGroupBuyNew',
        component: () => import('@/views/leader/PublishGroupBuyView.vue'),
        meta: { title: '发布团购' },
      },
      {
        path: 'group-buys',
        name: 'leaderGroupBuys',
        component: () => import('@/views/leader/LeaderGroupBuysView.vue'),
        meta: { title: '团购管理' },
      },
      {
        path: 'group-buys/:id',
        name: 'leaderGroupBuyDetail',
        component: () => import('@/views/leader/LeaderGroupBuyDetailView.vue'),
        meta: { title: '团购详情' },
      },
      {
        path: 'store',
        name: 'leaderStore',
        component: () => import('@/views/leader/LeaderStoreView.vue'),
        meta: { title: '我的店铺' },
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
