<script setup>
import { animate } from "motion";
import { LiquidGlass } from "@ybouane/liquidglass";
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getRegisterConfig, sendRegisterCode } from '../api/auth'
import { useAuthStore } from '../stores/auth'
import { pageBackground } from '../assets/background'

const router = useRouter()
const auth = useAuthStore()

const rootRef = ref(null)
const bgRef = ref(null)
const cardRef = ref(null)
const emailRef = ref(null)

const enabled = ref(false)
const loadingConfig = ref(true)
const sendingCode = ref(false)
const submitting = ref(false)
const countdown = ref(0)
const form = ref({
  displayName: '',
  email: '',
  code: '',
  password: '',
  confirmPassword: '',
})

let liquidGlass = null
let initToken = 0
let refreshFrame = 0
let stableRefreshTimers = []
let cardAnimation = null

const cardConfig = {
  blurAmount: 0.25,
  refraction: 0.69,
  chromAberration: 0.05,
  edgeHighlight: 0.05,
  specular: 0,
  fresnel: 1,
  distortion: 0,
  cornerRadius: 46,
  zRadius: 46,
  opacity: 1,
  saturation: 0,
  tintStrength: 0,
  brightness: -0.02,
  shadowOpacity: 0.3,
  shadowSpread: 12,
  shadowOffsetY: 2,
  floating: false,
  bevelMode: 0,
}

const canSendCode = computed(() => enabled.value && form.value.email.trim() && !sendingCode.value && countdown.value <= 0)
const canSubmit = computed(() =>
  enabled.value
  && form.value.email.trim()
  && form.value.code.trim()
  && form.value.password
  && form.value.confirmPassword
  && !submitting.value
)

function configJson(config) {
  return JSON.stringify(config)
}

function scheduleGlassRefresh() {
  if (refreshFrame) return
  refreshFrame = requestAnimationFrame(() => {
    refreshFrame = 0
    liquidGlass?.markChanged(rootRef.value)
  })
}

function scheduleStableGlassRefresh() {
  stableRefreshTimers.forEach((timer) => window.clearTimeout(timer))
  stableRefreshTimers = [0, 80, 220].map((delay) => window.setTimeout(() => scheduleGlassRefresh(), delay))
}

async function waitForBackgroundImage() {
  const image = bgRef.value
  if (!image || image.complete) return
  await new Promise((resolve) => {
    image.addEventListener('load', resolve, { once: true })
    image.addEventListener('error', resolve, { once: true })
  })
}

async function initLiquidGlass() {
  const root = rootRef.value
  if (!root) return

  const token = ++initToken
  liquidGlass?.destroy()
  liquidGlass = null

  await waitForBackgroundImage()
  await nextTick()

  const glassElements = Array.from(root.children).filter((child) =>
    child.classList.contains('liquid-glass'),
  )

  try {
    const instance = await LiquidGlass.init({
      root,
      glassElements,
      defaults: {
        blurAmount: 0.2,
        refraction: 0.66,
        chromAberration: 0.05,
        edgeHighlight: 0.05,
        shadowOpacity: 0.25,
        shadowSpread: 10,
        shadowOffsetY: 1,
      },
    })

    if (token !== initToken) {
      instance.destroy()
      return
    }

    liquidGlass = instance
    liquidGlass.markChanged()
  } catch (err) {
    console.error('Failed to initialize Register page LiquidGlass', err)
  }
}

function animateCardIn() {
  if (!cardRef.value) return
  cardAnimation?.stop()
  cardAnimation = animate(
    cardRef.value,
    {
      opacity: [0, 1],
      transform: ['translate(-50%, -46%) scale(0.94)', 'translate(-50%, -50%) scale(1)'],
    },
    {
      duration: 0.46,
      easing: [0.16, 1, 0.3, 1],
      onComplete: () => {
        if (cardRef.value) {
          cardRef.value.style.opacity = ''
          cardRef.value.style.transform = ''
        }
      },
    },
  )
}

onMounted(async () => {
  initLiquidGlass()
  nextTick(() => {
    animateCardIn()
    emailRef.value?.focus()
  })
  try {
    const config = await getRegisterConfig()
    enabled.value = Boolean(config?.enabled)
  } finally {
    loadingConfig.value = false
    await nextTick()
    scheduleStableGlassRefresh()
  }
})

onBeforeUnmount(() => {
  initToken += 1
  if (refreshFrame) cancelAnimationFrame(refreshFrame)
  stableRefreshTimers.forEach((timer) => window.clearTimeout(timer))
  cardAnimation?.stop()
  cardAnimation = null
  liquidGlass?.destroy()
  liquidGlass = null
})

