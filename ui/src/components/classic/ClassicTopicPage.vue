<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { useAuthStore } from '@/stores/auth'
import { getBoxProfile, getTopics, createTopic, closeTopic } from '@/api/owner'
import ClassicTopicList from '@/components/classic/ClassicTopicList.vue'

const router = useRouter()
const auth = useAuthStore()

const boxProfile = ref({ slug: '' })
const topics = ref([])
const loading = ref(true)
const saving = ref(false)
const topicStatusTab = ref('ACTIVE')
const createComposerOpen = ref(false)
const createComposerTouched = ref(false)
const selectedDuration = ref(24)
const draft = reactive({
  title: '',
  description: '',
  expiresAtLocal: '',
})

const activeLimit = computed(() => auth.user?.topicActiveLimit || 5)
const topicStatusTabs = [
  { id: 'ACTIVE', label: '进行中', empty: '还没有进行中的话题' },
  { id: 'EXPIRED', label: '已到期', empty: '还没有已到期的话题' },
  { id: 'CLOSED', label: '已关闭', empty: '还没有已关闭的话题' },
]
const activeTopics = computed(() => topics.value.filter((topic) => topic.available))
const filteredTopics = computed(() => topics.value.filter((topic) => topic.status === topicStatusTab.value))
const activeTopicStatusTab = computed(
  () => topicStatusTabs.find((tab) => tab.id === topicStatusTab.value) || topicStatusTabs[0],
)
const createLimitReached = computed(() => activeTopics.value.length >= activeLimit.value)
const canCreate = computed(
  () =>
    topicStatusTab.value === 'ACTIVE' &&
    !createLimitReached.value &&
    draft.title.trim().length > 0 &&
    !!expiresAtIso() &&
    !saving.value,
)

function pad(value) {
  return String(value).padStart(2, '0')
}

function toLocalInputValue(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

const minExpiresAtLocal = computed(() => toLocalInputValue(new Date(Date.now() + 60 * 1000)))
const maxExpiresAtLocal = computed(() => toLocalInputValue(new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)))

function setDuration(hours) {
  selectedDuration.value = hours
  const target = new Date(Date.now() + hours * 60 * 60 * 1000)
  draft.expiresAtLocal = toLocalInputValue(target)
}

function expiresAtIso() {
  if (!draft.expiresAtLocal) return null
  const date = new Date(draft.expiresAtLocal)
  return Number.isNaN(date.getTime()) ? null : date.toISOString()
}

function resetDraft() {
  draft.title = ''
  draft.description = ''
  setDuration(24)
}

function openCreateComposer() {
  if (topicStatusTab.value !== 'ACTIVE' || createLimitReached.value) return
  resetDraft()
  createComposerTouched.value = true
  createComposerOpen.value = true
}

function closeCreateComposer({ force = false } = {}) {
  if (saving.value && !force) return
  createComposerTouched.value = true
  createComposerOpen.value = false
}

async function load() {
  loading.value = true
  try {
    const [profile, rows] = await Promise.all([getBoxProfile(), getTopics(), auth.ensureUser()])
    boxProfile.value = { slug: profile?.slug || '' }
    topics.value = rows || []
  } finally {
    loading.value = false
  }
}

async function saveTopic() {
  if (!canCreate.value) return
  const expiresAt = expiresAtIso()
  const expiresDate = new Date(expiresAt)
  if (expiresDate.getTime() <= Date.now()) {
    showToast('到期时间必须晚于当前时间')
    return
  }
  if (expiresDate.getTime() > Date.now() + 7 * 24 * 60 * 60 * 1000) {
    showToast('话题最长不能超过7天')
    return
  }
  saving.value = true
  try {
    await createTopic({
      title: draft.title.trim(),
      description: draft.description.trim(),
      expiresAt,
    })
    topics.value = await getTopics()
    closeCreateComposer({ force: true })
    showToast('话题已创建')
  } catch (error) {
    showToast(error?.message || '创建失败')
  } finally {
    saving.value = false
  }
}

function topicUrl(topic) {
  return `${window.location.origin}/box/${boxProfile.value.slug}?topic=${topic.code}`
}

async function copyTopicUrl(topic) {
  if (topic.status !== 'ACTIVE') return
  const url = topicUrl(topic)
  try {
    await navigator.clipboard?.writeText(url)
    showToast('话题链接已复制')
  } catch {
    showToast(url)
  }
}

