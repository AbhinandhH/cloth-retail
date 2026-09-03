import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useSiteConfig } from '../context/SiteConfigContext'
import { toMediaUrl } from '../api/client'

function BrandMark({ businessName, logoUrl }: { businessName: string | null | undefined; logoUrl: string | null | undefined }) {
  const resolvedLogo = toMediaUrl(logoUrl)
  if (resolvedLogo) {
    return <img src={resolvedLogo} alt={businessName ?? 'Logo'} className="h-9 w-auto object-contain" />
  }
  if (businessName) {
    return <span className="text-xl font-bold tracking-tight text-zinc-900">{businessName}</span>
  }
  return (
    <span className="text-xl font-bold tracking-tight text-zinc-900">
      THREAD<span className="text-rose-600">CO</span>
    </span>
  )
}

export default function Navbar() {
  const [menuOpen, setMenuOpen] = useState(false)
  const { isAuthenticated, user, logout } = useAuth()
  const { config } = useSiteConfig()
  const navigate = useNavigate()

  const closeMenu = () => setMenuOpen(false)

  const handleLogout = async () => {
    closeMenu()
    await logout()
    navigate('/')
  }

  return (
    <header className="sticky top-0 z-40 border-b border-zinc-200 bg-white/95 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        <Link to="/" className="flex items-center" onClick={closeMenu}>
          <BrandMark businessName={config?.businessName} logoUrl={config?.logoUrl} />
        </Link>

        {/* Desktop nav */}
        <nav className="hidden items-center gap-8 md:flex">
          <Link to="/" className="text-sm font-medium text-zinc-700 hover:text-zinc-900">
            Shop
          </Link>
          {isAuthenticated ? (
            <div className="flex items-center gap-4">
              <span className="text-sm text-zinc-600">Hi, {user?.fullName?.split(' ')[0] ?? 'there'}</span>
              <button
                onClick={handleLogout}
                className="rounded-full border border-zinc-300 px-4 py-1.5 text-sm font-medium text-zinc-700 hover:bg-zinc-50"
              >
                Log out
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-3">
              <Link
                to="/login"
                className="text-sm font-medium text-zinc-700 hover:text-zinc-900"
              >
                Log in
              </Link>
              <Link
                to="/register"
                className="rounded-full bg-zinc-900 px-4 py-1.5 text-sm font-medium text-white hover:bg-zinc-700"
              >
                Sign up
              </Link>
            </div>
          )}
        </nav>

        {/* Mobile hamburger */}
        <button
          type="button"
          className="inline-flex items-center justify-center rounded-md p-2 text-zinc-700 md:hidden"
          aria-label="Toggle menu"
          aria-expanded={menuOpen}
          onClick={() => setMenuOpen((v) => !v)}
        >
          {menuOpen ? (
            <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          ) : (
            <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          )}
        </button>
      </div>

      {/* Mobile menu */}
      {menuOpen && (
        <div className="border-t border-zinc-200 bg-white md:hidden">
          <nav className="flex flex-col gap-1 px-4 py-3">
            <Link
              to="/"
              onClick={closeMenu}
              className="rounded-md px-2 py-2 text-base font-medium text-zinc-700 hover:bg-zinc-50"
            >
              Shop
            </Link>
            {isAuthenticated ? (
              <>
                <span className="px-2 py-1 text-sm text-zinc-500">
                  Signed in as {user?.fullName ?? user?.email}
                </span>
                <button
                  onClick={handleLogout}
                  className="rounded-md px-2 py-2 text-left text-base font-medium text-zinc-700 hover:bg-zinc-50"
                >
                  Log out
                </button>
              </>
            ) : (
              <>
                <Link
                  to="/login"
                  onClick={closeMenu}
                  className="rounded-md px-2 py-2 text-base font-medium text-zinc-700 hover:bg-zinc-50"
                >
                  Log in
                </Link>
                <Link
                  to="/register"
                  onClick={closeMenu}
                  className="rounded-md px-2 py-2 text-base font-medium text-zinc-700 hover:bg-zinc-50"
                >
                  Sign up
                </Link>
              </>
            )}
          </nav>
        </div>
      )}
    </header>
  )
}
