<script setup>
import { animate } from "motion";
import { LiquidGlass } from "@ybouane/liquidglass";
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from "vue";
import { useRoute } from "vue-router";
import {
  getAvatars,
  getPublicBoxProfile,
  getPublishedQA,
  submitQuestion,
} from "../api/public";
import { pageBackground } from "../assets/background";
import { formatTime } from "../utils";

const route = useRoute();
const slug = computed(() => route.params.slug || "xiaoxiu");

// --- 数据状态（取代 mock store） ---
const boxProfile = ref({ slug: "", displayName: "", description: "" });
const avatarList = ref([]);
const publishedQA = ref([]);
const selectedAvatar = ref(null);
const qaLoading = ref(false);
const qaPage = ref(1);
const qaHasMore = ref(false);

// 适配器：将后端 Avatar 转为组件内使用的格式
function toAvatarOption(a) {
  return { id: a.id, name: a.name, iconBase64: a.iconBase64, bg: a.bg };
}

async function loadAvatars() {
  try {
    const list = await getAvatars();
    avatarList.value = list.map(toAvatarOption);
    if (avatarList.value.length > 0) {
      selectedAvatar.value = avatarList.value[0];
    }
  } catch (err) {
    console.error("Failed to load avatars", err);
  }
}

async function loadPublishedQA(reset = false) {
  if (qaLoading.value) return;
  qaLoading.value = true;
  const page = reset ? 1 : qaPage.value;
  try {
    const result = await getPublishedQA(slug.value, page, 10);
    const items = result.records.map(q => ({
      id: q.id,
      profile: { name: q.avatar?.name, iconBase64: q.avatar?.iconBase64, bg: q.avatar?.bg },
      question: q.question,
      answer: q.answer,
      ts: q.ts,
      time: formatTime(q.ts),
    }));
    if (reset) {
      publishedQA.value = items;
    } else {
      publishedQA.value.push(...items);
    }
    qaPage.value = page + 1;
    qaHasMore.value = page < result.totalPages;
  } catch (err) {
    console.error("Failed to load QA", err);
  } finally {
    qaLoading.value = false;
  }
}

async function doSendQuestion(text) {
  if (!selectedAvatar.value) throw new Error("请先选择头像");
  await submitQuestion(slug.value, selectedAvatar.value.id, text);
}

const rootRef = ref(null);
const bgRef = ref(null);
const qaListRef = ref(null);
const textareaRef = ref(null);
const avatarPickerRef = ref(null);
const composerRef = ref(null);
const detailRef = ref(null);
const detailScrimRef = ref(null);
const composerToolsRef = ref(null);

const composerOpen = ref(false);
const content = ref("");
const sending = ref(false);
const selectedQA = ref(null);
const detailVisible = ref(false);
const toastMessage = ref("");
const avatarScrollFrame = ref(0);
const avatarRecenterTimer = ref(0);
const qaScrollFrame = ref(0);

let liquidGlass = null;
let initToken = 0;
let glassRefreshFrame = 0;
let toastTimer = 0;
let composerAnimation = null;
let composerToolsAnimation = null;
let detailAnimation = null;
let detailScrimAnimation = null;
let stableRefreshTimers = [];
let qaMotionState = {
  lastScrollTop: 0,
  lastTime: 0,
  velocity: 0,
  settleFrame: 0,
};

const avatarLoopOptions = computed(() => {
  if (!avatarList.value.length) return [];
  return Array.from({ length: 3 }, (_, cycle) =>
    avatarList.value.map((avatar, index) => ({
      ...avatar,
      cycle,
      index,
      key: `${cycle}-${avatar.id ?? index}`,
    })),
  ).flat();
});

const selectedAvatarIndex = computed(() => {
  const list = avatarList.value;
  const index = list.findIndex(
    (avatar) => avatar.id === selectedAvatar.value?.id,
  );
  return index >= 0 ? index : 0;
});

const charCount = computed(() => `${content.value.length} / 350`);
const canSend = computed(() => content.value.trim().length > 0 && !sending.value);
const draftText = computed(() => content.value.trim());
const visibleQA = computed(() => publishedQA.value);
const boxTitle = computed(() => {
  const name = boxProfile.value.displayName?.trim();
  return name || "AskBox";
});
const boxDescription = computed(() => {
  const description = boxProfile.value.description?.trim();
  return description || "匿名提问和公开回复";
});
const pageAriaLabel = computed(() => `${boxTitle.value} 匿名提问箱`);

async function loadBoxProfile() {
  try {
    const profile = await getPublicBoxProfile(slug.value);
    boxProfile.value = {
      slug: profile.slug || slug.value,
      displayName: profile.displayName || "",
      description: profile.description || "",
    };
  } catch (err) {
    boxProfile.value = { slug: slug.value, displayName: "", description: "" };
    console.error("Failed to load box profile", err);
  }
}

function loadMore() {
  if (!qaLoading.value && qaHasMore.value) {
    loadPublishedQA(false);
  }
}

function maybeLoadMoreQA() {
  const list = qaListRef.value;
  if (!list || qaLoading.value || !qaHasMore.value) return;
  const distance = list.scrollHeight - list.scrollTop - list.clientHeight;
  if (distance < 220) loadMore();
}

const composerConfig = {
  blurAmount: 0.24,
  refraction: 0.74,
  chromAberration: 0.04,
  edgeHighlight: 0.08,
  specular: 0,
  fresnel: 1,
  distortion: 0,
  cornerRadius: 48,
  zRadius: 46,
  opacity: 1,
  saturation: 0,
  tintStrength: 0,
  brightness: -0.08,
  shadowOpacity: 0.26,
  shadowSpread: 12,
  shadowOffsetY: 2,
  floating: false,
  bevelMode: 0,
};

