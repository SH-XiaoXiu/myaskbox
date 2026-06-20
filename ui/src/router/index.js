import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import publicRoutes from '@theme/routes'

const routes = [
  {
    path: '/',
    name: 'root',
    component: () => import('@/components/common/RoutePlaceholder.vue'),
  },
  ...publicRoutes.beforeAdmin,
  {
    path: '/admin',
    component: () => import('@/components/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'] },
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'admin-dashboard',
        component: () => import('@/components/admin/AdminDashboardPage.vue'),
      },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('@/components/admin/AdminUserListPage.vue'),
      },
      {
        path: 'manage',
        name: 'admin-manage',
        component: () => import('@/components/admin/AdminManagePage.vue'),
      },
      {
        path: 'boxes',
        name: 'admin-boxes',
        component: () => import('@/components/admin/AdminBoxListPage.vue'),
      },
      {
        path: 'topics',
        name: 'admin-topics',
        component: () => import('@/components/admin/AdminTopicListPage.vue'),
      },
      {
        path: 'questions',
        name: 'admin-questions',
        component: () => import('@/components/admin/AdminQuestionListPage.vue'),
      },
      {
        path: 'attachments',
        name: 'admin-attachments',
        component: () => import('@/components/admin/AdminAttachmentListPage.vue'),
      },
      {
        path: 'settings',
        name: 'admin-settings',
        component: () => import('@/components/admin/AdminSettingsPage.vue'),
      },
      {
        path: 'roles',
        name: 'admin-roles',
        component: () => import('@/components/admin/AdminRoleListPage.vue'),
      },
    ],
  },
  ...publicRoutes.afterAdmin,
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// --- 全局前置守卫 ---
router.beforeEach(async (to, from) => {
  const auth = useAuthStore()

  // 已登录但 user 信息未加载 → 先补加载（必须在权限判断之前）
  if (auth.isLoggedIn && !auth.user) {
    try {
      await auth.ensureUser()
    } catch {
      auth.clearSession()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  if (to.path === '/') {
    return auth.isLoggedIn ? auth.landingPath : '/login'
  }

  // 需要登录但未登录 → 跳转登录
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  const roles = to.meta.roles || []
  if (roles.length && !auth.hasAnyRole(roles)) {
    if (!auth.isLoggedIn) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    return '/forbidden'
  }

  // 已登录用户不要停留在 /login → 跳转对应首页
  if (to.path === '/login' && auth.isLoggedIn) {
    return auth.landingPath
  }
})

export default router
