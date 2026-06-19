export default {
  beforeAdmin: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/components/liquid/LiquidLoginPage.vue'),
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/components/liquid/LiquidRegisterPage.vue'),
    },
    {
      path: '/unauthorized',
      name: 'unauthorized',
      component: () => import('@/components/liquid/LiquidUnauthorizedPage.vue'),
    },
    {
      path: '/forbidden',
      name: 'forbidden',
      component: () => import('@/components/liquid/LiquidForbiddenPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/home',
      name: 'home',
      component: () => import('@/components/liquid/LiquidOwnerPage.vue'),
      meta: { requiresAuth: true, roles: ['BOX_OWNER'] },
    },
    {
      path: '/password',
      name: 'password',
      component: () => import('@/components/liquid/LiquidChangePasswordPage.vue'),
      meta: { requiresAuth: true },
    },
  ],
  afterAdmin: [
    {
      path: '/box/:slug',
      name: 'ask',
      component: () => import('@/components/liquid/LiquidAskBoxPage.vue'),
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/components/liquid/LiquidNotFoundPage.vue'),
    },
  ],
}
