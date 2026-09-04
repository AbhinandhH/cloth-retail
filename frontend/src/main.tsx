import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './App.tsx'
import { AuthProvider } from './context/AuthContext'
import { SiteConfigProvider } from './context/SiteConfigContext'
import { MasterDataProvider } from './context/MasterDataContext'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      {/* SiteConfigProvider and MasterDataProvider are siblings to AuthProvider —
          both load lazily/publicly and neither depends on auth state. Some
          MasterDataContext lists (brands/materials/vendors) do hit admin-only
          endpoints, but nothing fetches until a consumer mounts, by which
          point an admin session/token is already in place. */}
      <SiteConfigProvider>
        <MasterDataProvider>
          <AuthProvider>
            <App />
          </AuthProvider>
        </MasterDataProvider>
      </SiteConfigProvider>
    </BrowserRouter>
  </StrictMode>,
)
