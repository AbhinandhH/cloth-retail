import { useEffect, useRef, useState } from 'react'

interface UseInViewOptions {
  /** Fraction of the element that must be visible before it's considered "in view". */
  threshold?: number
  /** Shrinks/grows the viewport box used for intersection — a positive bottom margin reveals content slightly before it's fully on screen. */
  rootMargin?: string
  /** Once revealed, stop observing (the usual case for one-shot entrance animations). */
  once?: boolean
}

/**
 * Minimal IntersectionObserver wrapper — the entire scroll-reveal mechanism
 * for the storefront. No animation library: components pair the returned ref
 * with the `.reveal` / `.reveal.is-visible` CSS classes in index.css.
 */
export function useInView<T extends HTMLElement = HTMLDivElement>({
  threshold = 0.15,
  rootMargin = '0px 0px -10% 0px',
  once = true,
}: UseInViewOptions = {}) {
  const ref = useRef<T | null>(null)
  const [isInView, setIsInView] = useState(false)

  useEffect(() => {
    const node = ref.current
    if (!node) return

    // Environments without IntersectionObserver (or users who've already
    // scrolled past) should just see content, never permanently hidden.
    if (typeof IntersectionObserver === 'undefined') {
      setIsInView(true)
      return
    }

    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            setIsInView(true)
            if (once) observer.disconnect()
          } else if (!once) {
            setIsInView(false)
          }
        }
      },
      { threshold, rootMargin },
    )

    observer.observe(node)
    return () => observer.disconnect()
  }, [threshold, rootMargin, once])

  return [ref, isInView] as const
}
