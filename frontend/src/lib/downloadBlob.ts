/** Triggers a browser "Save As" for an already-fetched Blob (e.g. a PDF response) - a synthetic, invisible <a download> click, immediately revoked. */
export function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}
