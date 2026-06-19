import api from '@/api/client'

/** POST /api/auth/login */
export const login = (email, password) => api.post('/auth/login', { email, password })

/** POST /api/auth/login/code */
export const sendLoginCode = (email) => api.post('/auth/login/code', { email })

/** POST /api/auth/login/code/verify */
export const loginWithCode = (email, code) => api.post('/auth/login/code/verify', { email, code })

/** GET /api/auth/register/config */
export const getRegisterConfig = () => api.get('/auth/register/config')

/** POST /api/auth/register/code */
export const sendRegisterCode = (email) => api.post('/auth/register/code', { email })

/** POST /api/auth/register */
export const register = (data) => api.post('/auth/register', data)

/** GET /api/auth/me */
export const getMe = () => api.get('/auth/me')

/** PUT /api/auth/profile */
export const updateProfile = (data) => api.put('/auth/profile', data)

/** POST /api/auth/email/code */
export const sendEmailChangeCode = (email) => api.post('/auth/email/code', { email })

/** PUT /api/auth/email */
export const changeEmail = (email, code) => api.put('/auth/email', { email, code })

/** POST /api/auth/logout */
export const logout = () => api.post('/auth/logout')

/** PUT /api/auth/password */
export const changePassword = (currentPassword, newPassword, confirmPassword) =>
  api.put('/auth/password', { currentPassword, newPassword, confirmPassword })
