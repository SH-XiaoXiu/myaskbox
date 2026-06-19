<script setup>
import { ref, onMounted } from 'vue'
import { showDialog, showSuccessToast } from 'vant'
import { listAttachments, createAttachment, updateAttachment, deleteAttachment } from '@/api/admin'
import { uploadAttachmentObject } from '@/api/attachments'
import { assetSrc } from '@/utils/assets'

const loading = ref(true)
const error = ref('')
const attachments = ref([])
const currentPage = ref(1)
const hasMore = ref(false)
const loadingMore = ref(false)
const usageFilter = ref('ANONYMOUS_AVATAR')
const pageSize = 20

const usageOptions = [
  { text: '匿名头像', value: 'ANONYMOUS_AVATAR' },
  { text: '箱主头像', value: 'BOX_OWNER_AVATAR' },
  { text: '箱子背景', value: 'BOX_BACKGROUND' },
]
const usageColumns = usageOptions.map((option) => ({ text: option.text, value: option.value }))

const colorPresets = ['#6366f1', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6', '#ef4444', '#3b82f6', '#14b8a6', '#f97316', '#84cc16']
const bitmapAccept = 'image/png,image/jpeg,image/webp,image/gif'

onMounted(() => loadAttachments(true))

async function loadAttachments(reset = true) {
  if (!reset && (loading.value || loadingMore.value || !hasMore.value)) return
  if (reset) loading.value = true
  else loadingMore.value = true
  error.value = ''
  const nextPage = reset ? 1 : currentPage.value + 1
  try {
    const page = await listAttachments(nextPage, pageSize, usageFilter.value)
    const records = page.records || []
    if (reset) attachments.value = records
    else attachments.value.push(...records)
    currentPage.value = Number(page.page || nextPage)
    hasMore.value = currentPage.value < Number(page.totalPages || 0)
  } catch (err) {
    error.value = err.message || '加载失败'
  } finally {
    if (reset) loading.value = false
    else loadingMore.value = false
  }
}

function handleScroll(event) {
  const el = event.currentTarget
  const distance = el.scrollHeight - el.scrollTop - el.clientHeight
  if (distance < 160) loadAttachments(false)
}

function onUsageChange() {
  loadAttachments(true)
}

const showFormSheet = ref(false)
const showUsagePicker = ref(false)
const formTitle = ref('')
const isEditing = ref(false)
const editingAttachment = ref(null)
const form = ref({ name: '', usageType: 'ANONYMOUS_AVATAR', bg: '#6366f1', sortOrder: 0, objectKey: '', previewUrl: '', file: null, isActive: true })

function usageText(value) {
  return usageOptions.find((option) => option.value === value)?.text || ''
}

function imageAccept() {
  return form.value.usageType === 'ANONYMOUS_AVATAR'
    ? `${bitmapAccept},image/svg+xml`
    : bitmapAccept
}

function openCreate() {
  isEditing.value = false
  editingAttachment.value = null
  formTitle.value = '新增附件'
  form.value = { name: '', usageType: usageFilter.value, bg: '#6366f1', sortOrder: 0, objectKey: '', previewUrl: '', file: null, isActive: true }
  showFormSheet.value = true
}

function openEdit(attachment) {
  isEditing.value = true
  editingAttachment.value = attachment
  formTitle.value = '编辑附件'
  form.value = {
    name: attachment.name,
    usageType: attachment.usageType,
    bg: attachment.bg || '#6366f1',
    sortOrder: attachment.sortOrder || 0,
    objectKey: attachment.objectKey || '',
    previewUrl: assetSrc(attachment),
    file: null,
    isActive: attachment.isActive !== false,
  }
  showFormSheet.value = true
}

function handleFileChange(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (form.value.previewUrl?.startsWith('blob:')) URL.revokeObjectURL(form.value.previewUrl)
  form.value.file = file
  form.value.objectKey = ''
  form.value.previewUrl = URL.createObjectURL(file)
  if (!form.value.name) form.value.name = file.name.replace(/\.[^.]+$/, '')
}

function confirmUsage({ selectedOptions }) {
  const selected = selectedOptions?.[0]
  if (selected?.value) form.value.usageType = selected.value
  showUsagePicker.value = false
}

async function submitForm() {
  const objectKey = form.value.file
    ? await uploadAttachmentObject(form.value.file, form.value.usageType)
    : form.value.objectKey
  const payload = {
    name: form.value.name,
    usageType: form.value.usageType,
    bg: form.value.bg,
    sortOrder: form.value.sortOrder,
    objectKey,
    isActive: form.value.isActive,
  }
  try {
    if (isEditing.value && editingAttachment.value) {
      const updated = await updateAttachment(editingAttachment.value.id, payload)
      Object.assign(editingAttachment.value, updated)
      showSuccessToast('已更新')
    } else {
      const created = await createAttachment(payload)
      attachments.value.unshift(created)
      showSuccessToast('已创建')
    }
    showFormSheet.value = false
  } catch {}
}

async function handleDelete(attachment) {
  try {
    await showDialog({ title: '确认删除', message: `确定删除附件"${attachment.name}"吗？` })
    await deleteAttachment(attachment.id)
    attachments.value = attachments.value.filter((item) => item.id !== attachment.id)
    showSuccessToast('已删除')
  } catch {}
}

function formatSize(size) {
  if (!size) return '0 B'
  if (size < 1024) return `${size} B`
  return `${(size / 1024).toFixed(1)} KB`
}
</script>

<template>
  <div class="page">
    <div class="page-sticky">
      <van-dropdown-menu>
        <van-dropdown-item v-model="usageFilter" :options="usageOptions" @change="onUsageChange" />
      </van-dropdown-menu>
    </div>

    <div class="page-scroll" @scroll="handleScroll">
      <van-loading v-if="loading" class="loading-center" size="24" />
      <p v-else-if="error" class="error-msg">{{ error }}</p>
      <van-cell-group v-else inset>
        <van-cell v-for="attachment in attachments" :key="attachment.id" is-link @click="openEdit(attachment)">
          <template #icon>
            <div class="attachment-thumb" :style="{ background: attachment.bg || '#f2f3f5' }">
              <img :src="assetSrc(attachment)" alt="" />
            </div>
          </template>
          <template #title>
            <span>{{ attachment.name }}</span>
          </template>
          <template #label>
            <div class="cell-label">
              <span>{{ attachment.mimeType }}</span>
              <span>{{ formatSize(attachment.sizeBytes) }}</span>
              <span>#{{ attachment.sortOrder }}</span>
            </div>
          </template>
          <template #value>
            <van-tag :type="attachment.isActive ? 'success' : 'default'">
              {{ attachment.isActive ? '启用' : '停用' }}
            </van-tag>
          </template>
        </van-cell>
      </van-cell-group>
      <div v-if="!loading && !error && attachments.length === 0" class="empty-hint">暂无附件</div>
      <div v-if="!loading && !error && attachments.length > 0 && (loadingMore || hasMore)" class="load-more-hint">
        {{ loadingMore ? '加载中' : '继续下滑加载更多' }}
      </div>
    </div>

    <div class="page-bottom">
      <van-button type="primary" round block @click="openCreate">
        <i class="ri-add-line"></i> 新增附件
      </van-button>
    </div>

    <van-action-sheet v-model:show="showFormSheet" :title="formTitle">
      <div class="sheet-form">
        <van-field v-model="form.name" label="名称" placeholder="附件名称" />
        <van-field
          label="用途"
          :model-value="usageText(form.usageType)"
          readonly
          is-link
          placeholder="选择用途"
          @click="showUsagePicker = true"
        />
        <van-field v-model.number="form.sortOrder" label="排序" type="number" placeholder="0" />
        <van-field v-model="form.bg" label="背景色" placeholder="#6366f1">
          <template #input>
            <div class="color-field">
              <span class="color-dot" :style="{ background: form.bg }"></span>
              <input v-model="form.bg" class="color-input" placeholder="#6366f1" />
            </div>
          </template>
        </van-field>
        <div class="color-presets">
          <button
            v-for="color in colorPresets"
            :key="color"
            class="color-chip"
            :class="{ active: form.bg === color }"
            :style="{ background: color }"
            type="button"
            :aria-label="`选择颜色 ${color}`"
            @click="form.bg = color"
          ></button>
        </div>
        <van-field label="图片">
          <template #input>
            <label class="styled-file-button">
              <i class="ri-image-add-line" aria-hidden="true"></i>
              <span>{{ form.previewUrl || form.objectKey ? '重新选择图片' : '选择图片' }}</span>
              <input type="file" :accept="imageAccept()" @change="handleFileChange" />
            </label>
          </template>
        </van-field>
        <div v-if="form.previewUrl || form.objectKey" class="preview-row">
          <img :src="form.previewUrl || assetSrc(form.objectKey)" alt="" />
          <span>{{ form.file?.name || form.objectKey }}</span>
        </div>
        <van-cell title="启用">
          <template #right-icon>
            <van-switch v-model="form.isActive" size="22" />
          </template>
        </van-cell>
        <div class="sheet-actions">
          <van-button v-if="isEditing" plain type="danger" round @click="handleDelete(editingAttachment)">删除</van-button>
          <van-button type="primary" round @click="submitForm">保存</van-button>
        </div>
      </div>
    </van-action-sheet>

    <van-popup v-model:show="showUsagePicker" round position="bottom">
      <van-picker
        title="选择用途"
        :columns="usageColumns"
        @confirm="confirmUsage"
        @cancel="showUsagePicker = false"
      />
    </van-popup>
  </div>
</template>

<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }
.page-sticky { flex-shrink: 0; }
.page-scroll { flex: 1; overflow-y: auto; padding: 8px 0 12px; }
.page-bottom { flex-shrink: 0; padding: 8px 16px 16px; }
.attachment-thumb { display: grid; place-items: center; width: 44px; height: 44px; margin-right: 10px; border-radius: 10px; overflow: hidden; }
.attachment-thumb img { width: 100%; height: 100%; object-fit: cover; }
.cell-label { display: flex; gap: 8px; flex-wrap: wrap; font-size: 12px; }
.empty-hint, .load-more-hint, .error-msg { text-align: center; padding: 24px 0; font-size: 14px; color: #969799; }
.error-msg { color: #ee0a24; }
.loading-center { display: flex; justify-content: center; padding: 24px 0; }
.sheet-form { padding: 0 0 16px; }
.color-field { display: flex; align-items: center; gap: 8px; width: 100%; }
.color-dot { width: 24px; height: 24px; border-radius: 6px; flex-shrink: 0; border: 1px solid rgba(0,0,0,0.1); }
.color-input { flex: 1; border: none; outline: none; font-size: 14px; color: #323233; background: transparent; font-family: inherit; }
.color-presets { display: flex; flex-wrap: wrap; gap: 8px; padding: 0 16px 12px; }
.color-chip { width: 28px; height: 28px; border-radius: 8px; border: 2px solid transparent; cursor: pointer; padding: 0; }
.color-chip.active { border-color: #1989fa; box-shadow: 0 0 0 2px rgba(25,137,250,0.25); }
.styled-file-button {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 132px;
  min-height: 34px;
  padding: 0 14px;
  overflow: hidden;
  border-radius: 17px;
  background: #f2f3f5;
  color: #323233;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
}
.styled-file-button input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}
.preview-row { display: flex; align-items: center; gap: 10px; padding: 8px 16px; color: #646566; font-size: 12px; }
.preview-row img { width: 48px; height: 48px; border-radius: 10px; object-fit: cover; background: #f2f3f5; }
.sheet-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; padding: 12px 16px 0; }
.sheet-actions .van-button:only-child { grid-column: 1 / -1; }
</style>
