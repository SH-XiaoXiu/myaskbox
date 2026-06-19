<script setup>
import { LiquidGlass } from "@ybouane/liquidglass";
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { showSuccessToast, showToast } from "vant";
import { pageBackground } from "@/assets/background";
import { useAuthStore } from "@/stores/auth";
import { formatTime } from "@/utils";
import {
  getBoxProfile, updateBoxProfile,
  getBoxStats,
  getPendingQuestions, getHistoryQuestions,
  answerQuestion, dismissQuestion, deleteQuestion,
} from "@/api/owner";

const router = useRouter();
const auth = useAuthStore();

const boxProfile = ref({ displayName: "", slug: "", description: "", avatar: null, background: null });
const pendingQuestions = ref([]);
const publishedQA = ref([]);
const dismissedQuestions = ref([]);
const loading = ref(false);
const stats = ref({ pendingCount: 0, publishedCount: 0, dismissedCount: 0, todayReceivedCount: 0 });
const pageState = ref({
  pending: { page: 1, hasMore: false, loading: false },
  published: { page: 1, hasMore: false, loading: false },
  dismissed: { page: 1, hasMore: false, loading: false },
});

async function loadProfile() {
  const p = await getBoxProfile();
  if (!p) return;
  boxProfile.value = {
    displayName: p.displayName || "",
    slug: p.slug || "",
    description: p.description || "",
    avatar: p.avatar || null,
    background: p.background || null,
  };
  profileDraft.displayName = p.displayName || "";
  profileDraft.slug = p.slug || "";
  profileDraft.description = p.description || "";
}

async function loadStats() {
  stats.value = await getBoxStats();
}

async function loadPending(reset = false) {
  const s = pageState.value.pending;
  if (s.loading) return;
  s.loading = true;
  const page = reset ? 1 : s.page;
  try {
    const r = await getPendingQuestions(page, 20);
    const items = r.records.map(q => ({
      id: q.id,
      profile: { name: q.avatar?.name, contentBase64: q.avatar?.contentBase64, bg: q.avatar?.bg },
      question: q.question,
      ts: q.ts,
      time: formatTime(q.ts),
    }));
    pendingQuestions.value = reset ? items : [...pendingQuestions.value, ...items];
    s.page = page + 1;
    s.hasMore = page < r.totalPages;
  } finally {
    s.loading = false;
  }
}

async function loadHistory(status, reset = false) {
  const key = status === "PUBLISHED" ? "published" : "dismissed";
  const target = key === "published" ? publishedQA : dismissedQuestions;
  const s = pageState.value[key];
  if (s.loading) return;
  s.loading = true;
  const page = reset ? 1 : s.page;
  try {
    const r = await getHistoryQuestions(status, page, 20);
    const items = r.records.map(q => ({
      id: q.id,
      profile: { name: q.avatar?.name, contentBase64: q.avatar?.contentBase64, bg: q.avatar?.bg },
      ownerAvatar: q.ownerAvatar,
      question: q.question,
      answer: q.answer,
      ts: q.ts,
      time: formatTime(q.ts),
    }));
    target.value = reset ? items : [...target.value, ...items];
    s.page = page + 1;
    s.hasMore = page < r.totalPages;
  } finally {
    s.loading = false;
  }
}

async function handlePublishAnswer(qId, text) {
  await answerQuestion(qId, text);
  // 从 pending 移走，刷新
  pendingQuestions.value = pendingQuestions.value.filter(q => q.id !== qId);
  loadHistory("PUBLISHED", true);
  loadStats();
}

async function handleDismiss(qId) {
  await dismissQuestion(qId);
  pendingQuestions.value = pendingQuestions.value.filter(q => q.id !== qId);
  loadHistory("DISMISSED", true);
  loadStats();
}

async function handleDelete(qId) {
  await deleteQuestion(qId);
  dismissedQuestions.value = dismissedQuestions.value.filter(q => q.id !== qId);
}

async function handleUpdateProfile(data) {
  const updated = await updateBoxProfile(data);
  boxProfile.value = {
    ...boxProfile.value,
    ...updated,
    avatar: updated.avatar || null,
    background: updated.background || null,
  };
}

const rootRef = ref(null);
const bgRef = ref(null);
const drawerRef = ref(null);
const answerRef = ref(null);
const avatarInputRef = ref(null);

const activeTab = ref("pending");
const selectedQuestion = ref(null);
const answerText = ref("");
const answerError = ref(false);
const profileDraft = reactive({
  displayName: boxProfile.value.displayName,
  slug: boxProfile.value.slug,
  description: boxProfile.value.description,
});

let liquidGlass = null;
let initToken = 0;
let refreshFrame = 0;
let stableRefreshTimers = [];

