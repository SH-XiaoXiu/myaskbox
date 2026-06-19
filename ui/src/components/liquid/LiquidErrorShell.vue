<script setup>
import { LiquidGlass } from "@ybouane/liquidglass";
import { nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { pageBackground } from "@/assets/background";

defineProps({
  statusCode: {
    type: String,
    required: true,
  },
  eyebrow: {
    type: String,
    required: true,
  },
  title: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    required: true,
  },
  detail: {
    type: String,
    default: "",
  },
  icon: {
    type: String,
    required: true,
  },
  tone: {
    type: String,
    default: "blue",
  },
  primaryLabel: {
    type: String,
    required: true,
  },
  primaryIcon: {
    type: String,
    default: "ri-arrow-right-line",
  },
  secondaryLabel: {
    type: String,
    default: "",
  },
  secondaryIcon: {
    type: String,
    default: "ri-arrow-left-line",
  },
});

const emit = defineEmits(["primary", "secondary"]);

const rootRef = ref(null);
const bgRef = ref(null);
let liquidGlass = null;
let initToken = 0;
let glassRefreshFrame = 0;

const panelConfig = {
  blurAmount: 0.25,
  cornerRadius: 44,
  floating: true,
};

function configJson(config) {
  return JSON.stringify(config);
}

function scheduleGlassRefresh() {
  if (glassRefreshFrame) return;
  glassRefreshFrame = requestAnimationFrame(() => {
    glassRefreshFrame = 0;
    liquidGlass?.markChanged(rootRef.value);
  });
}

async function waitForBackgroundImage() {
  const image = bgRef.value;
  if (!image || image.complete) return;
  await new Promise((resolve) => {
    image.addEventListener("load", resolve, { once: true });
    image.addEventListener("error", resolve, { once: true });
  });
}

async function initLiquidGlass() {
  const root = rootRef.value;
  if (!root) return;

  const token = ++initToken;
  liquidGlass?.destroy();
  liquidGlass = null;

  await waitForBackgroundImage();
  await nextTick();

  const glassElements = Array.from(root.querySelectorAll(".liquid-glass"));

  try {
    const instance = await LiquidGlass.init({
      root,
      glassElements,
      defaults: {
        blurAmount: 0,
        refraction: 0.69,
        chromAberration: 0.05,
        edgeHighlight: 0.05,
        shadowOpacity: 0.25,
        shadowSpread: 10,
        shadowOffsetY: 1,
      },
    });

    if (token !== initToken) {
      instance.destroy();
      return;
    }

    liquidGlass = instance;
    liquidGlass.markChanged();
  } catch (err) {
    console.error("Failed to initialize auth error LiquidGlass", err);
  }
}

onMounted(() => {
  initLiquidGlass();
});

onBeforeUnmount(() => {
  initToken += 1;
  if (glassRefreshFrame) cancelAnimationFrame(glassRefreshFrame);
  liquidGlass?.destroy();
  liquidGlass = null;
});
</script>

<template>
  <main
    ref="rootRef"
    class="auth-error-page"
    :class="`tone-${tone}`"
    :aria-label="`${statusCode} ${title}`"
  >
    <img
      ref="bgRef"
      class="page-bg"
      :src="pageBackground.src"
      alt=""
      decoding="async"
      fetchpriority="high"
      loading="eager"
      draggable="false"
      @load="scheduleGlassRefresh"
    />

    <header class="brand-strip">
      <span class="brand-mark"><i class="ri-question-answer-line" aria-hidden="true"></i></span>
      <span>
        <strong>AskBox</strong>
        <em>匿名提问箱</em>
      </span>
    </header>

    <section
      class="liquid-glass error-panel"
      :data-config="configJson(panelConfig)"
      aria-labelledby="auth-error-title"
    >
      <div class="status-row" aria-hidden="true">
        <span class="status-code">{{ statusCode }}</span>
        <span class="status-chip">
          <i :class="icon" aria-hidden="true"></i>
          {{ eyebrow }}
        </span>
      </div>

      <span class="error-icon" aria-hidden="true">
        <i :class="icon"></i>
      </span>

      <h1 id="auth-error-title">{{ title }}</h1>
      <p class="error-description">{{ description }}</p>
      <p v-if="detail" class="error-detail">{{ detail }}</p>

      <div class="error-actions">
        <button class="error-button primary" type="button" @click="emit('primary')">
          <i :class="primaryIcon" aria-hidden="true"></i>
          <span>{{ primaryLabel }}</span>
        </button>
        <button
          v-if="secondaryLabel"
          class="error-button secondary"
          type="button"
          @click="emit('secondary')"
        >
          <i :class="secondaryIcon" aria-hidden="true"></i>
          <span>{{ secondaryLabel }}</span>
        </button>
      </div>
    </section>
  </main>
</template>

<style scoped>
.auth-error-page {
  position: fixed;
  inset: 0;
  width: 100vw;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  isolation: isolate;
  display: grid;
  place-items: center;
  padding: 96px 24px 32px;
  background: #0c0f14;
  color: #ffffff;
  touch-action: manipulation;
}

.page-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  filter: saturate(1.12) brightness(1.08) contrast(1.04);
  transform: scale(1.02);
  pointer-events: none;
  user-select: none;
}

.brand-strip {
  position: absolute;
  top: 32px;
  left: 26px;
  z-index: 4;
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.92);
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.28);
  pointer-events: none;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: rgba(0, 0, 0, 0.24);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.brand-mark i {
  font-size: 22px;
  line-height: 1;
}

.brand-strip strong,
.brand-strip em {
  display: block;
  letter-spacing: 0;
  font-style: normal;
  line-height: 1.18;
}

