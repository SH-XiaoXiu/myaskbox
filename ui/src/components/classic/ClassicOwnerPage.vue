<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { uploadAttachmentObject } from '@/api/attachments'
import { useAuthStore } from '@/stores/auth'
import { useLogoutConfirm } from '@/composables/useLogoutConfirm'
import { formatTime } from '@/utils'
import { assetSrc } from '@/utils/assets'
import ClassicAskBoxPage from '@/components/classic/ClassicAskBoxPage.vue'
import ClassicQuestionList from '@/components/classic/ClassicQuestionList.vue'
import {
  getBoxProfile,
  updateBoxProfile,
  getBoxStats,
  getPendingQuestions,
  getHistoryQuestions,
  answerQuestion,
  dismissQuestion,
  deleteQuestion,
  getTopics,
} from '@/api/owner'

const router = useRouter()
const auth = useAuthStore()
const { confirmLogout } = useLogoutConfirm()

const boxProfile = ref({ displayName: '', slug: '', description: '', avatar: null, background: null })
const pendingQuestions = ref([])
const publishedQA = ref([])
const dismissedQuestions = ref([])
const topics = ref([])
const stats = ref({ pendingCount: 0, publishedCount: 0, dismissedCount: 0, todayReceivedCount: 0 })
const pageState = ref({
  pending: { page: 1, hasMore: false, loading: false },
  published: { page: 1, hasMore: false, loading: false },
  dismissed: { page: 1, hasMore: false, loading: false },
})

const homeSection = ref('inbox')
const questionStatusTab = ref('pending')
const renderedQuestionStatusTab = ref('pending')
const questionListExiting = ref(false)
const questionListMinHeight = ref('')
const selectedQuestion = ref(null)
const detailOpen = ref(false)
const answerText = ref('')
const answerError = ref('')
const accountAvatarInputRef = ref(null)
const accountSheetOpen = ref(false)
const boxSettingsOpen = ref(false)
const emailSheetOpen = ref(false)
const refreshing = ref(false)
const publicContentRef = ref(null)
const accountAvatarPreview = ref('')
const profileDraft = reactive({
  displayName: '',
  slug: '',
  description: '',
})
const accountDraft = reactive({
  displayName: '',
})
const emailDraft = reactive({
  email: '',
  code: '',
})
const emailSending = ref(false)
const emailSaving = ref(false)

let pollTimer = 0
let questionTabSwitchToken = 0

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
    switchQuestionStatusTab(questionTabs[idx]?.id || 'pending')
  },
})
const publicUrl = computed(() => `/box/${boxProfile.value.slug}`)
const fullPublicUrl = computed(() => `${window.location.origin}${publicUrl.value}`)
const activeQuestionTab = computed(() => questionTabs.find((tab) => tab.id === questionStatusTab.value) || questionTabs[0])
const accountName = computed(() => auth.user?.displayName || auth.user?.username || '箱主')
const pageTitle = computed(() => `${accountName.value}的提问箱`)
const accountEmail = computed(() => auth.user?.email || auth.user?.username || '')
const accountAvatar = computed(() => accountAvatarPreview.value || auth.user?.avatar || boxProfile.value.avatar)
const activeTopics = computed(() => topics.value.filter((topic) => topic.available))
const displayedQuestions = computed(() => {
  if (renderedQuestionStatusTab.value === 'pending') return pendingQuestions.value
  if (renderedQuestionStatusTab.value === 'published') return publishedQA.value
  if (renderedQuestionStatusTab.value === 'dismissed') return dismissedQuestions.value
  return []
})
const visibleQuestions = computed(() => (questionListExiting.value ? [] : displayedQuestions.value))
const selectedIsPending = computed(() => questionStatusTab.value === 'pending' && !selectedQuestion.value?.answer && selectedQuestion.value)
const selectedIsPublished = computed(() => !!selectedQuestion.value?.answer)
const selectedCanDelete = computed(() => !!selectedQuestion.value && !selectedIsPending.value)
const answerCount = computed(() => `${answerText.value.length} / 5000`)
const canPublish = computed(() => answerText.value.trim().length > 0)

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
      topic: q.topic,
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
      topic: q.topic,
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

