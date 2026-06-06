<script setup>
import { ref, computed, onMounted } from 'vue'
import { showDialog, showSuccessToast } from 'vant'
import { listAllQuestions, forceDeleteQuestion, deleteAnswer } from '../../api/admin'
import { formatTime } from '../../utils'

const loading = ref(true)
const error = ref('')
const questions = ref([])
const keyword = ref('')
const activeTab = ref(0)
const statusFilters = ['', 'PENDING', 'PUBLISHED', 'DISMISSED']

const statusMap = { PENDING: '待回答', PUBLISHED: '已发布', DISMISSED: '已驳回' }
const statusType = { PENDING: 'warning', PUBLISHED: 'success', DISMISSED: 'default' }

async function loadData() {
  loading.value = true
  const params = {}
  const st = statusFilters[activeTab.value]
  if (st) params.status = st
  if (keyword.value.trim()) params.keyword = keyword.value.trim()
  try {
    const page = await listAllQuestions(1, 50, params)
    questions.value = page.records
  } catch (err) {
    error.value = err.message || '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

const filteredQuestions = computed(() => {
  // 服务端已筛选，这里直接返回
  return questions.value
})

function onTabChange() {
  loadData()
}

async function handleDelete(q) {
  try {
    await showDialog({ title: '确认删除', message: '确定要永久删除该问题吗？' })
    await forceDeleteQuestion(q.id)
    questions.value = questions.value.filter(x => x.id !== q.id)
    showSuccessToast('已删除')
  } catch {}
}

async function handleDeleteAnswer(q) {
  try {
    await showDialog({ title: '确认删除', message: '确定要删除该问题的回答吗？' })
    // 需要通过 admin/questions/{id} 获取回答ID，现在用简便方式
    await deleteAnswer(q.id) // FIXME: 后端需要 answerId，此简化版本暂不可用
    q.answer = null
    q.answeredAt = null
    showSuccessToast('回答已删除')
  } catch {}
}

const showDetail = ref(false)
const detailQuestion = ref(null)

function openDetail(q) {
  detailQuestion.value = q
  showDetail.value = true
}
</script>

<template>
  <div class="page">
    <div class="page-sticky">
      <van-search v-model="keyword" placeholder="搜索问题、回答、提问箱" shape="round" @search="loadData" />
      <van-tabs v-model:active="activeTab" :offset-top="0" swipeable class="inline-tabs" @change="onTabChange">
        <van-tab title="全部"></van-tab>
        <van-tab title="待回答"></van-tab>
        <van-tab title="已发布"></van-tab>
        <van-tab title="已驳回"></van-tab>
      </van-tabs>
    </div>

    <div class="page-scroll">
      <van-loading v-if="loading" class="loading-center" size="24" />
      <p v-else-if="error" class="error-msg">{{ error }}</p>
      <van-cell-group v-else inset>
        <van-cell v-for="q in filteredQuestions" :key="q.id" is-link @click="openDetail(q)">
          <template #title>
            <div class="q-title">
              <van-tag :type="statusType[q.status]" size="medium" class="q-tag">{{ statusMap[q.status] }}</van-tag>
              <span class="q-text">{{ q.question }}</span>
            </div>
          </template>
          <template #label>
            <div class="q-meta">
              <code class="slug-badge">{{ q.boxSlug }}</code>
              <span>{{ formatTime(q.createdAt) }}</span>
              <span v-if="q.answer" class="q-has-answer">有回答</span>
            </div>
          </template>
        </van-cell>
      </van-cell-group>
      <div v-if="!loading && !error && filteredQuestions.length === 0" class="empty-hint">暂无匹配问题</div>
    </div>

    <van-dialog v-model:show="showDetail" title="问题详情" :show-confirm-button="false" show-cancel-button cancel-button-text="关闭">
      <template v-if="detailQuestion">
        <div class="detail-body">
          <div class="detail-row">
            <span class="detail-key">提问箱</span>
            <code class="slug-badge">{{ detailQuestion.boxSlug }}</code>
          </div>
          <div class="detail-row">
            <span class="detail-key">状态</span>
            <van-tag :type="statusType[detailQuestion.status]" size="medium">{{ statusMap[detailQuestion.status] }}</van-tag>
          </div>
          <div class="detail-row">
            <span class="detail-key">问题</span>
            <p class="detail-text">{{ detailQuestion.question }}</p>
          </div>
          <div class="detail-row">
            <span class="detail-key">回答</span>
            <p v-if="detailQuestion.answer" class="detail-text">{{ detailQuestion.answer }}</p>
            <span v-else class="detail-none">--</span>
          </div>
          <div class="detail-row">
            <span class="detail-key">提问时间</span>
            <span>{{ formatTime(detailQuestion.createdAt) }}</span>
          </div>
          <div v-if="detailQuestion.answeredAt" class="detail-row">
            <span class="detail-key">回答时间</span>
            <span>{{ formatTime(detailQuestion.answeredAt) }}</span>
          </div>
          <div class="detail-actions">
            <van-button type="danger" size="small" plain round @click="handleDelete(detailQuestion); showDetail = false">删除问题</van-button>
          </div>
        </div>
      </template>
    </van-dialog>
  </div>
</template>

<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }
.page-sticky { flex-shrink: 0; }
.inline-tabs { margin-top: -8px; }
.page-scroll { flex: 1; overflow-y: auto; padding: 8px 0 12px; }
.q-title { display: flex; align-items: flex-start; gap: 8px; }
.q-tag { flex-shrink: 0; margin-top: 1px; }
.q-text { font-size: 14px; line-height: 1.5; color: #323233; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.q-meta { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-top: 4px; font-size: 12px; color: #969799; }
.q-has-answer { color: #07c160; font-weight: 500; }
.slug-badge { padding: 1px 6px; background: #f0f2f5; border-radius: 4px; font-size: 11px; color: #1989fa; font-family: "SF Mono", "Cascadia Code", monospace; }
.empty-hint { text-align: center; padding: 32px 0; color: #969799; font-size: 14px; }
.loading-center { display: flex; justify-content: center; padding: 24px 0; }
.error-msg { text-align: center; color: #ee0a24; padding: 24px 0; font-size: 14px; }
.detail-body { padding: 8px 16px 16px; }
.detail-row { margin-bottom: 14px; }
.detail-key { display: block; font-size: 11px; color: #969799; text-transform: uppercase; letter-spacing: 0.04em; margin-bottom: 4px; }
.detail-text { padding: 10px 12px; background: #f7f8fa; border-radius: 8px; font-size: 14px; line-height: 1.6; color: #323233; margin: 4px 0 0; }
.detail-none { color: #c8c9cc; }
.detail-actions { display: flex; gap: 10px; margin-top: 20px; padding-top: 16px; border-top: 1px solid #ebedf0; }
</style>
