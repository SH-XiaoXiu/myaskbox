<script setup>
import { ref, onMounted } from "vue";

const toasts = ref([]);
let idCounter = 0;

function addToast(message, duration = 2400) {
  const id = ++idCounter;
  toasts.value.push({ id, message });
  setTimeout(() => {
    toasts.value = toasts.value.filter((t) => t.id !== id);
  }, duration);
}

defineExpose({ addToast });
</script>

<template>
  <Teleport to="body">
    <div class="toast-container" v-if="toasts.length">
      <div v-for="t in toasts" :key="t.id" class="toast">
        <i class="ri-checkbox-circle-fill"></i>
        {{ t.message }}
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-container {
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1001;
  display: flex;
  flex-direction: column;
  gap: 8px;
  pointer-events: none;
}

.toast {
  background: #1a1a1a;
  color: #fff;
  padding: 10px 20px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.14);
  display: flex;
  align-items: center;
  gap: 8px;
  letter-spacing: 0.01em;
  animation:
    toastIn 0.3s cubic-bezier(0.21, 1.02, 0.73, 1) forwards,
    toastOut 0.22s 2.2s ease forwards;
  pointer-events: auto;
}

@keyframes toastIn {
  from {
    opacity: 0;
    transform: translateY(-12px) scale(0.92);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes toastOut {
  from {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
  to {
    opacity: 0;
    transform: translateY(-8px) scale(0.93);
  }
}
</style>
