<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const tabs = [
  { path: '/admin/dashboard', icon: 'ri-dashboard-3-line', label: '仪表盘' },
  { path: '/admin/users', icon: 'ri-user-settings-line', label: '用户' },
  { path: '/admin/boxes', icon: 'ri-mail-send-line', label: '提问箱' },
  { path: '/admin/questions', icon: 'ri-question-answer-line', label: '问题' },
  { path: '/admin/avatars', icon: 'ri-emotion-line', label: '头像' },
  { path: '/admin/roles', icon: 'ri-shield-keyhole-line', label: '权限' },
]

const activeIdx = computed(() => {
  const idx = tabs.findIndex((t) => route.path.startsWith(t.path))
  return idx >= 0 ? idx : 0
})

function onChange(idx) {
  router.replace(tabs[idx].path)
}

async function handleLogout() {
  await auth.logout()
  router.replace('/login')
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
        <span class="nav-user" @click="handleLogout">
          <i class="ri-user-3-line"></i>
          <span class="nav-username">{{ auth.user?.username || '' }}</span>
        </span>
      </template>
    </van-nav-bar>

    <!-- 内容区 -->
    <div class="admin-body">
      <router-view />
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

.nav-user {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 20px;
  color: var(--van-nav-bar-text-color, #323233);
  cursor: pointer;
}

.nav-username {
  font-size: 14px;
  font-weight: 520;
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
