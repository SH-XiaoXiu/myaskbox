<script setup>
import { LiquidGlass } from "@ybouane/liquidglass";
import { nextTick, onBeforeUnmount, onMounted, ref } from "vue";

const rootRef = ref(null);
const bgRef = ref(null);
const avatarPickerRef = ref(null);
const avatarHandleRef = ref(null);
const avatarIndicatorRef = ref(null);
const selectedAvatarIndex = ref(0);
const avatarSelectorStyle = ref({});
const avatarHandleStyle = ref({});
const avatarIndicatorStyle = ref({
  width: "0px",
  height: "0px",
  transform: "translate(-9999px, -9999px)",
});
let liquidGlass = null;
let initToken = 0;
let inputDrag = null;
let avatarMoveDrag = null;
let qaWheelFrame = 0;

const qaScroll = ref(0);

const qaItems = [
  {
    id: "qa-1",
    icon: "ri-leaf-fill",
    name: "Moss",
    time: "2天前",
    question: "你最近一次认真改变一个想法，是因为什么？",
    answer:
      "是因为发现自己一直在用旧经验判断新问题。真正改变我的不是某个道理，而是一次很具体的失败：我以为能复用的方法完全不适合当前场景，于是被迫重新观察、重新提问。",
  },
  {
    id: "qa-2",
    icon: "ri-moon-fill",
    name: "Moon",
    time: "3天前",
    question: "一个人独处的时候，怎么判断自己是在休息还是在逃避？",
    answer:
      "休息之后会更有力气面对事情，逃避之后通常只剩下更重的拖延感。所以我会看独处结束后的状态：如果更清醒，就是休息；如果更焦虑，大概率是在绕开问题。",
  },
  {
    id: "qa-3",
    icon: "ri-sparkling-fill",
    name: "Sparkle",
    time: "5天前",
    question: "你觉得长期坚持一件事最难的部分是什么？",
    answer:
      "不是开始，也不是努力，而是接受它大多数时候并不浪漫。长期坚持更像整理房间：重复、细碎、没什么掌声，但一段时间后你会发现自己确实住进了更好的秩序里。",
  },
  {
    id: "qa-4",
    icon: "ri-sun-fill",
    name: "Sun",
    time: "1周前",
    question: "如果必须给过去的自己一句建议，你会说什么？",
    answer:
      "不要把所有犹豫都解释成不够勇敢。有些犹豫是在提醒你信息还不够，节奏还不对。慢一点不等于退缩，只要你没有停止观察和行动。",
  },
  {
    id: "qa-5",
    icon: "ri-cloud-fill",
    name: "Cloud",
    time: "1周前",
    question: "怎么面对一段没有结果的努力？",
    answer:
      "我会先承认它确实令人难过，然后再看它留下了什么。很多努力不会以结果的形式回来，但会变成判断力、耐心，或者下一次少走弯路的直觉。",
  },
  {
    id: "qa-6",
    icon: "ri-heart-fill",
    name: "Heart",
    time: "2周前",
    question: "你最近觉得最珍贵的能力是什么？",
    answer:
      "是把事情说清楚的能力。说清楚不是为了赢，而是为了减少误解、降低内耗，也让别人知道你真正需要什么、愿意承担什么。",
  },
];

const qaCardConfig = {
  blurAmount: 0.25,
  refraction: 0.69,
  chromAberration: 0.05,
  edgeHighlight: 0.05,
  specular: 0,
  fresnel: 1,
  distortion: 0,
  cornerRadius: 22,
  zRadius: 22,
  opacity: 1,
  saturation: 0,
  tintStrength: 0,
  brightness: -0.3,
  shadowOpacity: 0.3,
  shadowSpread: 10,
  shadowOffsetY: 1,
  floating: false,
  bevelMode: 0,
};

