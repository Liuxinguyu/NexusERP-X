export function formatDateTime(value?: unknown): string {
  if (!value) return '-'
  if (typeof value !== 'string' && typeof value !== 'number' && !(value instanceof Date)) {
    return String(value)
  }
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return String(value)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(
    d.getHours(),
  )}:${pad(d.getMinutes())}`
}
