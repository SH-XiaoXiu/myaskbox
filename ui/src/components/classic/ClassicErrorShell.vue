<script setup>
const props = defineProps({
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
    default: '',
  },
  icon: {
    type: String,
    required: true,
  },
  tone: {
    type: String,
    default: 'blue',
  },
  primaryLabel: {
    type: String,
    required: true,
  },
  primaryIcon: {
    type: String,
    required: true,
  },
  secondaryLabel: {
    type: String,
    default: '',
  },
  secondaryIcon: {
    type: String,
    default: 'ri-arrow-left-line',
  },
})

const emit = defineEmits(['primary', 'secondary'])
</script>

<template>
  <main class="classic-error classic-page classic-enter" :class="`tone-${props.tone}`" :aria-label="`${statusCode} ${title}`">
    <section class="classic-card error-card" aria-labelledby="classic-error-title">
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
      <h1 id="classic-error-title">{{ title }}</h1>
      <p>{{ description }}</p>
      <em v-if="detail">{{ detail }}</em>
      <div class="actions">
        <van-button round type="primary" :icon="primaryIcon" @click="emit('primary')">
          {{ primaryLabel }}
        </van-button>
        <van-button v-if="secondaryLabel" round plain :icon="secondaryIcon" @click="emit('secondary')">
          {{ secondaryLabel }}
        </van-button>
      </div>
    </section>
  </main>
</template>

<style scoped>
.classic-error {
  display: grid;
  place-items: center;
  min-height: 100vh;
  min-height: 100dvh;
  padding: 20px;
}

.error-card {
  width: min(100%, 460px);
  padding: 22px 18px;
  text-align: center;
}

.status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}

.status-code {
  color: var(--classic-faint);
  font-size: 28px;
  font-weight: 800;
  line-height: 1;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 15px;
  background: var(--classic-primary-soft);
  color: var(--classic-primary);
  font-size: 13px;
  font-weight: 650;
}

.error-icon {
  display: grid;
  place-items: center;
  width: 58px;
  height: 58px;
  margin: 0 auto 14px;
  border-radius: 8px;
  background: var(--classic-primary-soft);
  color: var(--classic-primary);
  font-size: 28px;
}

h1 {
  margin: 0;
  font-size: 22px;
  line-height: 1.35;
}

p,
em {
  display: block;
  margin: 10px 0 0;
  color: var(--classic-muted);
  font-style: normal;
  font-size: 14px;
  line-height: 1.6;
}

.actions {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;
}

.tone-red .status-chip,
.tone-red .error-icon {
  background: #fef2f2;
  color: var(--classic-red);
}

.tone-amber .status-chip,
.tone-amber .error-icon {
  background: #fffbeb;
  color: var(--classic-amber);
}
</style>
