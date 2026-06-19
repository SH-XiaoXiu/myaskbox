<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { showToast } from 'vant'
import {
  getAnonymousAvatars,
  getPublicBoxProfile,
  getPublishedQA,
  submitQuestion,
} from '@/api/public'
import { formatTime } from '@/utils'

const route = useRoute()
const props = defineProps({
  slugOverride: {
    type: String,
    default: '',
  },
  embedded: {
    type: Boolean,
    default: false,
  },
  showComposer: {
    type: Boolean,
    default: true,
  },
  refreshKey: {
    type: [Number, String],
    default: 0,
  },
})

const routeSlug = computed(() => route.params.slug || 'xiaoxiu')
const slug = computed(() => props.slugOverride || routeSlug.value)

const boxProfile = ref({ slug: '', displayName: '', description: '', avatar: null })
const avatarList = ref([])
const selectedAvatar = ref(null)
const publishedQA = ref([])
const qaLoading = ref(false)
const qaPage = ref(1)
const qaHasMore = ref(false)
const composerOpen = ref(false)
const composerTouched = ref(false)
const detailOpen = ref(false)
const selectedQA = ref(null)
const content = ref('')
const sending = ref(false)
const receiptMessage = ref('')
const listRef = ref(null)
const ownerAvatarRef = ref(null)
const sendButtonRef = ref(null)
const avatarPickerRef = ref(null)

let receiptTimer = 0
let avatarSnapTimer = 0
let flyingFrame = 0
let activeFlyer = null

const boxTitle = computed(() => boxProfile.value.displayName?.trim() || 'AskBox')
const boxDescription = computed(() => boxProfile.value.description?.trim() || '匿名提问和公开回复')
const canSend = computed(() => content.value.trim().length > 0 && !sending.value && selectedAvatar.value)
const avatarLoopOptions = computed(() => {
  if (!avatarList.value.length) return []
  return Array.from({ length: 3 }, (_, cycle) =>
    avatarList.value.map((avatar, index) => ({
      ...avatar,
      cycle,
      index,
      key: `${cycle}-${avatar.id ?? index}`,
    })),
  ).flat()
})
const selectedAvatarIndex = computed(() => {
  const index = avatarList.value.findIndex((avatar) => avatar.id === selectedAvatar.value?.id)
  return index >= 0 ? index : 0
})

function toAvatarOption(a) {
  return { id: a.id, name: a.name, contentBase64: a.contentBase64, bg: a.bg }
}

function avatarSrc(avatar) {
  if (!avatar) return ''
  return typeof avatar === 'string' ? avatar : avatar.contentBase64 || ''
}

function avatarStyle(avatar, fallbackBg = '#eff6ff') {
  return {
    backgroundColor: avatar?.bg || fallbackBg,
  }
}

async function loadBoxProfile() {
  try {
    const profile = await getPublicBoxProfile(slug.value)
    boxProfile.value = {
      slug: profile.slug || slug.value,
      displayName: profile.displayName || '',
      description: profile.description || '',
      avatar: profile.avatar || null,
      background: profile.background || null,
    }
  } catch (err) {
    boxProfile.value = { slug: slug.value, displayName: '', description: '' }
    console.error('Failed to load box profile', err)
  }
}

async function loadAvatars() {
  try {
    const list = await getAnonymousAvatars()
    avatarList.value = list.map(toAvatarOption)
    selectedAvatar.value = avatarList.value[0] || null
    nextTick(() => centerAvatar(selectedAvatarIndex.value, 'auto'))
  } catch (err) {
    console.error('Failed to load avatars', err)
  }
}

function centerAvatar(index, behavior = 'smooth', targetEl = null) {
  const target =
    targetEl ||
    avatarPickerRef.value?.querySelector(
      `[data-avatar-cycle="1"][data-avatar-index="${index}"]`,
    )
  target?.scrollIntoView({ behavior, block: 'nearest', inline: 'center' })
}

function getAvatarCycleWidth() {
  const picker = avatarPickerRef.value
  const first = picker?.querySelector('[data-avatar-cycle="0"][data-avatar-index="0"]')
  const second = picker?.querySelector('[data-avatar-cycle="1"][data-avatar-index="0"]')
  if (!first || !second) return 0
  return second.offsetLeft - first.offsetLeft
}

