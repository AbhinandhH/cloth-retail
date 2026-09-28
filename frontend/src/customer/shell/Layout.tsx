import { Outlet } from 'react-router-dom'
import Navbar from './Navbar'
import Footer from './Footer'

export default function Layout() {
  return (
    // No bg-white here: body (index.css) already carries a soft cream gradient as the
    // page's true default background, and pages that render their own AmbientBackground
    // (Home, AdminHome) need that fixed, negative-z layer to show at FULL strength - an
    // opaque background painted on this wrapper sits between it and the viewport in the
    // actual paint order and washes most of its color out well before it reaches the
    // screen, which is why past attempts to make that background "richer" or "darker"
    // kept landing as barely-there changes: the source CSS was being tuned against an
    // invisible diluting layer instead of what's actually visible.
    <div className="flex min-h-dvh flex-col">
      <Navbar />
      <main className="flex-1">
        <Outlet />
      </main>
      <Footer />
    </div>
  )
}
