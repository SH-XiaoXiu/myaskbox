<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { changePassword } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import ClassicShell from '@/components/classic/ClassicShell.vue'

const router = useRouter()
const auth = useAuthStore()

const form = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const saving = ref(false)
const error = ref('')
const touched = reactive({
  currentPassword: false,
  newPassword: false,
  confirmPassword: false,
})

const newPasswordValid = computed(() => form.newPassword.length >= 8 && form.newPassword.length <= 64)
const confirmPasswordValid = computed(() => form.confirmPassword.length > 0 && form.newPassword === form.confirmPassword)
const canSubmit = computed(() => form.currentPassword.length > 0 && newPasswordValid.value && confirmPasswordValid.value && !saving.value)
const targetPath = computed(() => auth.landingPath || '/home')

function fieldError(name) {
  if (name === 'newPassword' && touched.newPassword && form.newPassword && !newPasswordValid.value) {
    return '新密码长度必须为 8-64 位'
  }
  if (name === 'confirmPassword' && touched.confirmPassword && form.confirmPassword && !confirmPasswordValid.value) {
    return '两次输入的新密码不一致'
  }
  return ''
}

async function submit() {
  touched.currentPassword = true
  touched.newPassword = true
  touched.confirmPassword = true
  error.value = ''

  if (!canSubmit.value) {
    error.value = fieldError('newPassword') || fieldError('confirmPassword') || '请完整填写密码信息'
    return
  }

  saving.value = true
  try {
    await changePassword(form.currentPassword, form.newPassword, form.confirmPassword)
    auth.clearSession()
    router.replace('/login')
  } catch (err) {
    error.value = err?.message || '修改密码失败'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <ClassicShell title="修改密码" subtitle="确认当前密码后设置新密码">
    <article class="classic-card password-card">
      <van-nav-bar title="账号安全" left-arrow @click-left="router.push(targetPath)" />
      <form class="form-stack" @submit.prevent="submit">
        <van-field
          v-model="form.currentPassword"
          label="当前密码"
          type="password"
          autocomplete="current-password"
          maxlength="64"
          placeholder="请输入当前密码"
          @blur="touched.currentPassword = true"
        />
        <van-field
          v-model="form.newPassword"
          label="新密码"
          type="password"
          autocomplete="new-password"
          minlength="8"
          maxlength="64"
          placeholder="8-64 位"
          :error-message="fieldError('newPassword')"
          @blur="touched.newPassword = true"
        />
        <van-field
          v-model="form.confirmPassword"
          label="确认密码"
          type="password"
          autocomplete="new-password"
          minlength="8"
          maxlength="64"
          placeholder="再次输入新密码"
          :error-message="fieldError('confirmPassword')"
          @blur="touched.confirmPassword = true"
        />
        <p v-if="error" class="form-error" role="alert">{{ error }}</p>
        <van-button block round type="primary" native-type="submit" :loading="saving" :disabled="!canSubmit">
          保存并重新登录
        </van-button>
      </form>
    </article>
  </ClassicShell>
</template>

<style scoped>
.password-card {
  width: min(100%, 500px);
  overflow: hidden;
}

.form-stack {
  display: grid;
  gap: 12px;
  padding: 16px;
}

.form-error {
  margin: 0;
  color: var(--classic-red);
  font-size: 13px;
  text-align: center;
}
</style>
