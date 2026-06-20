<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps({
  topics: {
    type: Array,
    default: () => [],
  },
  modelValue: {
    type: String,
    default: '',
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  label: {
    type: String,
    default: '话题',
  },
})

const emit = defineEmits(['update:modelValue', 'change'])

const open = ref(false)
const islandPulse = ref('')
const rootRef = ref(null)
let pulseTimer = 0

const selectedTopic = computed(() => props.topics.find((topic) => topic.code === props.modelValue) || null)
const islandLabel = computed(() => selectedTopic.value?.title || props.label)

function close() {
  if (open.value) playPulse('closing', 260)
  open.value = false
}

function toggle() {
  if (props.disabled || !props.topics.length) return
  if (open.value) {
    close()
    return
  }
  playPulse('opening', 360)
  open.value = true
}

function select(code) {
  emit('update:modelValue', code)
  emit('change', code)
  close()
}

function handleDocumentPointerDown(event) {
  if (!open.value) return
  const root = rootRef.value
  if (root && !root.contains(event.target)) close()
}

function handleKeydown(event) {
  if (event.key === 'Escape') close()
}

function playPulse(name, duration) {
  window.clearTimeout(pulseTimer)
  islandPulse.value = name
  pulseTimer = window.setTimeout(() => {
    islandPulse.value = ''
  }, duration)
}

watch(open, (value) => {
  if (value) {
    document.addEventListener('pointerdown', handleDocumentPointerDown, true)
    document.addEventListener('keydown', handleKeydown)
  } else {
    document.removeEventListener('pointerdown', handleDocumentPointerDown, true)
    document.removeEventListener('keydown', handleKeydown)
  }
})

onBeforeUnmount(() => {
  window.clearTimeout(pulseTimer)
  document.removeEventListener('pointerdown', handleDocumentPointerDown, true)
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <nav
    ref="rootRef"
    class="topic-island"
    :class="{ open, selected: selectedTopic, opening: islandPulse === 'opening', closing: islandPulse === 'closing' }"
    aria-label="话题筛选"
  >
    <button
      class="topic-island__button"
      type="button"
      :disabled="disabled || !topics.length"
      :aria-expanded="open"
      @click="toggle"
    >
      <span class="topic-island__button-text" :title="islandLabel">{{ islandLabel }}</span>
    </button>

    <Transition name="topic-island-menu">
      <div v-if="open" class="topic-island__menu" role="listbox">
        <button
          class="topic-island__option"
          :class="{ selected: !modelValue }"
          type="button"
          role="option"
          :aria-selected="!modelValue"
          @click="select('')"
        >
          <span class="topic-island__option-text" title="全部话题">全部话题</span>
          <i v-if="!modelValue" class="ri-check-line" aria-hidden="true"></i>
        </button>
        <button
          v-for="topic in topics"
          :key="topic.code"
          class="topic-island__option"
          :class="{ selected: modelValue === topic.code }"
          type="button"
          role="option"
          :aria-selected="modelValue === topic.code"
          @click="select(topic.code)"
        >
          <span class="topic-island__option-text" :title="topic.title">{{ topic.title }}</span>
          <i v-if="modelValue === topic.code" class="ri-check-line" aria-hidden="true"></i>
        </button>
      </div>
    </Transition>
  </nav>
</template>

<style scoped>
.topic-island {
  --island-width: 74px;
  --island-pulse-width: 74px;
  --island-open-width: 184px;
  position: relative;
  z-index: 8;
  justify-self: center;
  display: flex;
  justify-content: center;
  width: var(--island-open-width);
  min-height: 34px;
  border-radius: 999px;
}

.topic-island__button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: var(--island-width);
  min-height: 34px;
  border: 1px solid rgba(31, 41, 55, 0.1);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.94);
  color: var(--classic-text);
  font: inherit;
  font-size: 12px;
  font-weight: 700;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.06);
  padding: 0 11px;
  transition:
    width 360ms cubic-bezier(0.2, 0.92, 0.2, 1.12),
    border-radius 320ms cubic-bezier(0.2, 0.92, 0.2, 1),
    box-shadow 280ms ease;
}

.topic-island.selected {
  --island-width: 124px;
  --island-pulse-width: 124px;
}

