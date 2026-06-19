<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { uploadAttachmentObject } from '@/api/attachments'
import { useAuthStore } from '@/stores/auth'
import { formatTime } from '@/utils'
import { assetSrc } from '@/utils/assets'
import ClassicAskBoxPage from '@/components/classic/ClassicAskBoxPage.vue'
import {
  getBoxProfile,
  updateBoxProfile,
  getBoxStats,
  getPendingQuestions,
  getHistoryQuestions,
  answerQuestion,
  dismissQuestion,
  deleteQuestion,
} from '@/api/owner'

const router = useRouter()
const auth = useAuthStore()

const boxProfile = ref({ displayName: '', slug: '', description: '', avatar: null, background: null })
const pendingQuestions = ref([])
const publishedQA = ref([])
const dismissedQuestions = ref([])
const stats = ref({ pendingCount: 0, publishedCount: 0, dismissedCount: 0, todayReceivedCount: 0 })
const pageState = ref({
  pending: { page: 1, hasMore: false, loading: false },
  published: { page: 1, hasMore: false, loading: false },
  dismissed: { page: 1, hasMore: false, loading: false },
})

const homeSection = ref('inbox')
const questionStatusTab = ref('pending')
const selectedQuestion = ref(null)
const detailOpen = ref(false)
const answerText = ref('')
const answerError = ref('')
const avatarInputRef = ref(null)
const settingsOpen = ref(false)
const avatarMenuOpen = ref(false)
const refreshing = ref(false)
const publicContentRef = ref(null)
const profileDraft = reactive({
  displayName: '',
  slug: '',
  description: '',
})

let pollTimer = 0

const homeSections = [
  { id: 'public', label: '公开页', icon: 'ri-global-line' },
  { id: 'inbox', label: '提问箱', icon: 'ri-inbox-2-line' },
  { id: 'mine', label: '我的', icon: 'ri-user-smile-line' },
]

const questionTabs = [
  { id: 'pending', label: '待回答', icon: 'ri-inbox-2-line' },
  { id: 'published', label: '已发布', icon: 'ri-chat-check-line' },
  { id: 'dismissed', label: '已驳回', icon: 'ri-archive-line' },
]

const questionStatusIndex = computed({
  get: () => Math.max(0, questionTabs.findIndex((tab) => tab.id === questionStatusTab.value)),
  set: (idx) => {
    questionStatusTab.value = questionTabs[idx]?.id || 'pending'
  },
})
const publicUrl = computed(() => `/box/${boxProfile.value.slug}`)
const fullPublicUrl = computed(() => `${window.location.origin}${publicUrl.value}`)
const activeQuestionTab = computed(() => questionTabs.find((tab) => tab.id === questionStatusTab.value) || questionTabs[0])
const displayedQuestions = computed(() => {
  if (questionStatusTab.value === 'pending') return pendingQuestions.value
  if (questionStatusTab.value === 'published') return publishedQA.value
  if (questionStatusTab.value === 'dismissed') return dismissedQuestions.value
  return []
})
const selectedIsPending = computed(() => questionStatusTab.value === 'pending' && !selectedQuestion.value?.answer && selectedQuestion.value)
const selectedIsPublished = computed(() => !!selectedQuestion.value?.answer)
const selectedIsReadOnly = computed(() => questionStatusTab.value === 'dismissed' && selectedQuestion.value)
const answerCount = computed(() => `${answerText.value.length} / 5000`)
const canPublish = computed(() => answerText.value.trim().length > 0)
const avatarMenuActions = [
  { text: '设置', icon: 'setting-o' },
  { text: '退出登录', icon: 'revoke' },
]

async function loadProfile() {
  const p = await getBoxProfile()
  if (!p) return
  boxProfile.value = {
    displayName: p.displayName || '',
    slug: p.slug || '',
    description: p.description || '',
    avatar: p.avatar || null,
    background: p.background || null,
  }
  profileDraft.displayName = p.displayName || ''
  profileDraft.slug = p.slug || ''
  profileDraft.description = p.description || ''
}

