<script setup>
import { computed } from "vue";
import { useAskBox } from "../composables/useAskBox.js";
import QACard from "./QACard.vue";
import EmptyState from "./EmptyState.vue";

const { publishedQA, visibleCount, loadMore, hasMore } = useAskBox();

const visibleQA = computed(() => publishedQA.slice(0, visibleCount.value));
const total = computed(() => publishedQA.length);
</script>

<template>
  <div>
    <!-- Section Header -->
    <div v-if="total > 0" class="section-header">
      <span class="section-label">问答记录</span>
      <span class="section-count">共 {{ total }} 条</span>
    </div>

    <!-- Empty -->
    <EmptyState v-if="total === 0" />

    <!-- List -->
    <div v-else class="qa-list">
      <QACard
        v-for="(qa, i) in visibleQA"
        :key="qa.id"
        :qa="qa"
        :index="i"
      />
    </div>

    <!-- Load More -->
    <div v-if="hasMore" class="load-more-wrap">
      <button class="load-more-btn" @click="loadMore">
        <i class="ri-arrow-down-line"></i>
        加载更多
      </button>
    </div>
  </div>
</template>

<style scoped>
.section-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border);
  animation: pageIn 0.5s ease both;
  animation-delay: 0.14s;
}

.section-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
  letter-spacing: 0.01em;
}

.section-count {
  font-size: 12px;
  color: var(--text-muted);
}

.qa-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.load-more-wrap {
  text-align: center;
  margin-top: 28px;
}

.load-more-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
  transition: all var(--transition);
}

.load-more-btn:hover {
  color: var(--accent);
  border-color: var(--accent-border);
  background: var(--accent-bg);
}

.load-more-btn:active {
  transform: scale(0.97);
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
  .section-header {
    margin-bottom: 12px;
  }
}
</style>