const tabs = [
  { id: "pending", label: "待回答", icon: "ri-inbox-2-line" },
  { id: "published", label: "已发布", icon: "ri-chat-check-line" },
  { id: "dismissed", label: "已驳回", icon: "ri-archive-line" },
  { id: "settings", label: "设置", icon: "ri-settings-3-line" },
];

const topbarConfig = {
  blurAmount: 0.24,
  refraction: 0.64,
  chromAberration: 0.04,
  edgeHighlight: 0.06,
  specular: 0,
  fresnel: 1,
  cornerRadius: 34,
  zRadius: 34,
  brightness: -0.08,
  shadowOpacity: 0.28,
  shadowSpread: 14,
  shadowOffsetY: 2,
};

const drawerConfig = {
  blurAmount: 0.28,
  refraction: 0.68,
  chromAberration: 0.05,
  edgeHighlight: 0.05,
  specular: 0,
  fresnel: 1,
  cornerRadius: 34,
  zRadius: 36,
  brightness: -0.08,
  shadowOpacity: 0.34,
  shadowSpread: 18,
  shadowOffsetY: 3,
};

const allAnsweredCount = computed(() => stats.value.publishedCount);
const todayCount = computed(() => stats.value.todayReceivedCount);
const pendingCount = computed(() => stats.value.pendingCount);
const publicUrl = computed(() => `/box/${boxProfile.value.slug}`);
const displayedQuestions = computed(() => {
  if (activeTab.value === "pending") return pendingQuestions.value;
  if (activeTab.value === "published") return publishedQA.value;
  if (activeTab.value === "dismissed") return dismissedQuestions.value;
  return [];
});
const activeTabMeta = computed(() => tabs.find((tab) => tab.id === activeTab.value));
const selectedIsPending = computed(
  () => activeTab.value === "pending" && selectedQuestion.value,
);
const selectedIsPublished = computed(
  () => activeTab.value === "published" && selectedQuestion.value,
);
const selectedIsReadOnly = computed(
  () => (activeTab.value === "published" || activeTab.value === "dismissed") && selectedQuestion.value,
);
const answerCount = computed(() => `${answerText.value.length} / 5000`);
const canPublish = computed(() => answerText.value.trim().length > 0);

function configJson(config) {
  return JSON.stringify(config);
}

function scheduleGlassRefresh(element = rootRef.value) {
  if (refreshFrame) return;
  refreshFrame = requestAnimationFrame(() => {
    refreshFrame = 0;
    if (element) liquidGlass?.markChanged(element);
  });
}

function scheduleStableGlassRefresh() {
  stableRefreshTimers.forEach((timer) => window.clearTimeout(timer));
  stableRefreshTimers = [0, 80, 220].map((delay) =>
    window.setTimeout(() => scheduleGlassRefresh(rootRef.value), delay),
  );
}

function handleResize() {
  scheduleGlassRefresh(rootRef.value);
}

async function handleLogout() {
  await auth.logout();
  router.replace("/login");
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

  const glassElements = Array.from(root.children).filter((child) =>
    child.classList.contains("liquid-glass"),
  );

  try {
    const instance = await LiquidGlass.init({
      root,
      glassElements,
      defaults: {
        blurAmount: 0.2,
        refraction: 0.62,
        chromAberration: 0.04,
        edgeHighlight: 0.05,
        shadowOpacity: 0.25,
        shadowSpread: 12,
        shadowOffsetY: 2,
      },
    });

    if (token !== initToken) {
      instance.destroy();
      return;
    }

    liquidGlass = instance;
    liquidGlass.markChanged();
  } catch (error) {
    console.error("Failed to initialize owner LiquidGlass page", error);
  }
}

function selectTab(tabId) {
  activeTab.value = tabId;
  selectedQuestion.value = null;
  answerText.value = "";
  nextTick(() => scheduleGlassRefresh(rootRef.value));
}

function selectQuestion(question) {
  selectedQuestion.value = question;
  answerText.value = activeTab.value === "pending" ? "" : question.answer ?? "";
  answerError.value = false;
  nextTick(() => {
    if (activeTab.value === "pending") answerRef.value?.focus?.({ preventScroll: true });
  });
}

function loadActivePage() {
  if (activeTab.value === "pending") {
    loadPending(false);
    return;
  }
  if (activeTab.value === "published") {
    loadHistory("PUBLISHED", false);
    return;
  }
  if (activeTab.value === "dismissed") {
    loadHistory("DISMISSED", false);
  }
}

function currentPageState() {
  if (activeTab.value === "pending") return pageState.value.pending;
  if (activeTab.value === "published") return pageState.value.published;
  if (activeTab.value === "dismissed") return pageState.value.dismissed;
  return null;
}