watch([loadingConfig, enabled, countdown], () => {
  scheduleStableGlassRefresh()
})

async function handleSendCode() {
  if (!canSendCode.value) return
  sendingCode.value = true
  try {
    await sendRegisterCode(form.value.email.trim())
    showToast('验证码已发送')
    startCountdown()
    scheduleStableGlassRefresh()
  } finally {
    sendingCode.value = false
  }
}

function startCountdown() {
  countdown.value = 60
  const timer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) window.clearInterval(timer)
  }, 1000)
}

async function handleSubmit() {
  if (!canSubmit.value) return
  if (form.value.password !== form.value.confirmPassword) {
    showToast('两次输入的密码不一致')
    return
  }
  submitting.value = true
  try {
    await auth.register({
      displayName: form.value.displayName.trim(),
      email: form.value.email.trim(),
      code: form.value.code.trim(),
      password: form.value.password,
      confirmPassword: form.value.confirmPassword,
    })
    router.replace(auth.landingPath)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main ref="rootRef" class="register-page" aria-label="注册 AskBox">
    <img
      ref="bgRef"
      class="page-bg"
      :src="pageBackground.src"
      alt=""
      decoding="async"
      fetchpriority="high"
      loading="eager"
      draggable="false"
      @load="scheduleGlassRefresh"
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
      class="liquid-glass register-card"
      :data-config="configJson(cardConfig)"
      role="form"
      aria-label="注册表单"
    >
      <header class="register-head">
        <span class="register-icon"><i class="ri-user-add-line" aria-hidden="true"></i></span>
        <h1>注册 AskBox</h1>
        <p>创建你的匿名提问箱</p>
      </header>

      <van-loading v-if="loadingConfig" class="loading-center" size="24" />
      <div v-else-if="!enabled" class="disabled-state">
        <i class="ri-lock-line" aria-hidden="true"></i>
        <span>当前未开放注册</span>
        <button type="button" @click="router.replace('/login')">返回登录</button>
      </div>

      <form v-else class="register-form" @submit.prevent="handleSubmit">
        <label>
          <span>邮箱</span>
          <div class="code-row">
            <input ref="emailRef" v-model="form.email" type="email" autocomplete="email" maxlength="200" />
            <button type="button" :disabled="!canSendCode" @click="handleSendCode">
              {{ countdown > 0 ? `${countdown}s` : '验证码' }}
            </button>
          </div>
        </label>
        <label>
          <span>显示名</span>
          <input v-model="form.displayName" autocomplete="name" maxlength="100" />
        </label>
        <label>
          <span>邮箱验证码</span>
          <input v-model="form.code" inputmode="numeric" maxlength="12" />
        </label>
        <label>
          <span>密码</span>
          <input v-model="form.password" type="password" autocomplete="new-password" />
        </label>
        <label>
          <span>确认密码</span>
          <input v-model="form.confirmPassword" type="password" autocomplete="new-password" />
        </label>
        <button class="submit-button" :disabled="!canSubmit" type="submit">
          <i v-if="submitting" class="ri-loader-4-line is-spinning" aria-hidden="true"></i>
          <i v-else class="ri-user-add-line" aria-hidden="true"></i>
          <span>{{ submitting ? '注册中...' : '注册并登录' }}</span>
        </button>
        <button class="login-link" type="button" @click="router.replace('/login')">已有账号，去登录</button>
      </form>
    </article>
  </main>
</template>

<style scoped>
:global(html), :global(body), :global(#app) { width: 100%; height: 100%; overflow: hidden; overscroll-behavior: none; }
.register-page { position: fixed; inset: 0; width: 100vw; height: 100vh; height: 100dvh; overflow: hidden; isolation: isolate; background: #0c0f14; color: #fff; font-family: inherit; touch-action: manipulation; }
.page-bg { position: absolute; inset: 0; z-index: 0; display: block; width: 100%; height: 100%; object-fit: cover; object-position: center; pointer-events: none; user-select: none; }
.brand-strip { position: absolute; top: 32px; left: 26px; z-index: 4; display: inline-flex; align-items: center; gap: 12px; color: rgba(255,255,255,.92); text-shadow: 0 2px 10px rgba(0,0,0,.28); pointer-events: none; }
.brand-mark { display: grid; place-items: center; width: 42px; height: 42px; border-radius: 14px; background: rgba(0,0,0,.24); box-shadow: inset 0 1px 0 rgba(255,255,255,.2); }
.brand-mark i { font-size: 22px; line-height: 1; }
.brand-strip strong, .brand-strip em { display: block; letter-spacing: 0; font-style: normal; line-height: 1.18; }
.brand-strip strong { font-size: 17px; font-weight: 750; }
.brand-strip em { margin-top: 3px; color: rgba(255,255,255,.64); font-size: 12px; font-weight: 520; }
.liquid-glass { position: absolute; border: 0; background: transparent; font: inherit; }
.register-card { top: 50%; left: 50%; z-index: 10; width: min(450px, calc(100vw - 40px)); max-height: calc(100dvh - 42px); overflow-x: hidden; overflow-y: auto; padding: 30px 26px max(28px, env(safe-area-inset-bottom)); border-radius: 46px; color: rgba(255,255,255,.94); transform: translate(-50%, -50%); animation: register-float 5s ease-in-out infinite; scrollbar-width: none; will-change: opacity, transform; }
.register-card::-webkit-scrollbar { display: none; }
.register-card::before { content: ""; position: absolute; inset: 0; z-index: 0; border-radius: inherit; background: rgba(9,8,16,.16); pointer-events: none; }
.register-head, .register-form, .disabled-state, .loading-center { position: relative; z-index: 1; }
.register-head { text-align: center; margin-bottom: 20px; }
.register-icon { display: grid; place-items: center; width: 48px; height: 48px; margin: 0 auto 12px; border-radius: 16px; background: rgba(79,70,229,.22); box-shadow: inset 0 1px 0 rgba(255,255,255,.18); font-size: 24px; }
.register-icon i { color: rgba(165,160,255,.92); }
.register-head h1 { margin: 0 0 6px; font-size: 22px; font-weight: 750; letter-spacing: 0; line-height: 1.3; }
.register-head p { margin: 0; color: rgba(255,255,255,.62); font-size: 13px; }
.register-form { display: grid; gap: 13px; }
.register-form label { display: grid; gap: 7px; color: rgba(255,255,255,.68); font-size: 12px; font-weight: 650; }
.register-form input { width: 100%; min-height: 44px; border: 1px solid rgba(255,255,255,.22); border-radius: 16px; outline: 0; padding: 0 13px; background: rgba(0,0,0,.28); color: rgba(255,255,255,.94); font: inherit; font-size: 14px; transition: border-color 180ms ease, background 180ms ease; }
.register-form input:focus-visible { border-color: rgba(255,255,255,.58); background: rgba(0,0,0,.18); }
.register-form input:-webkit-autofill,
.register-form input:-webkit-autofill:hover,
.register-form input:-webkit-autofill:focus { box-shadow: 0 0 0 1000px transparent inset; -webkit-box-shadow: 0 0 0 1000px transparent inset; background-clip: text; -webkit-text-fill-color: rgba(255,255,255,.94); caret-color: rgba(255,255,255,.94); transition: background-color 999999s ease-in-out 0s; }
.code-row { display: grid; grid-template-columns: minmax(0, 1fr) 88px; gap: 8px; }
.code-row button, .submit-button { border: 0; border-radius: 16px; background: rgba(255,255,255,.88); color: #15151c; font: inherit; font-weight: 720; cursor: pointer; }
.code-row button:disabled, .submit-button:disabled { opacity: .42; cursor: default; }
.submit-button { display: inline-flex; align-items: center; justify-content: center; gap: 8px; min-height: 46px; margin-top: 4px; }
.login-link, .disabled-state button { border: 0; background: transparent; color: rgba(255,255,255,.66); font: inherit; cursor: pointer; }
.disabled-state { display: grid; place-items: center; gap: 12px; min-height: 180px; color: rgba(255,255,255,.72); }
.disabled-state i { font-size: 28px; }
.loading-center { display: flex; justify-content: center; padding: 30px 0; }
.is-spinning { animation: spin 760ms linear infinite; }
@keyframes register-float { 0%, 100% { transform: translate(-50%, -50%) translateY(0); } 50% { transform: translate(-50%, -50%) translateY(-7px); } }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 560px) {
  .brand-strip { top: 20px; left: 20px; }
  .register-card { width: calc(100vw - 28px); max-height: calc(100dvh - 24px); padding: 24px 22px max(26px, env(safe-area-inset-bottom)); border-radius: 38px; }
}
@media (max-height: 760px) {
  .register-card { padding-top: 22px; }
  .register-head { margin-bottom: 14px; }
  .register-icon { width: 42px; height: 42px; margin-bottom: 9px; border-radius: 14px; font-size: 21px; }
  .register-form { gap: 10px; }
  .register-form input { min-height: 40px; }
  .submit-button { min-height: 42px; }
}
@media (prefers-reduced-motion: reduce) { .is-spinning, .register-card { animation: none; } }
</style>
