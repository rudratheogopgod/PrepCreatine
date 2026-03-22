import DOMPurify from 'dompurify'

export function sanitizeHtml(dirty: string): string {
  return DOMPurify.sanitize(dirty, { USE_PROFILES: { html: true } })
}

export function sanitizeText(dirty: string): string {
  return DOMPurify.sanitize(dirty, { ALLOWED_TAGS: [], ALLOWED_ATTR: [] })
}

/** Strip null bytes and control chars (except newline in textareas) */
export function stripControlChars(value: string, allowNewlines = false): string {
  if (allowNewlines) return value.replace(/[\x00-\x09\x0B-\x1F\x7F]/g, '')
  return value.replace(/[\x00-\x1F\x7F]/g, '')
}

/** Strip emoji from fields where it's inappropriate (names, codes) */
export function stripEmoji(value: string): string {
  return value.replace(/\p{Emoji}/gu, '').trim()
}