function handleListScroll(event) {
  const state = currentPageState();
  if (!state || state.loading || !state.hasMore) return;
  const el = event.currentTarget;
  const distance = el.scrollHeight - el.scrollTop - el.clientHeight;
  if (distance < 180) loadActivePage();
}

function closeDrawer() {
  selectedQuestion.value = null;
  answerError.value = false;
}

async function publishSelected() {
  const question = selectedQuestion.value;
  const text = answerText.value.trim();
  if (!question || activeTab.value !== "pending") return;
  if (!text) {
    answerError.value = true;
    window.setTimeout(() => {
      answerError.value = false;
    }, 460);
    return;
  }

  await handlePublishAnswer(question.id, text);
  selectedQuestion.value = null;
  answerText.value = text;
  showSuccessToast("回答已发布");
}

async function dismissSelected() {
  const question = selectedQuestion.value;
  if (!question || activeTab.value !== "pending") return;
  await handleDismiss(question.id);
  selectedQuestion.value = null;
  answerText.value = "";
  showSuccessToast("问题已驳回");
}

function profilePayload(extra = {}) {
  return {
    displayName: profileDraft.displayName || boxProfile.value.displayName,
    slug: profileDraft.slug || boxProfile.value.slug,
    description: profileDraft.description ?? boxProfile.value.description ?? "",
    ...extra,
  };
}

async function saveProfile() {
  try {
    await handleUpdateProfile(profilePayload());
    showSuccessToast("设置已保存");
  } catch {}
  nextTick(() => scheduleGlassRefresh(rootRef.value));
}

function handleBackgroundFile(event) {
  const file = event.target.files?.[0];
  event.target.value = "";
  if (!file) return;
  const reader = new FileReader();
  reader.onload = async () => {
    try {
      await handleUpdateProfile(profilePayload({ backgroundBase64: String(reader.result || "") }));
      showSuccessToast("背景已更新");
    } catch {}
    nextTick(() => scheduleGlassRefresh(rootRef.value));
  };
  reader.readAsDataURL(file);
}

async function clearBackground() {
  try {
    await handleUpdateProfile(profilePayload({ backgroundBase64: "" }));
    showSuccessToast("已使用默认背景");
  } catch {}
  nextTick(() => scheduleGlassRefresh(rootRef.value));
}

function openAvatarPicker() {
  avatarInputRef.value?.click();
}

function handleAvatarFile(event) {
  const file = event.target.files?.[0];
  event.target.value = "";
  if (!file) return;

  const reader = new FileReader();
  reader.onload = async () => {
    try {
      await handleUpdateProfile(profilePayload({ avatarBase64: String(reader.result || "") }));
      showSuccessToast("头像已更新");
    } catch {}
    nextTick(() => scheduleGlassRefresh(rootRef.value));
  };
  reader.readAsDataURL(file);
}

async function copyPublicUrl() {
  const url = `${window.location.origin}${publicUrl.value}`;
  try {
    await navigator.clipboard?.writeText(url);
    showSuccessToast("公开页链接已复制");
  } catch {
    showToast(url);
  }
}

function questionTime(question) {
  return question?.ts ? formatTime(question.ts) : question?.time;
}

function avatarSrc(avatar) {
  if (!avatar) return "";
  return typeof avatar === "string" ? avatar : avatar.contentBase64 || "";
}

function avatarStyle(avatar, fallbackBg = "rgba(255,255,255,.16)") {
  return {
    backgroundColor: avatar?.bg || fallbackBg,
  };
}

watch(displayedQuestions, (questions) => {
  if (activeTab.value === "settings") return;
  if (!selectedQuestion.value || !questions.some((q) => q.id === selectedQuestion.value.id)) {
    selectedQuestion.value = null;
    answerText.value = "";
  }
});

watch(selectedQuestion, () => {
  nextTick(() => scheduleStableGlassRefresh());
});

watch(activeTab, () => {
  nextTick(() => scheduleStableGlassRefresh());
});

onMounted(async () => {
  try {
    await Promise.all([loadProfile(), loadStats(), loadPending(true), loadHistory("PUBLISHED", true), loadHistory("DISMISSED", true)]);
  } catch {
    // 已由拦截器 showToast
  }
  window.addEventListener("resize", handleResize);
  initLiquidGlass();
});

onBeforeUnmount(() => {
  initToken += 1;
  window.removeEventListener("resize", handleResize);
  if (refreshFrame) cancelAnimationFrame(refreshFrame);
  stableRefreshTimers.forEach((timer) => window.clearTimeout(timer));
  liquidGlass?.destroy();
  liquidGlass = null;
});
</script>

