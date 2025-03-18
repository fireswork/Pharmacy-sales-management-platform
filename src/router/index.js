import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/home',
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/login.vue'),
      meta: {
        title: '登录',
        requiresAuth: false,
      },
    },
    {
      path: '/home',
      name: 'home',
      component: () => import('../views/layout/home.vue'),
      meta: {
        title: '首页',
        requiresAuth: false,
      },
    },
    {
      path: '/user',
      name: 'user',
      component: () => import('../views/layout/home.vue'),
      meta: {
        requiresAuth: true,
      },
      // children: [
      //   {
      //     path: 'profile',
      //     name: 'userProfile',
      //     component: () => import('../views/user/profile.vue'),
      //     meta: {
      //       title: '个人中心',
      //     },
      //   },
      //   {
      //     path: 'address',
      //     name: 'userAddress',
      //     component: () => import('../views/user/address.vue'),
      //     meta: {
      //       title: '收货地址',
      //     },
      //   },
      //   {
      //     path: 'favorites',
      //     name: 'userFavorites',
      //     component: () => import('../views/user/favorites.vue'),
      //     meta: {
      //       title: '我的收藏',
      //     },
      //   },
      //   {
      //     path: 'orders',
      //     name: 'userOrders',
      //     component: () => import('../views/user/orders.vue'),
      //     meta: {
      //       title: '我的订单',
      //     },
      //   },
      //   {
      //     path: 'reviews',
      //     name: 'userReviews',
      //     component: () => import('../views/user/reviews.vue'),
      //     meta: {
      //       title: '我的评价',
      //     },
      //   },
      // ],
    },
    // {
    //   path: '/products',
    //   name: 'products',
    //   component: () => import('../views/products/index.vue'),
    //   meta: {
    //     title: '药品列表',
    //     requiresAuth: false,
    //   },
    // },
    // {
    //   path: '/products/:id',
    //   name: 'productDetail',
    //   component: () => import('../views/products/detail.vue'),
    //   meta: {
    //     title: '药品详情',
    //     requiresAuth: false,
    //   },
    // },
    // {
    //   path: '/cart',
    //   name: 'cart',
    //   component: () => import('../views/cart/index.vue'),
    //   meta: {
    //     title: '购物车',
    //     requiresAuth: true,
    //   },
    // },
  ],
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const isAuthenticated = localStorage.getItem('token')
  document.title = to.meta.title ? `${to.meta.title} - 药品管理系统` : '药品管理系统'

  if (to.meta.requiresAuth && !isAuthenticated) {
    next({ name: 'login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
