export default {
  beforeAdmin: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/components/classic/ClassicLoginPage.vue'),
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/components/classic/ClassicRegisterPage.vue'),
    },
    {
      path: '/unauthorized',
      name: 'unauthorized',
      component: () => import('@/components/classic/ClassicUnauthorizedPage.vue'),
    },
    {
      path: '/forbidden',
      name: 'forbidden',
      component: () => import('@/components/classic/ClassicForbiddenPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/home',
      name: 'home',
      component: () => import('@/components/classic/ClassicOwnerPage.vue'),
      meta: { requiresAuth: true, roles: ['BOX_OWNER'] },
    },
    {
      path: '/password',
      name: 'password',
      component: () => import('@/components/classic/ClassicChangePasswordPage.vue'),
      meta: { requiresAuth: true },
    },
  ],
  afterAdmin: [
    {
      path: '/reply/:token',
      name: 'reply-token',
      component: () => import('@/components/classic/ClassicReplyTokenPage.vue'),
    },
    {
      path: '/box/:slug',
      name: 'ask',
      component: () => import('@/components/classic/ClassicAskBoxPage.vue'),
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/components/classic/ClassicNotFoundPage.vue'),
    },
  ],
}
