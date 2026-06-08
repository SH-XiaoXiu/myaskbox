import api from './client'

/** POST /api/auth/login */
export const login = (username, password) => api.post('/auth/login', { username, password })

/** GET /api/auth/register/config */
export const getRegisterConfig = () => api.get('/auth/register/config')

/** POST /api/auth/register/code */
export const sendRegisterCode = (email) => api.post('/auth/register/code', { email })

/** POST /api/auth/register */
export const register = (data) => api.post('/auth/register', data)

/** GET /api/auth/me */
export const getMe = () => api.get('/auth/me')

/** POST /api/auth/logout */
export const logout = () => api.post('/auth/logout')

/** PUT /api/auth/password */
export const changePassword = (currentPassword, newPassword, confirmPassword) =>
  api.put('/auth/password', { currentPassword, newPassword, confirmPassword })
