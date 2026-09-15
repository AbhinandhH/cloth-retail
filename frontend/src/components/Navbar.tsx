import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useSiteConfig } from '../context/SiteConfigContext'
import { useCart } from '../context/CartContext'
import BrandMark from './BrandMark'
import Sheet from './Sheet'
import { HeartIcon } from './WishlistButton'

function CartIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={1.75}
        d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"
      />
    </svg>
  )
}

function CartBadge({ count }: { count: number }) {
  if (count <= 0) return null
  return (
    <span className="absolute -right-1.5 -top-1.5 flex h-[18px] min-w-[18px] items-center justify-center rounded-full bg-rose-600 px-1 text-[10px] font-semibold leading-none text-white">
      {count > 99 ? '99+' : count}
    </span>
  )
}

// Theme-derived tokens (same color-mix pattern as Footer/AdminHome) so the
// header's own chrome - background, borders, link text - tracks the active
// theme instead of a fixed white/zinc palette. This matters beyond looks: a
// dark theme (e.g. "Black & Rose Gold") sets --brand-text to a near-white
// color, which BrandMark's shimmer-base picks up - against a fixed white
// header that text goes nearly invisible. Deriving the header's own
// background/text from the same theme keeps them a guaranteed-contrasting
// pair regardless of which preset is active.
//
// Also applied to the mobile nav Sheet's panel (via panelClassName="nav-surface"
// below) so the drawer's own chrome matches the header exactly - the panel is
// portaled to document.body (see Sheet.tsx), a DOM sibling of <header>, not a
// descendant, so these custom properties wouldn't otherwise reach it.
const NAV_STYLES = `
  .nav-surface {
    --nav-text: color-mix(in srgb, var(--brand-text, #3f3f46) 78%, var(--brand-background, #ffffff));
    --nav-text-strong: var(--brand-text, #09090b);
    --nav-border: color-mix(in srgb, var(--brand-text, #e4e4e7) 14%, var(--brand-background, #ffffff));
    --nav-hover-bg: color-mix(in srgb, var(--brand-text, #fafafa) 6%, var(--brand-background, #ffffff));
  }
`