async function loadStats() {
  stats.value = await getBoxStats()
}

async function loadPending(reset = false) {
  const s = pageState.value.pending
  if (s.loading) return
  s.loading = true
  const page = reset ? 1 : s.page
  try {
    const r = await getPendingQuestions(page, 20)
    const items = r.records.map((q) => ({
      id: q.id,
      profile: q.avatar,
      question: q.question,
      ts: q.ts,
      time: formatTime(q.ts),
    }))
    pendingQuestions.value = reset ? items : [...pendingQuestions.value, ...items]
    s.page = page + 1
    s.hasMore = page < r.totalPages
  } finally {
    s.loading = false
  }
}

async function loadHistory(status, reset = false) {
  const key = status === 'PUBLISHED' ? 'published' : 'dismissed'
  const target = key === 'published' ? publishedQA : dismissedQuestions
  const s = pageState.value[key]
  if (s.loading) return
  s.loading = true
  const page = reset ? 1 : s.page
  try {
    const r = await getHistoryQuestions(status, page, 20)
    const items = r.records.map((q) => ({
      id: q.id,
      profile: q.avatar,
      ownerAvatar: q.ownerAvatar,
      question: q.question,
      answer: q.answer,
      ts: q.ts,
      time: formatTime(q.ts),
    }))
    target.value = reset ? items : [...target.value, ...items]
    s.page = page + 1
    s.hasMore = page < r.totalPages
  } finally {
    s.loading = false
  }
}

function currentPageState() {
  if (questionStatusTab.value === 'pending') return pageState.value.pending
  if (questionStatusTab.value === 'published') return pageState.value.published
  if (questionStatusTab.value === 'dismissed') return pageState.value.dismissed
  return null
}

function loadActivePage(reset = false) {
  if (questionStatusTab.value === 'pending') return loadPending(reset)
  if (questionStatusTab.value === 'published') return loadHistory('PUBLISHED', reset)
  if (questionStatusTab.value === 'dismissed') return loadHistory('DISMISSED', reset)
}

function handleListScroll(event) {
  const state = currentPageState()
  if (!state || state.loading || !state.hasMore) return
  const el = event.currentTarget
  const distance = el.scrollHeight - el.scrollTop - el.clientHeight
  if (distance < 180) loadActivePage()
}

async function refreshCurrent({ silent = false } = {}) {
  if (!silent) refreshing.value = true
  try {
    if (homeSection.value === 'public') {
      const jobs = [loadProfile(), loadStats()]
      if (!silent) jobs.push(publicContentRef.value?.refreshPublicContent?.() ?? Promise.resolve())
      await Promise.all(jobs)
      return
    }
    if (homeSection.value === 'mine') {
      await Promise.all([loadProfile(), loadStats()])
      return
    }
    await Promise.all([loadActivePage(true), loadStats()])
  } finally {
    if (!silent) refreshing.value = false
  }
}

async function handlePullRefresh() {
  await refreshCurrent({ silent: false })
}

function stopPolling() {
  window.clearInterval(pollTimer)
  pollTimer = 0
}

function startPolling() {
  stopPolling()
  if (homeSection.value !== 'inbox' || document.hidden) return
  pollTimer = window.setInterval(() => {
    refreshCurrent({ silent: true })
  }, 30000)
}

function handleVisibilityChange() {
  if (document.hidden) {
    stopPolling()
    return
  }
  refreshCurrent({ silent: true })
  startPolling()
}

function selectQuestion(question) {
  selectedQuestion.value = question
  answerText.value = questionStatusTab.value === 'pending' ? '' : question.answer ?? ''
  answerError.value = ''
  detailOpen.value = true
}

