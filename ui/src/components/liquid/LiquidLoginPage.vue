<script setup>
import { animate } from "motion";
import { LiquidGlass } from "@ybouane/liquidglass";
import { nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { pageBackground } from "@/assets/background";
import { getRegisterConfig, sendLoginCode } from "@/api/auth";
import { useAuthStore } from "@/stores/auth";

const router = useRouter();
const authStore = useAuthStore();

const rootRef = ref(null);
const bgRef = ref(null);
const cardRef = ref(null);
const buttonRef = ref(null);
const emailRef = ref(null);

const loginMode = ref("password");
const email = ref("");
const password = ref("");
const code = ref("");
const showPassword = ref(false);
const loading = ref(false);
const sendingCode = ref(false);
const countdown = ref(0);
const error = ref("");
const success = ref(false);
const registerEnabled = ref(false);

let liquidGlass = null;
let initToken = 0;
let glassRefreshFrame = 0;
let stableRefreshTimers = [];
let cardAnimation = null;
let errorAnimation = null;
let countdownTimer = 0;

// ==================== LiquidGlass configs ====================
const cardConfig = {
  blurAmount: 0.25,
  refraction: 0.69,
  chromAberration: 0.05,
  edgeHighlight: 0.05,
  specular: 0,
  fresnel: 1,
  distortion: 0,
  cornerRadius: 56,
  zRadius: 56,
  opacity: 1,
  saturation: 0,
  tintStrength: 0,
  brightness: 0,
  shadowOpacity: 0.3,
  shadowSpread: 10,
  shadowOffsetY: 0,
  floating: false,
  bevelMode: 0,
};

const buttonConfig = {
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

function configJson(config) {
  return JSON.stringify(config);
}

function scheduleGlassRefresh() {
  if (glassRefreshFrame) return;
  glassRefreshFrame = requestAnimationFrame(() => {
    glassRefreshFrame = 0;
    liquidGlass?.markChanged(rootRef.value);
  });
}

function scheduleStableGlassRefresh() {
  stableRefreshTimers.forEach((t) => window.clearTimeout(t));
  stableRefreshTimers = [0, 80, 220].map((delay) =>
    window.setTimeout(() => scheduleGlassRefresh(), delay),
  );
}

// ==================== LiquidGlass init ====================
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
  } catch (err) {
    console.error("Failed to initialize Login page LiquidGlass", err);
  }
}

// ==================== Animations ====================
function stopAnimation(ref) {
  if (ref) {
    ref.stop();
    ref = null;
  }
}

function animateCardIn() {
  cardAnimation = animate(
    cardRef.value,
    {
      opacity: [0, 1],
      transform: ["translate(-50%, -46%) scale(0.92)", "translate(-50%, -50%) scale(1)"],
    },
    {
      duration: 0.48,
      easing: [0.16, 1, 0.3, 1],
      onComplete: () => {
        // Remove inline transform so CSS float animation takes over
        if (cardRef.value) {
          cardRef.value.style.transform = "";
          cardRef.value.style.opacity = "";
        }
      },
    },
  );
}

function animateErrorShake() {
  if (errorAnimation) {
    errorAnimation.stop();
    errorAnimation = null;
  }
  errorAnimation = animate(
    cardRef.value,
    { transform: [
      "translate(-50%, -50%) translateX(0)",
      "translate(-50%, -50%) translateX(-6px)",
      "translate(-50%, -50%) translateX(6px)",
      "translate(-50%, -50%) translateX(-6px)",
      "translate(-50%, -50%) translateX(0)",
    ]},
    { duration: 0.38, easing: "ease-in-out" },
  );
}

// ==================== Form handlers ====================
function canSubmit() {
  if (loading.value) return false;
  if (!email.value.trim()) return false;
  if (loginMode.value === "code") return code.value.trim().length > 0;
  return password.value.trim().length > 0;
}

function canSendCode() {
  return email.value.trim().length > 0 && !loading.value && !sendingCode.value && countdown.value <= 0;
}