<template>
  <main
    ref="rootRef"
    class="owner-page"
    :class="{ 'drawer-open': selectedQuestion, 'readonly-open': selectedIsReadOnly }"
    aria-label="AskBox owner workspace"
  >
    <img
      ref="bgRef"
      class="owner-bg"
      :src="boxProfile.background?.contentBase64 || pageBackground.src"
      alt=""
      decoding="async"
      fetchpriority="high"
      loading="eager"
      draggable="false"
      @load="scheduleGlassRefresh()"
    />

    <header
      class="liquid-glass owner-topbar"
      :data-config="configJson(topbarConfig)"
    >
      <div class="owner-identity">
        <button class="owner-avatar" type="button" :style="avatarStyle(boxProfile.avatar)" aria-label="上传头像" @click="openAvatarPicker">
          <img v-if="avatarSrc(boxProfile.avatar)" :src="avatarSrc(boxProfile.avatar)" alt="" @load="scheduleGlassRefresh()" />
          <i v-else class="ri-user-heart-line" aria-hidden="true"></i>
        </button>
        <input
          ref="avatarInputRef"
          class="hidden-file-input"
          type="file"
          accept="image/png,image/jpeg,image/webp,image/gif"
          @change="handleAvatarFile"
        />
        <span>
          <strong>{{ boxProfile.displayName }}</strong>
          <em>{{ boxProfile.description }}</em>
        </span>
      </div>
      <div class="owner-actions">
        <button class="quiet-button" type="button" @click="copyPublicUrl">
          <i class="ri-links-line" aria-hidden="true"></i>
          <span>分享</span>
        </button>
        <a class="quiet-button" :href="publicUrl" target="_blank" rel="noreferrer">
          <i class="ri-external-link-line" aria-hidden="true"></i>
          <span>公开页</span>
        </a>
        <button class="quiet-button" type="button" @click="handleLogout">
          <i class="ri-logout-box-r-line" aria-hidden="true"></i>
          <span>退出</span>
        </button>
      </div>
    </header>

    <section v-show="!selectedQuestion" class="owner-workspace" aria-label="箱主控制台">
      <aside class="owner-rail" aria-label="控制台导航">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          class="owner-tab"
          :class="{ active: activeTab === tab.id }"
          type="button"
          @click="selectTab(tab.id)"
        >
          <i :class="tab.icon" aria-hidden="true"></i>
          <span>{{ tab.label }}</span>
        </button>
      </aside>

      <section class="owner-main">
        <div class="owner-summary">
          <article class="metric-block">
            <span>待回答</span>
            <strong>{{ pendingCount }}</strong>
          </article>
          <article class="metric-block">
            <span>已发布</span>
            <strong>{{ allAnsweredCount }}</strong>
          </article>
          <article class="metric-block">
            <span>今日收到</span>
            <strong>{{ todayCount }}</strong>
          </article>
          <article class="metric-block">
            <span>公开地址</span>
            <strong>{{ publicUrl }}</strong>
          </article>
        </div>

        <section v-if="activeTab !== 'settings'" class="owner-list" :aria-label="activeTabMeta?.label" @scroll="handleListScroll">
          <header class="list-head">
            <span>
              <i :class="activeTabMeta?.icon" aria-hidden="true"></i>
              {{ activeTabMeta?.label }}
            </span>
            <em>{{ displayedQuestions.length }} 条</em>
          </header>

          <article
            v-for="question in displayedQuestions"
            :key="question.id"
            class="owner-question"
            :class="{ selected: selectedQuestion?.id === question.id }"
            role="button"
            tabindex="0"
            @click="selectQuestion(question)"
            @keydown.enter.prevent="selectQuestion(question)"
            @keydown.space.prevent="selectQuestion(question)"
          >
            <header>
              <span class="question-author">
                <span class="mini-avatar" :style="avatarStyle(question.profile)">
                  <img v-if="avatarSrc(question.profile)" :src="avatarSrc(question.profile)" alt="" @load="scheduleGlassRefresh()" />
                </span>
              </span>
              <time>{{ questionTime(question) }}</time>
            </header>
            <p>{{ question.question }}</p>
            <blockquote v-if="question.answer">{{ question.answer }}</blockquote>
          </article>

          <div v-if="displayedQuestions.length === 0" class="owner-empty">
            <i class="ri-checkbox-circle-line" aria-hidden="true"></i>
            <span>这里暂时没有内容</span>
          </div>

          <div v-else-if="currentPageState()?.loading || currentPageState()?.hasMore" class="owner-load-state" aria-live="polite">
            {{ currentPageState()?.loading ? "加载中" : "继续下滑加载更多" }}
          </div>
        </section>

        <section v-else class="settings-panel" aria-label="提问箱设置">
          <header class="list-head">
            <span><i class="ri-settings-3-line" aria-hidden="true"></i>设置</span>
            <em>公开页资料</em>
          </header>
          <label>
            <span>箱子名称</span>
            <input v-model="profileDraft.displayName" maxlength="32" />
          </label>
          <label>
            <span>公开地址</span>
            <input v-model="profileDraft.slug" maxlength="32" />
          </label>
          <label>
            <span>简介</span>
            <textarea v-model="profileDraft.description" rows="5" maxlength="160"></textarea>
          </label>
          <div class="media-settings">
            <section class="media-setting">
              <span>背景</span>
              <div class="media-actions">
                <label class="file-button">
                  <i class="ri-image-add-line" aria-hidden="true"></i>
                  <span>修改背景</span>
                  <input type="file" accept="image/png,image/jpeg,image/webp,image/gif" @change="handleBackgroundFile" />
                </label>
                <button class="plain-action" type="button" @click="clearBackground">清空背景</button>
              </div>
            </section>
          </div>
          <div class="settings-actions">
            <button class="settings-save" type="button" @click="saveProfile">
              <i class="ri-save-3-line" aria-hidden="true"></i>
              <span>保存设置</span>
            </button>
            <button class="settings-save secondary" type="button" @click="router.push('/password')">
              <i class="ri-lock-password-line" aria-hidden="true"></i>
              <span>修改密码</span>
            </button>
          </div>
        </section>
      </section>
    </section>

    <button
      class="drawer-scrim"
      :class="{ visible: selectedQuestion }"
      :tabindex="selectedQuestion ? 0 : -1"
      type="button"
      aria-label="关闭详情"
      @click="closeDrawer"
    ></button>

    <aside
      ref="drawerRef"
      class="liquid-glass owner-drawer"
      :class="{ visible: selectedQuestion, readonly: selectedIsReadOnly }"
      :data-config="configJson(drawerConfig)"
      aria-label="问题详情"
    >
      <template v-if="selectedQuestion">
        <header class="drawer-head">
          <span class="question-author">
            <span
              class="mini-avatar"
              :style="avatarStyle(selectedIsPublished && selectedQuestion.ownerAvatar ? selectedQuestion.ownerAvatar : selectedQuestion.profile)"
            >
              <img
                v-if="avatarSrc(selectedIsPublished && selectedQuestion.ownerAvatar ? selectedQuestion.ownerAvatar : selectedQuestion.profile)"
                :src="avatarSrc(selectedIsPublished && selectedQuestion.ownerAvatar ? selectedQuestion.ownerAvatar : selectedQuestion.profile)"
                alt=""
                @load="scheduleGlassRefresh()"
              />
            </span>
          </span>
          <button class="drawer-close" type="button" aria-label="关闭详情" @click="closeDrawer">
            <i class="ri-close-line" aria-hidden="true"></i>
          </button>
        </header>
        <time class="drawer-time">{{ questionTime(selectedQuestion) }}</time>
        <p class="drawer-question">{{ selectedQuestion.question }}</p>

        <section v-if="selectedIsPending" class="answer-editor">
          <textarea
            ref="answerRef"
            v-model="answerText"
            :class="{ error: answerError }"
            rows="7"
            maxlength="5000"
            placeholder="写下你的回答。"
          ></textarea>
          <footer>
            <span>{{ answerCount }}</span>
            <div>
              <button class="plain-action" type="button" @click="dismissSelected">驳回</button>
              <button
                class="publish-action"
                :class="{ ready: canPublish }"
                :disabled="!canPublish"
                type="button"
                @click="publishSelected"
              >
                <i class="ri-send-plane-fill" aria-hidden="true"></i>
                <span>发布回答</span>
              </button>
            </div>
          </footer>
        </section>

        <section v-else-if="selectedIsPublished" class="published-answer">
          <span>公开回答</span>
          <p>{{ selectedQuestion.answer }}</p>
        </section>

        <section v-else class="published-answer muted">
          <span>已驳回</span>
          <p>这个问题不会显示在公开页，可以在后续接入真实接口后增加恢复或删除操作。</p>
        </section>
      </template>
    </aside>

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

