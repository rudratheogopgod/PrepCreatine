/** Format milliseconds to HH:MM:SS for exam timer */
export function formatTimer(seconds: number): string {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

/** "2 hours ago", "3 days ago", etc. */
export function timeAgo(isoDate: string): string {
  const diff = Date.now() - new Date(isoDate).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return 'just now'
  if (mins < 60) return `${mins}m ago`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}h ago`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}d ago`
  return new Date(isoDate).toLocaleDateString()
}

/** "47 days to exam" */
export function daysUntil(isoDate: string): number {
  const diff = new Date(isoDate).getTime() - Date.now()
  return Math.max(0, Math.ceil(diff / 86400000))
}

/** Capitalize first letter */
export function capitalize(s: string): string {
  return s.charAt(0).toUpperCase() + s.slice(1)
}

/** Score display: "86/120 (71.6%)" */
export function formatScore(score: number, total: number): string {
  const pct = total > 0 ? ((score / total) * 100).toFixed(1) : '0.0'
  return `${score}/${total} (${pct}%)`
}