export default function Navbar() {
  const [drawerOpen, setDrawerOpen] = useState(false)
  const { isAuthenticated, user, logout } = useAuth()
  const { config } = useSiteConfig()
  const { cart } = useCart()
  const navigate = useNavigate()
  const itemCount = cart?.itemCount ?? 0

  const closeDrawer = () => setDrawerOpen(false)

  const handleLogout = async () => {
    closeDrawer()
    await logout()
    navigate('/')
  }

  return (
    <header className="nav-surface sticky top-0 z-40 border-b border-[var(--nav-border)] bg-[var(--brand-background,#ffffff)]/95 backdrop-blur-sm">
      <style>{NAV_STYLES}</style>
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        <Link to="/" className="flex items-center" onClick={closeDrawer}>
          <BrandMark businessName={config?.businessName} logoUrl={config?.logoUrl} />
        </Link>

        {/* Desktop nav */}
        <nav className="hidden items-center gap-8 md:flex">
          <Link to="/" className="text-sm font-medium tracking-wide text-[var(--nav-text)] transition-colors hover:text-[var(--nav-text-strong)]">
            Shop
          </Link>
          {isAuthenticated ? (
            <div className="flex items-center gap-5">
              <Link to="/wishlist" className="text-[var(--nav-text)] transition-colors hover:text-[var(--nav-text-strong)]" aria-label="View wishlist">
                <HeartIcon filled={false} className="h-6 w-6" />
              </Link>
              <Link to="/cart" className="relative text-[var(--nav-text)] transition-colors hover:text-[var(--nav-text-strong)]" aria-label="View cart">
                <CartIcon />
                <CartBadge count={itemCount} />
              </Link>
              <Link to="/orders" className="text-sm font-medium text-[var(--nav-text)] transition-colors hover:text-[var(--nav-text-strong)]">
                Orders
              </Link>
              <Link to="/profile" className="text-sm font-medium text-[var(--nav-text)] transition-colors hover:text-[var(--nav-text-strong)]">
                Hi, {user?.fullName?.split(' ')[0] ?? 'there'}
              </Link>
              <button
                onClick={handleLogout}
                className="rounded-full border border-[var(--nav-border)] px-4 py-1.5 text-sm font-medium text-[var(--nav-text)] transition-colors hover:bg-[var(--nav-hover-bg)]"
              >
                Log out
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-3">
              <Link to="/login" className="text-sm font-medium text-[var(--nav-text)] transition-colors hover:text-[var(--nav-text-strong)]">
                Log in
              </Link>
              <Link
                to="/register"
                className="btn-primary-radius bg-[var(--brand-primary,#18181b)] px-4 py-1.5 text-sm font-medium text-white ring-2 ring-offset-1 ring-[var(--brand-secondary,#18181b)] transition-opacity hover:opacity-90"
              >
                Sign up
              </Link>
            </div>
          )}
        </nav>

        {/* Mobile: cart + hamburger */}
        <div className="flex items-center gap-1 md:hidden">
          {isAuthenticated && (
            <Link to="/cart" className="relative flex h-10 w-10 items-center justify-center text-[var(--nav-text)]" aria-label="View cart">
              <CartIcon />
              <CartBadge count={itemCount} />
            </Link>
          )}
          <button
            type="button"
            className="inline-flex h-10 w-10 items-center justify-center rounded-full text-[var(--nav-text)] transition-colors hover:bg-[var(--nav-hover-bg)]"
            aria-label="Open menu"
            aria-expanded={drawerOpen}
            onClick={() => setDrawerOpen(true)}
          >
            <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>
        </div>
      </div>

      {/* Mobile nav drawer — slides in from the right, not a top dropdown. */}
      <Sheet open={drawerOpen} onClose={closeDrawer} side="right" ariaLabel="Site menu" panelClassName="nav-surface">
        <div className="flex items-center justify-between border-b border-[var(--nav-border)] px-5 py-4">
          <span className="text-sm font-semibold uppercase tracking-wide text-[var(--nav-text)]">Menu</span>
          <button
            type="button"
            onClick={closeDrawer}
            aria-label="Close menu"
            className="flex h-9 w-9 items-center justify-center rounded-full text-[var(--nav-text)] transition-colors hover:bg-[var(--nav-hover-bg)]"
          >
            <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <nav className="flex flex-1 flex-col gap-1 overflow-y-auto px-3 py-4">
          <Link
            to="/"
            onClick={closeDrawer}
            className="animate-fade-in-up rounded-lg px-3 py-3 text-base font-medium text-[var(--nav-text-strong)] transition duration-200 hover:translate-x-0.5 hover:bg-[var(--nav-hover-bg)] active:scale-[0.98]"
            style={{ animationDelay: '20ms' }}
          >
            Shop
          </Link>

          {isAuthenticated ? (
            <>
              <Link
                to="/cart"
                onClick={closeDrawer}
                className="animate-fade-in-up flex items-center justify-between rounded-lg px-3 py-3 text-base font-medium text-[var(--nav-text-strong)] transition duration-200 hover:translate-x-0.5 hover:bg-[var(--nav-hover-bg)] active:scale-[0.98]"
                style={{ animationDelay: '60ms' }}
              >
                Cart
                {itemCount > 0 && (
                  <span className="flex h-5 min-w-[1.25rem] items-center justify-center rounded-full bg-rose-600 px-1.5 text-xs font-semibold leading-none text-white">
                    {itemCount > 99 ? '99+' : itemCount}
                  </span>
                )}
              </Link>
              <Link
                to="/wishlist"
                onClick={closeDrawer}
                className="animate-fade-in-up rounded-lg px-3 py-3 text-base font-medium text-[var(--nav-text-strong)] transition duration-200 hover:translate-x-0.5 hover:bg-[var(--nav-hover-bg)] active:scale-[0.98]"
                style={{ animationDelay: '100ms' }}
              >
                Wishlist
              </Link>
              <Link
                to="/orders"
                onClick={closeDrawer}
                className="animate-fade-in-up rounded-lg px-3 py-3 text-base font-medium text-[var(--nav-text-strong)] transition duration-200 hover:translate-x-0.5 hover:bg-[var(--nav-hover-bg)] active:scale-[0.98]"
                style={{ animationDelay: '140ms' }}
              >
                My orders
              </Link>
              <Link
                to="/profile"
                onClick={closeDrawer}
                className="animate-fade-in-up rounded-lg px-3 py-3 text-base font-medium text-[var(--nav-text-strong)] transition duration-200 hover:translate-x-0.5 hover:bg-[var(--nav-hover-bg)] active:scale-[0.98]"
                style={{ animationDelay: '180ms' }}
              >
                Account
              </Link>

              <div
                className="animate-fade-in-up mt-3 border-t border-[var(--nav-border)] px-3 pt-4"
                style={{ animationDelay: '220ms' }}
              >
                <p className="text-xs text-[var(--nav-text)]">Signed in as</p>
                <p className="mt-0.5 truncate text-sm font-medium text-[var(--nav-text-strong)]">{user?.fullName ?? user?.email}</p>
              </div>

              <button
                onClick={handleLogout}
                className="animate-fade-in-up mt-2 rounded-lg px-3 py-3 text-left text-base font-medium text-[var(--nav-text-strong)] transition duration-200 hover:translate-x-0.5 hover:bg-[var(--nav-hover-bg)] active:scale-[0.98]"
                style={{ animationDelay: '260ms' }}
              >
                Log out
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                onClick={closeDrawer}
                className="animate-fade-in-up rounded-lg px-3 py-3 text-base font-medium text-[var(--nav-text-strong)] transition duration-200 hover:translate-x-0.5 hover:bg-[var(--nav-hover-bg)] active:scale-[0.98]"
                style={{ animationDelay: '60ms' }}
              >
                Log in
              </Link>
              <Link
                to="/register"
                onClick={closeDrawer}
                className="animate-fade-in-up mt-1 btn-primary-radius bg-[var(--brand-primary,#18181b)] px-3 py-3 text-center text-base font-semibold text-white transition duration-200 hover:opacity-90 active:scale-[0.98]"
                style={{ animationDelay: '100ms' }}
              >
                Sign up
              </Link>
            </>
          )}
        </nav>
      </Sheet>
    </header>
  )
}