const avatarOptions = [
  {
    name: "Moss",
    icon: "ri-leaf-fill",
  },
  {
    name: "Sun",
    icon: "ri-sun-fill",
  },
  {
    name: "Bloom",
    icon: "ri-flower-fill",
  },
  {
    name: "Moon",
    icon: "ri-moon-fill",
  },
  {
    name: "Sparkle",
    icon: "ri-sparkling-fill",
  },
  {
    name: "Star",
    icon: "ri-star-fill",
  },
  {
    name: "Fire",
    icon: "ri-fire-fill",
  },
  {
    name: "Water",
    icon: "ri-water-flash-fill",
  },
  {
    name: "Cloud",
    icon: "ri-cloud-fill",
  },
  {
    name: "Heart",
    icon: "ri-heart-fill",
  },
  {
    name: "User",
    icon: "ri-user-smile-fill",
  },
  {
    name: "Robot",
    icon: "ri-robot-2-fill",
  },
];

const avatarLoopOptions = Array.from({ length: 3 }, (_, cycle) =>
  avatarOptions.map((avatar, index) => ({
    ...avatar,
    cycle,
    index,
    key: `${cycle}-${avatar.name}`,
  })),
).flat();

const avatarGlassConfig = {
  blurAmount: 0,
  refraction: 0.69,
  chromAberration: 0.05,
  edgeHighlight: 0.2,
  specular: 0,
  fresnel: 1,
  distortion: 0,
  cornerRadius: 16,
  zRadius: 16,
  opacity: 1,
  saturation: 0,
  tintStrength: 0,
  brightness: 0,
  shadowOpacity: 0.25,
  shadowSpread: 10,
  shadowOffsetY: 1,
  floating: false,
  bevelMode: 0,
};

const glasses = [
  {
    tag: "div",
    className: "glass-main",
    label: "Glass",
    config: {
      blurAmount: 0,
      refraction: 0.69,
      chromAberration: 0.05,
      edgeHighlight: 0.05,
      specular: 0,
      fresnel: 1,
      distortion: 0,
      cornerRadius: 40,
      zRadius: 40,
      opacity: 1,
      saturation: 0,
      tintStrength: 0,
      brightness: 0,
      shadowOpacity: 0.3,
      shadowSpread: 10,
      shadowOffsetY: 0,
      floating: true,
      bevelMode: 0,
    },
  },
  {
    tag: "button",
    className: "glass-button",
    label: "Button",
    config: {
      blurAmount: 0,
      refraction: 0.69,
      chromAberration: 0.05,
      edgeHighlight: 0.06,
      specular: 0,
      fresnel: 1,
      distortion: 0,
      cornerRadius: 30,
      zRadius: 30,
      opacity: 1,
      saturation: 0,
      tintStrength: 0,
      brightness: 0,
      shadowOpacity: 0.28,
      shadowSpread: 10,
      shadowOffsetY: 0,
      floating: true,
      button: true,
      bevelMode: 0,
    },
  },
  {
    tag: "div",
    kind: "textarea",
    className: "glass-input",
    label: "Write a paragraph...",
    config: {
      blurAmount: 0.04,
      refraction: 0.62,
      chromAberration: 0.035,
      edgeHighlight: 0.04,
      specular: 0,
      fresnel: 0.96,
      distortion: 0,
      cornerRadius: 28,
      zRadius: 24,
      opacity: 1,
      saturation: 0,
      tintStrength: 0.04,
      brightness: 0.04,
      shadowOpacity: 0.24,
      shadowSpread: 10,
      shadowOffsetY: 0,
      floating: false,
      bevelMode: 0,
    },
  },
];

function configJson(config) {
  return JSON.stringify(config);
}