async function publishSelected() {
  const question = selectedQuestion.value
  const text = answerText.value.trim()
  if (!question || questionStatusTab.value !== 'pending') return
  if (!text) {
    answerError.value = '请先填写回答'
    return
  }
  await answerQuestion(question.id, text)
  pendingQuestions.value = pendingQuestions.value.filter((q) => q.id !== question.id)
  detailOpen.value = false
  selectedQuestion.value = null
  answerText.value = ''
  showToast('回答已发布')
  loadHistory('PUBLISHED', true)
  loadStats()
}

async function dismissSelected() {
  const question = selectedQuestion.value
  if (!question || questionStatusTab.value !== 'pending') return
  await dismissQuestion(question.id)
  pendingQuestions.value = pendingQuestions.value.filter((q) => q.id !== question.id)
  detailOpen.value = false
  selectedQuestion.value = null
  answerText.value = ''
  showToast('问题已驳回')
  loadHistory('DISMISSED', true)
  loadStats()
}

async function deleteSelected() {
  const question = selectedQuestion.value
  if (!question || questionStatusTab.value !== 'dismissed') return
  await deleteQuestion(question.id)
  dismissedQuestions.value = dismissedQuestions.value.filter((q) => q.id !== question.id)
  detailOpen.value = false
  selectedQuestion.value = null
  showToast('已删除')
}

function profilePayload(extra = {}) {
  return {
    displayName: profileDraft.displayName || boxProfile.value.displayName,
    slug: profileDraft.slug || boxProfile.value.slug,
    description: profileDraft.description ?? boxProfile.value.description ?? '',
    ...extra,
  }
}

async function handleUpdateProfile(data) {
  const updated = await updateBoxProfile(data)
  boxProfile.value = {
    ...boxProfile.value,
    ...updated,
    avatar: updated.avatar || null,
    background: updated.background || null,
  }
}

async function saveProfile() {
  try {
    await handleUpdateProfile(profilePayload())
    settingsOpen.value = false
    showToast('设置已保存')
  } catch (error) {
    showToast(error?.message || '保存失败')
  }
}

async function handleBackgroundFile(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  const preview = URL.createObjectURL(file)
  boxProfile.value = { ...boxProfile.value, background: preview }
  try {
    const objectKey = await uploadAttachmentObject(file, 'BOX_BACKGROUND')
    await handleUpdateProfile(profilePayload({ backgroundObjectKey: objectKey }))
    showToast('背景已更新')
  } catch (error) {
    showToast(error?.message || '背景上传失败')
  } finally {
    URL.revokeObjectURL(preview)
  }
}

async function clearBackground() {
  try {
    await handleUpdateProfile(profilePayload({ backgroundObjectKey: '' }))
    showToast('已使用默认背景')
  } catch (error) {
    showToast(error?.message || '清空失败')
  }
}

function openAvatarPicker() {
  avatarInputRef.value?.click()
}

function openSettings() {
  profileDraft.displayName = boxProfile.value.displayName || ''
  profileDraft.slug = boxProfile.value.slug || ''
  profileDraft.description = boxProfile.value.description || ''
  settingsOpen.value = true
}

function handleAvatarMenuSelect(action) {
  avatarMenuOpen.value = false
  if (action.text === '设置') {
    openSettings()
    return
  }
  if (action.text === '退出登录') {
    handleLogout()
  }
}

async function handleAvatarFile(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  const preview = URL.createObjectURL(file)
  boxProfile.value = { ...boxProfile.value, avatar: preview }
  try {
    const objectKey = await uploadAttachmentObject(file, 'BOX_OWNER_AVATAR')
    await handleUpdateProfile(profilePayload({ avatarObjectKey: objectKey }))
    showToast('头像已更新')
  } catch (error) {
    showToast(error?.message || '头像上传失败')
  } finally {
    URL.revokeObjectURL(preview)
  }
}

async function copyPublicUrl() {
  try {
    await navigator.clipboard?.writeText(fullPublicUrl.value)
    showToast('公开页链接已复制')
  } catch {
    showToast(fullPublicUrl.value)
  }
}

async function handleLogout() {
  await auth.logout()
  router.replace('/login')
}

