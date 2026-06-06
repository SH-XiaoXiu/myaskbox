/**
 * 相对时间格式化。
 * @param {number} ts - 毫秒时间戳
 * @returns {string} 相对时间描述
 */
export function formatTime(ts) {
  const now = Date.now()
  const diff = now - ts
  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)
  const weeks = Math.floor(days / 7)
  const months = Math.floor(days / 30)

  if (seconds < 60) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  if (weeks < 4) return `${weeks}周前`
  return `${months}个月前`
}

/**
 * 将扁平权限列表构建为树（按 parentId 嵌套）。
 * @param {Array} flatList - 扁平权限节点，每项含 { id, code, name, type, parentId, sortOrder }
 * @returns {Array} 树形结构（type=GROUP 的节点包含 children 数组）
 */
export function buildPermissionTree(flatList) {
  if (!flatList || !flatList.length) return []
  const map = new Map()
  const roots = []

  // 创建节点映射
  for (const item of flatList) {
    map.set(item.id, { ...item, children: [] })
  }

  // 构建树
  for (const item of flatList) {
    const node = map.get(item.id)
    if (item.parentId && map.has(item.parentId)) {
      map.get(item.parentId).children.push(node)
    } else {
      roots.push(node)
    }
  }

  return roots
}
