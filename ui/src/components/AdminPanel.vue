<script setup>
import { useAskBox } from "../composables/useAskBox.js";
import AdminCard from "./AdminCard.vue";

const { pendingQuestions, isAdmin } = useAskBox();
</script>

<template>
  <div v-if="isAdmin" class="admin-section">
    <div class="admin-section-header">
      <i class="ri-inbox-line"></i>
      待回答问题
      <span class="badge">{{ pendingQuestions.length }}</span>
    </div>

    <!-- Empty -->
    <div v-if="pendingQuestions.length === 0" class="admin-empty">
      <div class="empty-icon-wrap">
        <i class="ri-check-double-line"></i>
      </div>
      <p>没有待回答的问题</p>
    </div>

    <!-- List -->
    <AdminCard
      v-for="pq in pendingQuestions"
      :key="pq.id"
      :question="pq"
    />
  </div>
</template>

<style scoped>
.admin-section {
  margin-bottom: 40px;
}

.admin-section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 12px;
}

.badge {
  background: var(--accent);
  color: #fff;
  font-size: 11px;
  padding: 1px 7px;
  border-radius: 10px;
  font-weight: 600;
  min-width: 18px;
  text-align: center;
  line-height: 1.6;
}

.admin-empty {
  text-align: center;
  padding: 40px 16px;
  color: var(--text-muted);
  font-size: 13px;
}

.empty-icon-wrap {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--accent-bg);
  color: var(--accent-soft);
  font-size: 24px;
  margin-bottom: 14px;
}
</style>
