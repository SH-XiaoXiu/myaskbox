<script setup>
import { ref, onMounted } from 'vue'
import { listRoles, listPermissions } from '../../api/admin'
import { buildPermissionTree } from '../../utils'

const loading = ref(true)
const error = ref('')
const roles = ref([])
const permissionTree = ref([])
const activeRole = ref(0)
const expandedGroups = ref([])

onMounted(async () => {
  try {
    const [rolesData, permsData] = await Promise.all([
      listRoles(),
      listPermissions(),
    ])
    roles.value = rolesData
    permissionTree.value = buildPermissionTree(permsData)
    // 默认展开所有组
    expandedGroups.value = permissionTree.value.map(g => g.code)
  } catch (err) {
    error.value = err.message || '加载失败'
  } finally {
    loading.value = false
  }
})

function toggleGroup(code) {
  const idx = expandedGroups.value.indexOf(code)
  if (idx >= 0) {
    expandedGroups.value.splice(idx, 1)
  } else {
    expandedGroups.value.push(code)
  }
}
</script>

<template>
  <div class="role-page">
    <van-loading v-if="loading" class="loading-center" size="24" />
    <p v-else-if="error" class="error-msg">{{ error }}</p>
    <van-tabs v-else v-model:active="activeRole" sticky swipeable>
      <van-tab v-for="role in roles" :key="role.code" :title="role.name">
        <div class="role-tab-content">
          <div class="role-info">
            <div class="role-header">
              <span class="role-name">{{ role.name }}</span>
              <van-tag v-if="role.builtIn" type="warning" size="medium" plain>内置</van-tag>
            </div>
            <p class="role-desc">{{ role.description }}</p>
            <div class="role-meta">
              <van-tag size="medium">{{ role.code }}</van-tag>
              <span class="role-count">{{ role.userCount }} 个用户</span>
            </div>
          </div>

          <div class="perm-section">
            <h4 class="perm-title">拥有的权限</h4>
            <div class="perm-tree">
              <div v-for="group in permissionTree" :key="group.code" class="perm-group">
                <div class="perm-group-header" @click="toggleGroup(group.code)">
                  <i
                    :class="expandedGroups.includes(group.code) ? 'ri-arrow-down-s-line' : 'ri-arrow-right-s-line'"
                    class="perm-arrow"
                  ></i>
                  <span class="perm-group-name">{{ group.name }}</span>
                  <van-tag size="small" type="primary" plain>分组</van-tag>
                </div>
                <div v-show="expandedGroups.includes(group.code)" class="perm-children">
                  <div
                    v-for="child in group.children"
                    :key="child.code"
                    class="perm-child"
                  >
                    <span class="perm-child-name">{{ child.name }}</span>
                    <code class="perm-child-code">{{ child.code }}</code>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </van-tab>
    </van-tabs>
  </div>
</template>

<style scoped>
.role-page { height: 100%; overflow: hidden; }
.loading-center { display: flex; justify-content: center; padding: 24px 0; }
.error-msg { text-align: center; color: #ee0a24; padding: 24px 0; font-size: 14px; }
.role-tab-content { padding: 16px; }
.role-info { background: #fff; border-radius: 12px; padding: 16px; margin-bottom: 16px; }
.role-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.role-name { font-size: 17px; font-weight: 650; color: #323233; }
.role-desc { font-size: 13px; color: #646566; line-height: 1.5; margin: 0 0 12px; }
.role-meta { display: flex; align-items: center; gap: 8px; }
.role-count { font-size: 12px; color: #969799; }
.perm-section { background: #fff; border-radius: 12px; padding: 16px; }
.perm-title { font-size: 14px; font-weight: 650; color: #323233; margin: 0 0 12px; }
.perm-tree { display: flex; flex-direction: column; }
.perm-group { border-bottom: 1px solid #f0f2f5; }
.perm-group:last-child { border-bottom: none; }
.perm-group-header { display: flex; align-items: center; gap: 6px; padding: 10px 0; cursor: pointer; user-select: none; }
.perm-arrow { font-size: 16px; color: #969799; flex-shrink: 0; }
.perm-group-name { font-size: 14px; font-weight: 550; color: #323233; flex: 1; }
.perm-children { padding: 0 0 8px 22px; }
.perm-child { display: flex; align-items: center; justify-content: space-between; padding: 8px 10px; margin-bottom: 4px; background: #f7f8fa; border-radius: 6px; }
.perm-child-name { font-size: 13px; color: #646566; }
.perm-child-code { font-size: 10px; color: #969799; font-family: "SF Mono", "Cascadia Code", monospace; }
</style>
