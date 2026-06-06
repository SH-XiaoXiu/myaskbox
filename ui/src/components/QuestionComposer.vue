<script setup>
import { ref, watch, nextTick } from "vue";
import { useAskBox } from "../composables/useAskBox.js";
import AvatarPicker from "./AvatarPicker.vue";

const emit = defineEmits(["sent"]);
const { sendQuestion } = useAskBox();

const content = ref("");
const charCount = ref("0 / 350");
const sending = ref(false);
const textareaRef = ref(null);
const sendBtnRef = ref(null);

function updateCharCount() {
  const len = content.value.length;
  charCount.value = `${len} / 350`;
}

function autoResize() {
  const el = textareaRef.value;
  if (!el) return;
  el.style.height = "auto";
  el.style.height = Math.min(el.scrollHeight, 220) + "px";
}

watch(content, () => {
  updateCharCount();
  nextTick(autoResize);
});

async function handleSend() {
  const text = content.value.trim();
  if (!text || sending.value) return;

  sending.value = true;
  await sendQuestion(text);

  // Emit for fly animation
  emit("sent", sendBtnRef.value);

  sending.value = false;
  content.value = "";
  updateCharCount();
  nextTick(autoResize);
}

function onKeydown(e) {
  if (e.key === "Enter" && (e.metaKey || e.ctrlKey)) {
    e.preventDefault();
    handleSend();
  }
}

defineExpose({ textareaRef });
</script>

<template>
  <div class="compose-card">
    <textarea
      ref="textareaRef"
      v-model="content"
      placeholder="写下你想问的，我会认真回答每一个问题"
      maxlength="350"
      rows="2"
      @keydown="onKeydown"
    ></textarea>

    <AvatarPicker />

    <div class="compose-footer">
      <span
        class="char-count"
        :class="{
          warn: content.length >= 280 && content.length < 340,
          danger: content.length >= 340,
        }"
      >
        {{ charCount }}
      </span>
      <button
        ref="sendBtnRef"
        class="send-btn"
        :class="{ loading: sending }"
        :disabled="sending"
        @click="handleSend"
      >
        <span class="btn-text">投递</span>
        <i class="ri-send-plane-fill btn-icon"></i>
        <span class="spinner"></span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.compose-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px 20px 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
  transition:
    border-color var(--transition),
    box-shadow 0.35s ease;
  position: relative;
  margin-bottom: 48px;
  animation: pageIn 0.5s ease both;
  animation-delay: 0.08s;
}

.compose-card:focus-within {
  border-color: var(--accent-border);
  box-shadow:
    0 1px 3px rgba(0, 0, 0, 0.04),
    0 4px 20px rgba(79, 70, 229, 0.07),
    0 0 0 3px rgba(79, 70, 229, 0.05);
}

textarea {
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  font-size: 15px;
  font-family: inherit;
  color: var(--text);
  background: transparent;
  line-height: 1.7;
  min-height: 64px;
  max-height: 220px;
  overflow-y: auto;
  letter-spacing: 0.005em;
}

textarea::placeholder {
  color: #b8b8b8;
  font-size: 15px;
}

.compose-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}

.char-count {
  font-size: 12px;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
  transition: color 0.15s ease;
}

.char-count.warn {
  color: #d97706;
}
.char-count.danger {
  color: #dc2626;
}

.send-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-width: 44px;
  min-height: 36px;
  padding: 8px 20px;
  border: none;
  border-radius: 24px;
  background: var(--accent);
  color: #fff;
  font-size: 14px;
  font-weight: 550;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  white-space: nowrap;
  position: relative;
  overflow: hidden;
}

.send-btn::after {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.15) 0%,
    transparent 50%
  );
  pointer-events: none;
}

.send-btn:hover {
  background: #4338ca;
  transform: translateY(-1px);
  box-shadow: 0 3px 12px rgba(79, 70, 229, 0.28);
}

.send-btn:active {
  transform: scale(0.96) translateY(0);
  box-shadow: none;
}
.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.send-btn .btn-icon {
  font-size: 16px;
  transition: transform 0.25s ease;
}

.send-btn:hover .btn-icon {
  transform: translateX(2px);
}

.send-btn.loading {
  pointer-events: none;
}
.send-btn.loading .btn-text,
.send-btn.loading .btn-icon {
  visibility: hidden;
}
.send-btn.loading::after {
  display: none;
}

.spinner {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 17px;
  height: 17px;
  border: 2px solid rgba(255, 255, 255, 0.2);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.5s linear infinite;
  display: none;
}

.send-btn.loading .spinner {
  display: block;
}

@keyframes spin {
  to {
    transform: translate(-50%, -50%) rotate(360deg);
  }
}

@keyframes pageIn {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 480px) {
  .compose-card {
    padding: 16px 14px 10px;
    margin-bottom: 36px;
  }
}
</style>
