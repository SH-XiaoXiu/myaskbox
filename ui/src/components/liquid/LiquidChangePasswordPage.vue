<script setup>
import { LiquidGlass } from "@ybouane/liquidglass";
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { changePassword } from "@/api/auth";
import { pageBackground } from "@/assets/background";
import { useAuthStore } from "@/stores/auth";

const router = useRouter();
const auth = useAuthStore();

const rootRef = ref(null);
const bgRef = ref(null);
const panelRef = ref(null);
const currentPasswordRef = ref(null);

const form = reactive({
  currentPassword: "",
  newPassword: "",
  confirmPassword: "",
});

const saving = ref(false);
const error = ref("");
const success = ref(false);
const touched = reactive({
  currentPassword: false,
  newPassword: false,
  confirmPassword: false,
});

let liquidGlass = null;
let initToken = 0;
let refreshFrame = 0;
let stableRefreshTimers = [];

const panelConfig = {
  blurAmount: 0.25,
  refraction: 0.69,
  chromAberration: 0.05,
  edgeHighlight: 0.05,
  specular: 0,
  fresnel: 1,
  cornerRadius: 42,
  zRadius: 42,
  opacity: 1,
  saturation: 0,
  tintStrength: 0,
  brightness: -0.03,
  shadowOpacity: 0.3,
  shadowSpread: 12,
  shadowOffsetY: 2,
  floating: false,
  bevelMode: 0,
};

const newPasswordValid = computed(() => form.newPassword.length >= 8 && form.newPassword.length <= 64);
const confirmPasswordValid = computed(
  () => form.confirmPassword.length > 0 && form.newPassword === form.confirmPassword,
);
const canSubmit = computed(
  () => form.currentPassword.length > 0 && newPasswordValid.value && confirmPasswordValid.value && !saving.value,
);
const targetPath = computed(() => auth.landingPath || "/home");

function configJson(config) {
  return JSON.stringify(config);
}

function scheduleGlassRefresh() {
  if (refreshFrame) return;
  refreshFrame = requestAnimationFrame(() => {
    refreshFrame = 0;
    liquidGlass?.markChanged(rootRef.value);
  });
}

