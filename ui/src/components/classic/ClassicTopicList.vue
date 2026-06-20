<script setup>
import ClassicAnimatedList from '@/components/classic/ClassicAnimatedList.vue'

defineProps({
  topics: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
  transitionName: {
    type: String,
    default: 'classic-list',
  },
  emptyDescription: {
    type: String,
    default: '暂无话题',
  },
})

const emit = defineEmits(['copy', 'close'])

function topicStatusText(topic) {
  if (topic.status === 'EXPIRED') return '已到期'
  if (topic.status === 'CLOSED') return '已关闭'
  return '进行中'
}

function topicExpiresText(topic) {
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(topic.expiresAt))
}
</script>

<template>
  <ClassicAnimatedList
    :items="topics"
    :transition-name="transitionName"
    :loading="loading"
    :empty-description="emptyDescription"
    loading-text="加载话题"
    aria-label="话题列表"
    :show-load-state="false"
  >
    <template #item="{ item: topic }">
      <article class="topic-item">
        <header>
          <span>
            <strong>{{ topic.title }}</strong>
            <small>{{ topicExpiresText(topic) }} 到期 · {{ topic.questionCount || 0 }} 个问题</small>
          </span>
          <em :class="{ inactive: !topic.available }">{{ topicStatusText(topic) }}</em>
        </header>
        <p v-if="topic.description">{{ topic.description }}</p>
        <footer v-if="topic.status === 'ACTIVE'">
          <button type="button" @click="emit('copy', topic)">
            <i class="ri-links-line" aria-hidden="true"></i>
            <span>复制链接</span>
          </button>
          <button type="button" class="danger" @click="emit('close', topic)">关闭</button>
        </footer>
      </article>
    </template>
  </ClassicAnimatedList>
</template>

<style scoped>
.topic-item {
  display: grid;
  gap: 10px;
  border: 1px solid var(--classic-line);
  border-radius: 12px;
  padding: 13px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.03);
}

.topic-item header,
.topic-item footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.topic-item header > span {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.topic-item strong,
.topic-item small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topic-item strong {
  color: var(--classic-text);
  font-size: 15px;
  font-weight: 700;
}

.topic-item small,
.topic-item p {
  color: var(--classic-muted);
  font-size: 12px;
  line-height: 1.5;
}

.topic-item p {
  margin: 0;
  overflow-wrap: anywhere;
}

.topic-item em {
  flex: 0 0 auto;
  border-radius: 999px;
  padding: 4px 8px;
  background: rgba(47, 111, 237, 0.1);
  color: var(--classic-primary);
  font-size: 12px;
  font-style: normal;
  font-weight: 650;
}

.topic-item em.inactive {
  background: var(--classic-surface-soft);
  color: var(--classic-muted);
}

.topic-item footer {
  justify-content: flex-start;
  min-height: 34px;
}

.topic-item footer button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 34px;
  border: 1px solid var(--classic-line);
  border-radius: 17px;
  padding: 0 12px;
  background: #fff;
  color: var(--classic-text);
  font: inherit;
  font-size: 13px;
}

.topic-item footer button.danger {
  border-color: rgba(240, 68, 56, 0.22);
  color: var(--classic-red);
}
</style>
