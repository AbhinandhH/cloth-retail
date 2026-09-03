# syntax=docker/dockerfile:1
#
# Build from the repo root (not this docker/ dir), e.g.:
#   docker build -f docker/frontend.Dockerfile -t clothing-retail-frontend .

# ---- Build stage -----------------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /app

# Install dependencies first for better layer caching.
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

# Copy the rest of the frontend source and build the production bundle.
COPY frontend/ ./
# VITE_API_BASE_URL is baked into the bundle at build time (Vite env vars are
# compile-time). Override at build with:
#   docker build --build-arg VITE_API_BASE_URL=https://api.example.com/api ...
ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

# ---- Run stage --------------------------------------------------------------
FROM nginx:1.27-alpine AS run

# SPA-friendly nginx config, written inline so this Dockerfile stays the only
# file this build needs: serve static assets, fall back to index.html for
# client-side routes handled by React Router.
RUN printf '%s\n' \
  'server {' \
  '    listen 80;' \
  '    server_name _;' \
  '    root /usr/share/nginx/html;' \
  '    index index.html;' \
  '' \
  '    location / {' \
  '        try_files $uri $uri/ /index.html;' \
  '    }' \
  '' \
  '    location ~* \.(js|css|svg|png|jpg|jpeg|gif|webp|woff2?)$ {' \
  '        expires 7d;' \
  '        add_header Cache-Control "public, max-age=604800, immutable";' \
  '    }' \
  '}' \
  > /etc/nginx/conf.d/default.conf

COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