const sendButtonConfig = {
  button: true,
  blurAmount: 0,
  refraction: 0.69,
  chromAberration: 0.05,
  edgeHighlight: 0.08,
  specular: 0,
  fresnel: 1,
  distortion: 0,
  cornerRadius: 28,
  zRadius: 28,
  opacity: 1,
  saturation: 0,
  tintStrength: 0,
  brightness: 0,
  shadowOpacity: 0.24,
  shadowSpread: 10,
  shadowOffsetY: 1,
  floating: false,
  bevelMode: 0,
};

const detailCardConfig = {
  blurAmount: 0.28,
  refraction: 0.68,
  chromAberration: 0.05,
  edgeHighlight: 0.05,
  specular: 0,
  fresnel: 1,
  distortion: 0,
  cornerRadius: 36,
  zRadius: 38,
  opacity: 1,
  saturation: 0,
  tintStrength: 0,
  brightness: -0.08,
  shadowOpacity: 0.34,
  shadowSpread: 16,
  shadowOffsetY: 3,
  floating: false,
  bevelMode: 0,
};

function configJson(config) {
  return JSON.stringify(config);
}

function scheduleGlassRefresh(element = rootRef.value) {
  if (glassRefreshFrame) return;
  glassRefreshFrame = requestAnimationFrame(() => {
    glassRefreshFrame = 0;
    if (element) {
      liquidGlass?.markChanged(element);
    }
  });
}

function scheduleStableGlassRefresh() {
  stableRefreshTimers.forEach((timer) => window.clearTimeout(timer));
  stableRefreshTimers = [0, 80, 220].map((delay) =>
    window.setTimeout(() => scheduleGlassRefresh(rootRef.value), delay),
  );
}

function openComposer() {
  selectedQA.value = null;
  detailVisible.value = false;
  composerOpen.value = true;
  nextTick(() => {
    textareaRef.value?.focus();
    resetTextareaScroll();
    scheduleStableGlassRefresh();
  });
}

function openDetail(qa) {
  selectedQA.value = qa;
  detailVisible.value = true;
  composerOpen.value = false;
  nextTick(() => {
    scheduleStableGlassRefresh();
  });
}

function closeDetail() {
  detailVisible.value = false;
  window.setTimeout(() => {
    if (!detailVisible.value) {
      selectedQA.value = null;
    }
  }, 220);
  nextTick(() => {
    scheduleStableGlassRefresh();
  });
}

function onQACardKeydown(event, qa) {
  if (event.key !== "Enter" && event.key !== " ") return;
  event.preventDefault();
  openDetail(qa);
}

function applyQAScrollMotion() {
  const list = qaListRef.value;
  if (!list) return;

  const now = performance.now();
  const dt = Math.max(16, now - (qaMotionState.lastTime || now));
  const rawVelocity = ((list.scrollTop - qaMotionState.lastScrollTop) / dt) * 16;
  qaMotionState.velocity = qaMotionState.velocity * 0.72 + rawVelocity * 0.28;
  qaMotionState.lastScrollTop = list.scrollTop;
  qaMotionState.lastTime = now;

  const listRect = list.getBoundingClientRect();
  const edgeRange = Math.min(150, listRect.height * 0.22);
  const velocityPull = Math.max(-18, Math.min(18, qaMotionState.velocity * 7));

  for (const card of list.querySelectorAll(".qa-card")) {
    const rect = card.getBoundingClientRect();
    const topProgress = Math.min(1, Math.max(0, (listRect.top - rect.top) / edgeRange));
    const bottomProgress = Math.min(1, Math.max(0, (rect.bottom - listRect.bottom) / edgeRange));
    const topEase = 1 - (1 - topProgress) ** 3;
    const bottomEase = 1 - (1 - bottomProgress) ** 3;
    const activeProgress = Math.max(topEase, bottomEase);
    const edgeDirection = bottomEase > topEase ? 1 : -1;
    const edgePull = edgeDirection * 30 * activeProgress;
    const velocityLag = -velocityPull * activeProgress;
    const y = edgePull + velocityLag;
    const scaleX = 1 - activeProgress * 0.025;
    const scaleY = 1 - activeProgress * 0.075;
    const opacity = 1 - activeProgress * 0.48;
    const blur = activeProgress * 1.35;

    card.style.opacity = `${opacity}`;
    card.style.filter = blur > 0.15 ? `blur(${blur.toFixed(2)}px)` : "";
    card.style.transformOrigin = bottomEase > topEase ? "50% 100%" : "50% 0";
    card.style.transform = `translate3d(0, ${y.toFixed(2)}px, 0) scale(${scaleX.toFixed(4)}, ${scaleY.toFixed(4)})`;
  }
}

function scheduleQAScrollMotion() {
  if (qaScrollFrame.value) return;
  qaScrollFrame.value = requestAnimationFrame(() => {
    qaScrollFrame.value = 0;
    applyQAScrollMotion();
  });
}

function settleQAScrollMotion() {
  if (Math.abs(qaMotionState.velocity) < 0.02) {
    qaMotionState.velocity = 0;
    applyQAScrollMotion();
    qaMotionState.settleFrame = 0;
    return;
  }

  qaMotionState.velocity *= 0.76;
  applyQAScrollMotion();
  qaMotionState.settleFrame = requestAnimationFrame(settleQAScrollMotion);
}

function handleQAScroll() {
  scheduleQAScrollMotion();
  maybeLoadMoreQA();
  if (qaMotionState.settleFrame) cancelAnimationFrame(qaMotionState.settleFrame);
  qaMotionState.settleFrame = requestAnimationFrame(settleQAScrollMotion);
}

