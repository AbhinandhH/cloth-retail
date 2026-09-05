import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import type { ReactNode } from 'react'

type Side = 'left' | 'right' | 'bottom'

interface SheetProps {
  open: boolean
  onClose: () => void
  side: Side
  children: ReactNode
  ariaLabel: string
  /** Panel width for left/right sheets (Tailwind width class). Ignored for side="bottom". */
  widthClassName?: string
}

const TRANSITION_MS = 300

const OFFSCREEN: Record<Side, string> = {
  left: '-translate-x-full',
  right: 'translate-x-full',
  bottom: 'translate-y-full',
}

const ONSCREEN = 'translate-x-0 translate-y-0'

const POSITION: Record<Side, string> = {
  left: 'inset-y-0 left-0 h-full',
  right: 'inset-y-0 right-0 h-full',
  bottom: 'inset-x-0 bottom-0 max-h-[85dvh] rounded-t-2xl',
}

/**
 * Generic slide-in overlay: backdrop + panel, body scroll-lock, Escape-to-close,
 * and — unlike the one-shot open animation on the legacy filter sheet — a real
 * closing transition (stays mounted through the exit transform, then unmounts).
 * Used for the mobile nav drawer; reusable anywhere else a sheet/drawer is needed.
 */
export default function Sheet({ open, onClose, side, children, ariaLabel, widthClassName = 'w-[86%] max-w-sm' }: SheetProps) {
  const [mounted, setMounted] = useState(open)
  const [entered, setEntered] = useState(false)

  useEffect(() => {
    if (open) {
      setMounted(true)
      const raf = requestAnimationFrame(() => setEntered(true))
      return () => cancelAnimationFrame(raf)
    }
    setEntered(false)
    const timeout = setTimeout(() => setMounted(false), TRANSITION_MS)
    return () => clearTimeout(timeout)
  }, [open])

  useEffect(() => {
    if (!mounted) return
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = previousOverflow
    }
  }, [mounted])

  useEffect(() => {
    if (!mounted) return
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [mounted, onClose])

  if (!mounted) return null

  // Portaled to document.body: a `fixed inset-0` child of any ancestor with a
  // transform/filter/backdrop-filter/contain (e.g. the sticky Navbar header's
  // `backdrop-blur-sm`) is positioned relative to that ancestor instead of the
  // viewport, per the CSS containing-block spec — a portal sidesteps that
  // regardless of where a given Sheet ends up being used from.
  return createPortal(
    <div className="fixed inset-0 z-50" role="dialog" aria-modal="true" aria-label={ariaLabel}>
      <div
        className={`absolute inset-0 bg-zinc-950/40 transition-opacity duration-300 ${entered ? 'opacity-100' : 'opacity-0'}`}
        onClick={onClose}
      />
      <div
        className={`absolute flex flex-col bg-white shadow-elevated transition-transform duration-300 ease-[cubic-bezier(0.16,1,0.3,1)] ${POSITION[side]} ${side === 'bottom' ? '' : widthClassName} ${entered ? ONSCREEN : OFFSCREEN[side]}`}
      >
        {children}
      </div>
    </div>,
    document.body,
  )
}