.owner-page {
  position: fixed;
  inset: 0;
  overflow: hidden;
  isolation: isolate;
  background: #0c0f14;
  color: #fff;
  font-family: inherit;
  --stage-width: min(920px, calc(100vw - 48px));
  --stage-left: calc((100vw - var(--stage-width)) / 2);
}

.owner-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  pointer-events: none;
  user-select: none;
}

.liquid-glass {
  position: absolute;
  border: 0;
  background: transparent;
  font: inherit;
}

.owner-topbar {
  top: 24px;
  left: var(--stage-left);
  z-index: 12;
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: var(--stage-width);
  min-height: 76px;
  padding: 12px 16px;
  border-radius: 34px;
}

.owner-identity,
.question-author {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  min-width: 0;
  gap: 10px;
}

.owner-avatar,
.mini-avatar {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  border: 0;
  border-radius: 50%;
  background-position: center;
  background-size: cover;
  color: #fff;
}

.owner-avatar {
  width: 46px;
  height: 46px;
  padding: 0;
  background: rgba(255, 255, 255, 0.16);
  cursor: pointer;
  font-size: 23px;
  overflow: hidden;
  transition:
    background 180ms ease,
    transform 180ms ease;
}

.owner-avatar:hover {
  background: rgba(255, 255, 255, 0.22);
  transform: scale(1.04);
}

