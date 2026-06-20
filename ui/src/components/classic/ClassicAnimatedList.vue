<script setup>
const props = defineProps({
  items: {
    type: Array,
    default: () => [],
  },
  itemKey: {
    type: [String, Function],
    default: 'id',
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
    default: '这里暂时没有内容',
  },
  loadingText: {
    type: String,
    default: '加载中',
  },
  moreText: {
    type: String,
    default: '继续下滑加载更多',
  },
  showLoadState: {
    type: Boolean,
    default: true,
  },
  emptyImage: {
    type: String,
    default: 'search',
  },
  ariaLabel: {
    type: String,
    default: '列表',
  },
  bodyClass: {
    type: [String, Array, Object],
    default: '',
  },
})

const emit = defineEmits(['leave', 'after-leave'])

function resolveKey(item, index) {
  if (typeof props.itemKey === 'function') return props.itemKey(item, index)
  return item?.[props.itemKey] ?? index
}

function handleLeave(el) {
  const siblings = Array.from(el.parentElement?.children || [])
  const index = siblings.indexOf(el)
  el.style.setProperty('--leave-index', String(Math.max(0, index)))
  emit('leave', el)
}

function handleAfterLeave(el) {
  emit('after-leave', el)
}
</script>

<template>
  <section
    class="classic-animated-list"
    :class="{ 'is-initial-loading': loading && items.length === 0 }"
    :style="{ minHeight: listMinHeight }"
    :aria-label="ariaLabel"
  >
    <TransitionGroup
      :name="transitionName"
      tag="div"
      class="classic-animated-list__inner"
      :class="bodyClass"
      :style="{ minHeight }"
      @leave="handleLeave"
      @after-leave="handleAfterLeave"
    >
      <div
        v-for="(item, index) in items"
        :key="resolveKey(item, index)"
        class="classic-animated-list__item"
        :style="{ '--card-index': index }"
      >
        <slot name="item" :item="item" :index="index" />
      </div>
    </TransitionGroup>

    <section v-if="error" class="classic-error-state" role="status">
      <span>{{ error }}</span>
    </section>
    <div v-else-if="loading && items.length === 0" class="load-state load-state--initial" aria-live="polite">
      <van-loading size="20" />
      <span>{{ loadingText }}</span>
    </div>
    <van-empty v-else-if="items.length === 0" :image="emptyImage" :description="emptyDescription" />
    <div v-if="showLoadState && items.length > 0 && (loading || hasMore)" class="load-state" aria-live="polite">
      <van-loading v-if="loading" size="18" />
      <span>{{ loading ? loadingText : moreText }}</span>
    </div>
  </section>
</template>

<style scoped>
.classic-animated-list {
  position: relative;
  min-height: 0;
}

.classic-animated-list.is-initial-loading {
  min-height: 220px;
}

.classic-animated-list__inner {
  position: relative;
  display: grid;
  gap: 10px;
}

.classic-animated-list__item {
  min-width: 0;
}

.classic-error-state {
  width: min(100%, 680px);
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 220px;
  color: var(--classic-muted);
  font-size: 14px;
}

.classic-error-state span {
  max-width: 100%;
  padding: 10px 14px;
  border: 1px solid var(--classic-line);
  border-radius: 999px;
  background: var(--classic-surface);
  overflow-wrap: anywhere;
}

.load-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 18px 0;
  color: var(--classic-muted);
  font-size: 13px;
}

.load-state--initial {
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  z-index: 2;
  height: 220px;
  max-height: 100%;
  min-height: 0;
  padding: 0;
}
</style>
