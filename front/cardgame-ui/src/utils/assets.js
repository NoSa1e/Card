export function assetUrl(path) {
  const base = (import.meta.env && import.meta.env.BASE_URL) || '/'
  const normalizedBase = base.endsWith('/') ? base.slice(0, -1) : base
  const normalizedPath = path.startsWith('/') ? path.slice(1) : path
  return `${normalizedBase}/${normalizedPath}`
}
