<script setup>
import { assetSrc } from '@/utils/assets'
import ClassicAnimatedList from '@/components/classic/ClassicAnimatedList.vue'

defineProps({
  questions: {
    type: Array,
    default: () => [],
  },
  variant: {
    type: String,
    default: 'public',
    validator: (value) => ['public', 'owner'].includes(value),
  },
  transitionName: {
    type: String,
    default: 'classic-list',
  },
  minHeight: {
    type: String,
    default: '',
  },
  listMinHeight: {
    type: String,
    default: '',
  },
  loading: {
    type: Boolean,
    default: false,
  },
  hasMore: {
    type: Boolean,
    default: false,
  },
  error: {
    type: String,
    default: '',
  },
  emptyDescription: {
    type: String,
    default: '暂时还没有内容',
  },
  showLoadState: {
    type: Boolean,
    default: true,
  },
  boxAvatar: {
    type: Object,
    default: null,
  },
  ariaLabel: {
    type: String,
    default: '问题列表',
  },
})

const emit = defineEmits(['open', 'leave', 'after-leave'])

function avatarSrc(avatar) {
  return avatar ? assetSrc(avatar) : ''
}

function avatarStyle(avatar, fallbackBg = '#eff6ff') {
  return {
    backgroundColor: avatar?.bg || fallbackBg,
  }
}

function questionTime(question) {
  return question?.time || ''
}
</script>

<template>
  <ClassicAnimatedList
    :items="questions"
    :transition-name="transitionName"
    :min-height="minHeight"
    :list-min-height="listMinHeight"
    :loading="loading"
    :has-more="hasMore"
    :error="error"
    :empty-description="emptyDescription"
    :show-load-state="showLoadState"
    :aria-label="ariaLabel"
    @leave="emit('leave', $event)"
    @after-leave="emit('after-leave', $event)"
  >
    <template v-if="variant === 'public'" #item="{ item: question }">
      <article
        class="classic-card classic-press qa-card"
        :class="{ 'has-topic': question.topic }"
        role="button"
        tabindex="0"
        @click="emit('open', question, $event)"
        @keydown.enter.prevent="emit('open', question, $event)"
        @keydown.space.prevent="emit('open', question, $event)"
      >
        <header class="qa-card-head">
          <span class="qa-identity">
            <span class="mini-avatar" :style="avatarStyle(question.profile)">
              <img v-if="avatarSrc(question.profile)" :src="avatarSrc(question.profile)" alt="" />
            </span>
          </span>
          <time>{{ questionTime(question) }}</time>
        </header>
        <div class="qa-body">
          <p class="qa-question">{{ question.question }}</p>
          <p class="qa-answer" :class="{ muted: !question.answer }">
            <span
              v-if="question.answer"
              class="mini-avatar owner qa-answer-avatar"
              :style="avatarStyle(question.ownerAvatar || boxAvatar)"
            >
              <img
                v-if="avatarSrc(question.ownerAvatar || boxAvatar)"
                :src="avatarSrc(question.ownerAvatar || boxAvatar)"
                alt=""
              />
              <i v-else class="ri-question-answer-line" aria-hidden="true"></i>
            </span>
            <span class="qa-answer-text">{{ question.answer || '还在等待回答' }}</span>
          </p>
        </div>
        <span v-if="question.topic" class="topic-badge card-topic" :title="question.topic.title">{{ question.topic.title }}</span>
      </article>
    </template>

    <template v-else #item="{ item: question }">
      <article
        class="classic-card classic-press question-card"
        role="button"
        tabindex="0"
        @click="emit('open', question, $event)"
        @keydown.enter.prevent="emit('open', question, $event)"
        @keydown.space.prevent="emit('open', question, $event)"
      >
        <header>
          <span class="mini-avatar" :style="avatarStyle(question.profile)">
            <img v-if="avatarSrc(question.profile)" :src="avatarSrc(question.profile)" alt="" />
          </span>
          <time>{{ questionTime(question) }}</time>
        </header>
        <p>{{ question.question }}</p>
        <blockquote v-if="question.answer">{{ question.answer }}</blockquote>
        <span v-if="question.topic" class="topic-badge card-topic" :title="question.topic.title">{{ question.topic.title }}</span>
      </article>
    </template>
  </ClassicAnimatedList>
</template>

<style scoped>
.mini-avatar,
.qa-answer-avatar {
  display: grid;
  place-items: center;
  overflow: hidden;
  background-position: center;
  background-size: cover;
}