async function closeOwnerTopic(topic) {
  try {
    await showConfirmDialog({
      title: '关闭这个话题？',
      message: '关闭后新问题不能再关联到这个话题，历史问题会保留标签。',
      confirmButtonText: '关闭',
      confirmButtonColor: '#f04438',
    })
  } catch {
    return
  }
  await closeTopic(topic.id)
  topics.value = await getTopics()
  showToast('话题已关闭')
}

onMounted(async () => {
  setDuration(24)
  await load()
})

watch(topicStatusTab, (status) => {
  if (status !== 'ACTIVE') closeCreateComposer()
})
</script>

<template>
  <main class="classic-topic-page">
    <van-nav-bar title="话题管理" left-arrow fixed placeholder safe-area-inset-top @click-left="router.push('/home')" />

    <van-tabs v-model:active="topicStatusTab" animated swipeable class="topic-tabs">
      <van-tab v-for="tab in topicStatusTabs" :key="tab.id" :name="tab.id" :title="tab.label" />
    </van-tabs>

    <section class="topic-list" :class="{ 'has-create-composer': topicStatusTab === 'ACTIVE' }" aria-label="话题列表">
      <ClassicTopicList
        :topics="filteredTopics"
        :loading="loading"
        :empty-description="activeTopicStatusTab.empty"
        @copy="copyTopicUrl"
        @close="closeOwnerTopic"
      />
    </section>

    <section
      v-if="topicStatusTab === 'ACTIVE'"
      class="topic-composer-morph"
      :class="{ open: createComposerOpen, touched: createComposerTouched }"
      aria-label="创建话题"
    >
      <div class="topic-composer-surface" :aria-hidden="!createComposerOpen" :inert="!createComposerOpen">
        <header class="topic-composer-head topic-composer-piece" style="--piece-delay: 70ms">
          <div>
            <h2>创建话题</h2>
          </div>
          <button
            class="topic-composer-close"
            type="button"
            aria-label="关闭创建面板"
            :disabled="saving"
            @click="closeCreateComposer"
          >
            <i class="ri-close-line" aria-hidden="true"></i>
          </button>
        </header>

        <div class="topic-create topic-composer-piece" style="--piece-delay: 96ms">
          <van-field v-model="draft.title" label="标题" maxlength="24" show-word-limit placeholder="例如：今天吃什么？" />
          <van-field v-model="draft.description" label="简介" type="textarea" rows="2" maxlength="500" autosize placeholder="给参与者一点上下文" />
          <div class="time-block">
            <span>到期时间</span>
            <div class="quick-actions" aria-label="快捷到期时间">
              <button type="button" :class="{ active: selectedDuration === 8 }" @click="setDuration(8)">8小时</button>
              <button type="button" :class="{ active: selectedDuration === 12 }" @click="setDuration(12)">12小时</button>
              <button type="button" :class="{ active: selectedDuration === 24 }" @click="setDuration(24)">24小时</button>
            </div>
            <input
              v-model="draft.expiresAtLocal"
              type="datetime-local"
              :min="minExpiresAtLocal"
              :max="maxExpiresAtLocal"
              aria-label="到期时间"
            />
          </div>
        </div>
      </div>

      <span class="topic-composer-count">{{ activeTopics.length }} / {{ activeLimit }}</span>
      <button
        class="topic-composer-action"
        type="button"
        :disabled="createComposerOpen ? !canCreate : createLimitReached"
        :aria-expanded="createComposerOpen"
        :aria-label="createComposerOpen ? '确认创建话题' : '创建话题'"
        @click="createComposerOpen ? saveTopic() : openCreateComposer()"
      >
        <span class="topic-composer-glint" aria-hidden="true"></span>
        <span class="topic-composer-icon" aria-hidden="true">
          <van-loading v-if="saving" size="16" color="#fff" />
          <i v-else :class="createComposerOpen ? 'ri-check-line' : 'ri-add-line'"></i>
        </span>
        <span class="topic-composer-text">
          {{ createLimitReached && !createComposerOpen ? '已达上限' : createComposerOpen ? '确认创建' : '创建话题' }}
        </span>
      </button>
    </section>
  </main>
</template>

<style scoped>
:global(html),
:global(body),
:global(#app) {
  height: 100%;
}

.classic-topic-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  background:
    radial-gradient(circle at top left, rgba(47, 111, 237, 0.08), transparent 34%),
    var(--classic-bg, #f6f4ef);
}

