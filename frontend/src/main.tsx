import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './App.tsx'
import { AuthProvider } from './context/AuthContext'
import { SiteConfigProvider } from './context/SiteConfigContext'
import { MasterDataProvider } from './context/MasterDataContext'
import { CartProvider } from './context/CartContext'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      {/* SiteConfigProvider and MasterDataProvider are siblings to AuthProvider —
          both load lazily/publicly and neither depends on auth state. Some
          MasterDataContext lists (brands/materials/vendors) do hit admin-only
          endpoints, but nothing fetches until a consumer mounts, by which
          point an admin session/token is already in place.
          CartProvider is nested INSIDE AuthProvider (unlike those two) since
          it reads isAuthenticated/isAuthChecking to decide whether to load —
          it no-ops for guests and clears on logout. */}
      <SiteConfigProvider>
        <MasterDataProvider>
          <AuthProvider>
            <CartProvider>
              <App />
            </CartProvider>
          </AuthProvider>
        </MasterDataProvider>
      </SiteConfigProvider>
    </BrowserRouter>
  </StrictMode>,
)