.topic-island.open {
  --island-width: 184px;
}

.topic-island.opening .topic-island__button {
  animation: topic-island-button-open 360ms cubic-bezier(0.18, 0.96, 0.22, 1.14) both;
}

.topic-island.closing .topic-island__button {
  animation: topic-island-button-close 260ms cubic-bezier(0.3, 0.78, 0.28, 1.12) both;
}

.topic-island.open .topic-island__button {
  border-color: transparent;
  background: transparent;
  box-shadow: none;
}

.topic-island.open .topic-island__button-text {
  opacity: 0;
}

.topic-island__button-text {
  display: block;
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: opacity 120ms ease;
}

.topic-island__menu {
  position: absolute;
  top: 0;
  left: 50%;
  z-index: 20;
  display: grid;
  gap: 4px;
  width: min(240px, calc(100vw - 32px));
  max-height: 212px;
  border: 1px solid rgba(31, 41, 55, 0.1);
  border-radius: 18px;
  padding: 6px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.13);
  transform: translateX(-50%);
  transform-origin: 50% 0;
  will-change: opacity, transform;
}

.topic-island__menu::after {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 20px;
  border-radius: 0 0 18px 18px;
  background: linear-gradient(to bottom, rgba(255, 255, 255, 0), #fff);
  content: "";
  pointer-events: none;
}

.topic-island__option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  min-width: 0;
  min-height: 38px;
  border: 0;
  border-radius: 12px;
  padding: 0 10px;
  overflow: hidden;
  background: transparent;
  color: var(--classic-text);
  font: inherit;
  font-size: 13px;
  text-align: left;
}

.topic-island__option-text {
  display: block;
  flex: 1 1 auto;
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topic-island__option i {
  flex: 0 0 auto;
  color: var(--classic-primary);
  font-size: 17px;
}

.topic-island__option.selected,
.topic-island__option:hover {
  background: rgba(47, 111, 237, 0.08);
  color: var(--classic-primary);
}

.topic-island-menu-enter-active {
  animation: topic-island-in 430ms cubic-bezier(0.18, 0.96, 0.22, 1.14) both;
}

.topic-island-menu-leave-active {
  animation: topic-island-out 180ms ease-in both;
}

@keyframes topic-island-in {
  0% {
    opacity: 0;
    transform: translate3d(-50%, 0, 0) scale3d(0.76, 0.28, 1);
  }

  58% {
    opacity: 1;
    transform: translate3d(-50%, 0, 0) scale3d(1.018, 1.035, 1);
  }

  78% {
    transform: translate3d(-50%, 0, 0) scale3d(0.992, 0.985, 1);
  }

  100% {
    opacity: 1;
    transform: translate3d(-50%, 0, 0) scale3d(1, 1, 1);
  }
}

@keyframes topic-island-out {
  from {
    opacity: 1;
    transform: translate3d(-50%, 0, 0) scale3d(1, 1, 1);
  }

  to {
    opacity: 0;
    transform: translate3d(-50%, -3px, 0) scale3d(0.76, 0.68, 1);
  }
}

@keyframes topic-island-button-open {
  0% {
    width: var(--island-pulse-width);
  }

  38% {
    width: calc(var(--island-width) + 18px);
  }

  68% {
    width: calc(var(--island-width) - 4px);
  }

  100% {
    width: var(--island-width);
  }
}

@keyframes topic-island-button-close {
  0% {
    width: var(--island-open-width);
  }

  42% {
    width: calc(var(--island-pulse-width) + 22px);
  }

  100% {
    width: var(--island-width);
  }
}

@media (max-width: 430px) {
  .topic-island {
    --island-width: 64px;
    --island-pulse-width: 64px;
    --island-open-width: 168px;
  }

  .topic-island.selected {
    --island-width: 112px;
    --island-pulse-width: 112px;
  }

  .topic-island.open {
    --island-width: 168px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .topic-island,
  .topic-island.opening .topic-island__button,
  .topic-island.closing .topic-island__button,
  .topic-island__menu {
    animation: none;
    transition: none;
  }

  .topic-island-menu-enter-active,
  .topic-island-menu-leave-active {
    animation: none;
    opacity: 1;
    transform: translateX(-50%);
  }
}
</style>
