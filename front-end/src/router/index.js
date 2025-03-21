import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
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
      component: () => import('../views/layout/home.vue'),
      children: [
        {
          path: 'products',
          name: 'products',
          component: () => import('../views/products/index.vue'),
          meta: {
            title: '药品列表',
            requiresAuth: false,
          },
        },
        {
          path: 'productsManagement',
          name: 'productsManagementManagement',
          component: () => import('../views/products/list.vue'),
          meta: {
            title: '药品管理',
            requiresAuth: false,
          },
        },
        {
          path: 'purchaseManagement',
          name: 'purchaseManagement',
          component: () => import('../views/purchase/list.vue'),
          meta: {
            title: '采购管理',
            requiresAuth: false,
          },
        },
        {
          path: 'cart',
          name: 'cart',
          component: () => import('../views/cart/index.vue'),
          meta: {
            title: '购物车',
            requiresAuth: false,
          },
        },
        {
          path: 'checkout',
          name: 'checkout',
          component: () => import('../views/order/checkout.vue'),
          meta: {
            title: '订单支付',
            requiresAuth: false,
          },
        },
        {
          path: 'user/profile',
          name: 'userProfile',
          component: () => import('../views/user/profile.vue'),
          meta: {
            title: '个人信息',
            requiresAuth: false,
          },
        },
        {
          path: 'user/address',
          name: 'userAddress',
          component: () => import('../views/user/address.vue'),
          meta: {
            title: '收货地址管理',
          },
        },
        {
          path: 'user/orders',
          name: 'userOrders',
          component: () => import('../views/user/orders.vue'),
          meta: {
            title: '我的订单',
          },
        },
        {
          path: 'user/favorites',
          name: 'userFavorites',
          component: () => import('../views/user/favorites.vue'),
          meta: {
            title: '我的收藏',
          },
        },
        {
          path: 'user/reviews',
          name: 'userReviews',
          component: () => import('../views/user/reviews.vue'),
          meta: {
            title: '我的评价',
          },
        },
        {
          path: 'purchase',
          name: 'purchase',
          component: () => import('../views/purchase/list.vue'),
          meta: {
            title: '采购管理',
          },
        },
        {
          path: 'supplierManagement',
          name: 'supplier',
          component: () => import('../views/supplier/list.vue'),
          meta: {
            title: '供应商管理',
          },
        },
        {
          path: 'storeManagement',
          name: 'store',
          component: () => import('../views/store/list.vue'),
          meta: {
            title: '分店管理',
          },
        },
        {
          path: 'memberManagement',
          name: 'member',
          component: () => import('../views/member/list.vue'),
          meta: {
            title: '会员管理',
          },
        },
        {
          path: 'employeeManagement',
          name: 'employeeManagement',
          component: () => import('../views/employee/list.vue'),
          meta: {
            title: '员工管理',
            requiresAuth: true,
            roles: ['admin']
          }
        },
        {
          path: 'order',
          name: 'order',
          component: () => import('../views/order/list.vue'),
          meta: {
            title: '订单管理',
          },
        },
        {
          path: 'finance',
          name: 'finance',
          component: () => import('../views/finance/dashboard.vue'),
          meta: {
            title: '财务管理',
          },
        },
        {
          path: 'warehouse',
          name: 'warehouse',
          component: () => import('../views/warehouse/list.vue'),
          meta: {
            title: '仓库管理',
          },
        },
      ],
    },
  ],
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('access_token')
  document.title = to.meta.title ? `${to.meta.title} - 药品管理系统` : '药品管理系统'

  if (to.path.startsWith('/home') && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
