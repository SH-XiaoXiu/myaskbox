<script setup>
import { animate } from 'motion'
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getRegisterConfig, sendLoginCode } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import ClassicShell from '@/components/classic/ClassicShell.vue'

const router = useRouter()
const authStore = useAuthStore()

const cardRef = ref(null)
const emailRef = ref(null)
const loginMode = ref('password')
const email = ref('')
const password = ref('')
const code = ref('')
const showPassword = ref(false)
const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
const error = ref('')
const registerEnabled = ref(false)

let cardAnimation = null
let errorAnimation = null
let countdownTimer = 0

function canSubmit() {
  if (loading.value) return false
  if (!email.value.trim()) return false
  if (loginMode.value === 'code') return code.value.trim().length > 0
  return password.value.trim().length > 0
}

function canSendCode() {
  return email.value.trim().length > 0 && !loading.value && !sendingCode.value && countdown.value <= 0
}

function resolveLoginTarget(redirect) {
  if (typeof redirect !== 'string' || !redirect.startsWith('/') || redirect.startsWith('//')) {
    return authStore.landingPath
  }
  const target = router.resolve(redirect)
  const excludedRouteNames = ['login', 'unauthorized', 'forbidden', 'not-found']
  if (target.path === '/' || excludedRouteNames.includes(target.name)) return authStore.landingPath
  const roles = target.meta.roles || []
  if (roles.length && !authStore.hasAnyRole(roles)) return authStore.landingPath
  return redirect
}

function animateCardIn() {
  if (!cardRef.value) return
  cardAnimation = animate(
    cardRef.value,
    { opacity: [0, 1], transform: ['translateY(18px) scale(.98)', 'translateY(0) scale(1)'] },
    { duration: 0.36, easing: [0.16, 1, 0.3, 1] },
  )
}

function animateErrorShake() {
  if (!cardRef.value) return
  errorAnimation?.stop?.()
  errorAnimation = animate(
    cardRef.value,
    { transform: ['translateX(0)', 'translateX(-7px)', 'translateX(7px)', 'translateX(-4px)', 'translateX(0)'] },
    { duration: 0.34, easing: 'ease-in-out' },
  )
}

async function handleSubmit() {
  if (!canSubmit()) return
  loading.value = true
  error.value = ''
  try {
    if (loginMode.value === 'code') {
      await authStore.loginWithCode(email.value.trim(), code.value.trim())
    } else {
      await authStore.login(email.value.trim(), password.value)
    }
    const redirect = router.currentRoute.value.query.redirect
    router.push(resolveLoginTarget(redirect))
  } catch (err) {
    error.value = err.message || (loginMode.value === 'code' ? '邮箱验证码错误或已过期' : '邮箱或密码错误')
    animateErrorShake()
  } finally {
    loading.value = false
  }
}

async function handleSendCode() {
  if (!canSendCode()) return
  sendingCode.value = true
  error.value = ''
  try {
    await sendLoginCode(email.value.trim())
    startCountdown()
  } catch (err) {
    error.value = err.message || '验证码发送失败'
    animateErrorShake()
  } finally {
    sendingCode.value = false
  }
}

function startCountdown() {
  countdown.value = 60
  if (countdownTimer) window.clearInterval(countdownTimer)
  countdownTimer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      window.clearInterval(countdownTimer)
      countdownTimer = 0
    }
  }, 1000)
}

function switchLoginMode(mode) {
  loginMode.value = mode
  error.value = ''
}

onMounted(() => {
  getRegisterConfig()
    .then((config) => {
      registerEnabled.value = Boolean(config?.enabled)
    })
    .catch(() => {
      registerEnabled.value = false
    })
  nextTick(() => {
    animateCardIn()
    emailRef.value?.focus()
  })
})

onBeforeUnmount(() => {
  if (countdownTimer) window.clearInterval(countdownTimer)
  cardAnimation?.stop?.()
  errorAnimation?.stop?.()
})
</script>

<template>
  <ClassicShell title="AskBox" subtitle="管理你的匿名提问箱" icon="ri-user-star-line">
    <article ref="cardRef" class="classic-card login-card" role="form" aria-label="登录表单">
      <header class="form-head">
        <h1>登录 AskBox</h1>
        <p>继续处理提问、回复和公开页面。</p>
      </header>

      <van-tabs v-model:active="loginMode" shrink animated @click-tab="({ name }) => switchLoginMode(name)">
        <van-tab title="密码登录" name="password" />
        <van-tab title="验证码登录" name="code" />
      </van-tabs>

      <form class="form-stack" @submit.prevent="handleSubmit">
        <van-field
          ref="emailRef"
          v-model="email"
          type="email"
          name="email"
          autocomplete="email"
          label="邮箱"
          placeholder="请输入邮箱"
          :disabled="loading"
        />

        <van-field
          v-if="loginMode === 'password'"
          v-model="password"
          :type="showPassword ? 'text' : 'password'"
          name="password"
          autocomplete="current-password"
          label="密码"
          placeholder="请输入密码"
          :disabled="loading"
        >
          <template #button>
            <button class="field-icon" type="button" :aria-label="showPassword ? '隐藏密码' : '显示密码'" @click="showPassword = !showPassword">
              <i :class="showPassword ? 'ri-eye-off-line' : 'ri-eye-line'" aria-hidden="true"></i>
            </button>
          </template>
        </van-field>

        <van-field
          v-else
          v-model="code"
          name="code"
          inputmode="numeric"
          autocomplete="one-time-code"
          label="验证码"
          placeholder="请输入验证码"
          :disabled="loading"
        >
          <template #button>
            <van-button size="small" type="primary" plain :loading="sendingCode" :disabled="!canSendCode()" @click="handleSendCode">
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </van-button>
          </template>
        </van-field>

        <p v-if="error" class="form-error" role="alert">{{ error }}</p>

        <van-button block round type="primary" native-type="submit" :loading="loading" :disabled="!canSubmit()">
          登录
        </van-button>

        <button v-if="registerEnabled" class="text-link" type="button" @click="router.push('/register')">
          没有账号？注册
        </button>
      </form>
    </article>
  </ClassicShell>
</template>

<style scoped>
.login-card {
  width: min(100%, 460px);
  padding: 18px 16px 16px;
}

.form-head {
  margin-bottom: 8px;
  text-align: center;
}

.form-head h1 {
  margin: 0;
  font-size: 22px;
  line-height: 1.3;
}

.form-head p {
  margin: 6px 0 0;
  color: var(--classic-muted);
  font-size: 13px;
}

.form-stack {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.field-icon {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--classic-muted);
}

.form-error {
  margin: 0;
  color: var(--classic-red);
  font-size: 13px;
  text-align: center;
}

.text-link {
  border: 0;
  background: transparent;
  color: var(--classic-primary);
  font: inherit;
  font-size: 14px;
}
</style>
