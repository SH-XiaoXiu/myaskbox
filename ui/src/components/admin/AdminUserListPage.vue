<script setup>
import { ref, onMounted } from 'vue'
import { showDialog, showSuccessToast } from 'vant'
import { listUsers, createUser, updateUser, disableUser, enableUser, assignUserRoles, listRoles } from '@/api/admin'
import { formatTime } from '@/utils'

const loading = ref(true)
const error = ref('')
const users = ref([])
const keyword = ref('')
const currentPage = ref(1)
const hasMore = ref(false)
const loadingMore = ref(false)
const pageSize = 20

const availableRoles = ref([])

onMounted(async () => {
  try {
    const [userPage, roles] = await Promise.all([
      listUsers(1, pageSize),
      listRoles(),
    ])
    users.value = userPage.records
    currentPage.value = userPage.page
    hasMore.value = userPage.page < userPage.totalPages
    availableRoles.value = roles.map(r => ({ code: r.code, name: r.name }))
  } catch (err) {
    error.value = err.message || '加载失败'
  } finally {
    loading.value = false
  }
})

async function doSearch() {
  loading.value = true
  try {
    const userPage = await listUsers(1, pageSize, keyword.value)
    users.value = userPage.records
    hasMore.value = userPage.page < userPage.totalPages
    currentPage.value = userPage.page
  } catch (err) {
    error.value = err.message || '搜索失败'
  } finally {
    loading.value = false
  }
}