.brand-strip strong {
  font-size: 17px;
  font-weight: 750;
}

.brand-strip em {
  margin-top: 3px;
  color: rgba(255, 255, 255, 0.64);
  font-size: 12px;
  font-weight: 520;
}

.liquid-glass {
  position: relative;
  border: 0;
  background: transparent;
  font: inherit;
}

.error-panel {
  z-index: 2;
  width: min(500px, calc(100vw - 48px));
  padding: 34px 32px 30px;
  border-radius: 44px;
  overflow: hidden;
  color: rgba(255, 255, 255, 0.94);
  animation: panel-float 5.5s ease-in-out infinite;
}

.error-panel::before {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 0;
  border-radius: inherit;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12);
  pointer-events: none;
}

.status-row,
.error-icon,
.error-panel h1,
.error-description,
.error-detail,
.error-actions {
  position: relative;
  z-index: 1;
}

.status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
}

.status-code {
  color: rgba(255, 255, 255, 0.82);
  font-size: 64px;
  font-weight: 780;
  line-height: 1;
  letter-spacing: 0;
  text-shadow: 0 8px 24px rgba(0, 0, 0, 0.24);
}

.status-chip {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 34px;
  max-width: 58%;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  color: rgba(255, 255, 255, 0.78);
  font-size: 13px;
  font-weight: 620;
  line-height: 1.25;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

.status-chip i {
  flex: 0 0 auto;
  font-size: 16px;
  color: var(--tone-color);
}

.error-icon {
  display: grid;
  place-items: center;
  width: 54px;
  height: 54px;
  margin-bottom: 18px;
  border-radius: 18px;
  background: var(--tone-bg);
  color: var(--tone-color);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.16),
    0 10px 28px var(--tone-shadow);
}

.error-icon i {
  font-size: 28px;
  line-height: 1;
}

.error-panel h1 {
  margin: 0 0 10px;
  color: rgba(255, 255, 255, 0.94);
  font-size: 25px;
  font-weight: 760;
  line-height: 1.28;
  letter-spacing: 0;
}

.error-description,
.error-detail {
  max-width: 42rem;
  margin: 0;
  color: rgba(255, 255, 255, 0.64);
  font-size: 15px;
  font-weight: 500;
  line-height: 1.75;
}

.error-detail {
  margin-top: 10px;
  color: rgba(255, 255, 255, 0.48);
  font-size: 13px;
}

.error-actions {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 12px;
  margin-top: 28px;
}

.error-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-width: 0;
  min-height: 48px;
  border: 0;
  border-radius: 28px;
  padding: 0 18px;
  color: rgba(255, 255, 255, 0.9);
  font: inherit;
  font-size: 14px;
  font-weight: 680;
  line-height: 1.2;
  cursor: pointer;
  touch-action: manipulation;
  backdrop-filter: blur(12px) saturate(1.2);
  -webkit-backdrop-filter: blur(12px) saturate(1.2);
  transition:
    background 180ms ease,
    color 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}

.error-button i {
  flex: 0 0 auto;
  font-size: 18px;
  line-height: 1;
}

.error-button span {
  min-width: 0;
  overflow-wrap: anywhere;
}

.error-button.primary {
  background: var(--tone-button);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.2),
    0 8px 24px var(--tone-shadow);
}

.error-button.secondary {
  background: rgba(255, 255, 255, 0.12);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.14),
    0 4px 16px rgba(0, 0, 0, 0.16);
}

.error-button:hover {
  transform: translateY(-1px);
}

.error-button.primary:hover {
  background: var(--tone-button-hover);
}

.error-button.secondary:hover {
  background: rgba(255, 255, 255, 0.18);
}

.error-button:active {
  transform: translateY(0);
}

.error-button:focus-visible {
  outline: 2px solid rgba(255, 255, 255, 0.78);
  outline-offset: 3px;
}

.tone-blue {
  --tone-color: #a5b4fc;
  --tone-bg: rgba(165, 180, 252, 0.2);
  --tone-shadow: rgba(99, 102, 241, 0.2);
  --tone-button: rgba(99, 102, 241, 0.32);
  --tone-button-hover: rgba(99, 102, 241, 0.42);
}

.tone-red {
  --tone-color: #fda4af;
  --tone-bg: rgba(253, 164, 175, 0.18);
  --tone-shadow: rgba(244, 63, 94, 0.18);
  --tone-button: rgba(244, 63, 94, 0.3);
  --tone-button-hover: rgba(244, 63, 94, 0.4);
}

.tone-amber {
  --tone-color: #fcd34d;
  --tone-bg: rgba(252, 211, 77, 0.17);
  --tone-shadow: rgba(245, 158, 11, 0.18);
  --tone-button: rgba(245, 158, 11, 0.32);
  --tone-button-hover: rgba(245, 158, 11, 0.42);
}

@keyframes panel-float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8px);
  }
}

@media (max-width: 560px) {
  .auth-error-page {
    padding: 84px 18px 24px;
    place-items: center;
  }

  .brand-strip {
    top: 22px;
    left: 18px;
  }

  .error-panel {
    width: 100%;
    padding: 28px 24px 24px;
    border-radius: 34px;
  }

  .status-row {
    align-items: flex-start;
    margin-bottom: 18px;
  }

  .status-code {
    font-size: 52px;
  }

  .status-chip {
    max-width: 64%;
    font-size: 12px;
  }

  .error-panel h1 {
    font-size: 22px;
  }

  .error-actions {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .error-panel {
    animation: none;
  }

  .error-button {
    transition-duration: 0.01ms;
  }
}
</style>