async function handleSubmit() {
  if (!canSubmit()) return;

  loading.value = true;
  error.value = "";
  success.value = false;

  try {
    if (loginMode.value === "code") {
      await authStore.loginWithCode(email.value.trim(), code.value.trim())
    } else {
      await authStore.login(email.value.trim(), password.value)
    }
    success.value = true
    // 短暂展示 success 状态后跳转
    await new Promise((resolve) => setTimeout(resolve, 600))
    const redirect = router.currentRoute.value.query.redirect
    router.push(resolveLoginTarget(redirect))
  } catch (err) {
    error.value = err.message || (loginMode.value === "code" ? "邮箱验证码错误或已过期" : "邮箱或密码错误")
    animateErrorShake()
  } finally {
    loading.value = false
  }
}

async function handleSendCode() {
  if (!canSendCode()) return;
  sendingCode.value = true;
  error.value = "";
  try {
    await sendLoginCode(email.value.trim());
    startCountdown();
  } catch (err) {
    error.value = err.message || "验证码发送失败";
    animateErrorShake();
  } finally {
    sendingCode.value = false;
  }
}

function startCountdown() {
  countdown.value = 60;
  if (countdownTimer) window.clearInterval(countdownTimer);
  countdownTimer = window.setInterval(() => {
    countdown.value -= 1;
    if (countdown.value <= 0) {
      window.clearInterval(countdownTimer);
      countdownTimer = 0;
    }
  }, 1000);
}

function resolveLoginTarget(redirect) {
  if (typeof redirect !== 'string' || !redirect.startsWith('/') || redirect.startsWith('//')) {
    return authStore.landingPath
  }
  const target = router.resolve(redirect)
  const excludedRouteNames = ['login', 'unauthorized', 'forbidden', 'not-found']
  if (target.path === '/' || excludedRouteNames.includes(target.name)) {
    return authStore.landingPath
  }
  const roles = target.meta.roles || []
  if (roles.length && !authStore.hasAnyRole(roles)) {
    return authStore.landingPath
  }
  return redirect
}

function togglePassword() {
  showPassword.value = !showPassword.value;
}

function switchLoginMode(mode) {
  loginMode.value = mode;
  error.value = "";
  nextTick(() => scheduleStableGlassRefresh());
}

function goHome() {
  router.push("/");
}

// ==================== Lifecycle ====================
onMounted(() => {
  getRegisterConfig().then((config) => {
    registerEnabled.value = Boolean(config?.enabled);
  }).catch(() => {
    registerEnabled.value = false;
  });
  initLiquidGlass();
  nextTick(() => {
    animateCardIn();
    emailRef.value?.focus();
  });
});

onBeforeUnmount(() => {
  initToken += 1;
  stableRefreshTimers.forEach((t) => window.clearTimeout(t));
  if (countdownTimer) window.clearInterval(countdownTimer);
  stopAnimation(cardAnimation);
  stopAnimation(errorAnimation);
  liquidGlass?.destroy();
  liquidGlass = null;
});
</script>

