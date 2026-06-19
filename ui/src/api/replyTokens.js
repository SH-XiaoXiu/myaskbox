import api from '@/api/client'

/** GET /api/reply-tokens/{token} */
export const getReplyTokenQuestion = (token) => api.get(`/reply-tokens/${encodeURIComponent(token)}`)

/** POST /api/reply-tokens/{token}/answer */
export const answerReplyTokenQuestion = (token, answer) =>
  api.post(`/reply-tokens/${encodeURIComponent(token)}/answer`, { answer })