function recenterAvatarLoop() {
  const picker = avatarPickerRef.value
  const cycleWidth = getAvatarCycleWidth()
  if (!picker || !cycleWidth) return
  let nextScrollLeft = picker.scrollLeft
  while (nextScrollLeft < cycleWidth * 0.5) nextScrollLeft += cycleWidth
  while (nextScrollLeft > cycleWidth * 1.5) nextScrollLeft -= cycleWidth
  if (Math.abs(nextScrollLeft - picker.scrollLeft) < 1) return
  const previousBehavior = picker.style.scrollBehavior
  picker.style.scrollBehavior = 'auto'
  picker.scrollLeft = nextScrollLeft
  picker.style.scrollBehavior = previousBehavior
}

function selectAvatarAt(index, event) {
  selectedAvatar.value = avatarList.value[index]
  centerAvatar(index, 'smooth', event?.currentTarget)
}

function updateSelectedAvatarFromCenter() {
  const picker = avatarPickerRef.value
  if (!picker || !avatarList.value.length) return
  const pickerRect = picker.getBoundingClientRect()
  const centerX = pickerRect.left + pickerRect.width / 2
  let nextIndex = selectedAvatarIndex.value
  let nearestDistance = Infinity
  for (const button of picker.querySelectorAll('.avatar-option')) {
    const rect = button.getBoundingClientRect()
    const distance = Math.abs(rect.left + rect.width / 2 - centerX)
    if (distance < nearestDistance) {
      nearestDistance = distance
      nextIndex = Number(button.dataset.avatarIndex)
    }
  }
  if (nextIndex !== selectedAvatarIndex.value) selectedAvatar.value = avatarList.value[nextIndex]
}

function handleAvatarScroll() {
  window.clearTimeout(avatarSnapTimer)
  avatarSnapTimer = window.setTimeout(() => {
    recenterAvatarLoop()
    updateSelectedAvatarFromCenter()
    centerAvatar(selectedAvatarIndex.value, 'smooth')
  }, 110)
}

function handleAvatarWheel(event) {
  const picker = avatarPickerRef.value
  if (!picker || Math.abs(event.deltaX) > Math.abs(event.deltaY)) return
  event.preventDefault()
  picker.scrollLeft += event.deltaY
}

async function animatePaperPlane() {
  const from = sendButtonRef.value?.getBoundingClientRect?.()
  const to = ownerAvatarRef.value?.getBoundingClientRect()
  if (!from || !to || window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches) return

  window.cancelAnimationFrame(flyingFrame)
  activeFlyer?.remove()

  const flyer = document.createElement('i')
  flyer.className = 'ri-send-plane-fill classic-paper-flyer'
  document.body.appendChild(flyer)
  activeFlyer = flyer

  const startX = from.left + from.width / 2 - 11
  const startY = from.top + from.height / 2 - 11
  const endX = to.left + to.width / 2 - 11
  const endY = to.top + to.height / 2 - 11
  const curveHeight = Math.min(88, Math.max(44, Math.abs(startY - endY) * 0.22))
  const duration = 680
  const startedAt = performance.now()

  flyer.style.left = `${startX}px`
  flyer.style.top = `${startY}px`

  await new Promise((resolve) => {
    function finish() {
      flyer.remove()
      if (activeFlyer === flyer) activeFlyer = null
      ownerAvatarRef.value?.classList.remove('is-catching')
      void ownerAvatarRef.value?.offsetWidth
      ownerAvatarRef.value?.classList.add('is-catching')
      resolve()
    }

    function tick(now) {
      const progress = Math.min((now - startedAt) / duration, 1)
      const eased = 1 - Math.pow(1 - progress, 4)
      const arc = Math.sin(progress * Math.PI) * curveHeight
      const x = startX + (endX - startX) * eased
      const y = startY + (endY - startY) * eased - arc
      const scale = progress < 0.22 ? 0.82 + progress * 1.45 : 1 - Math.max(0, progress - 0.5) * 0.56
      const opacity = progress > 0.64 ? 1 - (progress - 0.64) / 0.36 : 1
      const rotate = -12 - progress * 34

      flyer.style.transform = `translate3d(${x - startX}px, ${y - startY}px, 0) rotate(${rotate}deg) scale(${scale})`
      flyer.style.opacity = `${Math.max(0, opacity)}`

      if (progress < 1) {
        flyingFrame = window.requestAnimationFrame(tick)
      } else {
        finish()
      }
    }

    flyingFrame = window.requestAnimationFrame(tick)
  })
}

