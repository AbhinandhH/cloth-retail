import { useId, useRef, useState } from 'react'
import type { ChangeEvent } from 'react'
import { getErrorMessage, toMediaUrl } from '../api/client'
import { uploadMedia } from '../api/configuration'

interface ImageUploadFieldProps {
  label: string
  /** Current stored value (relative or absolute URL), or null/undefined if unset. */
  value: string | null | undefined
  /** Called with the uploaded file's URL (as returned by the upload API, relative) once the upload succeeds. */
  onUploaded: (url: string) => void
  helpText?: string
}

/**
 * A labeled file input that uploads immediately on selection via
 * POST /admin/media/upload, previews the result, and reports the returned
 * URL up to the parent form. Used for every image field in AdminConfiguration
 * (logo, favicon, login background, login promo, registration image).
 */
export default function ImageUploadField({ label, value, onUploaded, helpText }: ImageUploadFieldProps) {
  const id = useId()
  const inputRef = useRef<HTMLInputElement>(null)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleChange = async (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    setUploading(true)
    setError(null)
    try {
      const { url } = await uploadMedia(file)
      onUploaded(url)
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setUploading(false)
      if (inputRef.current) inputRef.current.value = ''
    }
  }

  const previewUrl = toMediaUrl(value)

  return (
    <div>
      <label htmlFor={id} className="block text-sm font-medium text-zinc-900">
        {label}
      </label>
      {helpText && <p className="text-xs text-zinc-500">{helpText}</p>}
      <div className="mt-1 flex items-center gap-3">
        {previewUrl ? (
          <img src={previewUrl} alt={label} className="h-16 w-16 rounded-md border border-zinc-200 bg-zinc-50 object-contain" />
        ) : (
          <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-md border border-dashed border-zinc-300 text-[10px] text-zinc-400">
            No image
          </div>
        )}
        <div className="min-w-0 flex-1">
          <input
            ref={inputRef}
            id={id}
            type="file"
            accept="image/*"
            onChange={handleChange}
            disabled={uploading}
            className="block w-full text-xs text-zinc-700 file:mr-3 file:rounded-md file:border-0 file:bg-zinc-900 file:px-3 file:py-1.5 file:text-xs file:font-medium file:text-white hover:file:bg-zinc-700 disabled:opacity-60"
          />
          {uploading && <p className="mt-1 text-xs text-zinc-500">Uploading…</p>}
          {error && <p className="mt-1 text-xs text-rose-600">{error}</p>}
        </div>
      </div>
    </div>
  )
}
