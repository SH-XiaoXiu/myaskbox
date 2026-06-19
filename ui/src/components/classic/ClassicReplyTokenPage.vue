<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { answerReplyTokenQuestion, getReplyTokenQuestion } from '@/api/replyTokens'
import { formatTime } from '@/utils'
import ClassicShell from '@/components/classic/ClassicShell.vue'

const route = useRoute()
const router = useRouter()

const token = computed(() => String(route.params.token || ''))
const loading = ref(true)
const saving = ref(false)
const submitted = ref(false)
const error = ref('')
const question = ref(null)
const answer = ref('')
const answerError = ref('')

const expiresText = computed(() => {
  if (!question.value?.expiresAt) return ''
  return formatTime(question.value.expiresAt)
})

const answerCount = computed(() => `${answer.value.length} / 5000`)
const canSubmit = computed(() => answer.value.trim().length > 0 && !saving.value && !submitted.value)

onMounted(loadQuestion)

async function loadQuestion() {
  loading.value = true
  error.value = ''
  try {
    question.value = await getReplyTokenQuestion(token.value)
  } catch (err) {
    error.value = err?.message || '链接无效或已过期'
  } finally {
    loading.value = false
  }
}

async function submitAnswer() {
  const text = answer.value.trim()
  if (!text) {
    answerError.value = '请先填写回答'
    return
  }
  answerError.value = ''
  saving.value = true
  try {
    await answerReplyTokenQuestion(token.value, text)
    submitted.value = true
    showToast('回答已发布')
  } catch (err) {
    answerError.value = err?.message || '发布失败'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <ClassicShell title="回复问题" subtitle="">
    <article class="classic-card reply-card">
      <van-loading v-if="loading" class="reply-loading" size="24" />

      <section v-else-if="error" class="reply-state" role="status">
        <h1>链接不可用</h1>
        <p>{{ error }}</p>
        <van-button round plain @click="router.replace('/login')">返回登录</van-button>
      </section>

      <section v-else-if="submitted" class="reply-state" role="status">
        <h1>回答已发布</h1>
        <p>问题已经发布到公开页。</p>
      </section>

      <section v-else class="reply-form">
        <header class="reply-head">
          <span>{{ question.boxDisplayName || '提问箱' }}</span>
          <time v-if="expiresText">有效期至 {{ expiresText }}</time>
        </header>

        <section class="question-box">
          <span>问题</span>
          <p>{{ question.question }}</p>
        </section>

        <van-field
          v-model="answer"
          class="answer-field"
          type="textarea"
          rows="7"
          maxlength="5000"
          autosize
          show-word-limit
          placeholder="写下你的回答"
          :error-message="answerError"
        />

        <footer class="reply-actions">
          <span>{{ answerCount }}</span>
          <van-button round type="primary" icon="guide-o" :loading="saving" :disabled="!canSubmit" @click="submitAnswer">
            发布回答
          </van-button>
        </footer>
      </section>
    </article>
  </ClassicShell>
</template>

<style scoped>
.reply-card {
  width: min(100%, 520px);
  overflow: hidden;
  padding: 18px 16px 16px;
}

.reply-loading {
  display: flex;
  justify-content: center;
  padding: 36px 0;
}

.reply-state {
  display: grid;
  gap: 12px;
  justify-items: center;
  padding: 24px 0;
  text-align: center;
}

.reply-state h1,
.reply-head span {
  margin: 0;
  color: var(--classic-text);
  font-size: 20px;
  font-weight: 720;
  line-height: 1.3;
}

.reply-state p {
  margin: 0;
  color: var(--classic-muted);
  font-size: 14px;
}

.reply-form {
  display: grid;
  gap: 14px;
}

.reply-head {
  display: grid;
  gap: 4px;
}

.reply-head time {
  color: var(--classic-muted);
  font-size: 12px;
}

.question-box {
  display: grid;
  gap: 8px;
  padding: 14px;
  border: 1px solid var(--classic-line);
  border-radius: 12px;
  background: #f8fafc;
}

.question-box span {
  color: var(--classic-muted);
  font-size: 12px;
}

.question-box p {
  margin: 0;
  color: var(--classic-text);
  font-size: 15px;
  line-height: 1.65;
  overflow-wrap: anywhere;
}

.answer-field {
  overflow: hidden;
  border: 1px solid var(--classic-line);
  border-radius: 12px;
}

.answer-field:deep(.van-cell) {
  padding: 14px;
}

.answer-field:deep(.van-field__control) {
  min-height: 168px;
}

.answer-field:deep(.van-field__control::selection) {
  background: rgba(47, 111, 237, 0.18);
}

.reply-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--classic-muted);
  font-size: 12px;
}
</style>
