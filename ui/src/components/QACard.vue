<script setup>
import { useAskBox } from "../composables/useAskBox.js";

const props = defineProps({
  qa: { type: Object, required: true },
  index: { type: Number, default: 0 },
});

const { justPublishedId } = useAskBox();
</script>

<template>
  <div
    class="qa-card"
    :class="{ 'just-published': qa.id === justPublishedId }"
    :style="{ animationDelay: index * 0.06 + 's' }"
  >
    <div class="qa-card-header">
      <span class="anonymous-id">
        <span class="avatar-circle" :style="{ background: qa.profile.bg }">
          <i :class="qa.profile.icon"></i>
        </span>
        {{ qa.profile.name }}
      </span>
      <span class="qa-time">{{ qa.time }}</span>
    </div>
    <div class="qa-body">
      <div class="qa-question">{{ qa.question }}</div>
      <div class="qa-answer">{{ qa.answer }}</div>
    </div>
  </div>
</template>

<style scoped>
.qa-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 20px 22px;
  transition:
    border-color var(--transition),
    box-shadow 0.3s ease,
    transform 0.2s ease;
  position: relative;
  animation: cardEnter 0.4s ease both;
}

.qa-card:hover {
  border-color: #d8d8d6;
  box-shadow: 0 3px 16px rgba(0, 0, 0, 0.04);
  transform: translateY(-1px);
}

.qa-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.anonymous-id {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  font-weight: 550;
  color: var(--text-secondary);
}

.avatar-circle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  font-size: 13px;
  color: #fff;
  flex-shrink: 0;
}

.qa-time {
  font-size: 12px;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.01em;
}

.qa-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.qa-question {
  font-size: 15px;
  color: var(--text);
  line-height: 1.7;
  font-weight: 480;
  letter-spacing: 0.005em;
}

.qa-answer {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.75;
  padding: 0 0 0 14px;
  margin-left: 2px;
  border-left: 1.5px solid var(--border);
  transition: border-color 0.4s ease;
}

.qa-card:hover .qa-answer {
  border-left-color: #c7c5e8;
}

/* Shimmer for newly published */
.just-published {
  animation:
    cardEnter 0.4s ease both,
    shimmer 1s ease 0.1s;
}

@keyframes cardEnter {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes shimmer {
  0% {
    box-shadow: 0 0 0 0 rgba(79, 70, 229, 0.25);
  }
  50% {
    box-shadow: 0 0 0 8px rgba(79, 70, 229, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(79, 70, 229, 0);
  }
}

@media (max-width: 480px) {
  .qa-card {
    padding: 16px 18px;
  }
}
</style>
