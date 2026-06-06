<script setup>
import { ref } from "vue";

const brandIcon = ref(null);

function bounce() {
  const el = brandIcon.value;
  if (!el) return;
  el.classList.remove("bounce");
  void el.offsetWidth;
  el.classList.add("bounce");
}

defineExpose({ bounce });
</script>

<template>
  <header class="brand-header">
    <div class="brand-mark">
      <div ref="brandIcon" class="brand-icon">
        <i class="ri-question-answer-line"></i>
      </div>
      <div class="brand-text">
        <h1>AskBox</h1>
        <p class="subtitle">匿名提问箱</p>
      </div>
    </div>
  </header>
</template>

<style scoped>
.brand-header {
  padding: 0 0 44px;
  animation: pageIn 0.5s ease both;
  animation-delay: 0s;
}

.brand-mark {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  color: #fff;
  font-size: 24px;
  box-shadow:
    0 2px 8px rgba(79, 70, 229, 0.18),
    0 0 0 1px rgba(79, 70, 229, 0.1);
  transition:
    transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1),
    box-shadow 0.35s ease;
  flex-shrink: 0;
}

.brand-icon.bounce {
  animation: brandBounce 0.55s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.brand-text h1 {
  font-size: 22px;
  font-weight: 650;
  color: var(--text);
  letter-spacing: -0.02em;
  line-height: 1.25;
}

.brand-text .subtitle {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 2px;
  letter-spacing: 0.01em;
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

@keyframes brandBounce {
  0% {
    transform: scale(1);
  }
  30% {
    transform: scale(1.12);
  }
  55% {
    transform: scale(0.93);
  }
  75% {
    transform: scale(1.04);
  }
  100% {
    transform: scale(1);
  }
}

@media (max-width: 480px) {
  .brand-header {
    padding-bottom: 36px;
  }
  .brand-icon {
    width: 42px;
    height: 42px;
    font-size: 21px;
    border-radius: 10px;
  }
  .brand-text h1 {
    font-size: 20px;
  }
}
</style>