function qaCardStyle(index) {
  const cardStep = 230;
  const baseTop = 132;
  const x = "calc(100vw - min(430px, calc(100vw - 48px)) - max(26px, 6vw))";
  const rawY = baseTop + index * cardStep - qaScroll.value;
  const fadeStart = 86;
  const fadeEnd = -118;
  const exitProgress = Math.min(
    Math.max((fadeStart - rawY) / (fadeStart - fadeEnd), 0),
    1,
  );
  const opacity = Math.max(0, 1 - exitProgress);
  const scale = 1 - exitProgress * 0.075;
  const blur = exitProgress * 7;
  const drift = exitProgress * -18;
  const pointerEvents = opacity < 0.08 ? "none" : "auto";

  return {
    "--qa-x": x,
    "--qa-y": `${rawY + drift}px`,
    "--qa-scale": scale,
    "--qa-opacity": opacity,
    "--qa-blur": `${blur}px`,
    "--qa-z": qaItems.length - index,
    pointerEvents,
  };
}

function handleQaWheel(event) {
  if (event.target.closest(".avatar-selector")) return;
  if (event.target.closest(".glass-input")) return;

  const maxScroll = Math.max(0, (qaItems.length - 2) * 230);
  qaScroll.value = Math.min(Math.max(qaScroll.value + event.deltaY, 0), maxScroll);

  if (qaWheelFrame) return;
  qaWheelFrame = requestAnimationFrame(() => {
    qaWheelFrame = 0;
    liquidGlass?.markChanged();
  });
}

function getAvatarIndicatorRect() {
  const root = rootRef.value;
  const picker = avatarPickerRef.value;
  const button = picker?.querySelector(".avatar-option");
  if (!root || !picker || !button) return null;

  const rootRect = root.getBoundingClientRect();
  const pickerRect = picker.getBoundingClientRect();
  const buttonRect = button.getBoundingClientRect();

  return {
    left: pickerRect.left - rootRect.left + (pickerRect.width - buttonRect.width) / 2,
    top: pickerRect.top - rootRect.top,
    width: buttonRect.width,
    height: buttonRect.height,
  };
}

function applyAvatarIndicatorRect(rect) {
  const style = {
    width: `${rect.width}px`,
    height: `${rect.height}px`,
    transform: `translate(${rect.left}px, ${rect.top}px)`,
  };
  avatarIndicatorStyle.value = style;

  const indicator = avatarIndicatorRef.value;
  if (indicator) {
    indicator.style.width = style.width;
    indicator.style.height = style.height;
    indicator.style.transform = style.transform;
    liquidGlass?.markChanged(indicator);
  }
}

function moveAvatarIndicator() {
  const target = getAvatarIndicatorRect();
  if (!target) return;
  applyAvatarIndicatorRect(target);
}

async function selectAvatar(index, event) {
  selectedAvatarIndex.value = index;
  await nextTick();

  centerAvatar(index, "smooth", event?.currentTarget);

  moveAvatarIndicator();

  if (avatarPickerRef.value) liquidGlass?.markChanged(avatarPickerRef.value);
}

function centerAvatar(index, behavior = "smooth", targetEl = null) {
  const target =
    targetEl ||
    avatarPickerRef.value?.querySelector(
      `[data-avatar-cycle="1"][data-avatar-index="${index}"]`,
    );

  target?.scrollIntoView({
    behavior,
    block: "nearest",
    inline: "center",
  });
}

function handleAvatarResize() {
  moveAvatarIndicator();
  updateSelectedAvatarFromCenter();
}

function handleAvatarScroll() {
  recenterAvatarLoop();
  moveAvatarIndicator();
  updateSelectedAvatarFromCenter();
  if (avatarPickerRef.value) liquidGlass?.markChanged(avatarPickerRef.value);
}

function handleAvatarWheel(event) {
  const picker = avatarPickerRef.value;
  if (!picker || Math.abs(event.deltaX) > Math.abs(event.deltaY)) return;

  event.stopPropagation();
  event.preventDefault();
  picker.scrollLeft += event.deltaY;
}