function updateViewportMetrics() {
  scheduleGlassRefresh(rootRef.value);
}

function closeComposerIfEmpty(event) {
  if (content.value.trim() || sending.value) return;
  const nextTarget = event?.relatedTarget;
  if (nextTarget?.closest?.(".composer-card")) return;
  composerOpen.value = false;
  scheduleStableGlassRefresh();
}

function resetTextareaScroll() {
  const el = textareaRef.value;
  if (!el) return;
  el.scrollTop = 0;
}

function stopAnimation(animation) {
  animation?.stop?.();
  animation?.cancel?.();
}

function animateComposer() {
  const panel = composerRef.value;
  if (!panel) return;

  stopAnimation(composerAnimation);
  stopAnimation(composerToolsAnimation);

  if (composerOpen.value) {
    composerAnimation = animate(
      panel,
      {
        opacity: [0.92, 1, 1],
        transform: [
          "translateY(-24px) scale(0.96)",
          "translateY(4px) scale(1.012)",
          "translateY(0) scale(1)",
        ],
      },
      { duration: 0.42, ease: [0.16, 1, 0.3, 1] },
    );

    if (composerToolsRef.value) {
      composerToolsAnimation = animate(
        composerToolsRef.value,
        { opacity: [0, 1], transform: ["translateY(-8px)", "translateY(0)"] },
        { duration: 0.24, delay: 0.08, ease: [0.16, 1, 0.3, 1] },
      );
    }
    return;
  }

  composerAnimation = animate(
    panel,
    {
      opacity: [0.98, 1],
      transform: ["translateY(-6px) scale(0.985)", "translateY(0) scale(1)"],
    },
    { duration: 0.22, ease: [0.2, 0.8, 0.2, 1] },
  );
}

function animateDetail() {
  const panel = detailRef.value;
  if (!panel) return;

  stopAnimation(detailAnimation);
  stopAnimation(detailScrimAnimation);

  if (detailVisible.value) {
    if (detailScrimRef.value) {
      detailScrimAnimation = animate(
        detailScrimRef.value,
        { opacity: [0, 1] },
        { duration: 0.22, ease: "ease-out" },
      );
    }
    detailAnimation = animate(
      panel,
      {
        opacity: [0, 1, 1],
        transform: [
          "translate(-50%, -44%) scale(0.9)",
          "translate(-50%, -51%) scale(1.025)",
          "translate(-50%, -50%) scale(1)",
        ],
      },
      { duration: 0.42, ease: [0.16, 1, 0.3, 1] },
    );
    return;
  }

  if (detailScrimRef.value) {
    detailScrimAnimation = animate(
      detailScrimRef.value,
      { opacity: [1, 0] },
      { duration: 0.18, ease: "ease-in" },
    );
  }
  detailAnimation = animate(
    panel,
    {
      opacity: [1, 0],
      transform: [
        "translate(-50%, -50%) scale(1)",
        "translate(-50%, -46%) scale(0.94)",
      ],
    },
    { duration: 0.2, ease: [0.4, 0, 1, 1] },
  );
}

function centerAvatar(index, behavior = "smooth", targetEl = null) {
  const target =
    targetEl ||
    avatarPickerRef.value?.querySelector(
      `[data-avatar-cycle="1"][data-avatar-index="${index}"]`,
    );

  target?.scrollIntoView({
    behavior,
    block: "nearest",
    inline: "center",
  });
}

function selectAvatarAt(index, event) {
  selectedAvatar.value = avatarList.value[index];
  centerAvatar(index, "smooth", event?.currentTarget);
  nextTick(() => {
    scheduleGlassRefresh(avatarPickerRef.value);
  });
}

function updateSelectedAvatarFromCenter() {
  const picker = avatarPickerRef.value;
  if (!picker) return;

  const pickerRect = picker.getBoundingClientRect();
  const centerX = pickerRect.left + pickerRect.width / 2;
  let nextIndex = selectedAvatarIndex.value;
  let nearestDistance = Infinity;

  for (const button of picker.querySelectorAll(".avatar-option")) {
    const rect = button.getBoundingClientRect();
    const distance = Math.abs(rect.left + rect.width / 2 - centerX);
    if (distance < nearestDistance) {
      nearestDistance = distance;
      nextIndex = Number(button.dataset.avatarIndex);
    }
  }

  if (nextIndex !== selectedAvatarIndex.value) {
    selectedAvatar.value = avatarList.value[nextIndex];
  }
}

function getAvatarCycleWidth() {
  const picker = avatarPickerRef.value;
  const first = picker?.querySelector('[data-avatar-cycle="0"][data-avatar-index="0"]');
  const second = picker?.querySelector('[data-avatar-cycle="1"][data-avatar-index="0"]');
  if (!first || !second) return 0;
  return second.offsetLeft - first.offsetLeft;
}

function recenterAvatarLoop() {
  const picker = avatarPickerRef.value;
  const cycleWidth = getAvatarCycleWidth();
  if (!picker || !cycleWidth) return;

  let nextScrollLeft = picker.scrollLeft;
  while (nextScrollLeft < cycleWidth * 0.5) nextScrollLeft += cycleWidth;
  while (nextScrollLeft > cycleWidth * 1.5) nextScrollLeft -= cycleWidth;
  if (Math.abs(nextScrollLeft - picker.scrollLeft) < 1) return;

  const previousBehavior = picker.style.scrollBehavior;
  picker.style.scrollBehavior = "auto";
  picker.scrollLeft = nextScrollLeft;
  picker.style.scrollBehavior = previousBehavior;
}