<template>
  <main ref="rootRef" class="login-page" aria-label="登录 AskBox">
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
        <strong>AskBox</strong>
        <em>匿名提问箱</em>
      </span>
    </header>

    <article
      ref="cardRef"
      class="liquid-glass login-card"
      :class="{ 'has-error': error, 'has-success': success }"
      :data-config="configJson(cardConfig)"
      role="form"
      aria-label="登录表单"
    >
      <div class="login-brand">
        <span class="login-brand-icon"><i class="ri-user-star-line" aria-hidden="true"></i></span>
        <h1 class="login-title">登录 AskBox</h1>
        <p class="login-desc">管理你的匿名提问箱</p>
      </div>

      <form class="login-form" @submit.prevent="handleSubmit">
        <div class="login-tabs" role="tablist" aria-label="登录方式">
          <button
            type="button"
            role="tab"
            :aria-selected="loginMode === 'password'"
            :class="{ active: loginMode === 'password' }"
            @click="switchLoginMode('password')"
          >
            密码登录
          </button>
          <button
            type="button"
            role="tab"
            :aria-selected="loginMode === 'code'"
            :class="{ active: loginMode === 'code' }"
            @click="switchLoginMode('code')"
          >
            验证码登录
          </button>
        </div>

        <div class="field">
          <label class="field-label" for="login-email">
            <i class="ri-mail-line" aria-hidden="true"></i>
          </label>
          <input
            id="login-email"
            ref="emailRef"
            v-model="email"
            class="field-input"
            type="email"
            name="email"
            autocomplete="email"
            placeholder="邮箱"
            :disabled="loading"
            aria-label="邮箱"
          />
        </div>

        <div v-if="loginMode === 'password'" class="field">
          <label class="field-label" for="login-password">
            <i class="ri-lock-line" aria-hidden="true"></i>
          </label>
          <input
            id="login-password"
            v-model="password"
            class="field-input"
            :type="showPassword ? 'text' : 'password'"
            name="password"
            autocomplete="current-password"
            placeholder="密码"
            :disabled="loading"
            aria-label="密码"
          />
          <button
            class="field-toggle"
            type="button"
            :aria-label="showPassword ? '隐藏密码' : '显示密码'"
            :tabindex="loading ? -1 : 0"
            @click="togglePassword"
          >
            <i :class="showPassword ? 'ri-eye-off-line' : 'ri-eye-line'" aria-hidden="true"></i>
          </button>
        </div>

        <div v-else class="code-field">
          <div class="field">
            <label class="field-label" for="login-code">
              <i class="ri-shield-keyhole-line" aria-hidden="true"></i>
            </label>
            <input
              id="login-code"
              v-model="code"
              class="field-input"
              type="text"
              inputmode="numeric"
              maxlength="12"
              autocomplete="one-time-code"
              placeholder="邮箱验证码"
              :disabled="loading"
              aria-label="邮箱验证码"
            />
          </div>
          <button class="code-button" type="button" :disabled="!canSendCode()" @click="handleSendCode">
            {{ sendingCode ? '发送中' : countdown > 0 ? `${countdown}s` : '验证码' }}
          </button>
        </div>

        <div v-if="error" class="login-error" role="alert" aria-live="assertive">
          <i class="ri-error-warning-line" aria-hidden="true"></i>
          <span>{{ error }}</span>
        </div>

        <button
          ref="buttonRef"
          class="login-button"
          :class="{ loading: loading, enabled: canSubmit() }"
          :disabled="!canSubmit()"
          type="submit"
        >
          <i v-if="loading" class="ri-loader-4-line is-spinning" aria-hidden="true"></i>
          <i v-else class="ri-login-box-line" aria-hidden="true"></i>
          <span>{{ loading ? "登录中..." : "登录" }}</span>
        </button>
      </form>

      <button class="login-home-link" type="button" @click="goHome">
        <i class="ri-arrow-left-line" aria-hidden="true"></i>
        <span>返回首页</span>
      </button>
      <button v-if="registerEnabled" class="login-home-link" type="button" @click="router.push('/register')">
        <i class="ri-user-add-line" aria-hidden="true"></i>
        <span>注册账号</span>
      </button>
    </article>
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

.login-page {
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

/* ============ Brand strip (shared with other pages) ============ */
.brand-strip {
  position: absolute;
  top: 32px;
  left: 26px;
  z-index: 4;
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.92);
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.28);
  pointer-events: none;
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
}

.brand-strip strong {
  font-size: 17px;
  font-weight: 750;
}

.brand-strip em {
  margin-top: 3px;
  color: rgba(255, 255, 255, 0.64);
  font-size: 12px;
  font-weight: 520;
}

/* ============ Liquid glass base ============ */
.liquid-glass {
  position: absolute;
  border: 0;
  background: transparent;
  font: inherit;
}

