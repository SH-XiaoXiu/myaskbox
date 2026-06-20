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
      redirect: '/home/inbox',
      meta: { requiresAuth: true, roles: ['BOX_OWNER'] },
      children: [
        {
          path: 'public',
          name: 'home-public',
          component: () => import('@/components/common/RoutePlaceholder.vue'),
        },
        {
          path: 'inbox',
          name: 'home-inbox',
          component: () => import('@/components/common/RoutePlaceholder.vue'),
        },
        {
          path: 'mine',
          name: 'home-mine',
          component: () => import('@/components/common/RoutePlaceholder.vue'),
        },
      ],
    },
    {
      path: '/password',
      name: 'password',
      component: () => import('@/components/classic/ClassicChangePasswordPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/topics',
      name: 'topics',
      component: () => import('@/components/classic/ClassicTopicPage.vue'),
      meta: { requiresAuth: true, roles: ['BOX_OWNER'] },
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
