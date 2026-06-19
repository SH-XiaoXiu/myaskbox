export function assetSrc(attachment) {
  if (!attachment) return ''
  if (typeof attachment === 'string') {
    if (attachment.startsWith('blob:') || attachment.startsWith('data:') || attachment.startsWith('http')) return attachment
    return `/api/assets/${encodeAssetKey(attachment)}`
  }
  return attachment.objectKey ? `/api/assets/${encodeAssetKey(attachment.objectKey)}` : ''
}

export function encodeAssetKey(objectKey) {
  return String(objectKey)
    .split('/')
    .map((segment) => encodeURIComponent(segment))
    .join('/')
}