.topic-tabs {
  flex: 0 0 auto;
  margin: 14px 16px 10px;
  overflow: hidden;
  border: 1px solid var(--classic-line);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.92);
}

.topic-tabs :deep(.van-tabs__wrap) {
  height: 42px;
}

.topic-tabs :deep(.van-tab) {
  color: var(--classic-muted);
  font-size: 13px;
  font-weight: 650;
}

.topic-tabs :deep(.van-tab--active) {
  color: var(--classic-primary);
}

.topic-tabs :deep(.van-tabs__line) {
  width: 24px;
  background: var(--classic-primary);
}

.time-block small {
  color: var(--classic-muted);
  font-size: 12px;
  line-height: 1.5;
}

.topic-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0 16px calc(120px + env(safe-area-inset-bottom));
}

.topic-list.has-create-composer {
  padding-bottom: calc(120px + env(safe-area-inset-bottom));
}

.quick-actions button {
  min-height: 34px;
  border: 1px solid var(--classic-line);
  border-radius: 17px;
  background: #fff;
  color: var(--classic-text);
  font: inherit;
  font-size: 13px;
}

.topic-composer-morph {
  --topic-composer-open-height: 446px;
  --topic-composer-surface-height: 386px;
  position: fixed;
  left: 50%;
  bottom: calc(18px + env(safe-area-inset-bottom));
  z-index: 20;
  width: 150px;
  height: 48px;
  overflow: visible;
  border-radius: 24px;
  color: var(--classic-text);
  transform: translateX(-50%);
  transform-origin: 50% 100%;
  pointer-events: none;
  will-change: width, height;
}

.topic-composer-morph.open {
  width: min(calc(100vw - 32px), 520px);
  height: var(--topic-composer-open-height);
  animation: topic-composer-shell-open 540ms linear both;
}

.topic-composer-morph.touched:not(.open) {
  animation: topic-composer-shell-close 280ms linear both;
}

.topic-composer-surface {
  position: absolute;
  right: 0;
  bottom: 60px;
  left: 0;
  z-index: 1;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 10px;
  height: var(--topic-composer-surface-height);
  overflow: hidden;
  padding: 14px;
  border: 1px solid rgba(226, 232, 240, 0.94);
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(248, 250, 252, 0.98)),
    #fff;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.16);
  clip-path: inset(100% 42% 0 42% round 24px);
  opacity: 0;
  pointer-events: none;
  transform: translate3d(0, 12px, 0) scale(0.94);
  transform-origin: 50% 100%;
  will-change: transform, opacity, clip-path;
}

.topic-composer-surface::before {
  content: '';
  position: absolute;
  right: 18px;
  bottom: -7px;
  width: 18px;
  height: 18px;
  border-right: 1px solid rgba(226, 232, 240, 0.9);
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
  background: #f8fafc;
  transform: rotate(45deg);
  pointer-events: none;
}

.topic-composer-surface::after {
  content: '';
  position: absolute;
  inset: 1px;
  border-radius: 21px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.72), rgba(255, 255, 255, 0));
  opacity: 0;
  pointer-events: none;
}

.topic-composer-morph.open .topic-composer-surface {
  opacity: 1;
  pointer-events: auto;
  animation: topic-composer-surface-open 540ms linear both;
}

.topic-composer-morph.open .topic-composer-surface::after {
  animation: topic-composer-highlight-in 420ms ease-out both;
}

.topic-composer-morph.touched:not(.open) .topic-composer-surface {
  animation: topic-composer-surface-close 260ms linear both;
}

.topic-composer-head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.topic-composer-head div {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.topic-composer-head h2,
.topic-composer-head p {
  margin: 0;
}

.topic-composer-head h2 {
  color: var(--classic-text);
  font-size: 16px;
  font-weight: 760;
}

.topic-composer-head p {
  color: var(--classic-muted);
  font-size: 12px;
  line-height: 1.4;
}

.topic-composer-close {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 17px;
  background: var(--classic-surface-soft);
  color: var(--classic-muted);
  font-size: 18px;
}

.topic-composer-close:disabled {
  opacity: 0.48;
}

.topic-composer-piece {
  opacity: 0;
}

.topic-composer-morph.open .topic-composer-piece {
  animation: topic-composer-piece-in 320ms cubic-bezier(0.17, 0.9, 0.22, 1.16) both;
  animation-delay: var(--piece-delay, 80ms);
}

.topic-composer-morph:not(.open) .topic-composer-piece {
  opacity: 0;
}

.topic-composer-action {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 48px;
  overflow: hidden;
  border: 0;
  border-radius: 24px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.14), rgba(255, 255, 255, 0)),
    var(--classic-primary);
  color: #fff;
  box-shadow: 0 12px 28px rgba(47, 111, 237, 0.24);
  cursor: pointer;
  font: inherit;
  font-size: 15px;
  font-weight: 680;
  pointer-events: auto;
  transition:
    background-color 180ms ease,
    box-shadow 220ms ease,
    opacity 180ms ease;
  will-change: box-shadow;
}