async function loadTopics() {
  topics.value = await getTopics()
}

function currentPageState() {
  return pageStateFor(renderedQuestionStatusTab.value)
}

function pageStateFor(tab) {
  if (tab === 'pending') return pageState.value.pending
  if (tab === 'published') return pageState.value.published
  if (tab === 'dismissed') return pageState.value.dismissed
  return null
}

function loadActivePage(reset = false) {
  return loadQuestionTab(renderedQuestionStatusTab.value, reset)
}

function loadQuestionTab(tab, reset = false) {
  if (tab === 'pending') return loadPending(reset)
  if (tab === 'published') return loadHistory('PUBLISHED', reset)
  if (tab === 'dismissed') return loadHistory('DISMISSED', reset)
}

function handleListScroll(event) {
  const state = currentPageState()
  if (!state || state.loading || !state.hasMore) return
  const el = event.currentTarget
  const distance = el.scrollHeight - el.scrollTop - el.clientHeight
  if (distance < 180) loadActivePage()
}

function prefersReducedMotion() {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches
}

async function switchQuestionStatusTab(tab) {
  if (!tab || tab === questionStatusTab.value) return
  const token = ++questionTabSwitchToken
  questionStatusTab.value = tab
  selectedQuestion.value = null
  detailOpen.value = false
  answerText.value = ''
  if (visibleQuestions.value.length && !prefersReducedMotion()) {
    await runQuestionListExit(token)
  }
  if (token !== questionTabSwitchToken) return
  renderedQuestionStatusTab.value = tab
  await nextTick()
  const state = pageStateFor(tab)
  if (state && state.page === 1 && !state.loading) {
    await loadQuestionTab(tab, true)
  }
  startPolling()
}

async function runQuestionListExit(token) {
  const list = document.querySelector('.question-list .classic-animated-list__inner')
  questionListMinHeight.value = list ? `${list.offsetHeight}px` : ''
  questionListExiting.value = true
  await nextTick()
  await waitForQuestionLeave()
  if (token === questionTabSwitchToken) {
    questionListExiting.value = false
    questionListMinHeight.value = ''
  }
}

