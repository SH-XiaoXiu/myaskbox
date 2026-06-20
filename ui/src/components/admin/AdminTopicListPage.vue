<script setup>
import { onMounted, ref } from 'vue'
import { showDialog, showSuccessToast, showToast } from 'vant'
import { closeAdminTopic, listTopics } from '@/api/admin'
import { formatTime } from '@/utils'

const loading = ref(true)
const error = ref('')
const topics = ref([])
const keyword = ref('')
const status = ref('')
const currentPage = ref(1)
const hasMore = ref(false)
const loadingMore = ref(false)
const pageSize = 20

const statusOptions = [
  { text: '全部状态', value: '' },
  { text: '进行中', value: 'ACTIVE' },
  { text: '已到期', value: 'EXPIRED' },
  { text: '已关闭', value: 'CLOSED' },
]

async function fetchPage(page = 1, append = false) {
  const result = await listTopics(page, pageSize, {
    keyword: keyword.value || undefined,
    status: status.value || undefined,
  })
  topics.value = append ? [...topics.value, ...result.records] : result.records
  currentPage.value = result.page
  hasMore.value = result.page < result.totalPages
}

async function loadInitial() {
  loading.value = true
  error.value = ''
  try {
    await fetchPage(1, false)
  } catch (err) {
    error.value = err.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadMoreTopics() {
  if (loading.value || loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  try {
    await fetchPage(currentPage.value + 1, true)
  } catch (err) {
    error.value = err.message || '加载失败'
  } finally {
    loadingMore.value = false
  }
}

function handleScroll(event) {
  const el = event.currentTarget
  const distance = el.scrollHeight - el.scrollTop - el.clientHeight
  if (distance < 160) loadMoreTopics()
}

function topicStatusText(topic) {
  if (topic.status === 'EXPIRED') return '已到期'
  if (topic.status === 'CLOSED') return '已关闭'
  return '进行中'
}

function topicStatusType(topic) {
  if (topic.status === 'EXPIRED') return 'warning'
  if (topic.status === 'CLOSED') return 'default'
  return 'success'
}

function topicUrl(topic) {
  return `${window.location.origin}/box/${topic.boxSlug}?topic=${topic.code}`
}

async function copyTopicUrl(topic) {
  const url = topicUrl(topic)
  try {
    await navigator.clipboard?.writeText(url)
    showSuccessToast('话题链接已复制')
  } catch {
    showToast(url)
  }
}

async function closeTopic(topic) {
  try {
    await showDialog({
      title: '关闭话题',
      message: `确定关闭「${topic.title}」吗？关闭后无法继续收集新问题。`,
      showCancelButton: true,
      confirmButtonText: '关闭',
      confirmButtonColor: '#ee0a24',
    })
    const updated = await closeAdminTopic(topic.id)
    Object.assign(topic, updated)
    showSuccessToast('话题已关闭')
  } catch {}
}

onMounted(loadInitial)
</script>

<template>
  <div class="page">
    <div class="page-sticky">
      <van-search v-model="keyword" placeholder="搜索标题、简介" shape="round" @search="loadInitial" />
      <van-dropdown-menu>
        <van-dropdown-item v-model="status" :options="statusOptions" @change="loadInitial" />
      </van-dropdown-menu>
    </div>

    <div class="page-scroll" @scroll="handleScroll">
      <van-loading v-if="loading" class="loading-center" size="24" />
      <p v-else-if="error" class="error-msg">{{ error }}</p>
      <van-cell-group v-else inset>
        <van-cell v-for="topic in topics" :key="topic.id">
          <template #title>
            <div class="cell-title">
              <span>{{ topic.title }}</span>
              <van-tag :type="topicStatusType(topic)" size="medium">{{ topicStatusText(topic) }}</van-tag>
            </div>
          </template>
          <template #label>
            <div class="topic-label">
              <span>{{ topic.ownerEmail || topic.ownerDisplayName }} · /box/{{ topic.boxSlug }}</span>
              <span>{{ formatTime(new Date(topic.createdAt).getTime()) }} 创建 · {{ topic.questionCount || 0 }} 个问题</span>
              <span>{{ formatTime(new Date(topic.expiresAt).getTime()) }} 到期</span>
              <span v-if="topic.description">{{ topic.description }}</span>
            </div>
          </template>
          <template #value>
            <div class="actions">
              <button type="button" aria-label="复制话题链接" @click.stop="copyTopicUrl(topic)">
                <i class="ri-links-line" aria-hidden="true"></i>
              </button>
              <button
                v-if="topic.status === 'ACTIVE'"
                class="danger"
                type="button"
                aria-label="关闭话题"
                @click.stop="closeTopic(topic)"
              >
                <i class="ri-close-circle-line" aria-hidden="true"></i>
              </button>
            </div>
          </template>
        </van-cell>
      </van-cell-group>
      <div v-if="!loading && !error && topics.length === 0" class="empty-hint">暂无话题</div>
      <div v-if="!loading && !error && topics.length > 0 && (loadingMore || hasMore)" class="load-more-hint">
        {{ loadingMore ? '加载中' : '继续下滑加载更多' }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.page-sticky {
  flex-shrink: 0;
}

.page-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 0 0 12px;
}

.cell-title {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.cell-title span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topic-label {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.topic-label span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.actions button {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 15px;
  background: #f2f3f5;
  color: #323233;
  font-size: 18px;
}

.actions button.danger {
  background: #fff1f0;
  color: #ee0a24;
}

.empty-hint {
  text-align: center;
  padding: 32px 0;
  color: #969799;
  font-size: 14px;
}

.load-more-hint {
  text-align: center;
  padding: 14px 0 18px;
  color: #969799;
  font-size: 12px;
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