.topic-composer-count {
  position: absolute;
  right: 0;
  bottom: 56px;
  left: 0;
  z-index: 3;
  display: inline-flex;
  justify-content: center;
  color: var(--classic-muted);
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  pointer-events: none;
}

.topic-composer-morph.open .topic-composer-count {
  opacity: 0;
}

.topic-composer-action:disabled {
  cursor: not-allowed;
  opacity: 0.58;
  box-shadow: 0 10px 22px rgba(47, 111, 237, 0.16);
}

.topic-composer-action:not(:disabled):active {
  transform: translate3d(0, 1px, 0) scale(0.992);
}

.topic-composer-morph.open .topic-composer-action {
  box-shadow: 0 14px 34px rgba(47, 111, 237, 0.22);
  animation: topic-composer-action-open 540ms linear both;
}

.topic-composer-morph.touched:not(.open) .topic-composer-action {
  animation: topic-composer-action-close 260ms linear both;
}

.topic-composer-glint {
  position: absolute;
  inset: -1px;
  background: linear-gradient(100deg, transparent 12%, rgba(255, 255, 255, 0.28) 44%, transparent 68%);
  opacity: 0;
  transform: translate3d(-45%, 0, 0);
  pointer-events: none;
}

.topic-composer-morph.open .topic-composer-glint {
  animation: topic-composer-button-glint 620ms ease-out 80ms both;
}

.topic-composer-icon,
.topic-composer-text {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
}

.topic-composer-icon {
  justify-content: center;
  width: 18px;
  height: 18px;
  font-size: 17px;
}

.topic-composer-text {
  animation: topic-composer-label-swap 180ms ease-out both;
}

.topic-create {
  display: grid;
  gap: 12px;
  min-height: 0;
  overflow-y: auto;
  padding: 0 2px 2px;
}

.topic-create :deep(.van-cell) {
  border: 1px solid var(--classic-line);
  border-radius: 12px;
  background: #fff;
  padding: 10px 12px;
}

.topic-create :deep(.van-cell::after) {
  display: none;
}

.time-block {
  display: grid;
  gap: 9px;
  border: 1px solid var(--classic-line);
  border-radius: 12px;
  padding: 12px;
}