function waitForQuestionLeave() {
  return new Promise((resolve) => {
    window.setTimeout(resolve, prefersReducedMotion() ? 0 : 720)
  })
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
  if (!question || !selectedCanDelete.value) return
  try {
    await showConfirmDialog({
      title: '删除这个问题？',
      message: '删除后问题和回答都会移除，公开页也不会再显示。',
      confirmButtonText: '删除',
      confirmButtonColor: '#f04438',
    })
  } catch {
    return
  }
  await deleteQuestion(question.id)
  pendingQuestions.value = pendingQuestions.value.filter((q) => q.id !== question.id)
  publishedQA.value = publishedQA.value.filter((q) => q.id !== question.id)
  dismissedQuestions.value = dismissedQuestions.value.filter((q) => q.id !== question.id)
  detailOpen.value = false
  selectedQuestion.value = null
  showToast('已删除')
  await Promise.all([loadStats(), publicContentRef.value?.refreshPublicContent?.() ?? Promise.resolve()])
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
    boxSettingsOpen.value = false
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

function openAccountAvatarPicker() {
  accountAvatarInputRef.value?.click()
}

function openBoxSettings() {
  profileDraft.displayName = boxProfile.value.displayName || ''
  profileDraft.slug = boxProfile.value.slug || ''
  profileDraft.description = boxProfile.value.description || ''
  boxSettingsOpen.value = true
}

function openAccountSheet() {
  accountDraft.displayName = auth.user?.displayName || auth.user?.username || ''
  accountAvatarPreview.value = ''
  accountSheetOpen.value = true
}

function openEmailSheet() {
  emailDraft.email = accountEmail.value
  emailDraft.code = ''
  accountSheetOpen.value = false
  emailSheetOpen.value = true
}

async function handleAccountAvatarFile(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  const preview = URL.createObjectURL(file)
  accountAvatarPreview.value = preview
  try {
    const objectKey = await uploadAttachmentObject(file, 'ACCOUNT_AVATAR')
    await auth.updateProfile({ displayName: accountDraft.displayName || accountName.value, avatarObjectKey: objectKey })
    await loadProfile()
    showToast('头像已更新')
  } catch (error) {
    showToast(error?.message || '头像上传失败')
  } finally {
    URL.revokeObjectURL(preview)
    accountAvatarPreview.value = ''
  }
}

async function saveAccountProfile() {
  try {
    await auth.updateProfile({ displayName: accountDraft.displayName })
    await loadProfile()
    accountSheetOpen.value = false
    showToast('账号资料已保存')
  } catch (error) {
    showToast(error?.message || '保存失败')
  }
}

async function sendEmailCode() {
  if (!emailDraft.email.trim()) {
    showToast('请填写新邮箱')
    return
  }
  emailSending.value = true
  try {
    await auth.sendEmailChangeCode(emailDraft.email)
    showToast('验证码已发送')
  } catch (error) {
    showToast(error?.message || '发送失败')
  } finally {
    emailSending.value = false
  }
}

async function saveEmail() {
  if (!emailDraft.email.trim() || !emailDraft.code.trim()) {
    showToast('请填写邮箱和验证码')
    return
  }
  emailSaving.value = true
  try {
    await auth.changeEmail(emailDraft.email, emailDraft.code)
    emailSheetOpen.value = false
    showToast('邮箱已更新')
  } catch (error) {
    showToast(error?.message || '更新失败')
  } finally {
    emailSaving.value = false
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

function handleLogout() {
  confirmLogout()
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

watch(homeSection, () => {
  selectedQuestion.value = null
  detailOpen.value = false
  answerText.value = ''
  if (homeSection.value === 'public') {
    startPolling()
    return
  }
  if (homeSection.value === 'inbox') {
    refreshCurrent({ silent: true })
    startPolling()
    return
  }
  refreshCurrent({ silent: true })
  startPolling()
})

onMounted(async () => {
  try {
    await Promise.all([auth.ensureUser(), loadProfile(), loadStats(), loadPending(true), loadTopics()])
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
        <span class="nav-title">{{ pageTitle }}</span>
      </template>
      <template #left>
        <button class="nav-profile" type="button" aria-label="账号资料" @click="openAccountSheet">
          <span :style="avatarStyle(accountAvatar)">
            <img v-if="avatarSrc(accountAvatar)" :src="avatarSrc(accountAvatar)" alt="" />
            <i v-else class="ri-user-smile-line" aria-hidden="true"></i>
          </span>
        </button>
        <input ref="accountAvatarInputRef" class="hidden-file-input" type="file" accept="image/png,image/jpeg,image/webp,image/gif" @change="handleAccountAvatarFile" />
      </template>
      <template #right>
        <button class="nav-icon" type="button" aria-label="复制公开页" @click="copyPublicUrl">
          <i class="ri-links-line" aria-hidden="true"></i>
        </button>
      </template>
    </van-nav-bar>

    <section class="owner-body">
      <section v-show="homeSection === 'public'" class="home-section public-section">
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

      <section v-show="homeSection === 'inbox'" class="home-section inbox-section">
        <van-tabs v-model:active="questionStatusIndex" animated swipeable class="owner-tabs">
          <van-tab v-for="tab in questionTabs" :key="tab.id" :title="tab.label" />
        </van-tabs>

        <van-pull-refresh v-model="refreshing" class="list-refresh" @refresh="handlePullRefresh">
          <section class="question-list" :aria-label="activeQuestionTab.label" @scroll="handleListScroll">
            <ClassicQuestionList
              :questions="visibleQuestions"
              variant="owner"
              :transition-name="questionListExiting ? 'classic-filter-list' : 'classic-list'"
              :min-height="questionListMinHeight"
              :loading="!questionListExiting && !!currentPageState()?.loading"
              :has-more="!questionListExiting && !!currentPageState()?.hasMore"
              empty-description="这里暂时没有内容"
              :show-load-state="!questionListExiting"
              :aria-label="activeQuestionTab.label"
              @open="selectQuestion"
            />
          </section>
        </van-pull-refresh>
      </section>

      <section v-show="homeSection === 'mine'" class="home-section mine-section">
        <button class="mine-identity classic-card classic-press" type="button" @click="openAccountSheet">
          <span class="profile-avatar" :style="avatarStyle(accountAvatar)">
            <img v-if="avatarSrc(accountAvatar)" :src="avatarSrc(accountAvatar)" alt="" />
            <i v-else class="ri-user-heart-line" aria-hidden="true"></i>
          </span>
          <span class="mine-identity__body">
            <strong>{{ accountName }}</strong>
            <small>{{ accountEmail || '箱主账号' }}</small>
          </span>
          <i class="ri-arrow-right-s-line" aria-hidden="true"></i>
        </button>

        <section class="mine-stat-strip" aria-label="提问箱数据">
          <div>
            <strong>{{ stats.pendingCount }}</strong>
            <span>待回答</span>
          </div>
          <div>
            <strong>{{ stats.publishedCount }}</strong>
            <span>已发布</span>
          </div>
          <div>
            <strong>{{ stats.todayReceivedCount }}</strong>
            <span>今日收到</span>
          </div>
        </section>

        <section class="mine-group" aria-label="提问箱设置">
          <header>提问箱</header>
          <button class="mine-row" type="button" @click="openBoxSettings">
            <span class="mine-row__icon"><i class="ri-inbox-2-line" aria-hidden="true"></i></span>
            <span class="mine-row__text">
              <strong>提问箱设置</strong>
              <small>{{ boxProfile.displayName || '未命名' }} · {{ publicUrl }}</small>
            </span>
            <i class="ri-arrow-right-s-line" aria-hidden="true"></i>
          </button>
          <button class="mine-row" type="button" @click="copyPublicUrl">
            <span class="mine-row__icon"><i class="ri-links-line" aria-hidden="true"></i></span>
            <span class="mine-row__text">
              <strong>复制公开链接</strong>
              <small>{{ publicUrl }}</small>
            </span>
            <i class="ri-file-copy-line" aria-hidden="true"></i>
          </button>
          <button class="mine-row" type="button" @click="router.push('/topics')">
            <span class="mine-row__icon"><i class="ri-price-tag-3-line" aria-hidden="true"></i></span>
            <span class="mine-row__text">
              <strong>话题管理</strong>
              <small>{{ activeTopics.length }} 个进行中话题</small>
            </span>
            <i class="ri-arrow-right-s-line" aria-hidden="true"></i>
          </button>
        </section>

        <button class="logout-button" type="button" @click="handleLogout">退出登录</button>
      </section>

      <van-tabbar v-model="homeSection" class="owner-tabbar" safe-area-inset-bottom>
        <van-tabbar-item v-for="section in homeSections" :key="section.id" :name="section.id">
          <span>{{ section.label }}</span>
          <template #icon>
            <i :class="section.icon" aria-hidden="true"></i>
          </template>
        </van-tabbar-item>
      </van-tabbar>
    </section>

    <van-popup v-model:show="accountSheetOpen" round position="bottom" :style="{ maxHeight: '82vh' }">
      <section class="settings-sheet">
        <header class="sheet-head">
          <h2>账号资料</h2>
          <button class="nav-icon" type="button" aria-label="关闭账号资料" @click="accountSheetOpen = false">
            <i class="ri-close-line" aria-hidden="true"></i>
          </button>
        </header>
        <button class="avatar-edit" type="button" @click="openAccountAvatarPicker">
          <span class="avatar-edit__image" :style="avatarStyle(accountAvatar)">
            <img v-if="avatarSrc(accountAvatar)" :src="avatarSrc(accountAvatar)" alt="" />
            <i v-else class="ri-user-heart-line" aria-hidden="true"></i>
          </span>
          <span class="avatar-edit__label">点击更换</span>
        </button>
        <div class="settings-fields">
          <van-field v-model="accountDraft.displayName" label="显示名称" maxlength="100" placeholder="显示名称" />
          <van-field :model-value="accountEmail" label="邮箱" readonly placeholder="登录邮箱" />
        </div>
        <div class="account-actions">
          <button class="mine-row" type="button" @click="openEmailSheet">
            <span class="mine-row__text"><strong>更换邮箱</strong></span>
            <i class="ri-arrow-right-s-line" aria-hidden="true"></i>
          </button>
          <button class="mine-row" type="button" @click="router.push('/password')">
            <span class="mine-row__text"><strong>修改密码</strong></span>
            <i class="ri-arrow-right-s-line" aria-hidden="true"></i>
          </button>
        </div>
        <button class="primary-pill" type="button" @click="saveAccountProfile">
          <i class="ri-check-line" aria-hidden="true"></i>
          <span>保存账号资料</span>
        </button>
      </section>
    </van-popup>

    <van-popup v-model:show="emailSheetOpen" round position="bottom" :style="{ maxHeight: '72vh' }">
      <section class="settings-sheet">
        <header class="sheet-head">
          <h2>更换邮箱</h2>
          <button class="nav-icon" type="button" aria-label="关闭更换邮箱" @click="emailSheetOpen = false">
            <i class="ri-close-line" aria-hidden="true"></i>
          </button>
        </header>
        <div class="settings-fields email-fields">
          <van-field v-model="emailDraft.email" label="新邮箱" type="email" maxlength="200" placeholder="name@example.com" />
          <van-field v-model="emailDraft.code" label="验证码" maxlength="12" placeholder="邮箱验证码">
            <template #button>
              <van-button size="small" plain :loading="emailSending" @click="sendEmailCode">发送</van-button>
            </template>
          </van-field>
        </div>
        <button class="primary-pill" type="button" :disabled="emailSaving" @click="saveEmail">
          <van-loading v-if="emailSaving" size="16" color="#fff" />
          <span v-else>确认更换</span>
        </button>
      </section>
    </van-popup>

    <van-popup v-model:show="boxSettingsOpen" round position="bottom" :style="{ maxHeight: '82vh' }">
      <section class="settings-sheet">
        <header class="sheet-head">
          <h2>提问箱设置</h2>
          <button class="nav-icon" type="button" aria-label="关闭设置" @click="boxSettingsOpen = false">
            <i class="ri-close-line" aria-hidden="true"></i>
          </button>
        </header>
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

    <van-popup v-model:show="detailOpen" round position="bottom" :style="{ height: 'min(72vh, 560px)' }">
      <article v-if="selectedQuestion" class="detail-panel">
        <header class="detail-head">
          <div>
            <time>{{ questionTime(selectedQuestion) }}</time>
            <h2 class="detail-title">
              <span>{{ selectedIsPending ? '回答问题' : '问题详情' }}</span>
              <em v-if="selectedQuestion.topic" :title="selectedQuestion.topic.title">#{{ selectedQuestion.topic.title }}</em>
            </h2>
          </div>
          <button class="nav-icon" type="button" aria-label="关闭" @click="detailOpen = false">
            <i class="ri-close-line" aria-hidden="true"></i>
          </button>
        </header>
        <div class="detail-scroll">
          <p class="detail-question">{{ selectedQuestion.question }}</p>

          <section v-if="selectedIsPending" class="answer-editor">
            <van-field
              v-model="answerText"
              type="textarea"
              rows="6"
              maxlength="5000"
              autosize
              placeholder="写下你的回答。"
              :error-message="answerError"
            />
          </section>

          <section v-else-if="selectedIsPublished" class="published-answer">
            <span>公开回答</span>
            <p>{{ selectedQuestion.answer }}</p>
          </section>

          <section v-else class="published-answer muted">
            <span>已驳回</span>
            <p>这个问题不会显示在公开页。</p>
          </section>
        </div>

        <footer class="detail-actions">
          <van-button v-if="selectedCanDelete" round plain type="danger" @click="deleteSelected">删除</van-button>
          <template v-if="selectedIsPending">
            <span>{{ answerCount }}</span>
            <van-button round plain @click="dismissSelected">驳回</van-button>
            <van-button round type="primary" icon="guide-o" :disabled="!canPublish" @click="publishSelected">
              发布回答
            </van-button>
          </template>
        </footer>
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

.nav-icon,
.nav-profile {
  display: grid;
  place-items: center;
  border: 0;
  background: transparent;
  font: inherit;
}

.profile-avatar img,
.avatar-edit__image img,
.nav-profile img,
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

.nav-profile {
  width: 34px;
  height: 34px;
  padding: 0;
  border-radius: 50%;
}

.nav-profile span {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  overflow: hidden;
  border-radius: 50%;
  background-position: center;
  background-size: cover;
  color: var(--classic-primary);
  font-size: 16px;
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
  position: relative;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 46px);
  height: calc(100dvh - 46px);
  overflow: hidden;
  padding: 0;
}

.home-section {
  position: absolute;
  inset: 12px 14px calc(52px + env(safe-area-inset-bottom));
  overflow-x: hidden;
  overflow-y: auto;
  padding: 0 0 4px;
  overscroll-behavior: contain;
}

.public-section {
  min-height: 0;
}

.mine-section {
  display: grid;
  align-content: start;
  gap: 14px;
}

.profile-avatar {
  display: grid;
  place-items: center;
  width: 58px;
  height: 58px;
  flex: 0 0 auto;
  overflow: hidden;
  border-radius: 16px;
  background-position: center;
  background-size: cover;
  color: var(--classic-primary);
  font-size: 24px;
  box-shadow: 0 8px 20px rgba(31, 41, 55, 0.08);
}

.mine-identity,
.mine-row,
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

.mine-identity {
  align-items: center;
  justify-content: flex-start;
  gap: 13px;
  width: 100%;
  min-height: 92px;
  padding: 16px;
  text-align: left;
}

.mine-identity > i {
  flex: 0 0 auto;
  color: var(--classic-faint);
  font-size: 22px;
}

.mine-identity__body,
.mine-row__text {
  display: grid;
  min-width: 0;
}

.mine-identity__body {
  flex: 1;
  gap: 3px;
}

.mine-identity__body strong {
  overflow: hidden;
  color: var(--classic-text);
  font-size: 20px;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mine-identity__body small,
.mine-identity__body em,
.mine-row__text small {
  overflow: hidden;
  color: var(--classic-muted);
  font-size: 12px;
  font-style: normal;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mine-stat-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  overflow: hidden;
  border: 1px solid var(--classic-line);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.78);
}

.mine-stat-strip div {
  display: grid;
  gap: 3px;
  min-width: 0;
  padding: 12px 6px;
  text-align: center;
}

.mine-stat-strip div + div {
  border-left: 1px solid rgba(228, 231, 236, 0.72);
}

.mine-stat-strip strong {
  overflow: hidden;
  color: var(--classic-text);
  font-size: 20px;
  line-height: 1.15;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mine-stat-strip span {
  color: var(--classic-muted);
  font-size: 12px;
}

.mine-group {
  overflow: hidden;
  border: 1px solid var(--classic-line);
  border-radius: 12px;
  background: #fff;
}

.mine-group header {
  padding: 12px 14px 5px;
  color: var(--classic-muted);
  font-size: 12px;
  font-weight: 650;
}

.mine-row {
  align-items: center;
  justify-content: flex-start;
  gap: 11px;
  width: 100%;
  min-height: 56px;
  padding: 9px 12px;
  text-align: left;
  transition:
    background-color 160ms ease,
    transform 180ms var(--classic-spring);
}

.mine-row + .mine-row {
  border-top: 1px solid rgba(228, 231, 236, 0.76);
}

.mine-row:active {
  background: var(--classic-surface-soft);
  transform: scale(0.992);
}

.mine-row > i {
  flex: 0 0 auto;
  color: var(--classic-faint);
  font-size: 20px;
}

.mine-row__icon {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  border-radius: 10px;
  background: var(--classic-surface-soft);
  color: var(--classic-primary);
  font-size: 17px;
}

.mine-row__text {
  flex: 1;
  gap: 2px;
}

.mine-row__text strong {
  overflow: hidden;
  color: var(--classic-text);
  font-size: 14px;
  font-weight: 650;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout-button {
  width: 100%;
  min-height: 48px;
  border: 1px solid rgba(240, 68, 56, 0.22);
  border-radius: 12px;
  background: #fff;
  color: var(--classic-red);
  font: inherit;
  font-size: 15px;
  font-weight: 650;
  transition:
    background-color 160ms ease,
    transform 180ms var(--classic-spring);
}

.logout-button:active {
  background: rgba(240, 68, 56, 0.06);
  transform: scale(0.992);
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

.list-refresh {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.list-refresh:deep(.van-pull-refresh__track) {
  height: 100%;
}

.question-list {
  height: 100%;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 12px 0 4px;
  overscroll-behavior: contain;
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

.load-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 18px 0;
  color: var(--classic-muted);
  font-size: 13px;
}

.owner-tabbar {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 10;
  width: 100%;
  border-top: 1px solid rgba(226, 232, 240, 0.86);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -8px 24px rgba(31, 41, 55, 0.04);
  transform: none;
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
  justify-self: center;
  display: grid;
  gap: 8px;
  place-items: center;
  min-width: 88px;
  padding: 0;
  border: 0;
  background: transparent;
  font: inherit;
}

.avatar-edit__image {
  display: grid;
  place-items: center;
  width: 64px;
  height: 64px;
  overflow: hidden;
  border-radius: 50%;
  background-position: center;
  background-size: cover;
  color: var(--classic-primary);
  font-size: 24px;
  box-shadow: 0 8px 20px rgba(31, 41, 55, 0.08);
}

.avatar-edit__label {
  display: grid;
  place-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--classic-surface-soft);
  color: var(--classic-muted);
  font-size: 12px;
  line-height: 1;
}

.settings-fields {
  overflow: hidden;
  border: 1px solid var(--classic-line);
  border-radius: 12px;
}

.email-fields:deep(.van-field__control) {
  background: transparent;
  box-shadow: none;
  outline: none;
}

.email-fields:deep(.van-field__control:focus) {
  background: transparent;
}

.email-fields:deep(.van-field__control::selection) {
  background: rgba(47, 111, 237, 0.18);
}

.email-fields:deep(.van-field__control:-webkit-autofill) {
  -webkit-text-fill-color: var(--classic-text);
  box-shadow: 0 0 0 1000px #fff inset;
  transition: background-color 9999s ease-out;
}

.account-actions {
  overflow: hidden;
  border: 1px solid var(--classic-line);
  border-radius: 12px;
  background: #fff;
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

.primary-pill:disabled {
  opacity: 0.6;
}

.detail-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 18px 16px calc(14px + env(safe-area-inset-bottom));
}

.detail-head {
  display: flex;
  flex: 0 0 auto;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.detail-head h2 {
  margin: 4px 0 0;
  color: var(--classic-text);
  font-size: 18px;
}

.detail-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
}

.detail-title span {
  flex: 0 0 auto;
}

.detail-title em {
  min-width: 0;
  overflow: hidden;
  color: var(--classic-primary);
  font-size: 13px;
  font-style: normal;
  font-weight: 650;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-scroll {
  flex: 1;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 0 1px 14px;
  overscroll-behavior: contain;
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

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex: 0 0 auto;
  padding-top: 12px;
  border-top: 1px solid rgba(228, 231, 236, 0.82);
  color: var(--classic-muted);
  font-size: 12px;
}

.detail-actions > span {
  margin-right: auto;
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