function updateSelectedAvatarFromCenter() {
  const picker = avatarPickerRef.value;
  if (!picker) return;

  const pickerRect = picker.getBoundingClientRect();
  const centerX = pickerRect.left + pickerRect.width / 2;
  let nextIndex = selectedAvatarIndex.value;
  let nearestDistance = Infinity;

  for (const button of picker.querySelectorAll(".avatar-option")) {
    const rect = button.getBoundingClientRect();
    const distance = Math.abs(rect.left + rect.width / 2 - centerX);
    if (distance < nearestDistance) {
      nearestDistance = distance;
      nextIndex = Number(button.dataset.avatarIndex);
    }
  }

  if (nextIndex !== selectedAvatarIndex.value) {
    selectedAvatarIndex.value = nextIndex;
  }
}

function getAvatarCycleWidth() {
  const picker = avatarPickerRef.value;
  const first = picker?.querySelector('[data-avatar-cycle="0"][data-avatar-index="0"]');
  const second = picker?.querySelector('[data-avatar-cycle="1"][data-avatar-index="0"]');
  if (!first || !second) return 0;
  return second.offsetLeft - first.offsetLeft;
}

function recenterAvatarLoop() {
  const picker = avatarPickerRef.value;
  const cycleWidth = getAvatarCycleWidth();
  if (!picker || !cycleWidth) return;

  let nextScrollLeft = picker.scrollLeft;
  while (nextScrollLeft < cycleWidth * 0.5) nextScrollLeft += cycleWidth;
  while (nextScrollLeft > cycleWidth * 1.5) nextScrollLeft -= cycleWidth;
  if (Math.abs(nextScrollLeft - picker.scrollLeft) < 1) return;

  const previousBehavior = picker.style.scrollBehavior;
  picker.style.scrollBehavior = "auto";
  picker.scrollLeft = nextScrollLeft;
  picker.style.scrollBehavior = previousBehavior;
}

function applyAvatarPickerPosition(left, top) {
  const picker = avatarPickerRef.value;
  const handle = avatarHandleRef.value;
  if (!picker || !handle) return;

  const selectorStyle = {
    left: `${left}px`,
    top: `${top}px`,
    transform: "none",
  };
  const handleStyle = {
    left: `${left}px`,
    top: `${top - 16}px`,
    transform: "none",
  };

  avatarSelectorStyle.value = selectorStyle;
  avatarHandleStyle.value = handleStyle;

  Object.assign(picker.style, selectorStyle);
  Object.assign(handle.style, handleStyle);

  moveAvatarIndicator();
  liquidGlass?.markChanged(picker);
}

function startAvatarMoveDrag(event) {
  if (event.button !== 0) return;

  const root = rootRef.value;
  const picker = avatarPickerRef.value;
  if (!root || !picker) return;

  event.preventDefault();

  const rootRect = root.getBoundingClientRect();
  const pickerRect = picker.getBoundingClientRect();
  const startLeft = pickerRect.left - rootRect.left;
  const startTop = pickerRect.top - rootRect.top;

  applyAvatarPickerPosition(startLeft, startTop);

  avatarMoveDrag = {
    startX: event.clientX,
    startY: event.clientY,
    startLeft,
    startTop,
  };

  window.addEventListener("pointermove", dragAvatarPicker);
  window.addEventListener("pointerup", stopAvatarMoveDrag, { once: true });
}

function dragAvatarPicker(event) {
  if (!avatarMoveDrag || !rootRef.value || !avatarPickerRef.value) return;

  const rootRect = rootRef.value.getBoundingClientRect();
  const picker = avatarPickerRef.value;
  const margin = 10;
  const dx = event.clientX - avatarMoveDrag.startX;
  const dy = event.clientY - avatarMoveDrag.startY;
  const maxLeft = rootRect.width - picker.offsetWidth - margin;
  const maxTop = rootRect.height - picker.offsetHeight - margin;
  const nextLeft = Math.min(
    Math.max(avatarMoveDrag.startLeft + dx, margin),
    maxLeft,
  );
  const nextTop = Math.min(
    Math.max(avatarMoveDrag.startTop + dy, margin + 16),
    maxTop,
  );

  applyAvatarPickerPosition(nextLeft, nextTop);
}