.owner-avatar:focus-visible {
  outline: 2px solid rgba(255, 255, 255, 0.78);
  outline-offset: 3px;
}

.mini-avatar {
  width: 28px;
  height: 28px;
  font-size: 16px;
  overflow: hidden;
}

.owner-avatar img,
.mini-avatar img {
  width: 100%;
  height: 100%;
  border-radius: inherit;
  object-fit: cover;
  clip-path: circle(50%);
}

.hidden-file-input {
  position: fixed;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.owner-identity strong,
.owner-identity em {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  letter-spacing: 0;
  font-style: normal;
  line-height: 1.2;
}

.owner-identity strong {
  max-width: 360px;
  font-size: 18px;
  font-weight: 760;
}

.owner-identity em {
  max-width: 420px;
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.62);
  font-size: 12px;
  font-weight: 520;
}

.owner-actions {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 8px;
}

.quiet-button,
.owner-tab,
.drawer-close,
.plain-action,
.settings-save {
  border: 0;
  font: inherit;
  cursor: pointer;
}

.quiet-button {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 40px;
  padding: 0 14px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.88);
  text-decoration: none;
  font-size: 13px;
  font-weight: 650;
}

.owner-workspace {
  position: absolute;
  left: var(--stage-left);
  top: 126px;
  bottom: 24px;
  z-index: 3;
  display: grid;
  width: var(--stage-width);
  grid-template-columns: 116px minmax(0, 1fr);
  gap: 24px;
  transition:
    opacity 180ms ease,
    transform 220ms cubic-bezier(0.2, 0.8, 0.2, 1);
}

.drawer-open .owner-workspace {
  opacity: 0;
  visibility: hidden;
  pointer-events: none;
  transform: scale(0.98);
  transition: none;
}

.owner-rail {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 8px;
}

.owner-tab {
  display: flex;
  align-items: center;
  gap: 9px;
  min-height: 42px;
  padding: 0 12px;
  border-radius: 22px;
  background: transparent;
  color: rgba(255, 255, 255, 0.66);
  font-size: 13px;
  font-weight: 640;
  text-align: left;
}

.owner-tab.active {
  background: rgba(255, 255, 255, 0.16);
  color: rgba(255, 255, 255, 0.96);
}

.owner-main {
  min-width: 0;
  overflow: hidden;
}

.owner-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 22px;
}

.metric-block {
  min-height: 74px;
  padding: 12px 14px;
  border-radius: 22px;
  background: rgba(0, 0, 0, 0.16);
}

.metric-block span,
.metric-block strong {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric-block span {
  color: rgba(255, 255, 255, 0.56);
  font-size: 12px;
  font-weight: 560;
}

.metric-block strong {
  margin-top: 8px;
  color: rgba(255, 255, 255, 0.94);
  font-size: 22px;
  font-weight: 780;
  letter-spacing: 0;
}

.owner-list,
.settings-panel {
  height: calc(100% - 96px);
  overflow-x: hidden;
  overflow-y: auto;
  padding: 2px 8px 120px 0;
  scrollbar-width: none;
  overscroll-behavior: contain;
}

.owner-list::-webkit-scrollbar,
.settings-panel::-webkit-scrollbar {
  display: none;
}

.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  color: rgba(255, 255, 255, 0.92);
}

.list-head span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 760;
}

.list-head em {
  color: rgba(255, 255, 255, 0.56);
  font-style: normal;
  font-size: 12px;
}

.owner-question {
  position: relative;
  min-height: 146px;
  margin-bottom: 24px;
  padding: 0;
  color: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  overflow: hidden;
  overflow-wrap: anywhere;
  transition:
    opacity 180ms ease,
    transform 240ms cubic-bezier(0.16, 1, 0.3, 1);
}