async function loadPublishedQA(reset = false) {
  if (qaLoading.value) return
  qaLoading.value = true
  const page = reset ? 1 : qaPage.value
  try {
    const result = await getPublishedQA(slug.value, page, 10)
    const items = result.records.map((q) => ({
      id: q.id,
      profile: { name: q.avatar?.name, contentBase64: q.avatar?.contentBase64, bg: q.avatar?.bg },
      ownerAvatar: q.ownerAvatar,
      question: q.question,
      answer: q.answer,
      ts: q.ts,
      time: formatTime(q.ts),
    }))
    publishedQA.value = reset ? items : [...publishedQA.value, ...items]
    qaPage.value = page + 1
    qaHasMore.value = page < result.totalPages
  } catch (err) {
    console.error('Failed to load QA', err)
  } finally {
    qaLoading.value = false
  }
}

function handleScroll() {
  const list = listRef.value
  if (!list || qaLoading.value || !qaHasMore.value) return
  const distance = list.scrollHeight - list.scrollTop - list.clientHeight
  if (distance < 220) loadPublishedQA(false)
}

function openDetail(qa) {
  selectedQA.value = qa
  detailOpen.value = true
}

function openComposer() {
  if (!props.showComposer) return
  composerTouched.value = true
  composerOpen.value = true
  nextTick(() => {
    centerAvatar(selectedAvatarIndex.value, 'auto')
  })
}

function closeComposer(force = false) {
  if (sending.value && !force) return
  composerOpen.value = false
}

function showReceipt(message) {
  window.clearTimeout(receiptTimer)
  receiptMessage.value = message
  receiptTimer = window.setTimeout(() => {
    receiptMessage.value = ''
  }, 2200)
}

async function handleSend() {
  const text = content.value.trim()
  if (!props.showComposer || !text || sending.value || !selectedAvatar.value) return
  sending.value = true
  try {
    await animatePaperPlane()
    await submitQuestion(slug.value, selectedAvatar.value.id, text)
    content.value = ''
    closeComposer(true)
    showReceipt('已投递，等待回答')
    await loadPublishedQA(true)
  } catch (err) {
    showToast(err?.message || '投递失败，请稍后再试')
  } finally {
    sending.value = false
  }
}

async function resetAndLoadPublicContent() {
  if (qaLoading.value) return
  selectedQA.value = null
  detailOpen.value = false
  composerOpen.value = false
  composerTouched.value = false
  content.value = ''
  publishedQA.value = []
  qaPage.value = 1
  qaHasMore.value = false
  await Promise.all([loadBoxProfile(), loadPublishedQA(true)])
}

defineExpose({
  refreshPublicContent: resetAndLoadPublicContent,
})

watch(slug, async () => {
  await resetAndLoadPublicContent()
})

watch(
  () => props.refreshKey,
  async () => {
    await resetAndLoadPublicContent()
  },
)

watch(
  () => props.showComposer,
  async (show) => {
    if (show && !avatarList.value.length) await loadAvatars()
  },
)

onMounted(async () => {
  const jobs = [loadBoxProfile(), loadPublishedQA(true)]
  if (props.showComposer) jobs.unshift(loadAvatars())
  await Promise.all(jobs)
})

onBeforeUnmount(() => {
  window.cancelAnimationFrame(flyingFrame)
  activeFlyer?.remove()
  window.clearTimeout(receiptTimer)
  window.clearTimeout(avatarSnapTimer)
})
</script>

