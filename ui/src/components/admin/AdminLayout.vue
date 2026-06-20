<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useLogoutConfirm } from '@/composables/useLogoutConfirm'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const { confirmLogout } = useLogoutConfirm()
const lastAdminRouteIndex = ref(0)
const adminRouteTransitionName = ref('ubuntu-route-forward')

const tabs = [
  { path: '/admin/dashboard', icon: 'ri-dashboard-3-line', label: '仪表盘' },
  { path: '/admin/users', icon: 'ri-user-settings-line', label: '用户' },
  { path: '/admin/manage', icon: 'ri-apps-2-line', label: '管理' },
  { path: '/admin/settings', icon: 'ri-settings-3-line', label: '设置' },
]

const activeIdx = computed(() => {
  const managePaths = ['/admin/boxes', '/admin/questions', '/admin/topics', '/admin/attachments', '/admin/roles']
  if (managePaths.some((path) => route.path.startsWith(path))) {
    return tabs.findIndex((t) => t.path === '/admin/manage')
  }
  const idx = tabs.findIndex((t) => route.path.startsWith(t.path))
  return idx >= 0 ? idx : 0
})

lastAdminRouteIndex.value = activeIdx.value

watch(
  activeIdx,
  (idx) => {
    adminRouteTransitionName.value = idx < lastAdminRouteIndex.value ? 'ubuntu-route-back' : 'ubuntu-route-forward'
    lastAdminRouteIndex.value = idx
  },
  { flush: 'sync' },
)

function onChange(idx) {
  router.replace(tabs[idx].path)
}

function handleLogout() {
  confirmLogout()
}

function refreshRouteEffects() {
  window.dispatchEvent(new Event('resize'))
}
</script>

<template>
  <div class="admin-layout">
    <!-- 顶部导航 -->
    <van-nav-bar title="管理后台" fixed placeholder safe-area-inset-top>
      <template #left>
        <a class="nav-home-link" href="/" target="_blank">
          <i class="ri-home-3-line"></i>
        </a>
      </template>
      <template #right>
        <div class="nav-actions">
          <span
            class="nav-user"
            :title="auth.user?.email || auth.user?.username || ''"
            :aria-label="`当前账号：${auth.user?.email || auth.user?.username || ''}`"
          >
            <i class="ri-user-3-line"></i>
          </span>
          <button class="nav-logout" type="button" @click="handleLogout">
            <i class="ri-logout-box-r-line"></i>
            <span>退出</span>
          </button>
        </div>
      </template>
    </van-nav-bar>

    <!-- 内容区 -->
    <div class="admin-body">
      <router-view v-slot="{ Component, route: childRoute }">
        <Transition :name="adminRouteTransitionName" mode="out-in" @after-enter="refreshRouteEffects">
          <component :is="Component" :key="childRoute.name || childRoute.path" class="route-page admin-route-page" />
        </Transition>
      </router-view>
    </div>

    <!-- 底部 Tabbar -->
    <van-tabbar
      :model-value="activeIdx"
      fixed
      placeholder
      safe-area-inset-bottom
      @change="onChange"
    >
      <van-tabbar-item
        v-for="(tab, idx) in tabs"
        :key="tab.path"
      >
        <template #icon="{ active: isActive }">
          <i :class="[tab.icon, 'tab-icon', { 'tab-icon--active': isActive }]"></i>
        </template>
        <span>{{ tab.label }}</span>
      </van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<style scoped>
:global(html),
:global(body),
:global(#app) {
  height: 100%;
  overflow: hidden;
}

.admin-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  background: var(--van-background, #f7f8fa);
}

.admin-body {
  flex: 1;
  overflow: hidden;
}

/* NavBar */
.nav-home-link {
  display: flex;
  align-items: center;
  color: var(--van-nav-bar-text-color, #323233);
  text-decoration: none;
  font-size: 20px;
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.nav-user {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  flex: 0 0 auto;
  font-size: 20px;
  color: var(--van-nav-bar-text-color, #323233);
}

.nav-logout {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  min-height: 30px;
  padding: 0 9px;
  border: 0;
  border-radius: 15px;
  flex: 0 0 auto;
  background: #f2f3f5;
  color: #323233;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

/* Tabbar icons */
.tab-icon {
  font-size: 22px;
  line-height: 1;
  color: var(--van-tabbar-item-text-color, #646566);
  transition: color 0.2s;
}

.tab-icon--active {
  color: var(--van-tabbar-item-active-color, #1989fa);
}
</style>
