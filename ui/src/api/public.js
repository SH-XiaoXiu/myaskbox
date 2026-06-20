import api from '@/api/client'

/** GET /api/attachments/anonymous-avatars */
export const getAnonymousAvatars = () => api.get('/attachments/anonymous-avatars')

/** GET /api/boxes/{slug} */
export const getPublicBoxProfile = (slug) => api.get(`/boxes/${slug}`)

/** GET /api/boxes/{slug}/questions */
export const getPublishedQA = (slug, page = 1, pageSize = 10, topicCode = '') =>
  api.get(`/boxes/${slug}/questions`, { params: { page, pageSize, topicCode: topicCode || undefined } })

/** POST /api/likes/batch */
export const getLikeCountsBatch = (targets) => api.post('/likes/batch', { targets })

/** POST /api/likes/change */
export const changeLike = (targetType, targetId, delta) => api.post('/likes/change', { targetType, targetId, delta })

/** POST /api/boxes/{slug}/questions */
export const submitQuestion = (slug, attachmentId, question, topicCode = '') =>
  api.post(`/boxes/${slug}/questions`, { attachmentId, question, topicCode: topicCode || undefined })

/** GET /api/boxes/{slug}/topics */
export const getPublicTopics = (slug) => api.get(`/boxes/${slug}/topics`)

/** GET /api/boxes/{slug}/topics/{code} */
export const resolvePublicTopic = (slug, code) => api.get(`/boxes/${slug}/topics/${code}`)