/* ============ Login card ============ */
.login-card {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 10;
  width: min(420px, calc(100vw - 48px));
  padding: 34px 30px 28px;
  border-radius: 56px;
  color: rgba(255, 255, 255, 0.94);
  overflow: hidden;
  transform: translate(-50%, -50%);
  animation: login-float 5s ease-in-out infinite;
  will-change: opacity, transform;
}

@keyframes login-float {
  0%, 100% {
    transform: translate(-50%, -50%) translateY(0);
  }
  50% {
    transform: translate(-50%, -50%) translateY(-8px);
  }
}

.login-card::before {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 0;
  border-radius: inherit;
  background: rgba(9, 8, 16, 0.14);
  pointer-events: none;
}

/* ============ Brand inside card ============ */
.login-brand {
  position: relative;
  z-index: 1;
  text-align: center;
  margin-bottom: 28px;
}

.login-brand-icon {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  margin: 0 auto 14px;
  border-radius: 16px;
  background: rgba(79, 70, 229, 0.22);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.18);
}

.login-brand-icon i {
  font-size: 24px;
  line-height: 1;
  color: rgba(165, 160, 255, 0.92);
}

.login-title {
  margin: 0 0 6px;
  color: rgba(255, 255, 255, 0.94);
  font-size: 22px;
  font-weight: 750;
  letter-spacing: 0;
  line-height: 1.28;
}

.login-desc {
  margin: 0;
  color: rgba(255, 255, 255, 0.56);
  font-size: 13px;
  font-weight: 480;
  line-height: 1.4;
}

/* ============ Form fields ============ */
.login-form {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.login-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px;
  min-height: 44px;
  padding: 4px;
  border-radius: 16px;
  background: rgba(0, 0, 0, 0.24);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

.login-tabs button {
  min-width: 0;
  min-height: 36px;
  border: 0;
  border-radius: 12px;
  background: transparent;
  color: rgba(255, 255, 255, 0.58);
  font: inherit;
  font-size: 13px;
  font-weight: 680;
  cursor: pointer;
  transition: background 160ms ease, color 160ms ease;
}

.login-tabs button.active {
  background: rgba(255, 255, 255, 0.16);
  color: rgba(255, 255, 255, 0.94);
}

.login-tabs button:focus-visible {
  outline: 2px solid rgba(255, 255, 255, 0.78);
  outline-offset: 2px;
}

.field {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  border-radius: 16px;
  background: rgba(0, 0, 0, 0.28);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06);
  transition: box-shadow 200ms ease, background 200ms ease;
}

.field:focus-within {
  background: rgba(0, 0, 0, 0.18);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.1),
    0 0 0 1px rgba(255, 255, 255, 0.2);
}

.field-label {
  display: grid;
  place-items: center;
  width: 44px;
  height: 48px;
  flex: 0 0 auto;
  color: rgba(255, 255, 255, 0.48);
  transition: color 200ms ease;
}

.field:focus-within .field-label {
  color: rgba(255, 255, 255, 0.72);
}

.field-label i {
  font-size: 18px;
  line-height: 1;
}

.field-input {
  flex: 1 1 auto;
  min-width: 0;
  height: 48px;
  border: 0;
  outline: none;
  padding: 0 12px 0 0;
  background: transparent;
  color: rgba(255, 255, 255, 0.92);
  font: inherit;
  font-size: 15px;
  font-weight: 520;
  line-height: 1;
}

.field-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.field-input:disabled {
  opacity: 0.5;
}

.field-input:-webkit-autofill,
.field-input:-webkit-autofill:hover,
.field-input:-webkit-autofill:focus {
  box-shadow: 0 0 0 1000px transparent inset;
  -webkit-box-shadow: 0 0 0 1000px transparent inset;
  background-clip: text;
  -webkit-text-fill-color: rgba(255, 255, 255, 0.92);
  caret-color: rgba(255, 255, 255, 0.92);
  transition: background-color 999999s ease-in-out 0s;
}

.field-toggle {
  display: grid;
  place-items: center;
  width: 40px;
  height: 48px;
  flex: 0 0 auto;
  border: 0;
  padding: 0;
  margin-right: 4px;
  background: transparent;
  color: rgba(255, 255, 255, 0.48);
  cursor: pointer;
  transition: color 160ms ease;
}

