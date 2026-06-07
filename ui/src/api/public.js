import api from './client'

/** GET /api/avatars */
export const getAvatars = () => api.get('/avatars')

/** GET /api/boxes/{slug} */
export const getPublicBoxProfile = (slug) => api.get(`/boxes/${slug}`)

/** GET /api/boxes/{slug}/questions */
export const getPublishedQA = (slug, page = 1, pageSize = 10) =>
  api.get(`/boxes/${slug}/questions`, { params: { page, pageSize } })

/** POST /api/boxes/{slug}/questions */
export const submitQuestion = (slug, avatarId, question) =>
  api.post(`/boxes/${slug}/questions`, { avatarId, question })