function stopAvatarMoveDrag() {
  window.removeEventListener("pointermove", dragAvatarPicker);
  avatarMoveDrag = null;
}

function startInputDrag(event) {
  const root = rootRef.value;
  const target = event.currentTarget?.closest(".glass-input");
  if (!root || !target) return;

  event.preventDefault();

  const rootRect = root.getBoundingClientRect();
  const rect = target.getBoundingClientRect();
  const startLeft = rect.left - rootRect.left;
  const startTop = rect.top - rootRect.top;

  target.style.left = `${startLeft}px`;
  target.style.top = `${startTop}px`;
  target.style.transform = "none";

  inputDrag = {
    target,
    startX: event.clientX,
    startY: event.clientY,
    startLeft,
    startTop,
  };

  window.addEventListener("pointermove", dragInput);
  window.addEventListener("pointerup", stopInputDrag, { once: true });
}

function dragInput(event) {
  if (!inputDrag || !rootRef.value) return;

  const rootRect = rootRef.value.getBoundingClientRect();
  const { target, startX, startY, startLeft, startTop } = inputDrag;
  const margin = 10;
  const maxLeft = rootRect.width - target.offsetWidth - margin;
  const maxTop = rootRect.height - target.offsetHeight - margin;
  const nextLeft = Math.min(
    Math.max(startLeft + event.clientX - startX, margin),
    maxLeft,
  );
  const nextTop = Math.min(
    Math.max(startTop + event.clientY - startY, margin),
    maxTop,
  );

  target.style.left = `${nextLeft}px`;
  target.style.top = `${nextTop}px`;
  liquidGlass?.markChanged(target);
}

function stopInputDrag() {
  window.removeEventListener("pointermove", dragInput);
  inputDrag = null;
}

async function waitForBackgroundImage() {
  const image = bgRef.value;
  if (!image || image.complete) return;

  if (typeof image.decode === "function") {
    try {
      await image.decode();
      return;
    } catch {
      // Fall back to load/error events below.
    }
  }

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
  if (document.fonts?.ready) await document.fonts.ready;
  await nextTick();
  centerAvatar(selectedAvatarIndex.value, "auto");
  moveAvatarIndicator();

  const glassElements = Array.from(root.children).filter((child) =>
    child.classList.contains("liquid-glass"),
  );

  try {
    const instance = await LiquidGlass.init({
      root,
      glassElements,
      defaults: {
        blurAmount: 0,
        refraction: 0.69,
        chromAberration: 0.06,
        edgeHighlight: 0.05,
        specular: 0,
        fresnel: 1,
        shadowOpacity: 0.3,
        shadowSpread: 10,
        shadowOffsetY: 0,
      },
    });

    if (token !== initToken) {
      instance.destroy();
      return;
    }

    liquidGlass = instance;
    liquidGlass.markChanged();
  } catch (error) {
    console.error("Failed to initialize LiquidGlass playground", error);
  }
}

onMounted(() => {
  window.addEventListener("resize", handleAvatarResize);
  initLiquidGlass();
});

onBeforeUnmount(() => {
  initToken += 1;
  window.removeEventListener("resize", handleAvatarResize);
  if (qaWheelFrame) cancelAnimationFrame(qaWheelFrame);
  stopAvatarMoveDrag();
  stopInputDrag();
  liquidGlass?.destroy();
  liquidGlass = null;
});
</script>

