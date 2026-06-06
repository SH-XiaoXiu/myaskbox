<script setup>
import { ref } from "vue";
import { useAskBox } from "../composables/useAskBox.js";

const props = defineProps({
  question: { type: Object, required: true },
});

const emit = defineEmits(["published", "dismissed"]);

const { publishAnswer, dismissQuestion } = useAskBox();
const answer = ref("");
const error = ref(false);

function handlePublish() {
  const text = answer.value.trim();
  if (!text) {
    error.value = true;
    setTimeout(() => {
      error.value = false;
    }, 800);
    return;
  }
  emit("published", { id: props.question.id, answer: text });
  publishAnswer(props.question.id, text);
}

function handleDismiss() {
  dismissQuestion(props.question.id);
  emit("dismissed", props.question.id);
}
</script>

<template>
  <div class="admin-card">
    <div class="pending-question">{{ question.question }}</div>
    <textarea
      v-model="answer"
      placeholder="写下你的回答…"
      rows="2"
      :style="{ borderColor: error ? '#dc2626' : '' }"
    ></textarea>
    <div class="admin-card-actions">
      <button class="btn-outline" @click="handleDismiss">跳过</button>
      <button class="btn-primary" @click="handlePublish">
        <i class="ri-send-plane-fill"></i>
        发布回答
      </button>
    </div>
  </div>
</template>

<style scoped>
.admin-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 18px 20px;
  margin-bottom: 10px;
}

.pending-question {
  font-size: 14px;
  color: var(--text);
  line-height: 1.65;
  padding: 12px 16px;
  background: #faf9ff;
  border-radius: var(--radius);
  margin-bottom: 14px;
  letter-spacing: 0.005em;
}

textarea {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 10px 14px;
  font-size: 14px;
  font-family: inherit;
  color: var(--text);
  background: #fafafa;
  resize: vertical;
  min-height: 88px;
  outline: none;
  transition:
    border-color var(--transition),
    box-shadow 0.2s ease;
  line-height: 1.65;
}

textarea:focus {
  border-color: var(--accent-border);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.06);
}

.admin-card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

.btn-outline,
.btn-primary {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 7px 18px;
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--transition);
}

.btn-outline {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: transparent;
  color: var(--text-secondary);
}

.btn-outline:hover {
  border-color: #d0d0d0;
  color: var(--text);
}

.btn-primary {
  gap: 6px;
  border: none;
  border-radius: var(--radius);
  background: var(--accent);
  color: #fff;
  font-weight: 550;
}

.btn-primary:hover {
  background: #4338ca;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.2);
}
.btn-primary:active {
  transform: scale(0.97);
}
</style>