.mini-avatar img,
.qa-answer-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.topic-badge {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  min-height: 24px;
  border-radius: 999px;
  padding: 0 9px;
  overflow: hidden;
  background: rgba(47, 111, 237, 0.08);
  color: var(--classic-primary);
  font-size: 12px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-topic {
  max-width: calc(100% - 28px);
}

.qa-card {
  --scroll-y: 0px;
  --scroll-scale: 1;
  --scroll-opacity: 1;
  --scroll-shadow: 0;
  position: relative;
  padding: 20px 22px;
  border-color: #ebebea;
  border-radius: 12px;
  background: #fff;
  box-shadow:
    0 1px 2px rgba(15, 23, 42, 0.03),
    0 calc(8px * var(--scroll-shadow)) calc(22px * var(--scroll-shadow)) rgba(15, 23, 42, calc(0.04 * var(--scroll-shadow)));
  cursor: pointer;
  opacity: var(--scroll-opacity);
  transform: translate3d(0, var(--scroll-y), 0) scale(var(--scroll-scale));
  transform-origin: 50% 50%;
  transition:
    border-color 250ms var(--classic-ease),
    box-shadow 300ms ease,
    transform 200ms ease,
    background-color 180ms ease;
  animation: qa-card-in 340ms cubic-bezier(0.17, 0.9, 0.22, 1.16) backwards;
  animation-delay: calc(min(var(--card-index), 7) * 48ms);
  will-change: transform, opacity;
}

.qa-card.has-topic {
  padding-bottom: 48px;
}

.qa-card.no-enter-motion {
  animation: none;
}

.qa-card:hover {
  border-color: #d8d8d6;
  box-shadow: 0 3px 16px rgba(15, 23, 42, 0.04);
  transform: translate3d(0, calc(var(--scroll-y) - 1px), 0) scale(var(--scroll-scale));
}

.qa-card:active {
  transform: translate3d(0, var(--scroll-y), 0) scale(calc(var(--scroll-scale) * 0.99));
}

.qa-card:focus-visible,
.question-card:focus-visible {
  outline: 2px solid rgba(37, 99, 235, 0.48);
  outline-offset: 3px;
}

.qa-card-head,
.qa-identity {
  display: flex;
  align-items: center;
  gap: 8px;
}

.qa-card-head {
  justify-content: space-between;
  margin-bottom: 14px;
}

.qa-identity {
  min-width: 0;
}

.qa-card .mini-avatar {
  width: 26px;
  height: 26px;
  flex: 0 0 auto;
  border-radius: 50%;
}

.mini-avatar.owner {
  width: 24px;
  height: 24px;
}

time {
  color: var(--classic-muted);
  font-size: 12px;
}

.qa-card-head time {
  flex: 0 0 auto;
  padding-left: 10px;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.01em;
}

.qa-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.qa-question {
  margin: 0;
  color: var(--classic-text);
  font-size: 15px;
  font-weight: 480;
  line-height: 1.7;
  letter-spacing: 0.005em;
  overflow-wrap: anywhere;
}

.qa-answer {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  margin: 0 0 0 2px;
  padding: 0 0 0 14px;
  overflow: hidden;
  color: var(--classic-muted);
  border-left: 1.5px solid #ebebea;
  font-size: 14px;
  line-height: 1.55;
  overflow-wrap: anywhere;
  transition: border-color 400ms ease;
}

.qa-answer-avatar {
  width: 18px;
  height: 18px;
  margin-top: 2px;
  color: var(--classic-primary);
  font-size: 11px;
  border-radius: 50%;
}

.qa-answer-text {
  display: -webkit-box;
  min-width: 0;
  overflow: hidden;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.qa-card:hover .qa-answer {
  border-left-color: #c7c5e8;
}

.qa-answer.muted {
  color: var(--classic-muted);
  border-left-color: #ebebea;
}

.qa-card .card-topic {
  position: absolute;
  right: 14px;
  bottom: 12px;
  margin-bottom: 0;
}

.question-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  padding: 14px;
}

.question-card header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.question-card .mini-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
}

.question-card p {
  margin: 10px 0 0;
  color: var(--classic-text);
  font-size: 14px;
  line-height: 1.65;
  overflow-wrap: anywhere;
}

.question-card blockquote {
  display: -webkit-box;
  margin: 10px 0 0;
  padding: 0 0 0 12px;
  overflow: hidden;
  border-left: 2px solid rgba(47, 111, 237, 0.35);
  color: #374151;
  font-size: 13px;
  line-height: 1.65;
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.question-card .card-topic {
  align-self: flex-end;
  margin-top: 12px;
}

@keyframes qa-card-in {
  from {
    opacity: 0;
    transform: translate3d(0, calc(var(--scroll-y) + 10px), 0) scale(var(--scroll-scale));
  }
  to {
    opacity: var(--scroll-opacity);
    transform: translate3d(0, var(--scroll-y), 0) scale(var(--scroll-scale));
  }
}

@media (prefers-reduced-motion: reduce) {
  .qa-card {
    animation: none;
    opacity: 1;
    transform: none;
  }
}
</style>