.time-block > span {
  color: var(--classic-muted);
  font-size: 13px;
  font-weight: 650;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.quick-actions button.active {
  border-color: rgba(47, 111, 237, 0.24);
  background: rgba(47, 111, 237, 0.1);
  color: var(--classic-primary);
}

.time-block input {
  min-width: 0;
  min-height: 40px;
  width: 100%;
  border: 1px solid var(--classic-line);
  border-radius: 10px;
  padding: 0 10px;
  background: #fff;
  color: var(--classic-text);
  font: inherit;
  font-size: 14px;
}

@keyframes topic-composer-shell-open {
  0% {
    width: 150px;
    height: 48px;
  }
  28% {
    width: min(calc(100vw - 116px), 336px);
    height: 48px;
  }
  54% {
    width: min(calc(100vw - 24px), 532px);
    height: calc(var(--topic-composer-open-height) - 32px);
  }
  72% {
    width: min(calc(100vw - 38px), 512px);
    height: calc(var(--topic-composer-open-height) + 4px);
  }
  88% {
    width: min(calc(100vw - 30px), 522px);
    height: calc(var(--topic-composer-open-height) - 2px);
  }
  100% {
    width: min(calc(100vw - 32px), 520px);
    height: var(--topic-composer-open-height);
  }
}

@keyframes topic-composer-shell-close {
  0% {
    width: min(calc(100vw - 32px), 520px);
    height: var(--topic-composer-open-height);
  }
  42% {
    width: min(calc(100vw - 74px), 384px);
    height: 76px;
  }
  72% {
    width: 174px;
    height: 48px;
  }
  100% {
    width: 150px;
    height: 48px;
  }
}

@keyframes topic-composer-surface-open {
  0% {
    clip-path: inset(100% 42% 0 42% round 24px);
    opacity: 0;
    transform: translate3d(0, 12px, 0) scale(0.94);
  }
  22% {
    clip-path: inset(100% 22% 0 22% round 24px);
    opacity: 0.18;
    transform: translate3d(0, 8px, 0) scale(0.97);
  }
  52% {
    clip-path: inset(12% 0 0 0 round 22px);
    opacity: 1;
    transform: translate3d(0, -3px, 0) scale(1.01);
  }
  72% {
    clip-path: inset(0 0 0 0 round 22px);
    opacity: 1;
    transform: translate3d(0, 1px, 0) scale(0.997);
  }
  100% {
    clip-path: inset(0 0 0 0 round 22px);
    opacity: 1;
    transform: translate3d(0, 0, 0) scale(1);
  }
}

@keyframes topic-composer-surface-close {
  0% {
    clip-path: inset(0 0 0 0 round 22px);
    opacity: 1;
    transform: translate3d(0, 0, 0) scale(1);
  }
  46% {
    clip-path: inset(72% 10% 0 10% round 24px);
    opacity: 0.42;
    transform: translate3d(0, 8px, 0) scale(0.97);
  }
  100% {
    clip-path: inset(100% 42% 0 42% round 24px);
    opacity: 0;
    transform: translate3d(0, 12px, 0) scale(0.94);
  }
}

@keyframes topic-composer-action-open {
  0% {
    border-radius: 24px;
    transform: scaleX(1);
  }
  34% {
    border-radius: 24px;
    transform: scaleX(1.025);
  }
  64% {
    border-radius: 24px;
    transform: scaleX(0.994);
  }
  100% {
    border-radius: 24px;
    transform: scaleX(1);
  }
}

@keyframes topic-composer-action-close {
  0% {
    transform: scaleX(1);
  }
  62% {
    transform: scaleX(0.978);
  }
  100% {
    transform: scaleX(1);
  }
}

@keyframes topic-composer-label-swap {
  0% {
    opacity: 0;
    transform: translate3d(0, 4px, 0) scale(0.94);
  }
  100% {
    opacity: 1;
    transform: translate3d(0, 0, 0) scale(1);
  }
}

@keyframes topic-composer-button-glint {
  0% {
    opacity: 0;
    transform: translate3d(-55%, 0, 0);
  }
  28% {
    opacity: 1;
  }
  100% {
    opacity: 0;
    transform: translate3d(70%, 0, 0);
  }
}

@keyframes topic-composer-highlight-in {
  0% {
    opacity: 0.68;
  }
  100% {
    opacity: 0;
  }
}

@keyframes topic-composer-piece-in {
  0% {
    opacity: 0;
    transform: translate3d(0, 7px, 0) scale(0.985);
  }
  72% {
    opacity: 1;
    transform: translate3d(0, -1px, 0) scale(1.004);
  }
  100% {
    opacity: 1;
    transform: translate3d(0, 0, 0);
  }
}

@media (max-width: 380px) {
  .topic-composer-morph {
    --topic-composer-open-height: 464px;
    --topic-composer-surface-height: 404px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .topic-composer-morph,
  .topic-composer-morph.open,
  .topic-composer-morph.touched:not(.open),
  .topic-composer-surface,
  .topic-composer-morph.open .topic-composer-surface,
  .topic-composer-morph.touched:not(.open) .topic-composer-surface,
  .topic-composer-action,
  .topic-composer-morph.open .topic-composer-action,
  .topic-composer-morph.touched:not(.open) .topic-composer-action,
  .topic-composer-morph.open .topic-composer-piece,
  .topic-composer-glint,
  .topic-composer-icon,
  .topic-composer-text,
  .topic-composer-count {
    animation: none;
    transition: none;
  }

  .topic-composer-morph.open {
    width: min(calc(100vw - 32px), 520px);
    height: var(--topic-composer-open-height);
  }

  .topic-composer-morph.open .topic-composer-surface {
    clip-path: inset(0 0 0 0 round 22px);
    opacity: 1;
    transform: none;
  }

  .topic-composer-morph.open .topic-composer-piece {
    opacity: 1;
  }
}
</style>
