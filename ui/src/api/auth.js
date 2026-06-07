import api from './client'

/** POST /api/auth/login */
export const login = (username, password) => api.post('/auth/login', { username, password })

/** GET /api/auth/me */
export const getMe = () => api.get('/auth/me')

/** POST /api/auth/logout */
export const logout = () => api.post('/auth/logout')

/** PUT /api/auth/password */
export const changePassword = (currentPassword, newPassword, confirmPassword) =>
  api.put('/auth/password', { currentPassword, newPassword, confirmPassword })