<template>
  <main
    ref="rootRef"
    class="liquid-playground"
    aria-label="LiquidGlass preview"
    @wheel="handleQaWheel"
  >
    <img
      ref="bgRef"
      class="liquid-bg"
      src="/bg.png"
      alt=""
      decoding="async"
      draggable="false"
      @load="liquidGlass?.markChanged()"
    />

    <button
      ref="avatarHandleRef"
      class="avatar-move-handle"
      :style="avatarHandleStyle"
      type="button"
      aria-label="Move avatar selector"
      @pointerdown="startAvatarMoveDrag"
    >
      <span></span>
    </button>

    <section
      ref="avatarPickerRef"
      class="avatar-selector"
      :style="avatarSelectorStyle"
      aria-label="Avatar selector"
      @scroll="handleAvatarScroll"
      @wheel="handleAvatarWheel"
    >
      <button
        v-for="avatar in avatarLoopOptions"
        :key="avatar.key"
        class="avatar-option"
        :class="{ 'is-selected': selectedAvatarIndex === avatar.index }"
        :data-avatar-cycle="avatar.cycle"
        :data-avatar-index="avatar.index"
        :aria-label="avatar.name"
        :aria-pressed="selectedAvatarIndex === avatar.index"
        type="button"
        @click="selectAvatar(avatar.index, $event)"
      >
        <span class="avatar-disc" :style="{ background: avatar.bg }">
          <i :class="avatar.icon" aria-hidden="true"></i>
        </span>
      </button>
    </section>

    <div
      ref="avatarIndicatorRef"
      class="liquid-glass avatar-glass-indicator"
      :style="avatarIndicatorStyle"
      :data-config="configJson(avatarGlassConfig)"
      aria-hidden="true"
    ></div>

    <article
      v-for="(qa, index) in qaItems"
      :key="qa.id"
      class="liquid-glass qa-glass-card"
      :style="qaCardStyle(index)"
      :data-config="configJson(qaCardConfig)"
    >
      <header class="qa-card-head">
        <span class="qa-author">
          <span class="qa-avatar"><i :class="qa.icon" aria-hidden="true"></i></span>
          <span>{{ qa.name }}</span>
        </span>
        <time>{{ qa.time }}</time>
      </header>

      <p class="qa-question">{{ qa.question }}</p>
      <p class="qa-answer">{{ qa.answer }}</p>
    </article>

    <component
      :is="glass.tag"
      v-for="glass in glasses"
      :key="glass.className"
      class="liquid-glass"
      :class="glass.className"
      :data-config="configJson(glass.config)"
      :type="glass.tag === 'button' ? 'button' : undefined"
      :aria-label="glass.tag === 'button' ? glass.label : undefined"
    >
      <input
        v-if="glass.kind === 'input'"
        class="glass-field"
        type="text"
        :placeholder="glass.label"
        aria-label="Liquid glass input"
      />
      <template v-else-if="glass.kind === 'textarea'">
        <div
          class="glass-input-handle"
          aria-label="Drag paragraph input"
          role="button"
          tabindex="0"
          @pointerdown="startInputDrag"
        >
          <span></span>
        </div>
        <textarea
          class="glass-textarea"
          :placeholder="glass.label"
          aria-label="Liquid glass paragraph input"
        ></textarea>
      </template>
      <span v-else-if="glass.label" class="glass-label">{{ glass.label }}</span>
    </component>
  </main>
</template>

<style scoped>
.liquid-playground {
  position: relative;
  width: 100vw;
  min-height: 100vh;
  overflow: hidden;
  isolation: isolate;
  background: #0c0f14;
  color: #ffffff;
}

.liquid-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  user-select: none;
  pointer-events: none;
}

.avatar-selector {
  --avatar-slot: 100px;
  --avatar-height: 80px;
  --avatar-width: 300px;
  position: absolute;
  top: 42px;
  left: 50%;
  z-index: 4;
  display: flex;
  align-items: center;
  gap: 0;
  box-sizing: border-box;
  width: var(--avatar-width);
  min-width: var(--avatar-width);
  max-width: var(--avatar-width);
  overflow-x: auto;
  overflow-y: hidden;
  padding: 0;
  border-radius: 20px;
  background: #00000033;
  scroll-behavior: smooth;
  scroll-snap-type: x mandatory;
  scrollbar-width: none;
  touch-action: pan-x;
  transform: translateX(-50%);
}

.avatar-selector::-webkit-scrollbar {
  display: none;
}