<template>
  <main class="classic-ask classic-page classic-enter" :class="{ 'is-embedded': embedded, 'has-composer': showComposer }">
    <header class="ask-head">
      <div class="ask-profile">
        <span ref="ownerAvatarRef" class="ask-avatar" :style="avatarStyle(boxProfile.avatar)">
          <img v-if="avatarSrc(boxProfile.avatar)" :src="avatarSrc(boxProfile.avatar)" alt="" />
          <i v-else class="ri-question-answer-line" aria-hidden="true"></i>
        </span>
        <div>
          <h1>{{ boxTitle }}</h1>
          <p>{{ boxDescription }}</p>
        </div>
      </div>
      <p class="ask-meta">{{ publishedQA.length }} 条公开回复</p>
    </header>

    <section ref="listRef" class="ask-list" aria-label="公开回复列表" @scroll="handleScroll">
      <TransitionGroup name="classic-list" tag="div" class="ask-list__inner">
        <article
          v-for="(qa, index) in publishedQA"
          :key="qa.id"
          class="classic-card classic-press qa-card"
          role="button"
          tabindex="0"
          :style="{ '--card-index': index }"
          @click="openDetail(qa)"
          @keydown.enter.prevent="openDetail(qa)"
          @keydown.space.prevent="openDetail(qa)"
        >
          <header class="qa-card-head">
            <span class="qa-identity">
              <span class="mini-avatar" :style="avatarStyle(qa.profile)">
                <img v-if="avatarSrc(qa.profile)" :src="avatarSrc(qa.profile)" alt="" />
              </span>
            </span>
            <time>{{ qa.time }}</time>
          </header>
          <div class="qa-body">
            <p class="qa-question">{{ qa.question }}</p>
            <p class="qa-answer" :class="{ muted: !qa.answer }">
              <span
                v-if="qa.answer"
                class="mini-avatar owner qa-answer-avatar"
                :style="avatarStyle(qa.ownerAvatar || boxProfile.avatar)"
              >
                <img
                  v-if="avatarSrc(qa.ownerAvatar || boxProfile.avatar)"
                  :src="avatarSrc(qa.ownerAvatar || boxProfile.avatar)"
                  alt=""
                />
                <i v-else class="ri-question-answer-line" aria-hidden="true"></i>
              </span>
              <span class="qa-answer-text">{{ qa.answer || '还在等待回答' }}</span>
            </p>
          </div>
        </article>
      </TransitionGroup>

      <van-empty v-if="!qaLoading && publishedQA.length === 0" image="search" description="暂时还没有公开回答" />
      <div v-if="qaLoading || qaHasMore" class="load-state" aria-live="polite">
        <van-loading v-if="qaLoading" size="18" />
        <span>{{ qaLoading ? '加载中' : '继续下滑加载更多' }}</span>
      </div>
    </section>

    <Transition v-if="showComposer" name="receipt">
      <div v-if="receiptMessage" class="ask-receipt" role="status">
        <i class="ri-check-line" aria-hidden="true"></i>
        <span>{{ receiptMessage }}</span>
      </div>
    </Transition>

    <section
      v-if="showComposer"
      class="composer-morph"
      :class="{ open: composerOpen, touched: composerTouched }"
      aria-label="匿名提问输入框"
    >
      <div class="composer-surface" :aria-hidden="!composerOpen" :inert="!composerOpen">
        <header class="panel-head composer-piece" style="--piece-delay: 80ms">
          <div>
            <h2>匿名问一个问题</h2>
          </div>
          <button
            class="composer-close"
            type="button"
            aria-label="关闭提问框"
            :disabled="sending"
            @click="closeComposer()"
          >
            <i class="ri-close-line" aria-hidden="true"></i>
          </button>
        </header>

        <div class="composer-row composer-piece" style="--piece-delay: 102ms">
          <div
            ref="avatarPickerRef"
            class="avatar-row"
            aria-label="选择匿名头像"
            @scroll="handleAvatarScroll"
            @wheel="handleAvatarWheel"
          >
            <button
              v-for="avatar in avatarLoopOptions"
              :key="avatar.key"
              class="avatar-option classic-press"
              :class="{ selected: selectedAvatar?.id === avatar.id }"
              :style="avatarStyle(avatar)"
              :data-avatar-cycle="avatar.cycle"
              :data-avatar-index="avatar.index"
              type="button"
              :aria-pressed="selectedAvatar?.id === avatar.id"
              @click="selectAvatarAt(avatar.index, $event)"
            >
              <img v-if="avatarSrc(avatar)" :src="avatarSrc(avatar)" alt="" />
            </button>
          </div>
        </div>

        <div class="composer-field-wrap composer-piece" style="--piece-delay: 124ms">
          <van-field
            v-model="content"
            class="composer-field"
            type="textarea"
            rows="5"
            maxlength="350"
            autosize
            placeholder="写下你的问题"
          />
          <p class="composer-count">{{ content.length }} / 350</p>
        </div>
      </div>

      <button
        ref="sendButtonRef"
        class="composer-action-button"
        :class="{ sending }"
        type="button"
        :disabled="composerOpen && !canSend"
        :aria-expanded="composerOpen"
        :aria-label="composerOpen ? '投递问题' : '匿名提问'"
        @click="composerOpen ? handleSend() : openComposer()"
      >
        <span class="composer-button-glint" aria-hidden="true"></span>
        <span class="composer-button-icon" aria-hidden="true">
          <van-loading v-if="composerOpen && sending" size="16" color="#fff" />
          <i v-else :class="composerOpen ? 'ri-send-plane-fill' : 'ri-pencil-line'"></i>
        </span>
        <span class="composer-button-text">{{ composerOpen ? '投递问题' : '匿名提问' }}</span>
      </button>
    </section>

    <van-popup
      v-model:show="detailOpen"
      round
      closeable
      class="chat-popup"
      :style="{ width: 'min(92vw, 430px)', maxHeight: 'min(78vh, 620px)' }"
    >
      <article v-if="selectedQA" class="detail-panel chat-detail">
        <header class="detail-headline">
          <time>{{ selectedQA.time }}</time>
        </header>
        <section class="chat-transcript">
          <div class="chat-turn question-turn">
            <span class="mini-avatar" :style="avatarStyle(selectedQA.profile)">
              <img v-if="avatarSrc(selectedQA.profile)" :src="avatarSrc(selectedQA.profile)" alt="" />
            </span>
            <p class="detail-question chat-bubble">{{ selectedQA.question }}</p>
          </div>
          <div class="chat-turn answer-turn">
            <p class="detail-answer chat-bubble">{{ selectedQA.answer }}</p>
            <span class="mini-avatar owner" :style="avatarStyle(selectedQA.ownerAvatar || boxProfile.avatar)">
              <img v-if="avatarSrc(selectedQA.ownerAvatar || boxProfile.avatar)" :src="avatarSrc(selectedQA.ownerAvatar || boxProfile.avatar)" alt="" />
              <i v-else class="ri-question-answer-line" aria-hidden="true"></i>
            </span>
          </div>
        </section>
      </article>
    </van-popup>
  </main>
