import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // Lets a single tunnel (e.g. one ngrok domain pointed at this dev server) serve the
    // whole app: the page itself plus /api and /media, all same-origin from the browser's
    // perspective. Forwards server-side to the local backend - VITE_API_BASE_URL should be
    // set to a relative "/api" (see .env.example) so requests actually hit this dev server
    // to be proxied, rather than an absolute http://localhost:8080 the tunnel's visitors
    // can't reach. Same-origin also means the refresh-token cookie's default SameSite=Lax
    // just works, no cross-site cookie config needed.
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/media': { target: 'http://localhost:8080', changeOrigin: true },
    },
    // Required for ngrok (or any tunnel using a *.ngrok-free.dev / *.ngrok.io host) to reach
    // this dev server - Vite otherwise rejects requests whose Host header isn't localhost/an
    // allowed name. Add another entry here if you use a different tunnel provider/domain.
    allowedHosts: ['.ngrok-free.dev', '.ngrok-free.app', '.ngrok.app', '.ngrok.io'],
  },
})