.avatar-move-handle {
  --avatar-width: 300px;
  position: absolute;
  top: 26px;
  left: 50%;
  z-index: 6;
  display: grid;
  place-items: center;
  width: var(--avatar-width);
  height: 18px;
  border: 0;
  border-radius: 999px;
  padding: 0;
  background: transparent;
  cursor: grab;
  touch-action: none;
  transform: translateX(-50%);
}

.avatar-move-handle:active {
  cursor: grabbing;
}

.avatar-move-handle:focus-visible {
  outline: 2px solid rgba(255, 255, 255, 0.72);
  outline-offset: 2px;
}

.avatar-move-handle span {
  width: 54px;
  height: 4px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.44);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.18);
}

.avatar-option {
  position: relative;
  display: grid;
  place-items: center;
  width: var(--avatar-slot);
  height: var(--avatar-height);
  flex: 0 0 auto;
  border: 0;
  border-radius: 20px;
  padding: 0;
  background: transparent;
  color: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  scroll-snap-align: center;
}

.avatar-option:focus-visible {
  outline: 2px solid rgba(255, 255, 255, 0.78);
  outline-offset: -4px;
}

.avatar-disc {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 0;
  background: transparent !important;
  box-shadow: none;
  transition: opacity 200ms ease;
}

.avatar-disc i {
  color: #fff;
  font-size: 40px;
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.3));
  line-height: 1;
}

.avatar-option:hover .avatar-disc {
  opacity: 0.85;
}

.avatar-option:active .avatar-disc {
  opacity: 0.7;
}

.avatar-glass-indicator {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 5;
  border-radius: 20px;
  pointer-events: none;
  cursor: default;
  transform-origin: center;
  transition:
    transform 450ms cubic-bezier(0.65, 0, 0.35, 1),
    width 450ms cubic-bezier(0.65, 0, 0.35, 1),
    height 450ms cubic-bezier(0.65, 0, 0.35, 1);
}

.liquid-glass {
  position: absolute;
  z-index: 2;
  display: grid;
  place-items: center;
  border: 0;
  background: transparent;
  font: inherit;
  color: rgba(255, 255, 255, 0.88);
  touch-action: none;
  cursor: grab;
  will-change: transform;
}

.liquid-glass:active {
  cursor: grabbing;
}

.qa-glass-card {
  left: var(--qa-x);
  top: 0;
  z-index: calc(3 + var(--qa-z));
  display: block;
  width: min(430px, calc(100vw - 48px));
  min-height: 168px;
  padding: 18px 20px 20px;
  border-radius: 22px;
  color: rgba(255, 255, 255, 0.92);
  cursor: default;
  touch-action: pan-y;
  opacity: var(--qa-opacity);
  filter: blur(var(--qa-blur));
  transform: translateY(var(--qa-y)) scale(var(--qa-scale));
  transform-origin: top center;
  transition:
    opacity 180ms ease,
    filter 180ms ease,
    transform 180ms cubic-bezier(0.2, 0.8, 0.2, 1);
}

.qa-glass-card:active {
  cursor: default;
}

.qa-card-head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

.qa-author {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  gap: 9px;
  font-size: 13px;
  font-weight: 650;
  letter-spacing: 0;
}

.qa-avatar {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.28);
  flex: 0 0 auto;
}

.qa-avatar i {
  font-size: 16px;
  line-height: 1;
}

.qa-card-head time {
  flex: 0 0 auto;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.62);
  font-variant-numeric: tabular-nums;
}

.qa-question,
.qa-answer {
  position: relative;
  z-index: 1;
  margin: 0;
  letter-spacing: 0;
}

.qa-question {
  margin-bottom: 13px;
  color: rgba(255, 255, 255, 0.96);
  font-size: 15px;
  font-weight: 650;
  line-height: 1.62;
}

.qa-answer {
  padding-left: 13px;
  border-left: 2px solid rgba(255, 255, 255, 0.28);
  color: rgba(255, 255, 255, 0.72);
  font-size: 14px;
  font-weight: 450;
  line-height: 1.72;
}

