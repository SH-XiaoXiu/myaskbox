<script setup>
import { ref, onMounted } from "vue";
import { useAskBox } from "./composables/useAskBox.js";
import BrandHeader from "./components/BrandHeader.vue";
import QuestionComposer from "./components/QuestionComposer.vue";
import QASection from "./components/QASection.vue";
import AdminPanel from "./components/AdminPanel.vue";
import AppToast from "./components/AppToast.vue";
import AppFooter from "./components/AppFooter.vue";

const { isAdmin } = useAskBox();

const brandHeaderRef = ref(null);
const toastRef = ref(null);

function onQuestionSent(sendBtnEl) {
  flyIcon(sendBtnEl);
  bounceBrand();
  toastRef.value?.addToast("问题已投递，我会尽快回答");
}

function flyIcon(fromEl) {
  const brandIcon = brandHeaderRef.value?.$el?.querySelector(".brand-icon");
  if (!fromEl || !brandIcon) return;

  const btnRect = fromEl.getBoundingClientRect();
  const iconRect = brandIcon.getBoundingClientRect();

  const flyer = document.createElement("i");
  flyer.className = "ri-send-plane-fill fly-icon";
  flyer.style.left = btnRect.left + btnRect.width / 2 - 11 + "px";
  flyer.style.top = btnRect.top + "px";
  document.body.appendChild(flyer);

  const sx = flyer.getBoundingClientRect().left;
  const sy = flyer.getBoundingClientRect().top;
  const ex = iconRect.left + iconRect.width / 2 - 11;
  const ey = iconRect.top;
  const dur = 480,
    t0 = performance.now();

  function tick(now) {
    const p = Math.min((now - t0) / dur, 1);
    const t = 1 - Math.pow(1 - p, 4);
    flyer.style.left = sx + (ex - sx) * t + "px";
    flyer.style.top = sy + (ey - sy) * t - Math.sin(p * Math.PI) * 48 + "px";
    flyer.style.opacity = p > 0.6 ? 1 - (p - 0.6) / 0.4 : 1;
    if (p < 1) requestAnimationFrame(tick);
    else {
      flyer.remove();
    }
  }
  requestAnimationFrame(tick);
}

function bounceBrand() {
  brandHeaderRef.value?.bounce();
}

function toggleAdmin() {
  isAdmin.value = !isAdmin.value;
  if (isAdmin.value) {
    history.replaceState(null, "", "?admin");
  } else {
    history.replaceState(null, "", window.location.pathname);
  }
}

onMounted(() => {
  // Auto focus textarea
  const ta = document.querySelector(".compose-card textarea");
  if (ta) ta.focus();
});
</script>

<template>
  <div class="container">
    <BrandHeader ref="brandHeaderRef" />

    <!-- Admin Banner -->
    <div v-if="isAdmin" class="admin-banner">
      <span><i class="ri-shield-check-line"></i>&nbsp; 管理模式</span>
      <button class="exit-btn" title="退出管理" @click="toggleAdmin">
        <i class="ri-close-line"></i>
      </button>
    </div>

    <QuestionComposer @sent="onQuestionSent" />

    <AdminPanel />

    <QASection />

    <AppFooter />

    <AppToast ref="toastRef" />
  </div>
</template>

<style scoped>
.container {
  width: 100%;
  max-width: 560px;
  padding: 56px 24px 72px;
  position: relative;
}

.admin-banner {
  background: #1a1a1a;
  color: #fff;
  padding: 10px 16px;
  border-radius: var(--radius);
  font-size: 13px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.exit-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.08);
  color: #a3a3a3;
  font-size: 16px;
  cursor: pointer;
  transition: all var(--transition);
}

.exit-btn:hover {
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
}

@media (max-width: 480px) {
  .container {
    padding: 40px 16px 56px;
  }
}
</style>

<!-- Global fly-icon style (not scoped, applied to body-attached element) -->
<style>
.fly-icon {
  position: fixed;
  pointer-events: none;
  z-index: 1000;
  color: var(--accent);
  will-change: transform, opacity;
  font-size: 22px;
  filter: drop-shadow(0 2px 6px rgba(79, 70, 229, 0.3));
}
</style>
