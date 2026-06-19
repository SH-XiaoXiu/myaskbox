import api from '@/api/client'

/** GET /api/box/profile */
export const getBoxProfile = () => api.get('/box/profile')

/** PUT /api/box/profile */
export const updateBoxProfile = (data) => api.put('/box/profile', data)

/** GET /api/box/stats */
export const getBoxStats = (zone = 'Asia/Shanghai') => api.get('/box/stats', { params: { zone } })

/** GET /api/box/questions/pending */
export const getPendingQuestions = (page = 1, pageSize = 20) =>
  api.get('/box/questions/pending', { params: { page, pageSize } })

/** GET /api/box/questions/history?status=PUBLISHED */
export const getHistoryQuestions = (status, page = 1, pageSize = 20) =>
  api.get('/box/questions/history', { params: { status, page, pageSize } })

/** POST /api/box/questions/{id}/answer */
export const answerQuestion = (id, answer) => api.post(`/box/questions/${id}/answer`, { answer })

/** POST /api/box/questions/{id}/dismiss */
export const dismissQuestion = (id) => api.post(`/box/questions/${id}/dismiss`)

/** DELETE /api/box/questions/{id} */
export const deleteQuestion = (id) => api.delete(`/box/questions/${id}`)
