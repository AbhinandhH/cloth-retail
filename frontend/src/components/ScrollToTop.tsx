import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'

/**
 * React Router's client-side navigation never touches scroll position on its
 * own (unlike a real full-page navigation) — so following a link from
 * partway down a scrolled page (e.g. a product card near the bottom of the
 * Home grid) landed on the new page still scrolled down, looking like it
 * "opened from the bottom" instead of the top. Mounted once near the root
 * (see App.tsx), resetting scroll on every pathname change.
 */
export default function ScrollToTop() {
  const { pathname } = useLocation()

  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])

  return null
}
