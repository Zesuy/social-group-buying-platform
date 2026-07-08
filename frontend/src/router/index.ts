import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'index',
    component: () => import('@/views/IndexView.vue'),
    meta: { title: '首页', showTabBar: true, h5Constrained: true, navigationGroup: 'main', isNavigationRoot: true },
  },
  {
    path: '/orders',
    name: 'orders',
    component: () => import('@/views/OrdersView.vue'),
    meta: { title: '订单', showTabBar: true, requiresAuth: true, navigationGroup: 'main', isNavigationRoot: true },
  },
  {
    path: '/open-group',
    name: 'openGroup',
    component: () => import('@/views/OpenGroupView.vue'),
    meta: { title: '开团', showTabBar: true, h5Constrained: true, navigationGroup: 'main', isNavigationRoot: true },
  },
  {
    path: '/messages',
    name: 'messages',
    component: () => import('@/views/MessagesView.vue'),
    meta: { title: '消息', showTabBar: true, requiresAuth: true, navigationGroup: 'main', isNavigationRoot: true },
  },
  {
    path: '/messages/orders',
    name: 'messageOrders',
    component: () => import('@/views/MessageNotificationCategoryView.vue'),
    meta: { title: '订单消息', requiresAuth: true, backFallback: '/messages', navigationGroup: 'messages' },
  },
  {
    path: '/messages/subscriptions',
    name: 'messageSubscriptions',
    component: () => import('@/views/MessageNotificationCategoryView.vue'),
    meta: { title: '新增订阅', requiresAuth: true, backFallback: '/messages', navigationGroup: 'messages' },
  },
  {
    path: '/chats/:conversationId',
    name: 'chatDetail',
    component: () => import('@/views/ChatDetailView.vue'),
    meta: { title: '联系团长', requiresAuth: true, backFallback: '/messages', navigationGroup: 'messages' },
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('@/views/ProfileView.vue'),
    meta: { title: '我的', showTabBar: true, navigationGroup: 'main', isNavigationRoot: true },
  },
  {
    path: '/profile/me',
    name: 'userProfile',
    component: () => import('@/views/UserProfileView.vue'),
    meta: { title: '我的主页', requiresAuth: true, backFallback: '/profile', navigationGroup: 'profile' },
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录', h5Constrained: true, backFallback: '/profile' },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '注册', h5Constrained: true, backFallback: '/profile' },
  },
  {
    path: '/group-buys/:id',
    name: 'groupBuyDetail',
    component: () => import('@/views/GroupBuyDetailView.vue'),
    meta: { title: '团购详情', h5Constrained: true, resetScrollOnEnter: true, backFallback: '/', navigationGroup: 'publicGroupBuy' },
  },
  {
    path: '/share/group-buys/:shareToken',
    name: 'groupBuyShareDetail',
    component: () => import('@/views/GroupBuyDetailView.vue'),
    meta: { title: '团购分享', h5Constrained: true, resetScrollOnEnter: true, backFallback: '/', navigationGroup: 'publicGroupBuy' },
  },
  {
    path: '/leaders/:id',
    name: 'leaderDetail',
    component: () => import('@/views/LeaderDetailView.vue'),
    meta: { title: '团长主页', h5Constrained: true, backFallback: '/', navigationGroup: 'leaderPublic' },
  },
  {
    path: '/checkout',
    name: 'checkout',
    component: () => import('@/views/CheckoutView.vue'),
    meta: { title: '确认订单', requiresAuth: true, backFallback: '/cart', navigationGroup: 'checkout' },
  },
  {
    path: '/cart',
    name: 'cart',
    component: () => import('@/views/CartView.vue'),
    meta: { title: '购物车', requiresAuth: true, backFallback: '/', navigationGroup: 'cart' },
  },
  {
    path: '/addresses',
    name: 'addresses',
    component: () => import('@/views/AddressesView.vue'),
    meta: { title: '地址管理', requiresAuth: true, backFallback: '/profile', navigationGroup: 'addresses' },
  },
  {
    path: '/addresses/new',
    name: 'addressNew',
    component: () => import('@/views/AddressNewView.vue'),
    meta: { title: '新增地址', requiresAuth: true, backFallback: '/addresses', navigationGroup: 'addresses' },
  },
  {
    path: '/addresses/:id/edit',
    name: 'addressEdit',
    component: () => import('@/views/AddressEditView.vue'),
    meta: { title: '编辑地址', requiresAuth: true, backFallback: '/addresses', navigationGroup: 'addresses' },
  },
  {
    path: '/orders/:id',
    name: 'orderDetail',
    component: () => import('@/views/OrderDetailView.vue'),
    meta: { title: '订单详情', requiresAuth: true, backFallback: '/orders', navigationGroup: 'orders' },
  },
  {
    path: '/store/create',
    name: 'createStore',
    component: () => import('@/views/CreateStoreView.vue'),
    meta: { title: '创建店铺', requiresAuth: true, backFallback: '/profile', navigationGroup: 'storeCreate' },
  },
  {
    path: '/subscriptions',
    name: 'subscriptions',
    component: () => import('@/views/SubscriptionsView.vue'),
    meta: { title: '订阅列表', requiresAuth: true, backFallback: '/profile', navigationGroup: 'subscriptions' },
  },
  {
    path: '/member-cards',
    name: 'memberCards',
    component: () => import('@/views/MemberCardsView.vue'),
    meta: { title: '会员卡', requiresAuth: true, backFallback: '/profile', navigationGroup: 'memberCards' },
  },
  {
    path: '/leader',
    component: () => import('@/views/leader/LeaderLayout.vue'),
    meta: { title: '团长管理', requiresAuth: true, requiresLeader: true, backFallback: '/profile', navigationGroup: 'leader' },
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
        meta: { title: '商家工作台', backFallback: '/profile', navigationGroup: 'leader', isNavigationRoot: true },
      },
      {
        path: 'orders',
        name: 'leaderOrders',
        component: () => import('@/views/leader/LeaderOrdersView.vue'),
        meta: { title: '团长订单', backFallback: '/leader/dashboard', navigationGroup: 'leaderOrders' },
      },
      {
        path: 'subscribers',
        name: 'leaderSubscribers',
        component: () => import('@/views/leader/LeaderSubscribersView.vue'),
        meta: { title: '订阅用户', backFallback: '/leader/dashboard', navigationGroup: 'leaderSubscribers' },
      },
      {
        path: 'coupons',
        name: 'leaderCoupons',
        component: () => import('@/views/leader/LeaderCouponsView.vue'),
        meta: { title: '店铺优惠券', backFallback: '/leader/dashboard', navigationGroup: 'leaderCoupons' },
      },
      {
        path: 'after-sales',
        name: 'leaderAfterSales',
        component: () => import('@/views/leader/LeaderAfterSalesView.vue'),
        meta: { title: '售后管理', backFallback: '/leader/dashboard', navigationGroup: 'leaderAfterSales' },
      },
      {
        path: 'after-sales/:id',
        name: 'leaderAfterSaleDetail',
        component: () => import('@/views/leader/LeaderAfterSaleDetailView.vue'),
        meta: { title: '售后详情', backFallback: '/leader/after-sales', navigationGroup: 'leaderAfterSales' },
      },
      {
        path: 'chats',
        name: 'leaderChats',
        component: () => import('@/views/leader/LeaderChatsView.vue'),
        meta: { title: '客服工作台', backFallback: '/leader/dashboard', navigationGroup: 'leaderChats' },
      },
      {
        path: 'orders/:id',
        name: 'leaderOrderDetail',
        component: () => import('@/views/leader/LeaderOrderDetailView.vue'),
        meta: { title: '订单详情', backFallback: '/leader/orders', navigationGroup: 'leaderOrders' },
      },
      {
        path: 'products',
        name: 'leaderProducts',
        component: () => import('@/views/leader/LeaderProductsView.vue'),
        meta: { title: '商品管理', backFallback: '/leader/dashboard', navigationGroup: 'leaderProducts' },
      },
      {
        path: 'products/new',
        name: 'leaderProductNew',
        component: () => import('@/views/leader/LeaderProductNewView.vue'),
        meta: { title: '新建商品', backFallback: '/leader/products', navigationGroup: 'leaderProducts' },
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
        meta: { title: '编辑商品', backFallback: '/leader/products', navigationGroup: 'leaderProducts' },
      },
      {
        path: 'group-buys/new',
        name: 'leaderGroupBuyNew',
        component: () => import('@/views/leader/PublishGroupBuyView.vue'),
        meta: { title: '发布团购', backFallback: '/leader/group-buys', navigationGroup: 'leaderGroupBuys' },
      },
      {
        path: 'group-buys',
        name: 'leaderGroupBuys',
        component: () => import('@/views/leader/LeaderGroupBuysView.vue'),
        meta: { title: '团购管理', backFallback: '/leader/dashboard', navigationGroup: 'leaderGroupBuys' },
      },
      {
        path: 'group-buys/:id',
        name: 'leaderGroupBuyDetail',
        component: () => import('@/views/leader/LeaderGroupBuyDetailView.vue'),
        meta: { title: '团购详情', backFallback: '/leader/group-buys', navigationGroup: 'leaderGroupBuys' },
      },
      {
        path: 'store',
        name: 'leaderStore',
        component: () => import('@/views/leader/LeaderStoreView.vue'),
        meta: { title: '我的店铺', backFallback: '/leader/dashboard', navigationGroup: 'leaderStore' },
      },
    ],
  },
  {
    path: '/merchant',
    component: () => import('@/views/merchant/MerchantLayout.vue'),
    meta: { title: '商家管理端', requiresAuth: true, requiresLeader: true, backFallback: '/profile', navigationGroup: 'merchant' },
    children: [
      {
        path: '',
        name: 'merchantIndex',
        redirect: { name: 'merchantDashboard' },
      },
      {
        path: 'dashboard',
        name: 'merchantDashboard',
        component: () => import('@/views/merchant/MerchantDashboardView.vue'),
        meta: { title: '商家管理端', backFallback: '/profile', navigationGroup: 'merchantDashboard', isNavigationRoot: true },
      },
      {
        path: 'orders',
        name: 'merchantOrders',
        component: () => import('@/views/merchant/MerchantOrdersView.vue'),
        meta: { title: '商家订单', backFallback: '/merchant/dashboard', navigationGroup: 'merchantOrders' },
      },
      {
        path: 'orders/:id',
        name: 'merchantOrderDetail',
        component: () => import('@/views/merchant/MerchantOrderDetailView.vue'),
        meta: { title: '订单详情', backFallback: '/merchant/orders', navigationGroup: 'merchantOrders' },
      },
      {
        path: 'after-sales',
        name: 'merchantAfterSales',
        component: () => import('@/views/merchant/MerchantAfterSalesView.vue'),
        meta: { title: '商家售后', backFallback: '/merchant/dashboard', navigationGroup: 'merchantAfterSales' },
      },
      {
        path: 'after-sales/:id',
        name: 'merchantAfterSaleDetail',
        component: () => import('@/views/merchant/MerchantAfterSaleDetailView.vue'),
        meta: { title: '售后详情', backFallback: '/merchant/after-sales', navigationGroup: 'merchantAfterSales' },
      },
      {
        path: 'chats',
        name: 'merchantChats',
        component: () => import('@/views/merchant/MerchantChatsView.vue'),
        meta: { title: '商家客服', backFallback: '/merchant/dashboard', navigationGroup: 'merchantChats' },
      },
      {
        path: 'chats/:conversationId',
        name: 'merchantChatDetail',
        component: () => import('@/views/merchant/MerchantChatsView.vue'),
        meta: { title: '商家客服', backFallback: '/merchant/chats', navigationGroup: 'merchantChats' },
      },
      {
        path: 'products',
        name: 'merchantProducts',
        component: () => import('@/views/merchant/MerchantProductsView.vue'),
        meta: { title: '商品管理', backFallback: '/merchant/dashboard', navigationGroup: 'merchantProducts' },
      },
      {
        path: 'products/new',
        name: 'merchantProductNew',
        component: () => import('@/views/merchant/MerchantProductFormView.vue'),
        meta: { title: '新建商品', backFallback: '/merchant/products', navigationGroup: 'merchantProducts' },
      },
      {
        path: 'products/:id/edit',
        name: 'merchantProductEdit',
        component: () => import('@/views/merchant/MerchantProductFormView.vue'),
        meta: { title: '编辑商品', backFallback: '/merchant/products', navigationGroup: 'merchantProducts' },
      },
      {
        path: 'group-buys',
        name: 'merchantGroupBuys',
        component: () => import('@/views/merchant/MerchantGroupBuysView.vue'),
        meta: { title: '商家团购', backFallback: '/merchant/dashboard', navigationGroup: 'merchantGroupBuys' },
      },
      {
        path: 'group-buys/new',
        name: 'merchantGroupBuyNew',
        component: () => import('@/views/merchant/MerchantGroupBuyFormView.vue'),
        meta: { title: '新建团购', backFallback: '/merchant/group-buys', navigationGroup: 'merchantGroupBuys' },
      },
      {
        path: 'group-buys/:id',
        name: 'merchantGroupBuyDetail',
        component: () => import('@/views/merchant/MerchantGroupBuyDetailView.vue'),
        meta: { title: '团购详情', backFallback: '/merchant/group-buys', navigationGroup: 'merchantGroupBuys' },
      },
      {
        path: 'coupons',
        name: 'merchantCoupons',
        component: () => import('@/views/merchant/MerchantCouponsView.vue'),
        meta: { title: '优惠券管理', backFallback: '/merchant/dashboard', navigationGroup: 'merchantCoupons' },
      },
      {
        path: 'subscribers',
        name: 'merchantSubscribers',
        component: () => import('@/views/merchant/MerchantSubscribersView.vue'),
        meta: { title: '订阅用户', backFallback: '/merchant/dashboard', navigationGroup: 'merchantSubscribers' },
      },
      {
        path: 'store',
        name: 'merchantStore',
        component: () => import('@/views/merchant/MerchantStoreView.vue'),
        meta: { title: '店铺资料', backFallback: '/merchant/dashboard', navigationGroup: 'merchantStore' },
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

  // 4. 已登录用户访问认证页，重定向到 redirect 查询参数或 /profile
  if ((to.name === 'login' || to.name === 'register') && authStore.isLoggedIn) {
    return next({ path: (to.query.redirect as string) || '/profile' })
  }

  next()
})

export default router
