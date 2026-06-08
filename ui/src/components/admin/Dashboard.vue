<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboardStats, listUsers } from '../../api/admin'
import { formatTime } from '../../utils'

const router = useRouter()

const loading = ref(true)
const error = ref('')

const stats = ref([
  { label: '用户总数', value: 0, icon: 'ri-user-3-line', color: '#6366f1' },
  { label: '提问箱', value: 0, icon: 'ri-mail-send-line', color: '#10b981' },
  { label: '问题总数', value: 0, icon: 'ri-question-answer-line', color: '#f59e0b' },
  { label: '附件数量', value: 0, icon: 'ri-attachment-2', color: '#ec4899' },
])

const recentUsers = ref([])

onMounted(async () => {
  try {
    const [data, usersPage] = await Promise.all([
      getDashboardStats(),
      listUsers(1, 5),
    ])
    stats.value[0].value = data.userCount
    stats.value[1].value = data.boxCount
    stats.value[2].value = data.questionCount
    stats.value[3].value = data.attachmentCount
    recentUsers.value = usersPage.records
  } catch (err) {
    error.value = err.message || '加载失败'
  } finally {
    loading.value = false
  }
})

function statusTag(status) {
  return status === 'ACTIVE' ? 'success' : 'danger'
}

function statusText(status) {
  return status === 'ACTIVE' ? '启用' : '禁用'
}
</script>

<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <van-grid :column-num="2" :gutter="12" :border="false">
      <van-grid-item v-for="s in stats" :key="s.label">
        <div class="stat-card">
          <div class="stat-icon" :style="{ color: s.color, background: s.color + '18' }">
            <i :class="s.icon"></i>
          </div>
          <div class="stat-body">
            <span class="stat-value">{{ s.value.toLocaleString() }}</span>
            <span class="stat-label">{{ s.label }}</span>
          </div>
        </div>
      </van-grid-item>
    </van-grid>

    <!-- 最近用户 -->
    <div class="section">
      <div class="section-header">
        <h3>最近注册用户</h3>
        <span class="section-link" @click="router.push('/admin/users')">
          全部 <i class="ri-arrow-right-s-line"></i>
        </span>
      </div>

      <van-loading v-if="loading" class="loading-center" size="24" />
      <p v-else-if="error" class="error-msg">{{ error }}</p>
      <van-cell-group v-else inset>
        <van-cell
          v-for="user in recentUsers"
          :key="user.id"
          :title="user.displayName || user.username"
          :label="`@${user.username} · ${formatTime(new Date(user.createdAt).getTime())}`"
        >
          <template #value>
            <van-tag :type="statusTag(user.status)" size="medium">
              {{ statusText(user.status) }}
            </van-tag>
          </template>
        </van-cell>
      </van-cell-group>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  height: 100%;
  overflow-y: auto;
  padding: 16px 16px 8px;
}

/* 统计卡片 */
.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 4px;
  width: 100%;
}

.stat-icon {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  font-size: 20px;
  flex-shrink: 0;
}

.stat-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #323233;
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: #969799;
}

/* 区域 */
.section {
  margin-top: 12px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  margin-bottom: 8px;
}

.section-header h3 {
  font-size: 16px;
  font-weight: 650;
  color: #323233;
  margin: 0;
}

.section-link {
  display: inline-flex;
  align-items: center;
  font-size: 13px;
  color: #1989fa;
  cursor: pointer;
}

.section-link i {
  font-size: 16px;
}

.loading-center {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}

.error-msg {
  text-align: center;
  color: #ee0a24;
  padding: 24px 0;
  font-size: 14px;
}
</style>
