<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { showToast } from 'vant'
import {
  getAnonymousAvatars,
  getPublicBoxProfile,
  getPublishedQA,
  getPublicTopics,
  resolvePublicTopic,
  submitQuestion,
} from '@/api/public'
import { formatTime } from '@/utils'
import { assetSrc } from '@/utils/assets'
import ClassicQuestionList from '@/components/classic/ClassicQuestionList.vue'
import ClassicTopicIsland from '@/components/classic/ClassicTopicIsland.vue'

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

const boxProfile = ref({ slug: '', displayName: '', ownerDisplayName: '', description: '', avatar: null })
const avatarList = ref([])
const publicTopics = ref([])
const selectedTopicCode = ref('')
const filterTopicCode = ref('')
const topicNotice = ref('')
const selectedAvatar = ref(null)
const publishedQA = ref([])
const qaTotal = ref(0)
const pageError = ref('')
const boxMissing = ref(false)
const qaLoading = ref(false)
const refreshing = ref(false)
const qaRefreshing = ref(false)
const qaFilterLoading = ref(false)
const qaFilterEntering = ref(false)
const qaFilterExiting = ref(false)
const qaListMinHeight = ref('')
const qaTransitionName = computed(() => (qaFilterExiting.value || qaFilterEntering.value ? 'classic-filter-list' : 'classic-list'))
const qaPage = ref(1)
const qaHasMore = ref(false)
const composerOpen = ref(false)
const composerTouched = ref(false)
const detailOpen = ref(false)
const selectedQA = ref(null)
const content = ref('')
const sending = ref(false)
const receiptMessage = ref('')
const headCollapsed = ref(false)
const listRef = ref(null)
const ownerAvatarRef = ref(null)
const sendButtonRef = ref(null)
const avatarPickerRef = ref(null)
const detailCardRef = ref(null)
const detailTriggerRef = ref(null)
const detailOriginStyle = ref({})

let receiptTimer = 0
let avatarSnapTimer = 0
let avatarProgrammaticTimer = 0
let flyingFrame = 0
let qaScrollFrame = 0
let qaSettleFrame = 0
let detailFocusTimer = 0
let activeFlyer = null
let loadRequestToken = 0
let filterTransitionToken = 0
let avatarProgrammaticScroll = false
const qaMotionState = {
  lastScrollTop: 0,
  lastTime: 0,
  velocity: 0,
}

const boxName = computed(() => boxProfile.value.displayName?.trim() || boxProfile.value.slug || '')
const ownerName = computed(() => boxProfile.value.ownerDisplayName?.trim() || boxProfile.value.slug || '')
const pageTitle = computed(() => {
  if (boxMissing.value) return '提问箱不存在'
  return ownerName.value ? `${ownerName.value}的提问箱` : boxName.value
})
const boxDescription = computed(() => boxProfile.value.description?.trim() || '')
const backgroundSrc = computed(() => assetSrc(boxProfile.value.background))
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
const activeSubmitTopicCode = computed(() => {
  const code = selectedTopicCode.value
  return publicTopics.value.some((topic) => topic.code === code && topic.available) ? code : ''
})
const selectedTopic = computed(() =>
  publicTopics.value.find((topic) => topic.code === selectedTopicCode.value) || null,
)
const selectedTopicDescription = computed(() => selectedTopic.value?.description?.trim() || '')
const showTopicPlaceholder = computed(() => selectedTopic.value && content.value.trim().length === 0)

