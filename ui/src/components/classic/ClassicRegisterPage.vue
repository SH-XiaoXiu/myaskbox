<script setup>
import { animate } from 'motion'
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getRegisterConfig, sendRegisterCode } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import ClassicShell from '@/components/classic/ClassicShell.vue'

const router = useRouter()
const auth = useAuthStore()

const cardRef = ref(null)
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

let cardAnimation = null
let countdownTimer = 0

const canSendCode = computed(() => enabled.value && form.value.email.trim() && !sendingCode.value && countdown.value <= 0)
const canSubmit = computed(() =>
  enabled.value
  && form.value.email.trim()
  && form.value.code.trim()
  && form.value.password
  && form.value.confirmPassword
  && !submitting.value,
)

function animateCardIn() {
  if (!cardRef.value) return
  cardAnimation = animate(
    cardRef.value,
    { opacity: [0, 1], transform: ['translateY(18px) scale(.98)', 'translateY(0) scale(1)'] },
    { duration: 0.36, easing: [0.16, 1, 0.3, 1] },
  )
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

async function handleSendCode() {
  if (!canSendCode.value) return
  sendingCode.value = true
  try {
    await sendRegisterCode(form.value.email.trim())
    showToast('验证码已发送')
    startCountdown()
  } finally {
    sendingCode.value = false
  }
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

onMounted(async () => {
  nextTick(animateCardIn)
  try {
    const config = await getRegisterConfig()
    enabled.value = Boolean(config?.enabled)
  } finally {
    loadingConfig.value = false
  }
})

onBeforeUnmount(() => {
  if (countdownTimer) window.clearInterval(countdownTimer)
  cardAnimation?.stop?.()
})
</script>

<template>
  <ClassicShell title="注册 AskBox" subtitle="创建你的匿名提问箱">
    <article ref="cardRef" class="classic-card register-card" role="form" aria-label="注册表单">
      <van-loading v-if="loadingConfig" class="center-state" size="24" />
      <van-empty v-else-if="!enabled" image="error" description="当前暂未开放注册">
        <van-button round type="primary" @click="router.push('/login')">返回登录</van-button>
      </van-empty>
      <form v-else class="form-stack" @submit.prevent="handleSubmit">
        <header class="form-head">
          <h1>创建账号</h1>
          <p>注册后可进入工作台处理提问。</p>
        </header>
        <van-field v-model="form.displayName" label="昵称" placeholder="显示名，可选" maxlength="32" />
        <van-field v-model="form.email" label="邮箱" type="email" autocomplete="email" placeholder="请输入邮箱" />
        <van-field v-model="form.code" label="验证码" inputmode="numeric" autocomplete="one-time-code" placeholder="请输入验证码">
          <template #button>
            <van-button size="small" type="primary" plain :loading="sendingCode" :disabled="!canSendCode" @click="handleSendCode">
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </van-button>
          </template>
        </van-field>
        <van-field v-model="form.password" label="密码" type="password" autocomplete="new-password" placeholder="8-64 位密码" />
        <van-field v-model="form.confirmPassword" label="确认密码" type="password" autocomplete="new-password" placeholder="再次输入密码" />
        <van-button block round type="primary" native-type="submit" :loading="submitting" :disabled="!canSubmit">
          注册
        </van-button>
        <button class="text-link" type="button" @click="router.push('/login')">已有账号？登录</button>
      </form>
    </article>
  </ClassicShell>
</template>

<style scoped>
.register-card {
  width: min(100%, 480px);
  min-height: 260px;
  padding: 18px 16px 16px;
}

.center-state {
  display: flex;
  justify-content: center;
  padding: 64px 0;
}

.form-head {
  text-align: center;
}

.form-head h1 {
  margin: 0;
  font-size: 22px;
}

.form-head p {
  margin: 6px 0 0;
  color: var(--classic-muted);
  font-size: 13px;
}

.form-stack {
  display: grid;
  gap: 12px;
}

.text-link {
  border: 0;
  background: transparent;
  color: var(--classic-primary);
  font: inherit;
  font-size: 14px;
}
</style>
