<script setup>
import { ref, onMounted } from 'vue'
import { showDialog, showSuccessToast } from 'vant'
import { listAvatars, createAvatar, updateAvatar, deleteAvatar } from '../../api/admin'

const loading = ref(true)
const error = ref('')
const avatars = ref([])
const currentPage = ref(1)
const hasMore = ref(false)
const loadingMore = ref(false)
const pageSize = 20

const colorPresets = ['#6366f1', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6', '#ef4444', '#3b82f6', '#14b8a6', '#f97316', '#84cc16']

// 默认 SVG 头像 base64 数据（圆形抽象图案）
const DEFAULT_SVG = 'data:image/svg+xml;base64,' + btoa('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 96 96"><circle cx="34" cy="38" r="20" fill="#fff" opacity=".34"/><circle cx="60" cy="58" r="14" fill="#111827" opacity=".12"/><circle cx="48" cy="48" r="45" fill="none" stroke="#fff" stroke-opacity=".45" stroke-width="2"/></svg>')

onMounted(() => loadAvatars(true))

async function loadAvatars(reset = true) {
  if (!reset && (loading.value || loadingMore.value || !hasMore.value)) return
  if (reset) loading.value = true
  else loadingMore.value = true
  error.value = ''
  const nextPage = reset ? 1 : currentPage.value + 1
  try {
    const page = await listAvatars(nextPage, pageSize)
    const records = page.records || []
    if (reset) avatars.value = records
    else avatars.value.push(...records)
    currentPage.value = Number(page.page || nextPage)
    hasMore.value = currentPage.value < Number(page.totalPages || 0)
  } catch (err) {
    error.value = err.message || '加载失败'
  } finally {
    if (reset) loading.value = false
    else loadingMore.value = false
  }
}

async function loadMoreAvatars() {
  await loadAvatars(false)
}

function handleScroll(event) {
  const el = event.currentTarget
  const distance = el.scrollHeight - el.scrollTop - el.clientHeight
  if (distance < 160) loadMoreAvatars()
}

const showFormSheet = ref(false)
const formTitle = ref('')
const isEditing = ref(false)
const editingAvatar = ref(null)
const form = ref({ name: '', bg: '#6366f1', sortOrder: 0, iconBase64: '' })

function openCreate() {
  isEditing.value = false
  editingAvatar.value = null
  formTitle.value = '新增头像'
  form.value = { name: '', bg: '#6366f1', sortOrder: 0, iconBase64: DEFAULT_SVG }
  showFormSheet.value = true
}

function openEdit(avatar) {
  isEditing.value = true
  editingAvatar.value = avatar
  formTitle.value = '编辑头像'
  form.value = { name: avatar.name, bg: avatar.bg, sortOrder: avatar.sortOrder, iconBase64: avatar.iconBase64 || '' }
  showFormSheet.value = true
}

async function submitForm() {
  try {
    if (isEditing.value && editingAvatar.value) {
      const updated = await updateAvatar(editingAvatar.value.id, {
        name: form.value.name,
        bg: form.value.bg,
        sortOrder: form.value.sortOrder,
        iconBase64: form.value.iconBase64 || editingAvatar.value.iconBase64,
      })
      Object.assign(editingAvatar.value, updated)
      showSuccessToast('已更新')
    } else {
      const newAvatar = await createAvatar({
        name: form.value.name,
        bg: form.value.bg,
        sortOrder: form.value.sortOrder,
        iconBase64: form.value.iconBase64 || DEFAULT_SVG,
      })
      avatars.value.unshift(newAvatar)
      showSuccessToast('已创建')
    }
  } catch (err) {
    showSuccessToast(err.message || '操作失败')
  }
  showFormSheet.value = false
}

async function handleDelete(avatar) {
  try {
    await showDialog({ title: '确认删除', message: `确定删除头像"${avatar.name}"吗？` })
    await deleteAvatar(avatar.id)
    avatars.value = avatars.value.filter(a => a.id !== avatar.id)
    showSuccessToast('已删除')
  } catch {}
}
</script>

<template>
  <div class="page">
    <div class="page-scroll" @scroll="handleScroll">
      <van-loading v-if="loading" class="loading-center" size="24" />
      <p v-else-if="error" class="error-msg">{{ error }}</p>
      <van-grid v-else :column-num="3" :gutter="12" :border="false">
        <van-grid-item v-for="avatar in avatars" :key="avatar.id" @click="openEdit(avatar)">
          <div class="avatar-card" :class="{ inactive: !avatar.isActive }">
            <div class="avatar-circle" :style="{ background: avatar.bg }">
              <img v-if="avatar.iconBase64" :src="avatar.iconBase64" class="avatar-img" alt="" />
              <span v-else class="avatar-char">{{ avatar.name.charAt(0) }}</span>
            </div>
            <span class="avatar-name">{{ avatar.name }}</span>
            <span class="avatar-order">#{{ avatar.sortOrder }}</span>
          </div>
        </van-grid-item>
      </van-grid>
      <div v-if="!loading && !error && avatars.length === 0" class="empty-hint">暂无头像</div>
      <div v-if="!loading && !error && avatars.length > 0 && (loadingMore || hasMore)" class="load-more-hint">
        {{ loadingMore ? '加载中' : '继续下滑加载更多' }}
      </div>
    </div>

    <div class="page-bottom">
      <van-button type="primary" round block @click="openCreate">
        <i class="ri-add-line"></i> 新增头像
      </van-button>
    </div>

    <van-action-sheet v-model:show="showFormSheet" :title="formTitle" close-on-click-action>
      <div class="sheet-form">
        <van-field v-model="form.name" label="名称" placeholder="头像名称" />
        <van-field v-model="form.bg" label="背景色" placeholder="#6366f1">
          <template #input>
            <div class="color-field">
              <span class="color-dot" :style="{ background: form.bg }"></span>
              <input v-model="form.bg" class="color-input" placeholder="#6366f1" />
            </div>
          </template>
        </van-field>
        <div class="color-presets">
          <button v-for="c in colorPresets" :key="c" class="color-chip" :class="{ active: form.bg === c }"
            :style="{ background: c }" @click="form.bg = c"></button>
        </div>
        <van-field v-model.number="form.sortOrder" label="排序" type="number" placeholder="0" />
        <van-field v-model="form.iconBase64" label="图标 Base64" placeholder="data:image/svg+xml;base64,..." />
        <div class="sheet-form-btn">
          <van-button type="primary" round block @click="submitForm">确定</van-button>
        </div>
      </div>
    </van-action-sheet>
  </div>
</template>

<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }
.page-scroll { flex: 1; overflow-y: auto; padding: 0 0 12px; }
.page-bottom { flex-shrink: 0; padding: 8px 16px 16px; }
.avatar-card { display: flex; flex-direction: column; align-items: center; gap: 6px; }
.avatar-card.inactive { opacity: 0.4; }
.avatar-circle { display: grid; place-items: center; width: 56px; height: 56px; border-radius: 50%; overflow: hidden; }
.avatar-img { width: 100%; height: 100%; object-fit: cover; }
.avatar-char { font-size: 22px; font-weight: 700; color: #fff; line-height: 1; }
.avatar-name { font-size: 12px; color: #323233; line-height: 1.2; }
.avatar-order { font-size: 10px; color: #c8c9cc; }
.empty-hint { text-align: center; padding: 32px 0; color: #969799; font-size: 14px; }
.load-more-hint { text-align: center; padding: 14px 0 18px; color: #969799; font-size: 12px; }
.loading-center { display: flex; justify-content: center; padding: 24px 0; }
.error-msg { text-align: center; color: #ee0a24; padding: 24px 0; font-size: 14px; }
.sheet-form { padding: 0 0 16px; }
.color-field { display: flex; align-items: center; gap: 8px; width: 100%; }
.color-dot { width: 24px; height: 24px; border-radius: 6px; flex-shrink: 0; border: 1px solid rgba(0,0,0,0.1); }
.color-input { flex: 1; border: none; outline: none; font-size: 14px; color: #323233; background: transparent; font-family: inherit; }
.color-presets { display: flex; flex-wrap: wrap; gap: 8px; padding: 0 16px 12px; }
.color-chip { width: 28px; height: 28px; border-radius: 8px; border: 2px solid transparent; cursor: pointer; padding: 0; transition: border-color 0.15s, box-shadow 0.15s; }
.color-chip:hover { border-color: rgba(0,0,0,0.2); }
.color-chip.active { border-color: #1989fa; box-shadow: 0 0 0 2px rgba(25,137,250,0.25); }
.sheet-form-btn { padding: 8px 16px 0; }
</style>