.owner-question:hover,
.owner-question.selected {
  transform: translateX(8px);
}

.owner-question.selected::before {
  content: "";
  position: absolute;
  top: 36px;
  left: -14px;
  width: 4px;
  height: 54px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
}

.owner-question header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 12px;
}

.question-author {
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  font-weight: 680;
}

.question-author > span:last-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.owner-question time {
  flex: 0 0 auto;
  color: rgba(255, 255, 255, 0.52);
  font-size: 12px;
}

.owner-question p {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: rgba(255, 255, 255, 0.98);
  font-size: 16px;
  font-weight: 720;
  line-height: 1.62;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.owner-question blockquote {
  display: -webkit-box;
  margin: 12px 0 0;
  overflow: hidden;
  padding-left: 13px;
  border-left: 2px solid rgba(255, 255, 255, 0.22);
  color: rgba(255, 255, 255, 0.66);
  font-size: 13px;
  line-height: 1.68;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.owner-empty {
  display: grid;
  place-items: center;
  min-height: 260px;
  gap: 10px;
  color: rgba(255, 255, 255, 0.58);
  font-size: 13px;
}

.owner-empty i {
  font-size: 30px;
}

.owner-load-state {
  padding: 4px 0 30px;
  color: rgba(255, 255, 255, 0.52);
  font-size: 12px;
  font-weight: 560;
  text-align: center;
}

.settings-panel {
  display: grid;
  align-content: start;
  gap: 12px;
}

.settings-panel > label {
  display: grid;
  gap: 7px;
  color: rgba(255, 255, 255, 0.64);
  font-size: 12px;
  font-weight: 650;
}

.settings-panel input,
.settings-panel textarea {
  width: 100%;
  border: 1px solid rgba(255, 255, 255, 0.26);
  border-radius: 16px;
  outline: none;
  padding: 13px 15px;
  background: rgba(0, 0, 0, 0.2);
  color: rgba(255, 255, 255, 0.94);
  font: inherit;
  font-size: 14px;
  line-height: 1.6;
  resize: none;
}

.settings-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 6px 0 max(28px, env(safe-area-inset-bottom));
}

.settings-save {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 42px;
  margin-top: 6px;
  padding: 0 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.88);
  color: rgba(20, 18, 28, 0.92);
  font-size: 13px;
  font-weight: 760;
}

.settings-save.secondary {
  background: rgba(255, 255, 255, 0.16);
  color: rgba(255, 255, 255, 0.9);
}

.media-settings {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 12px;
}

.media-setting {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
  min-width: 0;
}

.media-setting > span {
  grid-column: 1 / -1;
  color: rgba(255, 255, 255, 0.64);
  font-size: 12px;
  font-weight: 650;
}

.media-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-self: stretch;
}

.file-button {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-height: 42px;
  padding: 0 14px;
  border: 0;
  border-radius: 19px;
  background: rgba(255, 255, 255, 0.14);
  color: rgba(255, 255, 255, 0.88);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.media-actions .file-button,
.media-actions .plain-action {
  min-width: 132px;
}

.file-button input {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  border: 0;
  padding: 0;
  opacity: 0;
  cursor: pointer;
}

.drawer-scrim {
  position: absolute;
  inset: 0;
  z-index: 14;
  border: 0;
  background: rgba(6, 5, 12, 0.52);
  cursor: pointer;
  opacity: 0;
  pointer-events: none;
  will-change: opacity;
}

.drawer-scrim.visible {
  opacity: 1;
  pointer-events: auto;
  transition: opacity 220ms ease-out;
}

.drawer-scrim:not(.visible) {
  opacity: 0;
  pointer-events: none;
  transition: opacity 180ms ease-in;
}

.owner-drawer {
  top: 50%;
  left: 50%;
  z-index: 16;
  width: min(560px, calc(100vw - 48px));
  max-height: min(680px, calc(100vh - 92px));
  max-height: min(680px, calc(100dvh - 92px));
  padding: 20px;
  border-radius: 34px;
  color: rgba(255, 255, 255, 0.94);
  overflow: hidden;
  opacity: 0;
  pointer-events: none;
  transform: translate(-50%, -46%) scale(0.94);
  transition:
    opacity 220ms ease,
    transform 340ms cubic-bezier(0.16, 1, 0.3, 1);
}

.owner-drawer.visible {
  opacity: 1;
  pointer-events: auto;
  transform: translate(-50%, -50%) scale(1);
}

.owner-drawer::before {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 0;
  border-radius: inherit;
  background: rgba(8, 7, 16, 0.14);
  pointer-events: none;
}

.drawer-head,
.drawer-time,
.drawer-question,
.answer-editor,
.published-answer {
  position: relative;
  z-index: 1;
}

.drawer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.drawer-close {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
  color: rgba(255, 255, 255, 0.88);
  font-size: 20px;
}

.drawer-time {
  display: block;
  margin-top: 10px;
  color: rgba(255, 255, 255, 0.54);
  font-size: 12px;
}

.drawer-question {
  margin: 18px 0 18px;
  color: rgba(255, 255, 255, 0.98);
  font-size: 17px;
  font-weight: 740;
  line-height: 1.65;
  overflow-wrap: anywhere;
}

.answer-editor textarea {
  width: 100%;
  min-height: 190px;
  max-height: min(42vh, 300px);
  border: 1px solid rgba(255, 255, 255, 0.34);
  border-radius: 26px;
  outline: none;
  resize: none;
  padding: 15px 16px;
  background: rgba(255, 255, 255, 0.34);
  color: rgba(255, 255, 255, 0.96);
  font: inherit;
  font-size: 14px;
  line-height: 1.7;
}

.answer-editor textarea.error {
  animation: editor-shake 360ms ease;
  border-color: rgba(255, 116, 116, 0.9);
}

.answer-editor footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 12px;
}

