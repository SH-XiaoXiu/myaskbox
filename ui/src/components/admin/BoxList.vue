<script setup>
import { ref, onMounted } from 'vue'
import { showSuccessToast } from 'vant'
import { listBoxes, updateBox } from '../../api/admin'
import { formatTime } from '../../utils'

const loading = ref(true)
const error = ref('')
const boxes = ref([])
const keyword = ref('')
const currentPage = ref(1)
const hasMore = ref(false)
const loadingMore = ref(false)
const pageSize = 20

onMounted(() => loadBoxes(true))

async function loadBoxes(reset = true) {
  if (!reset && (loading.value || loadingMore.value || !hasMore.value)) return
  if (reset) loading.value = true
  else loadingMore.value = true
  error.value = ''
  const nextPage = reset ? 1 : currentPage.value + 1
  try {
    const page = await listBoxes(nextPage, pageSize, keyword.value.trim())
    const records = page.records || []
    if (reset) boxes.value = records
    else boxes.value.push(...records)
    currentPage.value = Number(page.page || nextPage)
    hasMore.value = currentPage.value < Number(page.totalPages || 0)
  } catch (err) {
    error.value = err.message || '加载失败'
  } finally {
    if (reset) loading.value = false
    else loadingMore.value = false
  }
}

async function doSearch() {
  await loadBoxes(true)
}

async function loadMoreBoxes() {
  await loadBoxes(false)
}

function handleScroll(event) {
  const el = event.currentTarget
  const distance = el.scrollHeight - el.scrollTop - el.clientHeight
  if (distance < 160) loadMoreBoxes()
}

// ========== 编辑 ==========
const showEditSheet = ref(false)
const editingBox = ref(null)
const editForm = ref({ slug: '', displayName: '', description: '' })

function openEdit(box) {
  editingBox.value = box
  editForm.value = { slug: box.slug, displayName: box.displayName, description: box.description || '' }
  showEditSheet.value = true
}

async function submitEdit() {
  if (editingBox.value) {
    try {
      await updateBox(editingBox.value.id, {
        slug: editForm.value.slug,
        displayName: editForm.value.displayName,
        description: editForm.value.description,
      })
      editingBox.value.slug = editForm.value.slug
      editingBox.value.displayName = editForm.value.displayName
      editingBox.value.description = editForm.value.description
      showSuccessToast('已更新')
    } catch (err) {
      showSuccessToast(err.message || '更新失败')
    }
  }
  showEditSheet.value = false
}
</script>

<template>
  <div class="page">
    <div class="page-sticky">
      <van-search v-model="keyword" placeholder="搜索 slug、显示名、用户名" shape="round" @search="doSearch" />
    </div>

    <div class="page-scroll" @scroll="handleScroll">
      <van-loading v-if="loading" class="loading-center" size="24" />
      <p v-else-if="error" class="error-msg">{{ error }}</p>
      <van-cell-group v-else inset>
        <van-cell v-for="box in boxes" :key="box.id" is-link @click="openEdit(box)">
          <template #title>
            <span>{{ box.displayName }}</span>
          </template>
          <template #value>
            <span class="question-count">{{ box.questionCount }} 问</span>
          </template>
          <template #label>
            <div class="cell-label">
              <code class="slug-badge">{{ box.slug }}</code>
              <span>{{ box.username }} · {{ formatTime(new Date(box.createdAt).getTime()) }}</span>
            </div>
          </template>
        </van-cell>
      </van-cell-group>
      <div v-if="!loading && !error && boxes.length === 0" class="empty-hint">暂无匹配提问箱</div>
      <div v-if="!loading && !error && boxes.length > 0 && (loadingMore || hasMore)" class="load-more-hint">
        {{ loadingMore ? '加载中' : '继续下滑加载更多' }}
      </div>
    </div>

    <van-action-sheet v-model:show="showEditSheet" title="编辑提问箱" close-on-click-action>
      <div class="sheet-form">
        <van-field v-model="editForm.slug" label="Slug" placeholder="URL 标识" />
        <van-field v-model="editForm.displayName" label="显示名" placeholder="提问箱名称" />
        <van-field v-model="editForm.description" label="描述" type="textarea" rows="3" placeholder="提问箱描述" autosize />
        <div class="sheet-form-btn">
          <van-button type="primary" round block @click="submitEdit">保存</van-button>
        </div>
      </div>
    </van-action-sheet>
  </div>
</template>

<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }
.page-sticky { flex-shrink: 0; }
.page-scroll { flex: 1; overflow-y: auto; padding: 0 0 12px; }
.cell-label { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.slug-badge { padding: 1px 6px; background: #f0f2f5; border-radius: 4px; font-size: 11px; color: #1989fa; font-family: "SF Mono", "Cascadia Code", monospace; }
.question-count { font-weight: 600; color: #323233; font-size: 14px; }
.empty-hint { text-align: center; padding: 32px 0; color: #969799; font-size: 14px; }
.load-more-hint { text-align: center; padding: 14px 0 18px; color: #969799; font-size: 12px; }
.loading-center { display: flex; justify-content: center; padding: 24px 0; }
.error-msg { text-align: center; color: #ee0a24; padding: 24px 0; font-size: 14px; }
.sheet-form { padding: 0 0 16px; }
.sheet-form-btn { padding: 16px 16px 0; }
</style>
