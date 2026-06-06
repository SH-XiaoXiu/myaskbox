<script setup>
import { useAskBox } from "../composables/useAskBox.js";

const { isAdmin } = useAskBox();

function toggleAdmin() {
  isAdmin.value = !isAdmin.value;
  if (isAdmin.value) {
    history.replaceState(null, "", "?admin");
  } else {
    history.replaceState(null, "", window.location.pathname);
  }
}
</script>

<template>
  <footer class="footer">
    <p>
      AskBox
      <span class="dot" title="管理入口" @click="toggleAdmin"></span>
    </p>
  </footer>
</template>

<style scoped>
.footer {
  text-align: center;
  padding: 56px 0 8px;
  color: var(--text-muted);
  font-size: 12px;
  letter-spacing: 0.02em;
}

.dot {
  display: inline-block;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--border);
  margin: 0 3px;
  vertical-align: middle;
  transition: all var(--transition);
  cursor: pointer;
}

.dot:hover {
  background: #aaa;
  transform: scale(1.5);
}
</style>
