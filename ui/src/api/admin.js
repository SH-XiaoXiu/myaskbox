import api from './client'

// ==================== 仪表盘 ====================

/** GET /api/admin/dashboard/stats */
export const getDashboardStats = () => api.get('/admin/dashboard/stats')

// ==================== 用户管理 ====================

/** GET /api/admin/users */
export const listUsers = (page = 1, pageSize = 10, keyword = '') =>
  api.get('/admin/users', { params: { page, pageSize, keyword } })

/** POST /api/admin/users */
export const createUser = (data) => api.post('/admin/users', data)

/** GET /api/admin/users/{id} */
export const getUser = (id) => api.get(`/admin/users/${id}`)

/** PUT /api/admin/users/{id} */
export const updateUser = (id, data) => api.put(`/admin/users/${id}`, data)

/** POST /api/admin/users/{id}/disable */
export const disableUser = (id) => api.post(`/admin/users/${id}/disable`)

/** POST /api/admin/users/{id}/enable */
export const enableUser = (id) => api.post(`/admin/users/${id}/enable`)

/** PUT /api/admin/users/{id}/roles */
export const assignUserRoles = (id, roleCodes) =>
  api.put(`/admin/users/${id}/roles`, { roleCodes })

// ==================== 提问箱管理 ====================

/** GET /api/admin/boxes */
export const listBoxes = (page = 1, pageSize = 10, keyword = '') =>
  api.get('/admin/boxes', { params: { page, pageSize, keyword } })

/** PUT /api/admin/boxes/{id} */
export const updateBox = (id, data) => api.put(`/admin/boxes/${id}`, data)

// ==================== 问题管理 ====================

/** GET /api/admin/questions */
export const listAllQuestions = (page = 1, pageSize = 10, params = {}) =>
  api.get('/admin/questions', { params: { page, pageSize, ...params } })

/** GET /api/admin/questions/{id} */
export const getQuestion = (id) => api.get(`/admin/questions/${id}`)

/** DELETE /api/admin/questions/{id} */
export const forceDeleteQuestion = (id) => api.delete(`/admin/questions/${id}`)

// ==================== 回答管理 ====================

/** DELETE /api/admin/answers/{id} */
export const deleteAnswer = (id) => api.delete(`/admin/answers/${id}`)

// ==================== 头像管理 ====================

/** GET /api/admin/avatars */
export const listAvatars = () => api.get('/admin/avatars')

/** POST /api/admin/avatars */
export const createAvatar = (data) => api.post('/admin/avatars', data)

/** PUT /api/admin/avatars/{id} */
export const updateAvatar = (id, data) => api.put(`/admin/avatars/${id}`, data)

/** DELETE /api/admin/avatars/{id} */
export const deleteAvatar = (id) => api.delete(`/admin/avatars/${id}`)

// ==================== 角色 & 权限 ====================

/** GET /api/admin/roles */
export const listRoles = () => api.get('/admin/roles')

/** GET /api/admin/permissions */
export const listPermissions = () => api.get('/admin/permissions')
