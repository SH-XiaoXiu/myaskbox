<template>
  <div class="empty-state">
    <div class="empty-icon-wrap">
      <i class="ri-inbox-line"></i>
    </div>
    <h3>还没有人提问</h3>
    <p>来做第一个提问的人吧</p>
  </div>
</template>

<style scoped>
.empty-state {
  text-align: center;
  padding: 64px 20px;
}

.empty-icon-wrap {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: var(--accent-bg);
  color: var(--accent-soft);
  font-size: 32px;
  margin-bottom: 24px;
}

h3 {
  font-size: 17px;
  color: var(--text);
  margin-bottom: 6px;
  font-weight: 600;
  letter-spacing: -0.01em;
}

p {
  font-size: 14px;
  color: var(--text-muted);
  line-height: 1.5;
}
</style>
