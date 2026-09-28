/**
 * Generates a random UUID (v4-shaped, good enough for a client-side dedupe/idempotency
 * key — not used for anything security-sensitive).
 *
 * `crypto.randomUUID()` is spec-restricted to secure contexts (HTTPS, or the
 * `localhost`/`127.0.0.1` exemption) — calling it elsewhere throws
 * "crypto.randomUUID is not a function", and since callers typically invoke this as a
 * `useRef`/`useState` initializer (i.e. during render, with no error boundary around it),
 * that throw takes down the whole page to a blank white screen. This matters here because
 * the app is deliberately also served over a plain-HTTP LAN IP for phone testing (see
 * frontend/.env's VITE_API_BASE_URL) — a non-secure context where `randomUUID` is missing
 * even though the rest of the Crypto API still works. `crypto.getRandomValues()` has no
 * such restriction, so it's the fallback; a `Math.random()` fallback below that covers the
 * (extremely unlikely) case neither exists.
 */
export function randomUUID(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }

  if (typeof crypto !== 'undefined' && typeof crypto.getRandomValues === 'function') {
    const bytes = crypto.getRandomValues(new Uint8Array(16))
    bytes[6] = (bytes[6] & 0x0f) | 0x40 // version 4
    bytes[8] = (bytes[8] & 0x3f) | 0x80 // variant 10
    const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('')
    return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
  }

  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}
