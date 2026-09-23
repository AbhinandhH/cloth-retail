import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './App.tsx'
import { AuthProvider } from './context/AuthContext'
import { SiteConfigProvider } from './context/SiteConfigContext'
import { MasterDataProvider } from './context/MasterDataContext'
import { CartProvider } from './context/CartContext'
import { WishlistProvider } from './context/WishlistContext'
import { LockoutProvider } from './context/LockoutContext'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      {/* SiteConfigProvider, MasterDataProvider, and LockoutProvider are siblings to
          AuthProvider — all load/react independently of auth state. Some
          MasterDataContext lists (brands/materials/vendors) do hit admin-only
          endpoints, but nothing fetches until a consumer mounts, by which
          point an admin session/token is already in place. LockoutProvider in
          particular must catch a 402 for an anonymous storefront visitor just
          as much as a logged-in account, so it can't be nested inside auth.
          CartProvider (and WishlistProvider, same reasoning) is nested INSIDE
          AuthProvider (unlike those) since it reads
          isAuthenticated/isAuthChecking to decide whether to load — it
          no-ops for guests and clears on logout. */}
      <LockoutProvider>
        <SiteConfigProvider>
          <MasterDataProvider>
            <AuthProvider>
              <CartProvider>
                <WishlistProvider>
                  <App />
                </WishlistProvider>
              </CartProvider>
            </AuthProvider>
          </MasterDataProvider>
        </SiteConfigProvider>
      </LockoutProvider>
    </BrowserRouter>
  </StrictMode>,
)
