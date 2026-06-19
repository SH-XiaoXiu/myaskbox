import axios from 'axios'
import api from '@/api/client'

export const presignAttachmentUpload = (data) => api.post('/attachments/uploads/presign', data)

export async function uploadAttachmentObject(file, usageType) {
  const presign = await presignAttachmentUpload({
    usageType,
    fileName: file.name || 'image',
    mimeType: file.type,
    sizeBytes: file.size,
  })
  const form = new FormData()
  Object.entries(presign.formData || {}).forEach(([key, value]) => {
    form.append(key, value)
  })
  form.append('file', file)
  await axios.post(presign.uploadUrl, form, { timeout: 60000 })
  return presign.objectKey
}
