# Clothing Retail

A mobile-first e-commerce platform for a small home-based clothing retail business, built for
Instagram-ad-driven customer acquisition, with room to grow into a larger multi-admin operation.

## Stack

- **Backend:** Java 25, Spring Boot, Spring Security, JWT auth (access + refresh), Spring Data
  JPA/Hibernate, MySQL, Flyway migrations, Maven. See [`backend/README.md`](backend/README.md)
  for the exact Spring Boot version and how to run it.
- **Frontend:** React, Vite, Tailwind CSS. See [`frontend`](frontend/) for details.
- **Infra:** Docker Compose (`docker-compose.yml`) for running backend + frontend + MySQL
  together once Docker is installed. Not required for local dev right now — see
  [`docs/SETUP.md`](docs/SETUP.md) for running everything natively.

## Project structure

```
clothing-retail/
├── backend/            Spring Boot API
├── frontend/            React + Vite + Tailwind storefront
├── docker/               Dockerfiles for backend & frontend
├── docs/                  Setup and other docs
├── docker-compose.yml      Full local stack (backend + frontend + MySQL)
└── README.md
```

## Quick start (no Docker yet)

See [`docs/SETUP.md`](docs/SETUP.md) for full step-by-step instructions (starting local MySQL,
creating the app database, running the backend and frontend).

```bash
cd backend && mvn spring-boot:run    # http://localhost:8080
cd frontend && npm install && npm run dev  # http://localhost:5173
```

## Current status

This repo currently covers the foundation of the platform:

- Customer registration/login and a separate admin login, JWT access tokens + httpOnly-cookie
  refresh tokens, role-based access control (`SUPER_ADMIN` / `ADMIN` / `CUSTOMER`)
- Product catalog with variants (size/color), categories/sub-categories, sizes, colors, brands,
  materials, vendors as admin-manageable master data
- Public storefront: product browsing, search, filtering, pagination, product detail pages
- Purchase / stock-entry flow that updates inventory with an auditable transaction log
- Seeded demo product data so the storefront has real content out of the box

Not yet built (planned in later phases, following the original requirements doc's own phased
roadmap): shopping cart, checkout, payments, returns, likes/sharing/reviews, admin dashboard &
finance module, theme/branding/homepage configuration, employee management, file/image upload to
object storage, and the admin CRUD UI (the backend admin APIs exist and are usable via
curl/Postman/the OpenAPI docs at `/swagger-ui.html`, but there's no admin frontend yet).

## Security notes

The backend is the source of truth for price, stock, and payment status — the frontend is never
trusted for any of these. Every protected endpoint independently validates authentication and
authorization server-side. See `backend/README.md` for details on JWT/refresh-token handling.
