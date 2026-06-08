<script setup>
import { computed, onMounted, ref } from 'vue'
import { showSuccessToast } from 'vant'
import { listSettings, updateSettings } from '../../api/admin'

const loading = ref(true)
const error = ref('')
const settings = ref([])
const form = ref({})

const groups = computed(() => {
  const map = new Map()
  for (const setting of settings.value) {
    if (!map.has(setting.groupCode)) map.set(setting.groupCode, [])
    map.get(setting.groupCode).push(setting)
  }
  return Array.from(map.entries())
})

onMounted(loadSettings)

async function loadSettings() {
  loading.value = true
  error.value = ''
  try {
    const rows = await listSettings()
    settings.value = rows || []
    form.value = Object.fromEntries(settings.value.map((item) => [
      item.key,
      item.valueType === 'BOOLEAN' ? item.value === 'true' : item.value || '',
    ]))
  } catch (err) {
    error.value = err.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function saveSettings() {
  const items = settings.value.map((item) => ({ key: item.key, value: normalizeValue(item) }))
  await updateSettings(items)
  showSuccessToast('设置已保存')
  await loadSettings()
}

function normalizeValue(item) {
  const value = form.value[item.key]
  if (item.valueType === 'BOOLEAN') return String(Boolean(value))
  return value == null ? '' : String(value)
}

function groupTitle(code) {
  if (code === 'registration') return '注册'
  if (code === 'mail') return '邮件'
  return code
}
</script>

<template>
  <div class="settings-page">
    <van-loading v-if="loading" class="loading-center" size="24" />
    <p v-else-if="error" class="error-msg">{{ error }}</p>
    <template v-else>
      <section v-for="[code, items] in groups" :key="code" class="setting-section">
        <h2>{{ groupTitle(code) }}</h2>
        <van-cell-group inset>
          <van-cell v-for="item in items" :key="item.key" :title="item.label" :label="item.description">
            <template #value>
              <van-switch v-if="item.valueType === 'BOOLEAN'" v-model="form[item.key]" size="22" />
              <input
                v-else
                v-model="form[item.key]"
                class="setting-input"
                :type="item.valueType === 'NUMBER' ? 'number' : item.isSecret ? 'password' : 'text'"
                :placeholder="item.isSecret ? '留空不修改' : ''"
              />
            </template>
          </van-cell>
        </van-cell-group>
      </section>
      <div class="save-bar">
        <van-button type="primary" round block @click="saveSettings">保存设置</van-button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.settings-page { height: 100%; overflow-y: auto; padding: 16px 0 92px; }
.setting-section { margin-bottom: 18px; }
.setting-section h2 { margin: 0 16px 8px; color: #323233; font-size: 16px; font-weight: 650; }
.setting-input { width: min(190px, 42vw); border: 1px solid #ebedf0; border-radius: 8px; outline: 0; padding: 7px 9px; background: #fff; color: #323233; font: inherit; font-size: 13px; text-align: right; }
.save-bar { position: fixed; left: 0; right: 0; bottom: var(--van-tabbar-height, 50px); padding: 10px 16px 14px; background: #f7f8fa; }
.loading-center { display: flex; justify-content: center; padding: 24px 0; }
.error-msg { text-align: center; color: #ee0a24; padding: 24px 0; font-size: 14px; }
</style>