function handleAvatarScroll() {
  window.clearTimeout(avatarRecenterTimer.value);

  if (!avatarScrollFrame.value) {
    avatarScrollFrame.value = requestAnimationFrame(() => {
      avatarScrollFrame.value = 0;
    });
  }

  avatarRecenterTimer.value = window.setTimeout(() => {
    recenterAvatarLoop();
    updateSelectedAvatarFromCenter();
  }, 140);
}

function isolateAvatarPointer(event) {
  event.stopPropagation();
}

function handleAvatarWheel(event) {
  const picker = avatarPickerRef.value;
  if (!picker || Math.abs(event.deltaX) > Math.abs(event.deltaY)) return;

  event.stopPropagation();
  event.preventDefault();
  picker.scrollLeft += event.deltaY;
}

function focusAvatarSelector(event) {
  event.stopPropagation();
  event.currentTarget.focus({ preventScroll: true });
}

async function handleSend() {
  const text = content.value.trim();
  if (!text || sending.value) return;

  sending.value = true;
  scheduleGlassRefresh(rootRef.value);

  try {
    await doSendQuestion(text);
  } catch (error) {
    sending.value = false;
    showToast("投递失败，请稍后再试");
    scheduleGlassRefresh(rootRef.value);
    console.error("Failed to send question", error);
    return;
  }

  sending.value = false;
  content.value = "";
  composerOpen.value = false;
  showToast("问题已投递，我会尽快回答");

  // 刷新列表以显示新问题（如果该箱自动发布的话）
  loadPublishedQA(true);

  await nextTick();
  resetTextareaScroll();
  scheduleGlassRefresh(rootRef.value);
}

function showToast(message) {
  window.clearTimeout(toastTimer);
  toastMessage.value = message;
  toastTimer = window.setTimeout(() => {
    toastMessage.value = "";
  }, 2400);
}

function onComposerKeydown(event) {
  if (event.key === "Enter" && (event.metaKey || event.ctrlKey)) {
    event.preventDefault();
    handleSend();
  }
}

async function waitForBackgroundImage() {
  const image = bgRef.value;
  if (!image || image.complete) return;

  await new Promise((resolve) => {
    image.addEventListener("load", resolve, { once: true });
    image.addEventListener("error", resolve, { once: true });
  });
}

async function initLiquidGlass() {
  const root = rootRef.value;
  if (!root) return;

  const token = ++initToken;
  liquidGlass?.destroy();
  liquidGlass = null;

  await waitForBackgroundImage();
  await nextTick();
  centerAvatar(selectedAvatarIndex.value, "auto");

  const glassElements = Array.from(root.children).filter((child) =>
    child.classList.contains("liquid-glass"),
  );

  try {
    const instance = await LiquidGlass.init({
      root,
      glassElements,
      defaults: {
        blurAmount: 0,
        refraction: 0.69,
        chromAberration: 0.05,
        edgeHighlight: 0.05,
        shadowOpacity: 0.25,
        shadowSpread: 10,
        shadowOffsetY: 1,
      },
    });

    if (token !== initToken) {
      instance.destroy();
      return;
    }

    liquidGlass = instance;
    liquidGlass.markChanged();
  } catch (error) {
    console.error("Failed to initialize AskBox Liquid page", error);
  }
}

watch(composerOpen, () => {
  nextTick(() => {
    animateComposer();
    scheduleStableGlassRefresh();
  });
});

watch(detailVisible, () => {
  nextTick(() => {
    animateDetail();
    scheduleStableGlassRefresh();
  });
});

watch(draftText, (next, previous) => {
  if (Boolean(next) === Boolean(previous)) return;
  nextTick(() => scheduleStableGlassRefresh());
});

watch(selectedQA, () => {
  nextTick(() => scheduleGlassRefresh(rootRef.value));
});

watch(visibleQA, () => {
  nextTick(() => scheduleQAScrollMotion());
});

watch(boxTitle, (title) => {
  document.title = `${title} - AskBox`;
}, { immediate: true });

watch(slug, async () => {
  selectedQA.value = null;
  detailVisible.value = false;
  composerOpen.value = false;
  content.value = "";
  publishedQA.value = [];
  qaPage.value = 1;
  qaHasMore.value = false;
  await Promise.all([
    loadBoxProfile(),
    loadPublishedQA(true),
  ]);
  await nextTick();
  scheduleStableGlassRefresh();
  scheduleQAScrollMotion();
});

onMounted(async () => {
  updateViewportMetrics();
  window.addEventListener("resize", updateViewportMetrics);
  await Promise.all([
    loadAvatars(),
    loadBoxProfile(),
    loadPublishedQA(true),
  ]);
  initLiquidGlass();
  nextTick(() => scheduleQAScrollMotion());
});

onBeforeUnmount(() => {
  initToken += 1;
  window.removeEventListener("resize", updateViewportMetrics);
  if (avatarScrollFrame.value) cancelAnimationFrame(avatarScrollFrame.value);
  if (qaScrollFrame.value) cancelAnimationFrame(qaScrollFrame.value);
  if (qaMotionState.settleFrame) cancelAnimationFrame(qaMotionState.settleFrame);
  window.clearTimeout(avatarRecenterTimer.value);
  window.clearTimeout(toastTimer);
  stableRefreshTimers.forEach((timer) => window.clearTimeout(timer));
  stopAnimation(composerAnimation);
  stopAnimation(composerToolsAnimation);
  stopAnimation(detailAnimation);
  stopAnimation(detailScrimAnimation);
  liquidGlass?.destroy();
  liquidGlass = null;
});
</script>

