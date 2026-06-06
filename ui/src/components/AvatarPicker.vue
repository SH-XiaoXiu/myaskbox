<script setup>
import { useAskBox, avatarOptions } from "../composables/useAskBox.js";

const { selectedAvatar, selectAvatar } = useAskBox();
</script>

<template>
  <div class="avatar-picker">
    <span class="avatar-picker-label">匿名身份</span>
    <button
      v-for="opt in avatarOptions"
      :key="opt.icon"
      class="avatar-option"
      :class="{ selected: selectedAvatar?.icon === opt.icon }"
      :style="{ background: opt.bg }"
      :title="opt.name"
      @click="selectAvatar(opt)"
    >
      <i :class="opt.icon"></i>
    </button>
  </div>
</template>

<style scoped>
.avatar-picker {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
  overflow-x: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
  flex-wrap: nowrap;
}

.avatar-picker::-webkit-scrollbar {
  display: none;
}

.avatar-picker-label {
  font-size: 12px;
  color: var(--text-muted);
  white-space: nowrap;
  flex-shrink: 0;
  letter-spacing: 0.02em;
}

.avatar-option {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  color: #fff;
  font-size: 14px;
  position: relative;
  padding: 0;
  background: none;
}

.avatar-option:hover {
  transform: scale(1.15);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.avatar-option:active {
  transform: scale(0.92);
}

.avatar-option.selected {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
  transform: scale(1.1);
}

.avatar-option.selected::after {
  content: "";
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--accent);
  border: 2px solid #fff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}
</style>