async function loadMoreUsers() {
  if (loading.value || loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  try {
    const nextPage = currentPage.value + 1
    const userPage = await listUsers(nextPage, pageSize, keyword.value)
    users.value.push(...userPage.records)
    currentPage.value = userPage.page
    hasMore.value = userPage.page < userPage.totalPages
  } catch (err) {
    error.value = err.message || '加载失败'
  } finally {
    loadingMore.value = false
  }
}

function handleScroll(event) {
  const el = event.currentTarget
  const distance = el.scrollHeight - el.scrollTop - el.clientHeight
  if (distance < 160) loadMoreUsers()
}

// ========== ActionSheet: 新增/编辑 ==========
const showFormSheet = ref(false)
const formTitle = ref('')
const isEditing = ref(false)
const editingUser = ref(null)
const form = ref({ password: '', displayName: '', email: '' })

function openCreate() {
  isEditing.value = false
  editingUser.value = null
  formTitle.value = '新增用户'
  form.value = { password: '', displayName: '', email: '' }
  showFormSheet.value = true
}

function openEdit(user) {
  isEditing.value = true
  editingUser.value = user
  formTitle.value = '编辑用户'
  form.value = { password: '', displayName: user.displayName, email: user.email || user.username }
  showFormSheet.value = true
}

async function submitForm() {
  try {
    if (isEditing.value && editingUser.value) {
      await updateUser(editingUser.value.id, { displayName: form.value.displayName, email: form.value.email })
      editingUser.value.displayName = form.value.displayName
      editingUser.value.email = form.value.email.trim().toLowerCase()
      editingUser.value.username = editingUser.value.email
      showSuccessToast('已更新')
    } else {
      const newUser = await createUser({
        password: form.value.password,
        displayName: form.value.displayName,
        email: form.value.email,
      })
      users.value.unshift(newUser)
      showSuccessToast('已创建')
    }
  } catch {}
  showFormSheet.value = false
}

// ========== 角色分配 ==========
const showRoleSheet = ref(false)
const roleUser = ref(null)
const roleForm = ref([])

function openRole(user) {
  roleUser.value = user
  roleForm.value = [...(user.roles || [])]
  showRoleSheet.value = true
}

async function submitRole() {
  if (roleUser.value) {
    try {
      await assignUserRoles(roleUser.value.id, roleForm.value)
      roleUser.value.roles = [...roleForm.value]
      showSuccessToast('角色已更新')
    } catch {}
  }
  showRoleSheet.value = false
}

function toggleRole(code) {
  const idx = roleForm.value.indexOf(code)
  if (idx >= 0) roleForm.value.splice(idx, 1)
  else roleForm.value.push(code)
}

// ========== 启用/禁用 ==========
async function toggleStatus(user) {
  const action = user.status === 'ACTIVE' ? '禁用' : '启用'
  try {
    await showDialog({ title: '确认操作', message: `确定要${action}用户 ${user.email || user.username} 吗？` })
    if (user.status === 'ACTIVE') {
      await disableUser(user.id)
    } else {
      await enableUser(user.id)
    }
    user.status = user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
    showSuccessToast(`已${action}`)
  } catch {}
}

// ========== 操作菜单 ==========
const showAction = ref(false)
const actionUser = ref(null)
const actionOptions = ref([])

function openAction(user) {
  actionUser.value = user
  actionOptions.value = [
    { name: '编辑信息', icon: 'ri-edit-line', action: () => openEdit(user) },
    { name: '分配角色', icon: 'ri-shield-user-line', action: () => openRole(user) },
    { name: user.status === 'ACTIVE' ? '禁用用户' : '启用用户', icon: user.status === 'ACTIVE' ? 'ri-forbid-line' : 'ri-check-line', action: () => toggleStatus(user) },
  ]
  showAction.value = true
}
</script>

<template>
  <div class="page">
    <div class="page-sticky">
      <van-search v-model="keyword" placeholder="搜索邮箱、显示名" shape="round" @search="doSearch" />
    </div>

    <div class="page-scroll" @scroll="handleScroll">
      <van-loading v-if="loading" class="loading-center" size="24" />
      <p v-else-if="error" class="error-msg">{{ error }}</p>
      <van-cell-group v-else inset>
        <van-swipe-cell v-for="user in users" :key="user.id">
          <van-cell is-link @click="openAction(user)">
            <template #title>
              <div class="cell-title">
                <span>{{ user.displayName }}</span>
                <van-tag v-if="(user.roles || []).includes('ADMIN')" type="primary" size="medium" plain>管理员</van-tag>
              </div>
            </template>
            <template #label>
              {{ user.email || user.username }} · {{ formatTime(new Date(user.createdAt).getTime()) }}
            </template>
            <template #value>
              <van-tag :type="user.status === 'ACTIVE' ? 'success' : 'danger'" size="medium">
                {{ user.status === 'ACTIVE' ? '启用' : '禁用' }}
              </van-tag>
            </template>
          </van-cell>
        </van-swipe-cell>
      </van-cell-group>
      <div v-if="!loading && !error && users.length === 0" class="empty-hint">暂无匹配用户</div>
      <div v-if="!loading && !error && users.length > 0 && (loadingMore || hasMore)" class="load-more-hint">
        {{ loadingMore ? '加载中' : '继续下滑加载更多' }}
      </div>
    </div>

    <div class="page-bottom">
      <van-button type="primary" round block @click="openCreate">
        <i class="ri-add-line"></i> 新增用户
      </van-button>
    </div>

    <!-- ActionSheets -->
    <van-action-sheet v-model:show="showAction" :actions="actionOptions" cancel-text="取消"
      @select="({ action }) => { showAction = false; action?.() }" />

    <van-action-sheet v-model:show="showFormSheet" :title="formTitle" close-on-click-action>
      <div class="sheet-form">
        <van-field v-if="!isEditing" v-model="form.password" label="密码" type="password" placeholder="请输入密码" />
        <van-field v-model="form.email" label="邮箱" type="email" autocomplete="email" placeholder="请输入邮箱" />
        <van-field v-model="form.displayName" label="显示名" placeholder="请输入显示名" />
        <div class="sheet-form-btn">
          <van-button type="primary" round block @click="submitForm">确定</van-button>
        </div>
      </div>
    </van-action-sheet>

    <van-action-sheet v-model:show="showRoleSheet" title="分配角色" close-on-click-action>
      <div class="sheet-form">
        <p class="role-hint">为 <strong>{{ roleUser?.email || roleUser?.username }}</strong> 分配角色：</p>
        <van-cell-group inset>
          <van-cell v-for="role in availableRoles" :key="role.code" :title="role.name" :label="role.code" clickable @click="toggleRole(role.code)">
            <template #right-icon>
              <i :class="roleForm.includes(role.code) ? 'ri-checkbox-circle-fill' : 'ri-checkbox-blank-circle-line'"
                :style="{ color: roleForm.includes(role.code) ? '#1989fa' : '#c8c9cc', fontSize: '20px' }"></i>
            </template>
          </van-cell>
        </van-cell-group>
        <div class="sheet-form-btn">
          <van-button type="primary" round block @click="submitRole">保存</van-button>
        </div>
      </div>
    </van-action-sheet>
  </div>
</template>

<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }
.page-sticky { flex-shrink: 0; }
.page-scroll { flex: 1; overflow-y: auto; padding: 0 0 12px; }
.page-bottom { flex-shrink: 0; padding: 8px 16px 16px; }
.cell-title { display: flex; align-items: center; gap: 6px; }
.empty-hint { text-align: center; padding: 32px 0; color: #969799; font-size: 14px; }
.load-more-hint { text-align: center; padding: 14px 0 18px; color: #969799; font-size: 12px; }
.loading-center { display: flex; justify-content: center; padding: 24px 0; }
.error-msg { text-align: center; color: #ee0a24; padding: 24px 0; font-size: 14px; }
.sheet-form { padding: 0 0 16px; }
.sheet-form-btn { padding: 16px 16px 0; }
.role-hint { padding: 12px 16px 8px; font-size: 14px; color: #646566; margin: 0; }
</style>