function scheduleStableGlassRefresh() {
  stableRefreshTimers.forEach((timer) => window.clearTimeout(timer));
  stableRefreshTimers = [0, 80, 220].map((delay) => window.setTimeout(() => scheduleGlassRefresh(), delay));
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

  const glassElements = Array.from(root.children).filter((child) =>
    child.classList.contains("liquid-glass"),
  );

  try {
    const instance = await LiquidGlass.init({
      root,
      glassElements,
      defaults: {
        blurAmount: 0.2,
        refraction: 0.66,
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
    console.error("Failed to initialize ChangePassword page LiquidGlass", err);
  }
}

function fieldError(name) {
  if (name === "newPassword" && touched.newPassword && form.newPassword && !newPasswordValid.value) {
    return "新密码长度必须为 8-64 位";
  }
  if (name === "confirmPassword" && touched.confirmPassword && form.confirmPassword && !confirmPasswordValid.value) {
    return "两次输入的新密码不一致";
  }
  return "";
}

async function submit() {
  touched.currentPassword = true;
  touched.newPassword = true;
  touched.confirmPassword = true;
  error.value = "";

  if (!canSubmit.value) {
    error.value = fieldError("newPassword") || fieldError("confirmPassword") || "请完整填写密码信息";
    return;
  }

  saving.value = true;
  try {
    await changePassword(form.currentPassword, form.newPassword, form.confirmPassword);
    success.value = true;
    auth.clearSession();
    window.setTimeout(() => {
      router.replace("/login");
    }, 720);
  } catch (err) {
    error.value = err?.message || "修改密码失败";
  } finally {
    saving.value = false;
  }
}

function goBack() {
  router.push(targetPath.value);
}

watch(form, () => {
  error.value = "";
  scheduleStableGlassRefresh();
});

onMounted(() => {
  initLiquidGlass();
  nextTick(() => currentPasswordRef.value?.focus());
});

onBeforeUnmount(() => {
  initToken += 1;
  if (refreshFrame) cancelAnimationFrame(refreshFrame);
  stableRefreshTimers.forEach((timer) => window.clearTimeout(timer));
  liquidGlass?.destroy();
  liquidGlass = null;
});
</script>

<template>
  <main ref="rootRef" class="password-page" aria-label="修改密码">
    <img
      ref="bgRef"
      class="password-bg"
      :src="pageBackground.src"
      alt=""
      decoding="async"
      fetchpriority="high"
      loading="eager"
      draggable="false"
      @load="scheduleGlassRefresh"
    />

    <section
      ref="panelRef"
      class="liquid-glass password-panel"
      :data-config="configJson(panelConfig)"
    >
      <button class="back-button" type="button" aria-label="返回" @click="goBack">
        <i class="ri-arrow-left-line" aria-hidden="true"></i>
      </button>

      <header class="password-head">
        <span class="password-icon"><i class="ri-lock-password-line" aria-hidden="true"></i></span>
        <div>
          <h1>修改密码</h1>
          <p>确认当前密码后设置新密码。</p>
        </div>
      </header>

      <form class="password-form" @submit.prevent="submit">
        <label>
          <span>当前密码</span>
          <input
            ref="currentPasswordRef"
            v-model="form.currentPassword"
            type="password"
            autocomplete="current-password"
            maxlength="64"
            @blur="touched.currentPassword = true"
          />
        </label>

        <label>
          <span>新密码</span>
          <input
            v-model="form.newPassword"
            type="password"
            autocomplete="new-password"
            minlength="8"
            maxlength="64"
            @blur="touched.newPassword = true"
          />
          <em v-if="fieldError('newPassword')" role="alert">{{ fieldError("newPassword") }}</em>
        </label>

        <label>
          <span>确认新密码</span>
          <input
            v-model="form.confirmPassword"
            type="password"
            autocomplete="new-password"
            minlength="8"
            maxlength="64"
            @blur="touched.confirmPassword = true"
          />
          <em v-if="fieldError('confirmPassword')" role="alert">{{ fieldError("confirmPassword") }}</em>
        </label>

        <p v-if="error" class="password-error" role="alert">{{ error }}</p>
        <p v-if="success" class="password-success" role="status">密码已修改，请重新登录。</p>

        <button class="submit-button" type="submit" :disabled="!canSubmit">
          <i class="ri-key-2-line" aria-hidden="true"></i>
          <span>{{ saving ? "提交中" : "确认修改" }}</span>
        </button>
      </form>
    </section>
  </main>
</template>

<style scoped>
:global(html),
:global(body),
:global(#app) {
  width: 100%;
  height: 100%;
  overflow: hidden;
  overscroll-behavior: none;
}

.password-page {
  position: fixed;
  inset: 0;
  overflow: hidden;
  isolation: isolate;
  background: #0c0f14;
  color: #fff;
  font-family: inherit;
}

.password-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  pointer-events: none;
  user-select: none;
}

.liquid-glass {
  position: absolute;
  border: 0;
  background: transparent;
  font: inherit;
}

.password-panel {
  top: 50%;
  left: 50%;
  z-index: 2;
  width: min(440px, calc(100vw - 36px));
  max-height: calc(100dvh - 32px);
  overflow-x: hidden;
  overflow-y: auto;
  padding: 26px 26px max(30px, env(safe-area-inset-bottom));
  border-radius: 42px;
  color: rgba(255, 255, 255, 0.94);
  transform: translate(-50%, -50%);
  scrollbar-width: none;
}

.password-panel::-webkit-scrollbar {
  display: none;
}

.password-panel::before {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 0;
  border-radius: inherit;
  background: rgba(10, 9, 18, 0.16);
  pointer-events: none;
}

.back-button,
.password-head,
.password-form {
  position: relative;
  z-index: 1;
}

.back-button {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  font-size: 20px;
}

.password-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 22px;
  margin-bottom: 24px;
}

.password-icon {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
  font-size: 24px;
}

.password-head h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 780;
  letter-spacing: 0;
}

.password-head p {
  margin: 6px 0 0;
  color: rgba(255, 255, 255, 0.64);
  font-size: 13px;
  line-height: 1.5;
}

.password-form {
  display: grid;
  gap: 14px;
}

.password-form label {
  display: grid;
  gap: 7px;
  color: rgba(255, 255, 255, 0.68);
  font-size: 12px;
  font-weight: 650;
}

.password-form input {
  width: 100%;
  min-height: 48px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 22px;
  outline: none;
  padding: 0 16px;
  background: rgba(0, 0, 0, 0.22);
  color: rgba(255, 255, 255, 0.96);
  font: inherit;
  font-size: 15px;
}

.password-form input:focus {
  border-color: rgba(255, 255, 255, 0.58);
}

.password-form em,
.password-error {
  margin: 0;
  color: rgba(255, 142, 142, 0.96);
  font-size: 12px;
  font-style: normal;
  font-weight: 650;
}

.password-success {
  margin: 0;
  color: rgba(164, 255, 198, 0.96);
  font-size: 12px;
  font-weight: 650;
}

.submit-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 48px;
  margin-top: 4px;
  border: 0;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.88);
  color: rgba(20, 18, 28, 0.92);
  cursor: pointer;
  font: inherit;
  font-size: 14px;
  font-weight: 780;
}

.submit-button:disabled {
  cursor: default;
  opacity: 0.48;
}

@media (max-width: 560px) {
  .password-panel {
    width: calc(100vw - 28px);
    max-height: calc(100dvh - 20px);
    padding: 22px 22px max(28px, env(safe-area-inset-bottom));
  }
}

@media (max-height: 680px) {
  .password-panel {
    padding: 20px 20px max(26px, env(safe-area-inset-bottom));
  }

  .password-head {
    margin-top: 14px;
    margin-bottom: 16px;
  }

  .password-form {
    gap: 10px;
  }
}
</style>