<template>
  <main
    ref="rootRef"
    class="askbox-page"
    :class="{
      'has-detail': detailVisible,
      'composer-open': composerOpen,
    }"
    :aria-label="pageAriaLabel"
  >
    <img
      ref="bgRef"
      class="page-bg"
      :src="pageBackground.src"
      alt=""
      decoding="async"
      fetchpriority="high"
      loading="eager"
      draggable="false"
      @load="scheduleGlassRefresh()"
    />

    <header class="brand-strip">
      <span class="brand-mark"><i class="ri-question-answer-line" aria-hidden="true"></i></span>
      <span>
        <strong>{{ boxTitle }}</strong>
        <em>{{ boxDescription }}</em>
      </span>
    </header>

    <section
      ref="qaListRef"
      class="qa-list"
      aria-label="公开回复列表"
      @scroll="handleQAScroll"
    >
      <article
        v-for="qa in visibleQA"
        :key="qa.id"
        class="qa-card"
        role="button"
        tabindex="0"
        @click="openDetail(qa)"
        @keydown="onQACardKeydown($event, qa)"
      >
        <header class="qa-head">
          <span class="qa-author">
            <span class="qa-avatar" :style="{ background: qa.profile.bg }">
              <img :src="qa.profile.iconBase64" class="qa-avatar-img" alt="" />
            </span>
          </span>
          <time>{{ qa.time }}</time>
        </header>
        <p class="qa-question">{{ qa.question }}</p>
        <p class="qa-answer">{{ qa.answer }}</p>
      </article>
      <div v-if="qaLoading || qaHasMore" class="qa-load-state" aria-live="polite">
        {{ qaLoading ? "加载中" : "继续下滑加载更多" }}
      </div>
    </section>

    <button
      ref="detailScrimRef"
      class="detail-scrim"
      :class="{ visible: detailVisible }"
      :tabindex="detailVisible ? 0 : -1"
      type="button"
      aria-label="关闭完整回复"
      @click="closeDetail"
    ></button>

    <article
      ref="detailRef"
      class="liquid-glass detail-card"
      :class="{ visible: detailVisible }"
      :data-config="configJson(detailCardConfig)"
      role="dialog"
      aria-modal="true"
      aria-live="polite"
    >
      <template v-if="selectedQA">
        <header class="detail-head">
          <span class="qa-author">
            <span class="qa-avatar" :style="{ background: selectedQA.profile.bg }">
              <img :src="selectedQA.profile.iconBase64" class="qa-avatar-img" alt="" />
            </span>
          </span>
          <button
            class="detail-close"
            type="button"
            aria-label="关闭完整回复"
            @click="closeDetail"
          >
            <i class="ri-close-line" aria-hidden="true"></i>
          </button>
        </header>
        <time class="detail-time">{{ selectedQA.time }}</time>
        <p class="detail-question">{{ selectedQA.question }}</p>
        <p class="detail-answer">{{ selectedQA.answer }}</p>
      </template>
    </article>

    <article
      class="liquid-glass draft-preview-card"
      :class="{ visible: composerOpen && draftText }"
      :data-config="configJson(detailCardConfig)"
      :aria-hidden="!(composerOpen && draftText)"
    >
      <header class="draft-preview-head">
        <span class="qa-author">
          <span class="qa-avatar" :style="{ background: selectedAvatar?.bg }">
            <img :src="selectedAvatar?.iconBase64" class="qa-avatar-img" alt="" />
          </span>
        </span>
        <span>{{ charCount }}</span>
      </header>
      <p class="draft-preview-text">{{ draftText }}</p>
    </article>

    <section
      ref="composerRef"
      class="liquid-glass composer-card"
      :class="{ expanded: composerOpen }"
      :data-config="configJson(composerConfig)"
      aria-label="Ask a question"
      @click.self="openComposer"
    >
      <button
        v-if="!composerOpen"
        class="composer-pill"
        type="button"
        @click="openComposer"
      >
        <span>匿名问一个问题</span>
        <i class="ri-pencil-line" aria-hidden="true"></i>
      </button>

      <template v-else>
        <textarea
          ref="textareaRef"
          v-model="content"
          class="composer-input"
          rows="4"
          maxlength="350"
          placeholder="写下你想问的，我会认真回答。"
          aria-label="提问内容"
          @keydown="onComposerKeydown"
          @blur="closeComposerIfEmpty"
        ></textarea>
        <div ref="composerToolsRef" class="composer-tools">
          <section
            ref="avatarPickerRef"
            class="avatar-selector"
            :class="{ visible: composerOpen }"
            tabindex="-1"
            aria-label="选择匿名头像"
            @pointerdown="focusAvatarSelector"
            @pointermove="isolateAvatarPointer"
            @pointerup="isolateAvatarPointer"
            @pointercancel="isolateAvatarPointer"
            @scroll="handleAvatarScroll"
            @wheel="handleAvatarWheel"
          >
            <button
              v-for="avatar in avatarLoopOptions"
              :key="avatar.key"
              class="avatar-option"
              :class="{ selected: selectedAvatarIndex === avatar.index }"
              :style="{ background: avatar.bg }"
              :data-avatar-cycle="avatar.cycle"
              :data-avatar-index="avatar.index"
              :aria-label="`匿名头像 ${avatar.index + 1}`"
              :aria-pressed="selectedAvatarIndex === avatar.index"
              type="button"
              @click="selectAvatarAt(avatar.index, $event)"
            >
              <img :src="avatar.iconBase64" class="avatar-option-img" alt="" />
            </button>
          </section>
          <div class="composer-meta">
            <span>{{ charCount }}</span>
          </div>
        </div>
      </template>
    </section>

    <button
      class="liquid-glass send-button"
      :class="{ visible: composerOpen, ready: canSend, loading: sending }"
      :disabled="!canSend"
      :data-config="configJson(sendButtonConfig)"
      type="button"
      aria-label="投递问题"
      @click="handleSend"
    >
      <i v-if="!sending" class="ri-send-plane-fill" aria-hidden="true"></i>
      <i v-else class="ri-loader-4-line is-spinning" aria-hidden="true"></i>
    </button>

    <div v-if="toastMessage" class="status-toast" role="status">
      <i class="ri-checkbox-circle-fill" aria-hidden="true"></i>
      <span>{{ toastMessage }}</span>
    </div>
  </main>