</template>

<style scoped>
.classic-ask {
  display: flex;
  flex-direction: column;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  padding: max(18px, env(safe-area-inset-top)) 16px calc(18px + env(safe-area-inset-bottom));
}

.classic-ask.has-composer {
  padding-bottom: calc(84px + env(safe-area-inset-bottom));
}

.classic-ask.is-embedded {
  height: 100%;
  min-height: 0;
  padding: 0 0 10px;
  background: transparent;
  animation: none;
}

.ask-head {
  flex: 0 0 auto;
  width: min(100%, 680px);
  margin: 0 auto 12px;
}

.ask-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.ask-avatar,
.mini-avatar,
.avatar-option {
  display: grid;
  place-items: center;
  overflow: hidden;
  background-position: center;
  background-size: cover;
}

.ask-avatar {
  width: 52px;
  height: 52px;
  flex: 0 0 auto;
  border-radius: 8px;
  color: var(--classic-primary);
  box-shadow: 0 8px 20px rgba(31, 41, 55, 0.08);
}

.ask-avatar.is-catching {
  animation: avatar-catch 560ms cubic-bezier(0.34, 1.56, 0.64, 1);
}

.ask-avatar img,
.mini-avatar img,
.avatar-option img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.ask-profile h1 {
  margin: 0;
  font-size: 21px;
  line-height: 1.22;
  font-weight: 780;
  letter-spacing: 0;
}

