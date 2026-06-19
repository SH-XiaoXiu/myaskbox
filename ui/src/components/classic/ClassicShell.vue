<script setup>
defineProps({
  title: {
    type: String,
    default: 'AskBox',
  },
  subtitle: {
    type: String,
    default: '匿名提问箱',
  },
})
</script>

<template>
  <main class="classic-shell classic-page classic-enter">
    <header class="classic-shell__brand">
      <span class="classic-shell__mark">
        <img src="/icon.svg" alt="" />
      </span>
      <span>
        <strong>{{ title }}</strong>
        <em v-if="subtitle">{{ subtitle }}</em>
      </span>
    </header>
    <section class="classic-shell__body">
      <slot />
    </section>
  </main>
</template>

<style scoped>
.classic-shell {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  min-height: 100dvh;
  padding: max(18px, env(safe-area-inset-top)) 18px max(18px, env(safe-area-inset-bottom));
}

.classic-shell__brand {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 12px;
  width: min(100%, 520px);
  margin: 0 auto 18px;
  color: var(--classic-text);
}

.classic-shell__mark {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  overflow: hidden;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(47, 111, 237, 0.12);
}

.classic-shell__mark img {
  width: 30px;
  height: 30px;
  object-fit: contain;
}

.classic-shell__brand strong,
.classic-shell__brand em {
  display: block;
  line-height: 1.25;
  letter-spacing: 0;
  font-style: normal;
}

.classic-shell__brand strong {
  font-size: 18px;
  font-weight: 760;
}

.classic-shell__brand em {
  margin-top: 2px;
  color: var(--classic-muted);
  font-size: 13px;
}

.classic-shell__body {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  width: 100%;
}
</style>