.glass-label {
  position: relative;
  z-index: 1;
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  padding: 0 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 700;
  letter-spacing: 0;
  line-height: 1;
  text-shadow: 0 1px 6px rgba(8, 12, 18, 0.2);
  pointer-events: none;
}

.glass-main {
  top: calc(50% - 66px);
  left: 50%;
  width: 240px;
  height: 86px;
  border-radius: 40px;
  transform: translate(-50%, -50%);
}

.glass-main .glass-label {
  font-size: 14px;
}

.glass-button {
  top: calc(50% + 62px);
  left: 50%;
  width: 180px;
  height: 64px;
  border-radius: 30px;
  transform: translate(-50%, -50%);
}

.glass-button .glass-label {
  font-size: 14px;
}

.glass-input {
  top: calc(50% + 180px);
  left: 50%;
  width: min(520px, calc(100vw - 44px));
  height: 190px;
  padding: 10px;
  border-radius: 28px;
  cursor: default;
  transform: translate(-50%, -50%);
}

.glass-field,
.glass-textarea {
  position: relative;
  z-index: 1;
  width: 100%;
  min-width: 0;
  border: 1px solid rgba(255, 255, 255, 0.42);
  border-radius: 22px;
  outline: none;
  background: rgba(255, 255, 255, 0.32);
  color: rgba(255, 255, 255, 0.94);
  font: inherit;
  font-size: 15px;
  font-weight: 560;
  letter-spacing: 0;
  line-height: 1;
  padding: 0 18px;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.32),
    inset 0 -1px 0 rgba(0, 0, 0, 0.1);
}

.glass-field {
  height: 100%;
}

.glass-textarea {
  height: calc(100% - 32px);
  resize: none;
  line-height: 1.55;
  padding: 14px 18px;
}

.glass-field::placeholder,
.glass-textarea::placeholder {
  color: rgba(255, 255, 255, 0.66);
}

.glass-field:focus,
.glass-textarea:focus {
  border-color: rgba(255, 255, 255, 0.72);
  background: rgba(255, 255, 255, 0.4);
}

.glass-input-handle {
  position: relative;
  z-index: 2;
  display: grid;
  place-items: center;
  width: 100%;
  height: 28px;
  margin-bottom: 4px;
  border-radius: 18px;
  cursor: grab;
  touch-action: none;
}

.glass-input-handle:active {
  cursor: grabbing;
}

.glass-input-handle:focus-visible {
  outline: 2px solid rgba(255, 255, 255, 0.78);
  outline-offset: 2px;
}

.glass-input-handle span {
  width: 48px;
  height: 4px;
  border-radius: 99px;
  background: rgba(255, 255, 255, 0.52);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.12);
}

.glass-button:focus-visible {
  outline: 2px solid rgba(255, 255, 255, 0.82);
  outline-offset: 5px;
}

@media (max-width: 640px) {
  .avatar-selector {
    --avatar-slot: 76px;
    --avatar-height: 68px;
    --avatar-width: 228px;
    top: 28px;
    width: var(--avatar-width);
    min-width: var(--avatar-width);
    max-width: var(--avatar-width);
    overflow-x: auto;
    scrollbar-width: none;
  }

  .avatar-move-handle {
    --avatar-width: 228px;
    top: 12px;
  }

  .avatar-selector::-webkit-scrollbar {
    display: none;
  }

  .avatar-option {
    width: var(--avatar-slot);
    height: var(--avatar-height);
  }

  .avatar-disc {
    width: 34px;
    height: 34px;
  }

  .avatar-disc i {
    font-size: 32px;
  }

  .glass-main {
    width: 220px;
    height: 80px;
  }

  .glass-button {
    width: 168px;
    height: 60px;
  }

  .glass-input {
    top: calc(50% + 174px);
    height: 170px;
  }

  .qa-glass-card {
    left: 22px;
    width: calc(100vw - 44px);
    min-height: 150px;
    padding: 16px;
  }
}
</style>