.ask-profile p {
  display: -webkit-box;
  margin: 4px 0 0;
  overflow: hidden;
  color: var(--classic-muted);
  font-size: 13px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.ask-meta {
  margin: 8px 0 0;
  color: var(--classic-muted);
  font-size: 12px;
}

.ask-list {
  flex: 1;
  width: min(100%, 680px);
  margin: 0 auto;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 2px 0 14px;
  overscroll-behavior: contain;
}

.ask-list__inner {
  display: grid;
  gap: 10px;
}

.qa-card {
  padding: 20px 22px;
  border-color: #ebebea;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.03);
  cursor: pointer;
  transition:
    border-color 250ms var(--classic-ease),
    box-shadow 300ms ease,
    transform 200ms ease,
    background-color 180ms ease;
  animation: qa-card-in 280ms var(--classic-spring) both;
  animation-delay: calc(min(var(--card-index), 6) * 24ms);
}

.qa-card:hover {
  border-color: #d8d8d6;
  box-shadow: 0 3px 16px rgba(15, 23, 42, 0.04);
  transform: translate3d(0, -1px, 0);
}

.qa-card:active {
  transform: translate3d(0, 0, 0) scale(0.99);
}

.qa-card:focus-visible,
.avatar-option:focus-visible,
.composer-action-button:focus-visible,
.composer-close:focus-visible,
.icon-button:focus-visible {
  outline: 2px solid rgba(37, 99, 235, 0.48);
  outline-offset: 3px;
}

.qa-card-head,
.qa-identity,
.owner-line,
.detail-author {
  display: flex;
  align-items: center;
  gap: 8px;
}

.qa-card-head {
  justify-content: space-between;
  margin-bottom: 14px;
}

.qa-identity {
  min-width: 0;
}

.mini-avatar {
  width: 26px;
  height: 26px;
  flex: 0 0 auto;
  border-radius: 50%;
}

.mini-avatar.owner {
  width: 24px;
  height: 24px;
}

time,
.owner-line {
  color: var(--classic-muted);
  font-size: 12px;
}

.qa-card-head time {
  flex: 0 0 auto;
  padding-left: 10px;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.01em;
}

.qa-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.qa-question {
  margin: 0;
  color: var(--classic-text);
  font-size: 15px;
  font-weight: 480;
  line-height: 1.7;
  letter-spacing: 0.005em;
  overflow-wrap: anywhere;
}

.qa-answer {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  margin: 0 0 0 2px;
  padding: 0 0 0 14px;
  overflow: hidden;
  color: var(--classic-muted);
  border-left: 1.5px solid #ebebea;
  font-size: 14px;
  line-height: 1.55;
  overflow-wrap: anywhere;
  transition: border-color 400ms ease;
}

.qa-answer-avatar {
  width: 18px;
  height: 18px;
  margin-top: 2px;
  color: var(--classic-primary);
  font-size: 11px;
}

.qa-answer-text {
  display: -webkit-box;
  min-width: 0;
  overflow: hidden;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.qa-card:hover .qa-answer {
  border-left-color: #c7c5e8;
}

.qa-answer.muted {
  color: var(--classic-muted);
  border-left-color: #ebebea;
}

.owner-line {
  margin-top: 12px;
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

.composer-morph {
  position: fixed;
  left: 50%;
  bottom: calc(18px + env(safe-area-inset-bottom));
  z-index: 30;
  width: 156px;
  height: 48px;
  overflow: visible;
  border-radius: 24px;
  color: var(--classic-text);
  transform: translateX(-50%);
  transform-origin: 50% 100%;
  pointer-events: none;
  will-change: width, height;
}

.composer-morph.open {
  width: min(calc(100vw - 32px), 520px);
  height: 336px;
  animation: composer-shell-open 540ms linear both;
}

.composer-morph.touched:not(.open) {
  animation: composer-shell-close 280ms linear both;
}

.composer-surface {
  position: absolute;
  right: 0;
  bottom: 60px;
  left: 0;
  z-index: 1;
  display: grid;
  grid-template-rows: 36px 40px minmax(0, 1fr);
  gap: 8px;
  height: 276px;
  overflow: hidden;
  padding: 14px;
  border: 1px solid rgba(226, 232, 240, 0.94);
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.98)),
    #fff;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.16);
  clip-path: inset(100% 42% 0 42% round 24px);
  opacity: 0;
  pointer-events: none;
  transform: translate3d(0, 12px, 0) scale(0.94);
  transform-origin: 50% 100%;
  will-change: transform, opacity, clip-path;
}

.composer-surface::before {
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

.composer-surface::after {
  content: '';
  position: absolute;
  inset: 1px;
  border-radius: 21px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.72), rgba(255, 255, 255, 0));
  opacity: 0;
  pointer-events: none;
}

.composer-morph.open .composer-surface {
  opacity: 1;
  pointer-events: auto;
  animation: composer-surface-open 540ms linear both;
}

.composer-morph.open .composer-surface::after {
  animation: composer-highlight-in 420ms ease-out both;
}

.composer-morph.touched:not(.open) .composer-surface {
  animation: composer-surface-close 260ms linear both;
}

