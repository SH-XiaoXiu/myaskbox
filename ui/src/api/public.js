import api from './client'

/** GET /api/attachments/anonymous-avatars */
export const getAnonymousAvatars = () => api.get('/attachments/anonymous-avatars')

/** GET /api/boxes/{slug} */
export const getPublicBoxProfile = (slug) => api.get(`/boxes/${slug}`)

/** GET /api/boxes/{slug}/questions */
export const getPublishedQA = (slug, page = 1, pageSize = 10) =>
  api.get(`/boxes/${slug}/questions`, { params: { page, pageSize } })

/** POST /api/boxes/{slug}/questions */
export const submitQuestion = (slug, attachmentId, question) =>
  api.post(`/boxes/${slug}/questions`, { attachmentId, question })
