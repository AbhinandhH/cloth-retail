import { useId, useRef, useState } from 'react'
import type { ChangeEvent } from 'react'
import { getErrorMessage, toMediaUrl } from '../api/client'
import { uploadMedia } from '../api/configuration'

interface ImageUploadFieldProps {
  label: string
  /** Current stored value (relative or absolute URL), or null/undefined if unset. */
  value: string | null | undefined
  /**
   * Called with the uploaded file's URL once an upload succeeds, or `null` when the user removes
   * the current image. The second argument carries the raw file's content type (e.g.
   * "image/png", "video/mp4") so a caller juggling mixed media — see AdminProductForm's Images
   * tab — can tell what kind of file was just uploaded without a second round trip.
   */
  onUploaded: (url: string | null, contentType?: string) => void
  helpText?: string
  /** File picker MIME filter. Defaults to images only. */
  accept?: string
  /** Renders a <video> preview instead of <img> — pass true when `value` is a video URL. */
  isVideo?: boolean
}

/**
 * A labeled file input that uploads immediately on selection via
 * POST /admin/media/upload, previews the result, and reports the returned
 * URL up to the parent form. Used for every image field in AdminConfiguration
 * (logo, favicon, login background, login promo, registration image), and for
 * per-variant image/video uploads in AdminProductForm's Images tab.
 */
export default function ImageUploadField({ label, value, onUploaded, helpText, accept = 'image/*', isVideo = false }: ImageUploadFieldProps) {
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
      onUploaded(url, file.type)
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setUploading(false)
      if (inputRef.current) inputRef.current.value = ''
    }
  }

  const previewUrl = toMediaUrl(value)

  const handleRemove = () => {
    setError(null)
    if (inputRef.current) inputRef.current.value = ''
    onUploaded(null)
  }

  return (
    <div>
      <label htmlFor={id} className="block text-sm font-medium text-zinc-900">
        {label}
      </label>
      {helpText && <p className="text-xs text-zinc-500">{helpText}</p>}
      <div className="mt-1 flex items-center gap-3">
        {previewUrl ? (
          isVideo ? (
            <video
              src={previewUrl}
              muted
              playsInline
              className="h-16 w-16 rounded-md border border-zinc-200 bg-zinc-50 object-contain"
            />
          ) : (
            <img src={previewUrl} alt={label} className="h-16 w-16 rounded-md border border-zinc-200 bg-zinc-50 object-contain" />
          )
        ) : (
          <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-md border border-dashed border-zinc-300 text-[10px] text-zinc-400">
            {isVideo ? 'No video' : 'No image'}
          </div>
        )}
        <div className="min-w-0 flex-1">
          <input
            ref={inputRef}
            id={id}
            type="file"
            accept={accept}
            onChange={handleChange}
            disabled={uploading}
            className="block w-full text-xs text-zinc-700 file:mr-3 file:rounded-md file:border-0 file:bg-zinc-900 file:px-3 file:py-1.5 file:text-xs file:font-medium file:text-white hover:file:bg-zinc-700 disabled:opacity-60"
          />
          {uploading && <p className="mt-1 text-xs text-zinc-500">Uploading{isVideo ? ' video, this can take a moment' : ''}…</p>}
          {error && <p className="mt-1 text-xs text-rose-600">{error}</p>}
          {!uploading && previewUrl && (
            <button
              type="button"
              onClick={handleRemove}
              className="mt-1 text-xs font-medium text-rose-600 hover:text-rose-700"
            >
              Remove {isVideo ? 'video' : 'image'}
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