.composer-action-button {
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

.composer-action-button:disabled {
  cursor: not-allowed;
  opacity: 0.58;
  box-shadow: 0 10px 22px rgba(47, 111, 237, 0.16);
}

.composer-action-button:not(:disabled):active {
  transform: translate3d(0, 1px, 0) scale(0.992);
}

.composer-morph.open .composer-action-button {
  box-shadow: 0 14px 34px rgba(47, 111, 237, 0.22);
  animation: composer-action-open 540ms linear both;
}

.composer-morph.touched:not(.open) .composer-action-button {
  animation: composer-action-close 260ms linear both;
}

.composer-button-glint {
  position: absolute;
  inset: -1px;
  background: linear-gradient(100deg, transparent 12%, rgba(255, 255, 255, 0.28) 44%, transparent 68%);
  opacity: 0;
  transform: translate3d(-45%, 0, 0);
  pointer-events: none;
}

.composer-morph.open .composer-button-glint {
  animation: composer-button-glint 620ms ease-out 80ms both;
}

.composer-button-icon,
.composer-button-text {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
}

.composer-button-icon {
  justify-content: center;
  width: 18px;
  height: 18px;
  font-size: 17px;
}

.composer-button-text {
  min-width: 58px;
  justify-content: center;
  white-space: nowrap;
}

.composer-morph.open .composer-button-icon,
.composer-morph.open .composer-button-text {
  animation: composer-label-swap 300ms cubic-bezier(0.2, 0.92, 0.18, 1.12) both;
}

.composer-field-wrap {
  position: relative;
  min-width: 0;
  min-height: 0;
}

.composer-row,
.panel-head {
  position: relative;
  z-index: 1;
}

.composer-morph.open .composer-piece {
  animation: composer-piece-in 320ms cubic-bezier(0.17, 0.9, 0.22, 1.16) both;
  animation-delay: var(--piece-delay, 0ms);
}

.composer-morph:not(.open) .composer-piece {
  opacity: 0;
}

.ask-receipt {
  position: fixed;
  left: 50%;
  bottom: calc(76px + env(safe-area-inset-bottom));
  z-index: 13;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid var(--classic-line);
  border-radius: 17px;
  background: #fff;
  color: var(--classic-text);
  box-shadow: var(--classic-shadow-soft);
  font-size: 13px;
  transform: translateX(-50%);
}

.ask-receipt i {
  color: var(--classic-green);
}

.detail-panel {
  position: relative;
  padding: 26px 16px calc(18px + env(safe-area-inset-bottom));
  background: #fff;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 36px;
  margin-bottom: 0;
}

.panel-head h2 {
  margin: 0;
  color: var(--classic-text);
  font-size: 18px;
  line-height: 1.3;
}

.panel-head p,
.panel-head > span {
  margin: 4px 0 0;
  color: var(--classic-muted);
  font-size: 13px;
}

.composer-close {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  border: 0;
  border-radius: 50%;
  background: var(--classic-surface-soft);
  color: var(--classic-muted);
  font: inherit;
  font-size: 18px;
  transition:
    background-color 180ms ease,
    color 180ms ease,
    transform 180ms var(--classic-spring);
}

.composer-close:disabled {
  opacity: 0.42;
}

.composer-close:not(:disabled):active {
  transform: scale(0.92);
}

.composer-row {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  min-height: 40px;
  margin-bottom: 0;
}

.avatar-row {
  display: flex;
  gap: 4px;
  width: 116px;
  overflow-x: auto;
  padding: 5px 41px 6px;
  scroll-snap-type: x mandatory;
  scrollbar-width: none;
  overscroll-behavior-x: contain;
  mask-image: linear-gradient(90deg, transparent, #000 28%, #000 72%, transparent);
}

.avatar-row::-webkit-scrollbar {
  display: none;
}

.avatar-option {
  position: relative;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  border: 2px solid transparent;
  border-radius: 50%;
  padding: 0;
  scroll-snap-align: center;
  transform: scale(0.84);
  opacity: 0.54;
}

.avatar-option.selected {
  border-color: var(--classic-primary);
  box-shadow: 0 0 0 3px rgba(47, 111, 237, 0.1);
  transform: scale(1);
  opacity: 1;
}

.icon-button {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 50%;
  background: var(--classic-surface-soft);
  color: var(--classic-text);
  font: inherit;
}

.composer-count {
  position: absolute;
  right: 13px;
  bottom: 12px;
  z-index: 2;
  margin: 0;
  padding: 2px 6px;
  border-radius: 10px;
  background: rgba(248, 250, 252, 0.88);
  color: var(--classic-muted);
  font-size: 12px;
  line-height: 1;
  text-align: right;
  pointer-events: none;
}

.composer-field {
  height: 100%;
  overflow: hidden;
  border: 1px solid rgba(226, 232, 240, 0.88);
  border-radius: 14px;
  background: #f8fafc;
}

.composer-field:deep(.van-field__control) {
  min-height: 118px;
  color: var(--classic-text);
  font-size: 15px;
  line-height: 1.65;
}

.composer-field:deep(.van-cell) {
  height: 100%;
  padding: 11px 12px 30px;
}

.chat-detail {
  display: flex;
  flex-direction: column;
  max-height: min(78vh, 620px);
  padding: 18px 14px 16px;
  overflow: hidden;
}

.detail-headline {
  flex: 0 0 auto;
  min-height: 20px;
  padding-right: 28px;
}

.chat-transcript {
  display: grid;
  gap: 14px;
  margin-top: 4px;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 2px 2px calc(2px + env(safe-area-inset-bottom));
  overscroll-behavior: contain;
}

.chat-detail .chat-turn {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  min-width: 0;
}

.chat-detail .question-turn {
  justify-content: flex-start;
}

.chat-detail .answer-turn {
  justify-content: flex-end;
}

.chat-detail .chat-bubble {
  max-width: min(78%, 310px);
  margin: 0;
  padding: 9px 12px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.detail-question {
  background: var(--classic-surface-soft);
  color: var(--classic-text);
  border-bottom-left-radius: 3px;
}

.detail-answer {
  background: var(--classic-primary);
  color: #fff;
  border-bottom-right-radius: 3px;
}

@keyframes qa-card-in {
  from {
    opacity: 0;
    transform: translate3d(0, 10px, 0);
  }
  to {
    opacity: 1;
    transform: translate3d(0, 0, 0);
  }
}

@keyframes avatar-catch {
  0% {
    transform: scale(1);
  }
  32% {
    transform: scale(1.12);
  }
  56% {
    transform: scale(0.94);
  }
  78% {
    transform: scale(1.04);
  }
  100% {
    transform: scale(1);
  }
}

@keyframes composer-shell-open {
  0% {
    width: 156px;
    height: 48px;
  }
  28% {
    width: min(calc(100vw - 116px), 336px);
    height: 48px;
  }
  54% {
    width: min(calc(100vw - 24px), 532px);
    height: 304px;
  }
  72% {
    width: min(calc(100vw - 38px), 512px);
    height: 340px;
  }
  88% {
    width: min(calc(100vw - 30px), 522px);
    height: 334px;
  }
  100% {
    width: min(calc(100vw - 32px), 520px);
    height: 336px;
  }
}

@keyframes composer-shell-close {
  0% {
    width: min(calc(100vw - 32px), 520px);
    height: 336px;
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
    width: 156px;
    height: 48px;
  }
}

@keyframes composer-surface-open {
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

@keyframes composer-surface-close {
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

@keyframes composer-action-open {
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

@keyframes composer-action-close {
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

@keyframes composer-label-swap {
  0% {
    opacity: 0;
    transform: translate3d(0, 4px, 0) scale(0.94);
  }
  100% {
    opacity: 1;
    transform: translate3d(0, 0, 0) scale(1);
  }
}

@keyframes composer-button-glint {
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

@keyframes composer-highlight-in {
  0% {
    opacity: 0.68;
  }
  100% {
    opacity: 0;
  }
}

@keyframes composer-piece-in {
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

.receipt-enter-active,
.receipt-leave-active {
  transition:
    opacity 180ms ease,
    transform 220ms var(--classic-spring);
}

.receipt-enter-from,
.receipt-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(8px);
}

@media (prefers-reduced-motion: reduce) {
  .qa-card {
    animation: none;
  }

  .receipt-enter-active,
  .receipt-leave-active {
    transition: none;
  }

  .ask-avatar.is-catching {
    animation: none;
  }

  .composer-morph,
  .composer-morph.open,
  .composer-morph.touched:not(.open),
  .composer-surface,
  .composer-morph.open .composer-surface,
  .composer-morph.touched:not(.open) .composer-surface,
  .composer-action-button,
  .composer-morph.open .composer-action-button,
  .composer-morph.touched:not(.open) .composer-action-button,
  .composer-morph.open .composer-piece,
  .composer-button-glint,
  .composer-button-icon,
  .composer-button-text {
    animation: none;
  }
}
</style>

<style>
.classic-paper-flyer {
  position: fixed;
  z-index: 10000;
  color: #4f46e5;
  font-size: 22px;
  line-height: 1;
  opacity: 1;
  pointer-events: none;
  filter: drop-shadow(0 2px 6px rgba(79, 70, 229, 0.28));
  transform-origin: center;
  will-change: transform, opacity;
}

@media (prefers-reduced-motion: reduce) {
  .classic-paper-flyer {
    display: none;
  }
}
</style>