</template>

<style scoped>
:global(html),
:global(body),
:global(#app) {
  width: 100%;
  height: 100%;
  overflow: hidden;
  overscroll-behavior: none;
}

.askbox-page {
  --page-pad: 26px;
  --stage-width: min(640px, calc(100vw - var(--page-pad) * 2));
  --stage-left: calc((100vw - var(--stage-width)) / 2);
  --qa-width: var(--stage-width);
  --composer-width: var(--stage-width);
  --brand-top: 32px;
  --composer-top: 94px;
  --composer-expanded-height: 264px;
  --list-top: 176px;
  --list-bottom: 28px;
  --preview-top: calc(var(--composer-top) + var(--composer-expanded-height) + 16px);
  position: fixed;
  inset: 0;
  width: 100vw;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  isolation: isolate;
  background: #0c0f14;
  color: #ffffff;
  font-family: inherit;
  touch-action: manipulation;
}

.page-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  pointer-events: none;
  user-select: none;
}

.brand-strip {
  position: absolute;
  top: var(--brand-top);
  left: var(--stage-left);
  z-index: 4;
  display: inline-flex;
  align-items: center;
  gap: 12px;
  max-width: var(--stage-width);
  color: rgba(255, 255, 255, 0.92);
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.28);
  pointer-events: none;
}

.brand-strip > span:last-child {
  min-width: 0;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: rgba(0, 0, 0, 0.24);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.brand-mark i {
  font-size: 22px;
  line-height: 1;
}

.brand-strip strong,
.brand-strip em {
  display: block;
  letter-spacing: 0;
  font-style: normal;
  line-height: 1.18;
  overflow: hidden;
  text-overflow: ellipsis;
}

.brand-strip strong {
  font-size: 17px;
  font-weight: 750;
  white-space: nowrap;
}

.brand-strip em {
  display: -webkit-box;
  margin-top: 3px;
  color: rgba(255, 255, 255, 0.64);
  font-size: 12px;
  font-weight: 520;
  line-height: 1.35;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.liquid-glass {
  position: absolute;
  border: 0;
  background: transparent;
  font: inherit;
}

.qa-list {
  position: absolute;
  left: var(--stage-left);
  top: var(--list-top);
  bottom: var(--list-bottom);
  z-index: 5;
  width: var(--qa-width);
  max-height: calc(100vh - var(--list-top) - var(--list-bottom));
  max-height: calc(100dvh - var(--list-top) - var(--list-bottom));
  overflow-x: hidden;
  overflow-y: auto;
  padding: 10px 6px 96px 0;
  scrollbar-width: none;
  touch-action: pan-y;
  overscroll-behavior: contain;
  transition:
    opacity 180ms ease,
    transform 220ms cubic-bezier(0.2, 0.8, 0.2, 1);
}

.qa-list::-webkit-scrollbar {
  display: none;
}

.has-detail .qa-list {
  opacity: 0;
  visibility: hidden;
  pointer-events: none;
  transform: scale(0.98);
  transition: none;
}

.composer-open .qa-list {
  opacity: 0;
  visibility: hidden;
  pointer-events: none;
  transform: translateY(10px) scale(0.985);
  transition: none;
}

.qa-card {
  display: block;
  width: 100%;
  min-height: 178px;
  margin: 0 0 38px;
  padding: 0;
  background: transparent;
  color: rgba(255, 255, 255, 0.92);
  overflow: hidden;
  overflow-wrap: anywhere;
  user-select: none;
  cursor: pointer;
  transform-origin: 50% 0;
  will-change: transform, opacity, filter;
}

.qa-card:focus-visible {
  outline: 2px solid rgba(255, 255, 255, 0.72);
  outline-offset: 4px;
}

.qa-head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 12px;
}

.qa-author {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  gap: 9px;
  font-size: 13px;
  font-weight: 650;
}

.qa-author > span:last-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qa-avatar {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  flex: 0 0 auto;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.28);
  overflow: hidden;
}

.qa-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.qa-avatar i {
  font-size: 16px;
  line-height: 1;
}