function avatarSrc(avatar) {
  if (!avatar) return ''
  return assetSrc(avatar)
}

function avatarStyle(avatar, fallbackBg = '#eff6ff') {
  return {
    backgroundColor: avatar?.bg || fallbackBg,
  }
}

function questionTime(question) {
  return question?.ts ? formatTime(question.ts) : question?.time
}

watch([homeSection, questionStatusTab], () => {
  selectedQuestion.value = null
  detailOpen.value = false
  answerText.value = ''
  refreshCurrent({ silent: true })
  startPolling()
})

onMounted(async () => {
  try {
    await Promise.all([loadProfile(), loadStats(), loadPending(true)])
    document.addEventListener('visibilitychange', handleVisibilityChange)
    startPolling()
  } catch {
    // errors are surfaced by the API interceptor
  }
})

onBeforeUnmount(() => {
  stopPolling()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<template>
  <main class="classic-owner classic-page classic-enter">
    <van-nav-bar fixed placeholder safe-area-inset-top>
      <template #title>
        <span class="nav-title">{{ boxProfile.displayName || 'AskBox' }}</span>
      </template>
      <template #left>
        <van-popover
          v-model:show="avatarMenuOpen"
          :actions="avatarMenuActions"
          placement="bottom-start"
          @select="handleAvatarMenuSelect"
        >
          <template #reference>
            <button class="avatar-button" type="button" aria-label="打开账户菜单" :style="avatarStyle(boxProfile.avatar)">
              <img v-if="avatarSrc(boxProfile.avatar)" :src="avatarSrc(boxProfile.avatar)" alt="" />
              <i v-else class="ri-user-heart-line" aria-hidden="true"></i>
            </button>
          </template>
        </van-popover>
        <input ref="avatarInputRef" class="hidden-file-input" type="file" accept="image/png,image/jpeg,image/webp,image/gif" @change="handleAvatarFile" />
      </template>
      <template #right>
        <button class="nav-icon" type="button" aria-label="复制公开页" @click="copyPublicUrl">
          <i class="ri-links-line" aria-hidden="true"></i>
        </button>
      </template>
    </van-nav-bar>

    <section class="owner-body">
      <van-pull-refresh v-model="refreshing" class="owner-refresh" @refresh="handlePullRefresh">
        <section v-if="homeSection === 'public'" class="home-section public-section">
          <ClassicAskBoxPage
            v-if="boxProfile.slug"
            ref="publicContentRef"
            :slug-override="boxProfile.slug"
            embedded
            :show-composer="false"
          />
          <div v-else class="load-state" aria-live="polite">
            <van-loading size="18" />
            <span>加载公开页</span>
          </div>
        </section>

        <section v-else-if="homeSection === 'inbox'" class="home-section inbox-section">
          <van-tabs v-model:active="questionStatusIndex" animated swipeable class="owner-tabs">
            <van-tab v-for="tab in questionTabs" :key="tab.id" :title="tab.label" />
          </van-tabs>

          <section class="question-list" :aria-label="activeQuestionTab.label" @scroll="handleListScroll">
            <TransitionGroup name="classic-list" tag="div" class="question-list__inner">
              <article
                v-for="question in displayedQuestions"
                :key="question.id"
                class="classic-card classic-press question-card"
                role="button"
                tabindex="0"
                @click="selectQuestion(question)"
                @keydown.enter.prevent="selectQuestion(question)"
                @keydown.space.prevent="selectQuestion(question)"
              >
                <header>
                  <span class="mini-avatar" :style="avatarStyle(question.profile)">
                    <img v-if="avatarSrc(question.profile)" :src="avatarSrc(question.profile)" alt="" />
                  </span>
                  <time>{{ questionTime(question) }}</time>
                </header>
                <p>{{ question.question }}</p>
                <blockquote v-if="question.answer">{{ question.answer }}</blockquote>
              </article>
            </TransitionGroup>

            <van-empty v-if="displayedQuestions.length === 0 && !currentPageState()?.loading" image="search" description="这里暂时没有内容" />
            <div
              v-else-if="(currentPageState()?.loading && displayedQuestions.length === 0) || currentPageState()?.hasMore"
              class="load-state"
              aria-live="polite"
            >
              <van-loading v-if="currentPageState()?.loading && displayedQuestions.length === 0" size="18" />
              <span>{{ currentPageState()?.loading ? '加载中' : '继续下滑加载更多' }}</span>
            </div>
          </section>
        </section>

        <section v-else class="home-section mine-section">
          <article class="classic-card mine-card">
            <div class="public-profile">
              <span class="profile-avatar" :style="avatarStyle(boxProfile.avatar)">
                <img v-if="avatarSrc(boxProfile.avatar)" :src="avatarSrc(boxProfile.avatar)" alt="" />
                <i v-else class="ri-user-heart-line" aria-hidden="true"></i>
              </span>
              <div>
                <h1>{{ boxProfile.displayName || 'AskBox' }}</h1>
                <p>{{ auth.user?.email || auth.user?.username || '箱主账号' }}</p>
              </div>
            </div>
            <div class="mine-actions">
              <button type="button" @click="openSettings">
                <i class="ri-settings-3-line" aria-hidden="true"></i>
                <span>箱子设置</span>
              </button>
              <button type="button" @click="router.push('/password')">
                <i class="ri-lock-password-line" aria-hidden="true"></i>
                <span>修改密码</span>
              </button>
              <button type="button" @click="handleLogout">
                <i class="ri-logout-box-r-line" aria-hidden="true"></i>
                <span>退出登录</span>
              </button>
            </div>
          </article>
        </section>
      </van-pull-refresh>

      <van-tabbar v-model="homeSection" class="owner-tabbar" fixed safe-area-inset-bottom>
        <van-tabbar-item v-for="section in homeSections" :key="section.id" :name="section.id">
          <span>{{ section.label }}</span>
          <template #icon>
            <i :class="section.icon" aria-hidden="true"></i>
          </template>
        </van-tabbar-item>
      </van-tabbar>
    </section>

    <van-popup v-model:show="settingsOpen" round position="bottom" :style="{ maxHeight: '88vh' }">
      <section class="settings-sheet">
        <header class="sheet-head">
          <h2>箱子设置</h2>
          <button class="nav-icon" type="button" aria-label="关闭设置" @click="settingsOpen = false">
            <i class="ri-close-line" aria-hidden="true"></i>
          </button>
        </header>
        <button class="avatar-edit" type="button" :style="avatarStyle(boxProfile.avatar)" @click="openAvatarPicker">
          <img v-if="avatarSrc(boxProfile.avatar)" :src="avatarSrc(boxProfile.avatar)" alt="" />
          <i v-else class="ri-user-heart-line" aria-hidden="true"></i>
          <span>修改头像</span>
        </button>
        <div class="settings-fields">
          <van-field v-model="profileDraft.displayName" label="箱子名称" maxlength="32" placeholder="提问箱名称" />
          <van-field v-model="profileDraft.slug" label="公开地址" maxlength="32" placeholder="URL 标识" />
          <van-field v-model="profileDraft.description" label="简介" type="textarea" rows="3" maxlength="160" autosize placeholder="提问箱描述" />
        </div>
        <div class="setting-actions">
          <label class="outline-pill classic-press">
            <i class="ri-image-add-line" aria-hidden="true"></i>
            <span>修改背景</span>
            <input type="file" accept="image/png,image/jpeg,image/webp,image/gif" @change="handleBackgroundFile" />
          </label>
          <button class="outline-pill" type="button" @click="clearBackground">
            <i class="ri-delete-bin-line" aria-hidden="true"></i>
            <span>清空背景</span>
          </button>
        </div>
        <button class="primary-pill" type="button" @click="saveProfile">
          <i class="ri-check-line" aria-hidden="true"></i>
          <span>保存设置</span>
        </button>
      </section>
    </van-popup>

    <van-popup v-model:show="detailOpen" round position="bottom" :style="{ maxHeight: '88vh' }">
      <article v-if="selectedQuestion" class="detail-panel">
        <header class="detail-head">
          <div>
            <time>{{ questionTime(selectedQuestion) }}</time>
            <h2>{{ selectedIsPending ? '回答问题' : '问题详情' }}</h2>
          </div>
          <button class="nav-icon" type="button" aria-label="关闭" @click="detailOpen = false">
            <i class="ri-close-line" aria-hidden="true"></i>
          </button>
        </header>
        <p class="detail-question">{{ selectedQuestion.question }}</p>

        <section v-if="selectedIsPending" class="answer-editor">
          <van-field
            v-model="answerText"
            type="textarea"
            rows="6"
            maxlength="5000"
            autosize
            show-word-limit
            placeholder="写下你的回答。"
            :error-message="answerError"
          />
          <footer>
            <span>{{ answerCount }}</span>
            <div>
              <van-button round plain @click="dismissSelected">驳回</van-button>
              <van-button round type="primary" icon="guide-o" :disabled="!canPublish" @click="publishSelected">
                发布回答
              </van-button>
            </div>
          </footer>
        </section>

        <section v-else-if="selectedIsPublished" class="published-answer">
          <span>公开回答</span>
          <p>{{ selectedQuestion.answer }}</p>
        </section>

        <section v-else-if="selectedIsReadOnly" class="published-answer muted">
          <span>已驳回</span>
          <p>这个问题不会显示在公开页。</p>
          <van-button round plain type="danger" @click="deleteSelected">删除</van-button>
        </section>
      </article>
    </van-popup>
  </main>
</template>

<style scoped>
.classic-owner {
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
}

.nav-title {
  font-weight: 720;
}

.avatar-button,
.nav-icon {
  display: grid;
  place-items: center;
  border: 0;
  background: transparent;
  font: inherit;
}

.avatar-button {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  overflow: hidden;
  color: var(--classic-primary);
}

.avatar-button img,
.profile-avatar img,
.avatar-edit img,
.mini-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.nav-icon {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  color: var(--classic-text);
  font-size: 20px;
}

.hidden-file-input,
.outline-pill input {
  position: fixed;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.owner-body {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 46px);
  height: calc(100dvh - 46px);
  overflow: hidden;
  padding: 12px 14px calc(62px + env(safe-area-inset-bottom));
}

.owner-refresh {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.owner-refresh:deep(.van-pull-refresh__track) {
  height: 100%;
}

.home-section {
  height: 100%;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 0 0 10px;
  overscroll-behavior: contain;
}

.mine-section {
  display: grid;
  align-content: start;
  gap: 12px;
}

.metric {
  width: 100%;
  padding: 10px 0;
}

.metric.compact {
  min-width: 0;
  padding: 0;
}

.metric span,
.metric strong {
  display: block;
}

.metric span {
  color: var(--classic-muted);
  font-size: 12px;
}

.metric strong {
  margin-top: 4px;
  color: var(--classic-text);
  font-size: 22px;
  line-height: 1.25;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mine-card {
  padding: 16px;
}

.public-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.profile-avatar {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  flex: 0 0 auto;
  overflow: hidden;
  border-radius: 12px;
  background-position: center;
  background-size: cover;
  color: var(--classic-primary);
  font-size: 22px;
}

.public-profile h1 {
  margin: 0;
  color: var(--classic-text);
  font-size: 20px;
  line-height: 1.25;
}

.public-profile p {
  display: -webkit-box;
  margin: 4px 0 0;
  overflow: hidden;
  color: var(--classic-muted);
  font-size: 13px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.mine-actions button,
.outline-pill,
.primary-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  background: transparent;
  color: inherit;
  font: inherit;
}

.owner-tabs {
  flex: 0 0 auto;
  border-radius: 8px;
  overflow: hidden;
}

.inbox-section {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.question-list {
  flex: 1;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 12px 0 4px;
  overscroll-behavior: contain;
}

.question-list__inner {
  display: grid;
  gap: 10px;
}

.question-card {
  padding: 14px;
}

.question-card header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.mini-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  overflow: hidden;
  background-position: center;
  background-size: cover;
}

.mini-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

time {
  color: var(--classic-muted);
  font-size: 12px;
}

.question-card p {
  margin: 10px 0 0;
  color: var(--classic-text);
  font-size: 14px;
  line-height: 1.65;
  overflow-wrap: anywhere;
}

.question-card blockquote {
  margin: 10px 0 0;
  padding: 0 0 0 12px;
  border-left: 2px solid rgba(47, 111, 237, 0.35);
  color: #374151;
  font-size: 13px;
  line-height: 1.65;
  display: -webkit-box;
  overflow: hidden;
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.load-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 18px 0;
  color: var(--classic-muted);
  font-size: 13px;
}

.mine-actions {
  display: grid;
  gap: 8px;
  margin-top: 18px;
}

.mine-actions button {
  justify-content: flex-start;
  gap: 10px;
  min-height: 46px;
  padding: 0 12px;
  border: 1px solid var(--classic-line);
  border-radius: 10px;
  color: var(--classic-text);
  font-size: 14px;
}

.mine-actions i {
  color: var(--classic-primary);
  font-size: 18px;
}

.owner-tabbar {
  border-top: 1px solid rgba(226, 232, 240, 0.86);
}

.owner-tabbar i {
  font-size: 20px;
}

.settings-sheet {
  display: grid;
  gap: 14px;
  max-height: 88vh;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 18px 16px calc(18px + env(safe-area-inset-bottom));
}

.sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.sheet-head h2 {
  margin: 0;
  color: var(--classic-text);
  font-size: 18px;
  line-height: 1.3;
}

.avatar-edit {
  justify-self: start;
  position: relative;
  display: grid;
  place-items: center;
  width: 58px;
  height: 58px;
  overflow: hidden;
  border: 0;
  border-radius: 14px;
  background-position: center;
  background-size: cover;
  color: var(--classic-primary);
  font: inherit;
}

.avatar-edit span {
  position: absolute;
  right: -6px;
  bottom: -6px;
  display: grid;
  place-items: center;
  width: 34px;
  height: 20px;
  border-radius: 10px;
  background: rgba(15, 23, 42, 0.78);
  color: #fff;
  font-size: 10px;
}

.settings-fields {
  overflow: hidden;
  border: 1px solid var(--classic-line);
  border-radius: 12px;
}

.setting-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.outline-pill,
.primary-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 44px;
  padding: 0 16px;
  border: 1px solid var(--classic-line);
  border-radius: 22px;
  background: #fff;
  color: var(--classic-text);
  font-size: 14px;
}

.primary-pill {
  width: 100%;
  border-color: transparent;
  background: var(--classic-primary);
  color: #fff;
  font-weight: 650;
}

.detail-panel {
  max-height: 88vh;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 18px 16px calc(18px + env(safe-area-inset-bottom));
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.detail-head h2 {
  margin: 4px 0 0;
  color: var(--classic-text);
  font-size: 18px;
}

.detail-question {
  margin: 16px 0 0;
  color: var(--classic-text);
  font-size: 16px;
  font-weight: 650;
  line-height: 1.65;
  overflow-wrap: anywhere;
}

.answer-editor {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.answer-editor footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: var(--classic-muted);
  font-size: 12px;
}

.answer-editor footer > div {
  display: flex;
  gap: 8px;
}

.published-answer {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.published-answer span {
  color: var(--classic-muted);
  font-size: 12px;
}

.published-answer p {
  margin: 0;
  color: #374151;
  font-size: 15px;
  line-height: 1.7;
  overflow-wrap: anywhere;
}
</style>