.field-toggle:hover {
  color: rgba(255, 255, 255, 0.72);
}

.field-toggle i {
  font-size: 18px;
  line-height: 1;
}

.code-field {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 92px;
  gap: 8px;
  align-items: stretch;
}

.code-button {
  min-width: 0;
  min-height: 48px;
  border: 0;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.86);
  color: #15151c;
  font: inherit;
  font-size: 13px;
  font-weight: 740;
  cursor: pointer;
  transition: opacity 160ms ease, background 160ms ease;
}

.code-button:disabled {
  cursor: default;
  opacity: 0.42;
}

.code-button:not(:disabled):hover {
  background: rgba(255, 255, 255, 0.94);
}

.code-button:focus-visible {
  outline: 2px solid rgba(255, 255, 255, 0.78);
  outline-offset: 3px;
}

/* ============ Error ============ */
.login-error {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 12px;
  background: rgba(255, 80, 60, 0.12);
  color: rgba(255, 160, 140, 0.92);
  font-size: 13px;
  font-weight: 540;
  line-height: 1.4;
}

.login-error i {
  font-size: 16px;
  flex: 0 0 auto;
}

/* ============ Submit button ============ */
.login-button {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 48px;
  margin-top: 4px;
  border: 0;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.14);
  color: rgba(255, 255, 255, 0.92);
  font: inherit;
  cursor: pointer;
  touch-action: manipulation;
  opacity: 0.4;
  backdrop-filter: blur(12px) saturate(1.2);
  -webkit-backdrop-filter: blur(12px) saturate(1.2);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.16),
    0 2px 12px rgba(0, 0, 0, 0.18);
  transition:
    opacity 200ms ease,
    background 200ms ease,
    box-shadow 200ms ease;
}

.login-button.enabled {
  opacity: 1;
}

.login-button.enabled:hover {
  background: rgba(255, 255, 255, 0.2);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.22),
    0 4px 18px rgba(0, 0, 0, 0.24);
}

.login-button.enabled:active {
  background: rgba(255, 255, 255, 0.12);
  box-shadow:
    inset 0 0 0 rgba(255, 255, 255, 0.1),
    0 1px 6px rgba(0, 0, 0, 0.16);
}

.login-button:disabled {
  cursor: default;
}

.login-button i {
  position: relative;
  z-index: 1;
  font-size: 18px;
  line-height: 1;
}

.login-button span {
  position: relative;
  z-index: 1;
  font-size: 15px;
  font-weight: 680;
  letter-spacing: 0;
}

.login-button:focus-visible {
  outline: 2px solid rgba(255, 255, 255, 0.78);
  outline-offset: 3px;
}

/* ============ Back to home ============ */
.login-home-link {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  margin-top: 22px;
  border: 0;
  padding: 8px 0;
  background: transparent;
  color: rgba(255, 255, 255, 0.5);
  font: inherit;
  font-size: 13px;
  font-weight: 520;
  cursor: pointer;
  transition: color 160ms ease;
}

.login-home-link:hover {
  color: rgba(255, 255, 255, 0.72);
}

.login-home-link i {
  font-size: 16px;
}

/* ============ Spinner ============ */
.is-spinning {
  animation: spin 760ms linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* ============ Responsive ============ */
@media (max-width: 480px) {
  .login-card {
    width: calc(100vw - 40px);
    padding: 28px 22px 24px;
  }

  .login-brand {
    margin-bottom: 22px;
  }

  .login-title {
    font-size: 20px;
  }

  .brand-strip {
    top: 20px;
    left: 18px;
  }

  .code-field {
    grid-template-columns: minmax(0, 1fr) 86px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-card {
    animation: none;
  }

  .is-spinning {
    animation: none;
  }

  .field,
  .field-label,
  .field-toggle,
  .login-tabs button,
  .code-button,
  .login-button,
  .login-home-link {
    transition: none;
  }
}
</style>