.answer-editor footer > span {
  color: rgba(255, 255, 255, 0.54);
  font-size: 12px;
}

.answer-editor footer > div {
  display: flex;
  gap: 8px;
}

.plain-action {
  min-height: 40px;
  padding: 0 14px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.74);
  font-size: 13px;
  font-weight: 680;
}

.publish-action {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 40px;
  padding: 0 15px;
  border-radius: 24px;
  background: rgba(0, 0, 0, 0.38);
  color: rgba(255, 255, 255, 0.95);
  opacity: 0.5;
  cursor: pointer;
}

.publish-action.ready {
  background: rgba(255, 255, 255, 0.88);
  color: rgba(20, 18, 28, 0.92);
  opacity: 1;
}

.publish-action:disabled {
  cursor: default;
}

.publish-action > * {
  position: relative;
  z-index: 1;
}

.published-answer {
  max-height: min(360px, calc(100vh - 360px));
  overflow-y: auto;
  padding-left: 14px;
  border-left: 2px solid rgba(255, 255, 255, 0.28);
  scrollbar-width: none;
}

.published-answer::-webkit-scrollbar {
  display: none;
}

.published-answer span {
  display: block;
  margin-bottom: 10px;
  color: rgba(255, 255, 255, 0.56);
  font-size: 12px;
  font-weight: 680;
}

.published-answer p {
  margin: 0;
  color: rgba(255, 255, 255, 0.78);
  font-size: 14px;
  line-height: 1.78;
  overflow-wrap: anywhere;
}

.published-answer.muted p {
  color: rgba(255, 255, 255, 0.62);
}

@keyframes editor-shake {
  20% {
    transform: translateX(-5px);
  }
  40% {
    transform: translateX(4px);
  }
  60% {
    transform: translateX(-3px);
  }
  80% {
    transform: translateX(2px);
  }
}

@media (max-width: 940px) {
  .owner-page {
    --stage-width: calc(100vw - 36px);
    --stage-left: 18px;
  }

  .owner-topbar {
    top: 18px;
    min-height: 68px;
  }

  .owner-actions span {
    display: none;
  }

  .owner-workspace {
    top: 108px;
    bottom: 18px;
    width: var(--stage-width);
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .owner-rail {
    flex-direction: row;
    overflow-x: auto;
    padding: 0 0 2px;
    scrollbar-width: none;
  }

  .owner-rail::-webkit-scrollbar {
    display: none;
  }

  .owner-tab {
    flex: 0 0 auto;
  }

  .owner-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .owner-list,
  .settings-panel {
    height: calc(100dvh - 286px);
  }

  .owner-drawer {
    width: calc(100vw - 36px);
    max-height: min(76vh, 680px);
  }
}

@media (max-width: 560px) {
  .owner-identity strong {
    max-width: 190px;
    font-size: 16px;
  }

  .owner-identity em {
    max-width: 190px;
  }

  .owner-summary {
    grid-template-columns: 1fr 1fr;
  }

  .media-setting {
    grid-template-columns: 1fr;
  }

  .metric-block {
    min-height: 66px;
  }

  .metric-block strong {
    font-size: 18px;
  }

  .owner-question {
    min-height: 132px;
    margin-bottom: 22px;
  }

  .drawer-question {
    font-size: 16px;
  }

  .answer-editor footer {
    align-items: stretch;
    flex-direction: column;
  }

  .answer-editor footer > div {
    justify-content: flex-end;
  }
}
</style>