.qa-head time {
  flex: 0 0 auto;
  color: rgba(255, 255, 255, 0.62);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.qa-question,
.qa-answer {
  position: relative;
  z-index: 1;
  margin: 0;
  letter-spacing: 0;
}

.qa-question {
  display: -webkit-box;
  margin-bottom: 13px;
  overflow: hidden;
  color: rgba(255, 255, 255, 0.96);
  font-size: 15px;
  font-weight: 650;
  line-height: 1.62;
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.qa-answer {
  display: -webkit-box;
  padding-left: 13px;
  overflow: hidden;
  border-left: 2px solid rgba(255, 255, 255, 0.22);
  color: rgba(255, 255, 255, 0.72);
  font-size: 14px;
  font-weight: 450;
  line-height: 1.72;
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.detail-scrim {
  position: absolute;
  inset: 0;
  z-index: 23;
  border: 0;
  background: rgba(6, 5, 12, 0.5);
  backdrop-filter: blur(8px) saturate(0.88);
  cursor: pointer;
  opacity: 0;
  pointer-events: none;
  will-change: opacity;
}

.detail-scrim.visible {
  pointer-events: auto;
}

.detail-card {
  top: 50%;
  left: 50%;
  z-index: 24;
  width: min(560px, calc(100vw - 40px));
  max-height: min(680px, calc(100vh - 92px));
  max-height: min(680px, calc(100dvh - 92px));
  padding: 22px 24px 24px;
  border-radius: 36px;
  color: rgba(255, 255, 255, 0.94);
  overflow: hidden;
  opacity: 0;
  pointer-events: none;
  transform: translate(-50%, -46%) scale(0.94);
  will-change: opacity, transform;
}

.detail-card.visible {
  pointer-events: auto;
}

.detail-card::before {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 0;
  border-radius: inherit;
  background: rgba(9, 8, 16, 0.14);
  pointer-events: none;
}

.detail-head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 8px;
}

.detail-close {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  border: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
  color: rgba(255, 255, 255, 0.86);
  cursor: pointer;
}

.detail-close:focus-visible {
  outline: 2px solid rgba(255, 255, 255, 0.78);
  outline-offset: 3px;
}

.detail-close i {
  font-size: 20px;
  line-height: 1;
}

.detail-time {
  position: relative;
  z-index: 1;
  display: block;
  margin-bottom: 18px;
  color: rgba(255, 255, 255, 0.58);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.detail-question,
.detail-answer {
  position: relative;
  z-index: 1;
  margin: 0;
  letter-spacing: 0;
}

.detail-question {
  margin-bottom: 18px;
  color: rgba(255, 255, 255, 0.98);
  font-size: 18px;
  font-weight: 720;
  line-height: 1.62;
  overflow-wrap: anywhere;
}

.detail-answer {
  max-height: min(360px, calc(100vh - 330px));
  max-height: min(360px, calc(100dvh - 330px));
  overflow-y: auto;
  padding-left: 14px;
  border-left: 2px solid rgba(255, 255, 255, 0.28);
  color: rgba(255, 255, 255, 0.78);
  font-size: 15px;
  font-weight: 460;
  line-height: 1.78;
  overflow-wrap: anywhere;
  scrollbar-width: none;
}

.detail-answer::-webkit-scrollbar {
  display: none;
}

.draft-preview-card {
  top: var(--preview-top);
  left: 50%;
  z-index: 12;
  width: min(560px, var(--stage-width));
  max-height: min(280px, calc(100vh - var(--preview-top) - 28px));
  max-height: min(280px, calc(100dvh - var(--preview-top) - 28px));
  padding: 20px 22px 22px;
  border-radius: 36px;
  color: rgba(255, 255, 255, 0.94);
  overflow: hidden;
  opacity: 0;
  pointer-events: none;
  transform: translateX(-50%) translateY(18px) scale(0.96);
  transition:
    opacity 220ms ease,
    transform 300ms cubic-bezier(0.16, 1, 0.3, 1);
  will-change: opacity, transform;
}

.draft-preview-card.visible {
  opacity: 1;
  transform: translateX(-50%) translateY(0) scale(1);
}

.draft-preview-card::before {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 0;
  border-radius: inherit;
  background: rgba(9, 8, 16, 0.12);
  pointer-events: none;
}

.draft-preview-head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
  color: rgba(255, 255, 255, 0.68);
  font-size: 12px;
  font-weight: 620;
}

.draft-preview-head > span:last-child {
  flex: 0 0 auto;
  font-variant-numeric: tabular-nums;
}

.draft-preview-text {
  position: relative;
  z-index: 1;
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: rgba(255, 255, 255, 0.96);
  font-size: 18px;
  font-weight: 700;
  line-height: 1.64;
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 6;
}

.composer-card {
  left: var(--stage-left);
  top: var(--composer-top);
  z-index: 18;
  display: block;
  width: var(--composer-width);
  min-height: 58px;
  padding: 8px;
  border-radius: 48px;
  color: rgba(255, 255, 255, 0.94);
  cursor: default;
  touch-action: auto;
  transform: translateY(0) scale(1);
  will-change: opacity, transform;
}

.composer-card.expanded {
  min-height: var(--composer-expanded-height);
  padding: 10px;
  transform: translateY(0) scale(1);
}

.composer-pill {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 42px;
  border: 0;
  border-radius: 22px;
  padding: 0 16px 0 18px;
  background: rgba(255, 255, 255, 0.22);
  color: rgba(255, 255, 255, 0.84);
  font: inherit;
  font-size: 14px;
  font-weight: 620;
  cursor: pointer;
  min-width: 0;
}

.composer-pill i {
  font-size: 18px;
}

.composer-pill:focus-visible,
.avatar-option:focus-visible,
.send-button:focus-visible {
  outline: 2px solid rgba(255, 255, 255, 0.78);
  outline-offset: 3px;
}

.composer-input {
  position: relative;
  z-index: 1;
  display: block;
  width: 100%;
  height: 154px;
  resize: none;
  overflow-y: auto;
  border: 1px solid rgba(255, 255, 255, 0.38);
  border-radius: 38px;
  outline: none;
  padding: 16px 18px;
  background: transparent;
  color: rgba(255, 255, 255, 0.96);
  font: inherit;
  font-size: 15px;
  font-weight: 520;
  line-height: 1.58;
  cursor: text;
  scrollbar-width: none;
  overflow-wrap: anywhere;
}

.composer-input::-webkit-scrollbar {
  display: none;
}

.composer-input:focus-visible {
  border-color: rgba(255, 255, 255, 0.62);
}

.composer-input::placeholder {
  color: rgba(255, 255, 255, 0.58);
}

.composer-tools {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 10px;
  min-height: 48px;
  padding: 0 58px 0 2px;
  opacity: 1;
  transform: translateY(0);
  will-change: opacity, transform;
}

.composer-meta {
  display: flex;
  min-width: 0;
  flex: 1 1 auto;
  flex-direction: column;
  align-items: flex-end;
  gap: 3px;
  color: rgba(255, 255, 255, 0.62);
  font-size: 12px;
  font-weight: 560;
  line-height: 1.2;
  text-align: right;
}

.composer-meta span {
  max-width: 130px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.avatar-selector {
  --avatar-slot: 34px;
  --avatar-height: 34px;
  --avatar-width: 102px;
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  width: var(--avatar-width);
  min-width: var(--avatar-width);
  max-width: var(--avatar-width);
  overflow-x: auto;
  overflow-y: hidden;
  flex: 0 0 auto;
  border-radius: 15px;
  background: rgba(0, 0, 0, 0.22);
  opacity: 0;
  pointer-events: none;
  scroll-behavior: smooth;
  scroll-snap-type: x mandatory;
  scrollbar-width: none;
  touch-action: pan-x;
  overscroll-behavior-inline: contain;
  scroll-padding-inline: calc(var(--avatar-width) / 2 - var(--avatar-slot) / 2);
  transform: translateY(12px);
  transition:
    opacity 220ms ease,
    transform 260ms cubic-bezier(0.2, 0.8, 0.2, 1);
}

.avatar-selector.visible {
  opacity: 1;
  pointer-events: auto;
  transform: translateY(0);
}

.avatar-selector::-webkit-scrollbar {
  display: none;
}

.avatar-option {
  display: grid;
  place-items: center;
  width: var(--avatar-slot);
  height: var(--avatar-height);
  flex: 0 0 auto;
  border: 0;
  border-radius: 50%;
  padding: 4px;
  color: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  scroll-snap-align: center;
  scroll-snap-stop: always;
  transition: opacity 160ms ease;
}

.avatar-option-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: 50%;
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.3));
}

