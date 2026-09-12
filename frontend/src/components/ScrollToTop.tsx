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
// The browser's own scroll restoration (default 'auto') restores a route's PRE-navigation
// scroll position on a back/forward transition - which fights with both the reset below and
// Home/AdminHome's own "skip the hero on a return trip" scroll (see their HOME_VISITED_KEY /
// ADMIN_HOME_VISITED_KEY effects), since native restoration can win a race against a React
// effect on a popstate navigation. Set once, outside the component, since it's a one-time global
// browser setting, not something that needs re-applying per navigation.
if (typeof window !== 'undefined' && 'scrollRestoration' in window.history) {
  window.history.scrollRestoration = 'manual'
}

export default function ScrollToTop() {
  const { pathname } = useLocation()

  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])

  return null
}
