<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const entries = [
  { path: '/admin/boxes', icon: 'ri-mail-send-line', title: '提问箱', desc: '查看和编辑箱主公开资料' },
  { path: '/admin/questions', icon: 'ri-question-answer-line', title: '问题', desc: '查看、筛选和强制删除问题' },
  { path: '/admin/topics', icon: 'ri-price-tag-3-line', title: '话题', desc: '查看全站话题并关闭异常话题' },
  { path: '/admin/attachments', icon: 'ri-attachment-2', title: '附件', desc: '管理头像、背景和公共资源' },
  { path: '/admin/roles', icon: 'ri-shield-keyhole-line', title: '权限', desc: '查看角色和权限配置' },
]
</script>

<template>
  <div class="page">
    <van-cell-group inset>
      <van-cell
        v-for="entry in entries"
        :key="entry.path"
        is-link
        center
        :title="entry.title"
        :label="entry.desc"
        @click="router.push(entry.path)"
      >
        <template #icon>
          <i :class="[entry.icon, 'entry-icon']" aria-hidden="true"></i>
        </template>
      </van-cell>
    </van-cell-group>
  </div>
</template>

<style scoped>
.page {
  height: 100%;
  overflow-y: auto;
  padding: 12px 0;
}

.entry-icon {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  margin-right: 10px;
  border-radius: 8px;
  background: #f2f3f5;
  color: #1989fa;
  font-size: 19px;
}
</style>