.avatar-option i {
  font-size: 23px;
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.3));
}

.avatar-option:not(.selected) {
  opacity: 0.72;
}

.avatar-option.selected {
  opacity: 1;
  filter: brightness(1.12);
}

.qa-load-state {
  padding: 8px 0 22px;
  color: rgba(255, 255, 255, 0.56);
  font-size: 12px;
  font-weight: 560;
  text-align: center;
}

.send-button {
  left: calc(var(--stage-left) + var(--composer-width) - 58px);
  top: calc(var(--composer-top) + var(--composer-expanded-height) - 58px);
  z-index: 22;
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 28px;
  color: rgba(255, 255, 255, 0.92);
  opacity: 0;
  pointer-events: none;
  cursor: pointer;
  touch-action: manipulation;
  transition:
    opacity 180ms ease,
    transform 220ms ease;
}

.send-button.visible {
  opacity: 0.45;
  pointer-events: auto;
}

.send-button.ready {
  opacity: 1;
}

.send-button:disabled {
  cursor: default;
}

.send-button i {
  position: relative;
  z-index: 1;
  font-size: 20px;
  line-height: 1;
}

.is-spinning {
  animation: spin 760ms linear infinite;
}

.status-toast {
  position: absolute;
  top: 34px;
  left: 50%;
  z-index: 30;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.62);
  color: rgba(255, 255, 255, 0.92);
  font-size: 13px;
  font-weight: 620;
  transform: translateX(-50%);
  animation: toast-in 260ms ease both;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes toast-in {
  from {
    opacity: 0;
    transform: translate(-50%, -10px) scale(0.94);
  }
  to {
    opacity: 1;
    transform: translate(-50%, 0) scale(1);
  }
}

@media (max-width: 760px) {
  .askbox-page {
    --page-pad: 22px;
    --stage-width: calc(100vw - 44px);
    --stage-left: 22px;
    --qa-width: var(--stage-width);
    --composer-width: var(--stage-width);
    --brand-top: 24px;
    --composer-top: 82px;
    --composer-expanded-height: 254px;
    --list-top: 154px;
    --list-bottom: 24px;
    --preview-top: calc(var(--composer-top) + var(--composer-expanded-height) + 14px);
  }

  .brand-strip {
    left: 22px;
  }

  .qa-list {
    left: 22px;
    width: calc(100vw - 44px);
    padding: 8px 0 28px;
  }

  .qa-card {
    min-height: 168px;
    margin-bottom: 34px;
  }

  .qa-question {
    font-size: 14px;
    line-height: 1.58;
  }

  .qa-answer {
    font-size: 13px;
    line-height: 1.68;
  }

  .detail-card {
    width: calc(100vw - 32px);
    max-height: calc(100vh - 72px);
    max-height: calc(100dvh - 72px);
    padding: 20px;
  }

  .detail-question {
    font-size: 16px;
  }

  .detail-answer {
    max-height: calc(100vh - 316px);
    max-height: calc(100dvh - 316px);
    font-size: 14px;
  }

  .draft-preview-card {
    width: var(--stage-width);
    padding: 18px 20px 20px;
  }

  .draft-preview-text {
    font-size: 16px;
    line-height: 1.6;
    -webkit-line-clamp: 5;
  }

  .composer-card {
    left: var(--stage-left);
    width: var(--composer-width);
  }

  .composer-card.expanded {
    min-height: 254px;
  }

  .composer-input {
    height: 144px;
  }

  .composer-tools {
    padding-right: 54px;
  }

  .avatar-selector {
    right: auto;
    bottom: auto;
  }

  .send-button {
    left: calc(var(--stage-left) + var(--composer-width) - 56px);
  }
}
</style>