function toAvatarOption(a) {
  return { ...a }
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

async function loadBoxProfile(token = loadRequestToken) {
  try {
    const profile = await getPublicBoxProfile(slug.value)
    if (token !== loadRequestToken) return
    boxProfile.value = {
      slug: profile.slug || slug.value,
      displayName: profile.displayName || '',
      ownerDisplayName: profile.ownerDisplayName || '',
      description: profile.description || '',
      avatar: profile.avatar || null,
      background: profile.background || null,
    }
    boxMissing.value = false
  } catch (err) {
    if (token !== loadRequestToken) return
    boxProfile.value = { slug: slug.value, displayName: '', ownerDisplayName: '', description: '', avatar: null }
    boxMissing.value = true
    console.error('Failed to load box profile', err)
    throw err
  }
}

async function loadAvatars(token = loadRequestToken) {
  try {
    const list = await getAnonymousAvatars()
    if (token !== loadRequestToken) return
    avatarList.value = list.map(toAvatarOption)
    selectedAvatar.value = avatarList.value[0] || null
    nextTick(() => centerAvatar(selectedAvatarIndex.value, 'auto'))
  } catch (err) {
    console.error('Failed to load avatars', err)
    throw err
  }
}

async function loadTopics(token = loadRequestToken) {
  try {
    const list = await getPublicTopics(slug.value)
    if (token !== loadRequestToken) return
    publicTopics.value = list || []
  } catch (err) {
    if (token !== loadRequestToken) return
    publicTopics.value = []
    console.error('Failed to load topics', err)
  }
}

async function resolveInitialTopic(token = loadRequestToken) {
  const code = typeof route.query.topic === 'string' ? route.query.topic : ''
  topicNotice.value = ''
  if (!code) {
    selectedTopicCode.value = ''
    return
  }
  try {
    const topic = await resolvePublicTopic(slug.value, code)
    if (token !== loadRequestToken) return
    if (topic.available) {
      if (!publicTopics.value.some((item) => item.code === topic.code)) {
        publicTopics.value = [topic, ...publicTopics.value]
      }
      selectedTopicCode.value = topic.code
      return
    }
    selectedTopicCode.value = ''
    topicNotice.value = '话题已结束了哦，下次早点来吧'
    showToast(topicNotice.value)
  } catch (err) {
    if (token !== loadRequestToken) return
    selectedTopicCode.value = ''
    topicNotice.value = '话题已结束了哦，下次早点来吧'
    showToast(topicNotice.value)
    console.error('Failed to resolve topic', err)
  }
}

function selectTopic(code) {
  selectedTopicCode.value = code
}

async function selectFilterTopic(code) {
  const token = ++filterTransitionToken
  const requestToken = ++loadRequestToken
  qaLoading.value = false
  qaFilterLoading.value = true
  const previousItems = publishedQA.value
  filterTopicCode.value = code
  qaTotal.value = 0
  qaPage.value = 1
  qaHasMore.value = false
  if (previousItems.length && !prefersReducedMotion()) {
    await runQAFilterExit(token)
  } else {
    publishedQA.value = []
  }
  if (token !== filterTransitionToken) {
    qaFilterLoading.value = false
    qaListMinHeight.value = ''
    return
  }
  listRef.value?.scrollTo?.({ top: 0, behavior: 'auto' })
  qaFilterEntering.value = !prefersReducedMotion()
  try {
    await loadPublishedQA(true, requestToken, { animate: true })
  } finally {
    if (token === filterTransitionToken) qaFilterLoading.value = false
  }
  if (token === filterTransitionToken && qaFilterEntering.value) {
    window.setTimeout(() => {
      if (token === filterTransitionToken) {
        qaFilterEntering.value = false
        qaListMinHeight.value = ''
      }
    }, 620)
  } else if (token === filterTransitionToken) {
    qaListMinHeight.value = ''
  }
}

async function runQAFilterExit(token) {
  const list = listRef.value
  const listEl = list?.querySelector?.('.classic-animated-list')
  const inner = list?.querySelector?.('.classic-animated-list__inner')
  qaListMinHeight.value = listEl ? `${listEl.offsetHeight}px` : ''
  if (inner) inner.style.minHeight = `${inner.offsetHeight}px`
  qaFilterExiting.value = true
  await nextTick()
  publishedQA.value = []
  await waitForQALeave()
  if (token === filterTransitionToken) {
    qaFilterExiting.value = false
    if (inner) inner.style.removeProperty('min-height')
  }
}

function waitForQALeave() {
  return new Promise((resolve) => {
    window.setTimeout(resolve, prefersReducedMotion() ? 0 : 720)
  })
}

function markAvatarProgrammaticScroll(duration = 80) {
  window.clearTimeout(avatarProgrammaticTimer)
  avatarProgrammaticScroll = true
  avatarProgrammaticTimer = window.setTimeout(() => {
    avatarProgrammaticScroll = false
  }, duration)
}

function centerAvatar(index, behavior = 'smooth', targetEl = null) {
  const picker = avatarPickerRef.value
  const target =
    targetEl ||
    picker?.querySelector(
      `[data-avatar-cycle="1"][data-avatar-index="${index}"]`,
    )
  if (!picker || !target) return

  const nextLeft = target.offsetLeft - (picker.clientWidth - target.clientWidth) / 2
  markAvatarProgrammaticScroll(behavior === 'smooth' ? 340 : 80)
  picker.scrollTo({ left: nextLeft, behavior })
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
  markAvatarProgrammaticScroll(80)
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
  if (avatarProgrammaticScroll) return
  window.clearTimeout(avatarSnapTimer)
  avatarSnapTimer = window.setTimeout(() => {
    recenterAvatarLoop()
    updateSelectedAvatarFromCenter()
    centerAvatar(selectedAvatarIndex.value, 'auto')
  }, 150)
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

async function loadPublishedQA(reset = false, token = loadRequestToken, { animate = true } = {}) {
  if (qaLoading.value) return
  qaLoading.value = true
  const page = reset ? 1 : qaPage.value
  try {
    const result = await getPublishedQA(slug.value, page, 10, filterTopicCode.value)
    if (token !== loadRequestToken) return
    const items = result.records.map((q, index) => ({
      id: q.id,
      profile: q.avatar,
      ownerAvatar: q.ownerAvatar,
      topic: q.topic,
      question: q.question,
      answer: q.answer,
      ts: q.ts,
      time: formatTime(q.ts),
      motionIndex: index,
    }))
    qaRefreshing.value = reset && !animate
    publishedQA.value = reset ? items : [...publishedQA.value, ...items]
    qaTotal.value = Number(result.total || 0)
    qaPage.value = page + 1
    qaHasMore.value = page < result.totalPages
  } catch (err) {
    console.error('Failed to load QA', err)
    throw err
  } finally {
    if (token === loadRequestToken) {
      qaLoading.value = false
      if (qaRefreshing.value) {
        await nextTick()
        qaRefreshing.value = false
      }
    }
  }
}

function handleScroll() {
  scheduleQAScrollMotion()
  if (qaSettleFrame) window.cancelAnimationFrame(qaSettleFrame)
  qaSettleFrame = window.requestAnimationFrame(settleQAScrollMotion)

  const list = listRef.value
  if (!list) return
  headCollapsed.value = list.scrollTop > 18
  if (qaLoading.value || !qaHasMore.value) return
  const distance = list.scrollHeight - list.scrollTop - list.clientHeight
  if (distance < 220) loadPublishedQA(false)
}

function prefersReducedMotion() {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches
}

function resetQACardMotion() {
  const list = listRef.value
  if (!list) return
  for (const card of list.querySelectorAll('.qa-card')) {
    card.style.removeProperty('--scroll-y')
    card.style.removeProperty('--scroll-scale')
    card.style.removeProperty('--scroll-opacity')
    card.style.removeProperty('--scroll-shadow')
  }
}

function applyQAScrollMotion() {
  const list = listRef.value
  if (!list || prefersReducedMotion() || detailOpen.value) {
    resetQACardMotion()
    return
  }

  const now = performance.now()
  const previousTime = qaMotionState.lastTime || now
  const dt = Math.max(16, now - previousTime)
  const rawVelocity = ((list.scrollTop - qaMotionState.lastScrollTop) / dt) * 16
  qaMotionState.velocity = qaMotionState.velocity * 0.68 + rawVelocity * 0.32
  qaMotionState.lastScrollTop = list.scrollTop
  qaMotionState.lastTime = now

  const listRect = list.getBoundingClientRect()
  const edgeRange = Math.min(132, listRect.height * 0.22)
  const velocityPull = Math.max(-14, Math.min(14, qaMotionState.velocity * 5.6))

  for (const card of list.querySelectorAll('.qa-card')) {
    const rect = card.getBoundingClientRect()
    const topProgress = Math.min(1, Math.max(0, (listRect.top - rect.top) / edgeRange))
    const bottomProgress = Math.min(1, Math.max(0, (rect.bottom - listRect.bottom) / edgeRange))
    const edgeProgress = Math.max(topProgress, bottomProgress)
    const easedEdge = 1 - (1 - edgeProgress) ** 3
    const edgeDirection = bottomProgress > topProgress ? 1 : -1
    const edgePull = edgeDirection * 18 * easedEdge
    const lag = -velocityPull * easedEdge
    const y = edgePull + lag
    const scale = 1 - easedEdge * 0.025
    const opacity = 1 - easedEdge * 0.36
    const shadowLift = Math.min(1, Math.abs(qaMotionState.velocity) / 3.2)

    card.style.setProperty('--scroll-y', `${y.toFixed(2)}px`)
    card.style.setProperty('--scroll-scale', scale.toFixed(4))
    card.style.setProperty('--scroll-opacity', opacity.toFixed(3))
    card.style.setProperty('--scroll-shadow', shadowLift.toFixed(3))
  }
}

function scheduleQAScrollMotion() {
  if (qaScrollFrame) return
  qaScrollFrame = window.requestAnimationFrame(() => {
    qaScrollFrame = 0
    applyQAScrollMotion()
  })
}

function settleQAScrollMotion() {
  if (Math.abs(qaMotionState.velocity) < 0.018) {
    qaMotionState.velocity = 0
    applyQAScrollMotion()
    qaSettleFrame = 0
    return
  }

  qaMotionState.velocity *= 0.74
  applyQAScrollMotion()
  qaSettleFrame = window.requestAnimationFrame(settleQAScrollMotion)
}

function getDetailOriginStyle(event) {
  const rect = event?.currentTarget?.getBoundingClientRect?.()
  if (!rect) return {}
  const vw = window.innerWidth || document.documentElement.clientWidth
  const vh = window.innerHeight || document.documentElement.clientHeight
  const panelWidth = Math.min(vw * 0.92, 430)
  const panelHeight = Math.min(vh * 0.78, 620)
  const originX = rect.left + rect.width / 2 - vw / 2
  const originY = rect.top + rect.height / 2 - vh / 2
  const originScaleX = Math.max(0.34, Math.min(1.06, rect.width / panelWidth))
  const originScaleY = Math.max(0.24, Math.min(0.86, rect.height / panelHeight))

  return {
    '--detail-origin-x': `${originX.toFixed(1)}px`,
    '--detail-origin-y': `${originY.toFixed(1)}px`,
    '--detail-origin-scale-x': originScaleX.toFixed(3),
    '--detail-origin-scale-y': originScaleY.toFixed(3),
  }
}

function openDetail(qa, event) {
  if (composerOpen.value) closeComposer(true)
  detailTriggerRef.value = event?.currentTarget || null
  detailOriginStyle.value = getDetailOriginStyle(event)
  selectedQA.value = qa
  detailOpen.value = true
  nextTick(() => {
    detailCardRef.value?.focus?.({ preventScroll: true })
    resetQACardMotion()
  })
}

function closeDetail() {
  detailOpen.value = false
  window.clearTimeout(detailFocusTimer)
  detailFocusTimer = window.setTimeout(() => {
    detailTriggerRef.value?.focus?.({ preventScroll: true })
    detailTriggerRef.value = null
  }, prefersReducedMotion() ? 0 : 260)
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
    await submitQuestion(slug.value, selectedAvatar.value.id, text, activeSubmitTopicCode.value)
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

async function handlePullRefresh() {
  refreshing.value = true
  try {
    await resetAndLoadPublicContent({ preserveList: true, animateList: false })
  } finally {
    refreshing.value = false
  }
}

async function resetAndLoadPublicContent({ preserveList = false, animateList = true } = {}) {
  if (qaLoading.value) return
  const token = ++loadRequestToken
  pageError.value = ''
  boxMissing.value = false
  headCollapsed.value = false
  if (boxProfile.value.slug !== slug.value) {
    boxProfile.value = { slug: slug.value, displayName: '', ownerDisplayName: '', description: '', avatar: null }
  }
  if (props.showComposer && !avatarList.value.length) {
    avatarList.value = []
    selectedAvatar.value = null
  }
  selectedQA.value = null
  detailOpen.value = false
  composerOpen.value = false
  composerTouched.value = false
  content.value = ''
  if (!preserveList) {
    publicTopics.value = []
    selectedTopicCode.value = ''
    filterTopicCode.value = ''
    topicNotice.value = ''
    publishedQA.value = []
    qaTotal.value = 0
  }
  qaPage.value = 1
  qaHasMore.value = false

  try {
    const jobs = [loadBoxProfile(token), loadTopics(token)]
    if (props.showComposer) jobs.unshift(loadAvatars(token))
    await Promise.all(jobs)
    if (preserveList && filterTopicCode.value && !publicTopics.value.some((topic) => topic.code === filterTopicCode.value)) {
      filterTopicCode.value = ''
    }
    if (preserveList && selectedTopicCode.value && !publicTopics.value.some((topic) => topic.code === selectedTopicCode.value && topic.available)) {
      selectedTopicCode.value = ''
    }
    if (!preserveList) await resolveInitialTopic(token)
    await loadPublishedQA(true, token, { animate: animateList })
    if (token !== loadRequestToken) return
    await nextTick()
    centerAvatar(selectedAvatarIndex.value, 'auto')
  } catch (err) {
    if (token !== loadRequestToken) return
    pageError.value = err?.message || '加载失败'
  }
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

watch(publishedQA, () => {
  nextTick(() => scheduleQAScrollMotion())
})

watch(
  pageTitle,
  (title) => {
    if (!props.embedded && title) document.title = title
  },
  { immediate: true },
)

onMounted(async () => {
  await resetAndLoadPublicContent()
  await nextTick()
  qaMotionState.lastScrollTop = listRef.value?.scrollTop || 0
  qaMotionState.lastTime = performance.now()
  scheduleQAScrollMotion()
})

onBeforeUnmount(() => {
  window.cancelAnimationFrame(flyingFrame)
  if (qaScrollFrame) window.cancelAnimationFrame(qaScrollFrame)
  if (qaSettleFrame) window.cancelAnimationFrame(qaSettleFrame)
  activeFlyer?.remove()
  window.clearTimeout(receiptTimer)
  window.clearTimeout(avatarSnapTimer)
  window.clearTimeout(avatarProgrammaticTimer)
  window.clearTimeout(detailFocusTimer)
})
</script>

<template>
  <main
    class="classic-ask classic-page classic-enter"
    :class="{
      'is-embedded': embedded,
      'has-composer': showComposer && !boxMissing,
      'has-background': backgroundSrc && !embedded,
    }"
  >
    <img
      v-if="backgroundSrc && !embedded"
      class="classic-page-bg"
      :src="backgroundSrc"
      alt=""
      decoding="async"
      fetchpriority="high"
      loading="eager"
      draggable="false"
    />

    <section v-if="boxMissing" class="box-missing-state" role="status">
      <img src="/icon.svg" alt="" />
      <h1>提问箱不存在</h1>
    </section>

    <header v-else class="ask-head" :class="{ 'is-detail-muted': detailOpen, 'is-collapsed': headCollapsed }">
      <div class="ask-profile">
        <span ref="ownerAvatarRef" class="ask-avatar" :style="avatarStyle(boxProfile.avatar)">
          <img v-if="avatarSrc(boxProfile.avatar)" :src="avatarSrc(boxProfile.avatar)" alt="" />
          <i v-else class="ri-user-smile-line" aria-hidden="true"></i>
        </span>
        <div>
          <h1>{{ boxName }}</h1>
          <p v-if="boxDescription">{{ boxDescription }}</p>
        </div>
      </div>
      <ClassicTopicIsland
        :model-value="filterTopicCode"
        :topics="publicTopics"
        @change="selectFilterTopic"
      />
      <p class="ask-meta">{{ qaTotal }}条</p>
    </header>

    <div v-if="!boxMissing" ref="listRef" class="ask-scroll" @scroll="handleScroll">
      <van-pull-refresh
        v-model="refreshing"
        class="ask-refresh"
        @refresh="handlePullRefresh"
      >
      <section
        class="ask-list"
        :class="{ 'is-detail-muted': detailOpen }"
        aria-label="公开回复列表"
      >
        <ClassicQuestionList
          :questions="publishedQA"
          :transition-name="qaRefreshing ? '' : qaTransitionName"
          :list-min-height="qaListMinHeight"
          :loading="qaLoading || qaFilterLoading"
          :has-more="qaHasMore"
          :error="pageError"
          :box-avatar="boxProfile.avatar"
          empty-description="暂时还没有公开回答"
          aria-label="公开回复列表"
          @open="openDetail"
        />
      </section>
      </van-pull-refresh>
    </div>

    <Transition v-if="showComposer && !boxMissing" name="receipt">
      <div v-if="receiptMessage" class="ask-receipt" role="status">
        <i class="ri-check-line" aria-hidden="true"></i>
        <span>{{ receiptMessage }}</span>
      </div>
    </Transition>

    <button
      v-if="showComposer && !boxMissing && composerOpen"
      class="composer-dismiss-layer"
      type="button"
      aria-label="关闭提问框"
      @pointerdown.stop
      @click.stop.prevent="closeComposer()"
    ></button>

    <section
      v-if="showComposer && !boxMissing"
      class="composer-morph"
      :class="{ open: composerOpen, touched: composerTouched, 'has-topic-tools': publicTopics.length }"
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
          <div v-if="topicNotice || publicTopics.length" class="composer-topic-area">
            <p v-if="topicNotice" class="topic-notice">{{ topicNotice }}</p>
            <div v-if="publicTopics.length" class="composer-topic-tags" aria-label="选择参与话题">
              <button
                class="composer-topic-tag"
                :class="{ selected: !selectedTopicCode }"
                type="button"
                :aria-pressed="!selectedTopicCode"
                @click="selectTopic('')"
              >
                #无
              </button>
              <button
                v-for="topic in publicTopics"
                :key="topic.code"
                class="composer-topic-tag"
                :class="{ selected: selectedTopicCode === topic.code }"
                type="button"
                :aria-pressed="selectedTopicCode === topic.code"
                @click="selectTopic(topic.code)"
              >
                #{{ topic.title }}
              </button>
            </div>
          </div>
          <section class="composer-input-box" :class="{ 'has-topic-placeholder': showTopicPlaceholder }" aria-label="问题内容">
            <Transition name="composer-topic-preview">
              <div v-if="showTopicPlaceholder" class="composer-topic-preview" aria-hidden="true">
                <strong>参与「{{ selectedTopic.title }}」。</strong>
                <p v-if="selectedTopicDescription">{{ selectedTopicDescription }}</p>
              </div>
            </Transition>
            <van-field
              v-model="content"
              class="composer-field"
              type="textarea"
              rows="5"
              maxlength="350"
              autosize
              :placeholder="showTopicPlaceholder ? '' : '写下你的问题'"
            />
            <p class="composer-count">{{ content.length }} / 350</p>
          </section>
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

    <Teleport to="body">
      <Transition name="detail-layer">
        <div v-if="detailOpen" class="detail-layer" role="presentation">
          <button
            class="detail-scrim"
            type="button"
            aria-label="关闭回复详情"
            @click="closeDetail"
          ></button>
          <article
            v-if="selectedQA"
            ref="detailCardRef"
            class="detail-panel chat-detail"
            :style="detailOriginStyle"
            role="dialog"
            aria-modal="true"
            aria-label="回复详情"
            tabindex="-1"
            @keydown.esc="closeDetail"
          >
            <button class="detail-close" type="button" aria-label="关闭回复详情" @click="closeDetail">
              <i class="ri-close-line" aria-hidden="true"></i>
            </button>
            <header class="detail-headline detail-piece" style="--detail-piece-delay: 80ms">
              <time>{{ selectedQA.time }}</time>
              <span v-if="selectedQA.topic" class="topic-badge detail-topic">{{ selectedQA.topic.title }}</span>
            </header>
            <section class="chat-transcript">
              <div class="chat-turn question-turn detail-piece" style="--detail-piece-delay: 132ms">
                <span class="mini-avatar" :style="avatarStyle(selectedQA.profile)">
                  <img v-if="avatarSrc(selectedQA.profile)" :src="avatarSrc(selectedQA.profile)" alt="" />
                </span>
                <p class="detail-question chat-bubble">{{ selectedQA.question }}</p>
              </div>
              <div class="chat-turn answer-turn detail-piece" style="--detail-piece-delay: 188ms">
                <p class="detail-answer chat-bubble">{{ selectedQA.answer }}</p>
                <span class="mini-avatar owner" :style="avatarStyle(selectedQA.ownerAvatar || boxProfile.avatar)">
                  <img v-if="avatarSrc(selectedQA.ownerAvatar || boxProfile.avatar)" :src="avatarSrc(selectedQA.ownerAvatar || boxProfile.avatar)" alt="" />
                  <i v-else class="ri-question-answer-line" aria-hidden="true"></i>
                </span>
              </div>
            </section>
          </article>
        </div>
      </Transition>
    </Teleport>
  </main>
</template>

<style scoped>
.classic-ask {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  padding: max(18px, env(safe-area-inset-top)) 16px calc(18px + env(safe-area-inset-bottom));
  isolation: isolate;
}

.classic-ask.has-background {
  background: transparent;
}

.classic-ask.is-embedded {
  height: 100%;
  min-height: 0;
  padding: 0;
  background: transparent;
  animation: none;
}

.classic-page-bg {
  position: absolute;
  inset: 0;
  z-index: -2;
  width: 100%;
  height: 100%;
  object-fit: cover;
  pointer-events: none;
  user-select: none;
}

.classic-ask.has-background::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: -1;
  background:
    linear-gradient(180deg, rgba(248, 250, 252, 0.2), rgba(248, 250, 252, 0.46)),
    rgba(255, 255, 255, 0.12);
  pointer-events: none;
}

.box-missing-state {
  position: relative;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 12px;
  flex: 1;
  width: min(100%, 680px);
  min-height: 0;
  margin: 0 auto;
  color: var(--classic-muted);
  text-align: center;
}

.box-missing-state img {
  width: 58px;
  height: 58px;
  object-fit: contain;
  opacity: 0.82;
}

.box-missing-state h1 {
  margin: 0;
  color: var(--classic-text);
  font-size: 18px;
  font-weight: 720;
  line-height: 1.35;
}

.ask-head {
  position: absolute;
  top: max(18px, env(safe-area-inset-top));
  right: 0;
  left: 0;
  z-index: 9;
  height: 96px;
  flex: 0 0 auto;
  width: min(calc(100% - 32px), 680px);
  margin: 0 auto;
  overflow: visible;
  pointer-events: none;
  transition:
    opacity 220ms ease,
    filter 260ms ease;
}

.ask-head.is-detail-muted,
.ask-list.is-detail-muted {
  opacity: 0.42;
  filter: saturate(0.82);
  pointer-events: none;
}

.ask-head.is-detail-muted {
  opacity: 0.42;
}

.ask-profile {
  position: absolute;
  top: 28px;
  left: 0;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  width: min(100%, 260px);
  min-width: 0;
  pointer-events: auto;
  transition:
    opacity 240ms ease,
    transform 320ms cubic-bezier(0.16, 1, 0.3, 1),
    filter 260ms ease;
  will-change: transform, opacity;
}

.ask-profile > div {
  display: grid;
  min-width: 0;
  gap: 3px;
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
  border: 1px solid rgba(47, 111, 237, 0.12);
  background: linear-gradient(180deg, #f8fbff, #eef5ff);
  color: var(--classic-primary);
  font-size: 24px;
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
  overflow: hidden;
  font-size: 21px;
  line-height: 1.22;
  font-weight: 780;
  letter-spacing: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ask-profile p {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: var(--classic-muted);
  font-size: 13px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.ask-meta {
  position: absolute;
  top: 45px;
  right: 0;
  z-index: 1;
  max-width: 52px;
  overflow: hidden;
  margin: 0;
  color: var(--classic-muted);
  font-size: 12px;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
  pointer-events: none;
  transition:
    opacity 220ms ease,
    transform 300ms cubic-bezier(0.16, 1, 0.3, 1),
    filter 260ms ease;
  will-change: transform, opacity;
}

.ask-head.is-collapsed .ask-profile {
  opacity: 0;
  filter: blur(2px);
  pointer-events: none;
  transform: translate3d(-36px, -2px, 0) scale(0.96);
}

.ask-head.is-collapsed .ask-meta {
  opacity: 0;
  filter: blur(2px);
  transform: translate3d(28px, -2px, 0) scale(0.96);
}

.ask-head :deep(.topic-island) {
  position: absolute;
  top: 4px;
  left: 50%;
  z-index: 2;
  transform: translateX(-50%);
  pointer-events: auto;
}

.ask-scroll {
  position: relative;
  flex: 1;
  width: 100%;
  min-height: 0;
  margin: 0 auto;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.classic-ask:not(.is-embedded) .ask-scroll {
  padding-top: 112px;
}

.classic-ask.is-embedded .ask-head {
  top: 0;
  width: 100%;
}

.classic-ask.is-embedded .ask-scroll {
  padding-top: 104px;
}

.ask-scroll::-webkit-scrollbar {
  display: none;
}

.ask-refresh {
  width: min(100%, 680px);
  min-height: 100%;
  margin: 0 auto;
}

.ask-refresh:deep(.van-pull-refresh__track) {
  min-height: 100%;
}

.ask-list {
  min-height: 0;
  padding: 2px 0 calc(92px + env(safe-area-inset-bottom));
  transition:
    opacity 220ms ease,
    transform 280ms var(--classic-ease),
    filter 260ms ease;
}

.ask-list.is-detail-muted {
  transform: translate3d(0, 8px, 0) scale(0.988);
}

.composer-topic-tags {
  display: flex;
  gap: 7px;
  min-width: 0;
  overflow-x: auto;
  padding: 1px 2px 4px;
  scrollbar-width: none;
}

.composer-topic-tags::-webkit-scrollbar {
  display: none;
}

.composer-topic-tag {
  flex: 0 0 auto;
  max-width: 180px;
  min-height: 30px;
  border: 1px solid rgba(47, 111, 237, 0.14);
  border-radius: 15px;
  padding: 0 10px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.72);
  color: var(--classic-muted);
  font: inherit;
  font-size: 12px;
  font-weight: 680;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition:
    border-color 180ms ease,
    background-color 180ms ease,
    color 180ms ease,
    transform 180ms var(--classic-ease);
}

.composer-topic-tag.selected {
  border-color: rgba(47, 111, 237, 0.28);
  background: rgba(47, 111, 237, 0.1);
  color: var(--classic-primary);
  transform: translateY(-1px) scale(1.02);
}

.classic-ask.has-background :deep(.qa-card) {
  border-color: rgba(255, 255, 255, 0.58);
  background: rgba(255, 255, 255, 0.72);
  box-shadow:
    0 1px 2px rgba(15, 23, 42, 0.04),
    0 calc(8px * var(--scroll-shadow)) calc(22px * var(--scroll-shadow)) rgba(15, 23, 42, calc(0.06 * var(--scroll-shadow)));
}

.classic-ask.has-background :deep(.qa-card:hover) {
  border-color: rgba(255, 255, 255, 0.76);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 3px 16px rgba(15, 23, 42, 0.08);
}

.avatar-option:focus-visible,
.composer-action-button:focus-visible,
.composer-close:focus-visible,
.icon-button:focus-visible {
  outline: 2px solid rgba(37, 99, 235, 0.48);
  outline-offset: 3px;
}

.owner-line,
.detail-author {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mini-avatar {
  width: 24px;
  height: 24px;
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

.owner-line {
  margin-top: 12px;
}

.composer-morph {
  --composer-open-height: 336px;
  --composer-surface-height: 276px;
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

.composer-dismiss-layer {
  position: fixed;
  inset: 0;
  z-index: 29;
  border: 0;
  padding: 0;
  background: transparent;
  cursor: default;
}

.composer-morph::before {
  content: '';
  position: absolute;
  right: 0;
  bottom: 60px;
  left: 0;
  z-index: 0;
  height: var(--composer-surface-height);
  border-radius: 22px;
  opacity: 0;
  pointer-events: none;
  box-shadow:
    0 2px 8px rgba(15, 23, 42, 0.08),
    0 18px 42px rgba(15, 23, 42, 0.18),
    0 34px 78px rgba(15, 23, 42, 0.16);
  transform: translate3d(0, 12px, 0) scale(0.96);
  transform-origin: 50% 100%;
  transition:
    opacity 180ms ease,
    transform 260ms var(--classic-ease);
}

.composer-morph.open {
  width: min(calc(100vw - 32px), 520px);
  height: var(--composer-open-height);
  animation: composer-shell-open 540ms linear both;
}

.composer-morph.open::before {
  opacity: 1;
  transform: translate3d(0, 0, 0) scale(1);
}

.composer-morph.touched:not(.open)::before {
  opacity: 0;
  transform: translate3d(0, 10px, 0) scale(0.96);
}

.composer-morph.open.has-topic-tools {
  --composer-open-height: 396px;
  --composer-surface-height: 336px;
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
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 8px;
  height: var(--composer-surface-height);
  overflow: hidden;
  padding: 14px;
  border: 1px solid rgba(226, 232, 240, 0.94);
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(248, 250, 252, 0.99)),
    #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
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
  border-right: 1px solid rgba(226, 232, 240, 0.94);
  border-bottom: 1px solid rgba(226, 232, 240, 0.94);
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
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  min-height: 0;
}

.composer-topic-area {
  display: grid;
  gap: 8px;
}

.topic-notice {
  margin: 0;
  border: 1px solid rgba(47, 111, 237, 0.14);
  border-radius: 10px;
  padding: 9px 10px;
  background: rgba(47, 111, 237, 0.06);
  color: var(--classic-muted);
  font-size: 12px;
  line-height: 1.5;
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
  right: 0;
  bottom: 2px;
  z-index: 2;
  margin: 0;
  padding: 2px 0 0 8px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0), #fff 16px);
  color: var(--classic-muted);
  font-size: 12px;
  line-height: 1;
  text-align: right;
  pointer-events: none;
}

.composer-input-box {
  position: relative;
  flex: 1 1 auto;
  min-height: 176px;
  overflow: visible;
  border: 0;
  border-radius: 0;
  background: transparent;
  transition:
    min-height 260ms var(--classic-ease),
    background-color 220ms ease;
}

.composer-input-box.has-topic-placeholder {
  min-height: 204px;
}

.composer-topic-preview {
  position: absolute;
  top: 11px;
  right: 0;
  left: 0;
  z-index: 1;
  overflow: hidden;
  pointer-events: none;
}

.composer-topic-preview strong {
  display: block;
  overflow: hidden;
  color: #9ca3af;
  font-size: 15px;
  font-weight: 400;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.composer-topic-preview p {
  display: -webkit-box;
  margin: 4px 0 0;
  overflow: hidden;
  color: #9ca3af;
  font-size: 12px;
  font-style: italic;
  line-height: 1.45;
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.composer-topic-preview-enter-active,
.composer-topic-preview-leave-active {
  transition:
    opacity 220ms ease,
    transform 260ms var(--classic-ease);
}

.composer-topic-preview-enter-from,
.composer-topic-preview-leave-to {
  opacity: 0;
  transform: translate3d(0, -5px, 0);
}

.composer-topic-preview-enter-to,
.composer-topic-preview-leave-from {
  opacity: 1;
  transform: translate3d(0, 0, 0);
}

.composer-field {
  height: 100%;
  overflow-y: auto;
  border: 0;
  padding: 11px 0 38px;
  background: transparent;
  scrollbar-width: none;
}

.composer-field::-webkit-scrollbar {
  display: none;
}

.composer-field:deep(.van-field__control) {
  min-height: 118px;
  color: var(--classic-text);
  font-size: 15px;
  line-height: 1.65;
}

.composer-field:deep(.van-field__body) {
  height: 100%;
}

.composer-field:deep(.van-cell__value) {
  min-height: 100%;
}

.composer-field:deep(.van-field__control)::placeholder {
  color: #9ca3af;
}

.detail-layer {
  position: fixed;
  inset: 0;
  z-index: 70;
  display: grid;
  place-items: center;
  padding: max(18px, env(safe-area-inset-top)) 16px max(18px, env(safe-area-inset-bottom));
  pointer-events: none;
}

.detail-scrim {
  position: absolute;
  inset: 0;
  border: 0;
  background: rgba(15, 23, 42, 0.26);
  cursor: pointer;
  pointer-events: auto;
}

.detail-panel {
  position: relative;
  z-index: 1;
  width: min(92vw, 430px);
  max-height: min(78vh, 620px);
  max-height: min(78dvh, 620px);
  padding: 18px 14px 16px;
  border: 1px solid rgba(226, 232, 240, 0.96);
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.99)),
    #fff;
  box-shadow: 0 22px 58px rgba(15, 23, 42, 0.2);
  overflow: hidden;
  pointer-events: auto;
  transform-origin: 50% 50%;
  will-change: transform, opacity, clip-path;
}

.detail-panel::after {
  content: '';
  position: absolute;
  inset: 1px;
  border-radius: 21px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.74), rgba(255, 255, 255, 0));
  opacity: 0;
  pointer-events: none;
}

.detail-layer-enter-active,
.detail-layer-leave-active {
  transition: opacity 260ms ease;
}

.detail-layer-enter-from,
.detail-layer-leave-to {
  opacity: 0;
}

.detail-layer-enter-active .detail-scrim,
.detail-layer-leave-active .detail-scrim {
  transition: opacity 260ms ease;
}

.detail-layer-enter-from .detail-scrim,
.detail-layer-leave-to .detail-scrim {
  opacity: 0;
}

.detail-layer-enter-active .detail-panel {
  animation: detail-panel-open 500ms linear both;
}

.detail-layer-leave-active .detail-panel {
  animation: detail-panel-close 240ms linear both;
}

.detail-layer-enter-active .detail-panel::after {
  animation: composer-highlight-in 420ms ease-out 60ms both;
}

.chat-detail {
  display: flex;
  flex-direction: column;
}

.detail-close {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 3;
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 50%;
  background: var(--classic-surface-soft);
  color: var(--classic-muted);
  cursor: pointer;
  font: inherit;
  font-size: 18px;
  transition:
    background-color 180ms ease,
    color 180ms ease,
    transform 180ms var(--classic-spring);
}

.detail-close:active {
  transform: scale(0.92);
}

.detail-close:focus-visible {
  outline: 2px solid rgba(37, 99, 235, 0.48);
  outline-offset: 3px;
}

.detail-headline {
  display: flex;
  flex-direction: column;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 8px;
  flex: 0 0 auto;
  min-height: 20px;
  padding-right: 28px;
}

.detail-topic {
  margin-top: 8px;
  margin-bottom: 0;
}

.chat-transcript {
  display: grid;
  gap: 14px;
  margin-top: 18px;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 2px 2px calc(2px + env(safe-area-inset-bottom));
  overscroll-behavior: contain;
  scrollbar-width: none;
}

.chat-transcript::-webkit-scrollbar {
  display: none;
}

.detail-piece {
  opacity: 1;
}

.detail-layer-enter-active .detail-piece {
  animation: detail-piece-in 330ms cubic-bezier(0.17, 0.9, 0.22, 1.16) both;
  animation-delay: var(--detail-piece-delay, 0ms);
}

.detail-layer-leave-active .detail-piece {
  animation: detail-piece-out 150ms ease both;
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

@keyframes detail-panel-open {
  0% {
    clip-path: inset(38% 18% 38% 18% round 14px);
    opacity: 0;
    transform: translate3d(var(--detail-origin-x, 0), var(--detail-origin-y, 0), 0)
      scale(var(--detail-origin-scale-x, 0.8), var(--detail-origin-scale-y, 0.42));
  }
  28% {
    clip-path: inset(24% 8% 24% 8% round 16px);
    opacity: 0.72;
    transform: translate3d(calc(var(--detail-origin-x, 0) * 0.38), calc(var(--detail-origin-y, 0) * 0.38), 0)
      scale(0.94, 0.82);
  }
  56% {
    clip-path: inset(4% 0 4% 0 round 22px);
    opacity: 1;
    transform: translate3d(0, -4px, 0) scale(1.012);
  }
  76% {
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

@keyframes detail-panel-close {
  0% {
    clip-path: inset(0 0 0 0 round 22px);
    opacity: 1;
    transform: translate3d(0, 0, 0) scale(1);
  }
  46% {
    clip-path: inset(18% 4% 18% 4% round 18px);
    opacity: 0.76;
    transform: translate3d(calc(var(--detail-origin-x, 0) * 0.12), calc(var(--detail-origin-y, 0) * 0.12), 0)
      scale(0.94, 0.82);
  }
  100% {
    clip-path: inset(42% 20% 42% 20% round 14px);
    opacity: 0;
    transform: translate3d(var(--detail-origin-x, 0), var(--detail-origin-y, 0), 0)
      scale(var(--detail-origin-scale-x, 0.8), var(--detail-origin-scale-y, 0.42));
  }
}

@keyframes detail-piece-in {
  0% {
    opacity: 0;
    transform: translate3d(0, 9px, 0) scale(0.985);
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

@keyframes detail-piece-out {
  to {
    opacity: 0;
    transform: translate3d(0, 4px, 0) scale(0.99);
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
    height: calc(var(--composer-open-height) - 32px);
  }
  72% {
    width: min(calc(100vw - 38px), 512px);
    height: calc(var(--composer-open-height) + 4px);
  }
  88% {
    width: min(calc(100vw - 30px), 522px);
    height: calc(var(--composer-open-height) - 2px);
  }
  100% {
    width: min(calc(100vw - 32px), 520px);
    height: var(--composer-open-height);
  }
}

@keyframes composer-shell-close {
  0% {
    width: min(calc(100vw - 32px), 520px);
    height: var(--composer-open-height);
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

@media (max-width: 430px) {
  .ask-head {
    grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
    gap: 8px;
  }

  .ask-meta {
    align-self: center;
    font-size: 11px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .receipt-enter-active,
  .receipt-leave-active,
  .detail-layer-enter-active,
  .detail-layer-leave-active,
  .detail-layer-enter-active .detail-scrim,
  .detail-layer-leave-active .detail-scrim {
    transition: none;
  }

  .ask-avatar.is-catching {
    animation: none;
  }

  .detail-layer-enter-active .detail-panel,
  .detail-layer-leave-active .detail-panel,
  .detail-layer-enter-active .detail-panel::after,
  .detail-layer-enter-active .detail-piece,
  .detail-layer-leave-active .detail-piece {
    animation: none;
  }

  .detail-piece {
    opacity: 1;
  }

  .composer-morph,
  .composer-morph::before,
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
  .composer-button-text,
  .composer-input-box,
  .composer-topic-preview,
  .composer-topic-preview-enter-active,
  .composer-topic-preview-leave-active,
  .composer-topic-tag {
    animation: none;
    transition: none;
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
