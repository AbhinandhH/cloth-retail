# Local Setup (no Docker yet)

This project is Docker-ready (`docker-compose.yml`, `docker/backend.Dockerfile`,
`docker/frontend.Dockerfile`) but Docker isn't installed on this machine yet, so for now we run
everything locally: the backend via Maven, the frontend via npm, and MySQL as a native service.

Once Docker is installed, `docker compose up --build` will run the whole stack (backend, frontend,
MySQL) with no manual setup — see the root `README.md`.

## 1. Start MySQL

You already have MySQL Enterprise Server installed at `/usr/local/mysql`, but its service isn't
running. Easiest way to start it:

- **System Settings pane** (if present): open System Settings → scroll down to the "MySQL" pane
  (installed by the Oracle package) → click **Start MySQL Server**.
- **Or via terminal** (needs your password, run it yourself — not something Claude should run for
  you):
  ```bash
  sudo /usr/local/mysql/support-files/mysql.server start
  ```

Alternatively, if you'd rather not manage the Enterprise install, `brew install mysql` gives you a
Homebrew-managed MySQL you control without `sudo` (`brew services start mysql`) — either works,
just be consistent about which one you're pointing the app at.

## 2. Create the app database and user

```bash
mysql -u root -p
```
```sql
CREATE DATABASE clothing_retail;
CREATE USER 'clothing_retail'@'localhost' IDENTIFIED BY 'clothing_retail';
GRANT ALL PRIVILEGES ON clothing_retail.* TO 'clothing_retail'@'localhost';
FLUSH PRIVILEGES;
```

These match the backend's default env vars (`DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` all default to
`clothing_retail` for local dev) — change the password if you want, just set the matching env var
when you run the backend.

## 3. Run the backend

```bash
cd backend
mvn spring-boot:run
```

Flyway will create the schema and seed demo products automatically on first run. See
`backend/README.md` for the bootstrap admin login credentials and env vars.

## 4. Run the frontend

```bash
cd frontend
cp .env.example .env   # if present — sets VITE_API_BASE_URL
npm install
npm run dev
```

Visit the printed local URL (Vite default `http://localhost:5173`).

## 5. Run backend tests (no MySQL needed)

```bash
cd backend
mvn test
```

Tests run against an in-memory H2 database, so they work even without MySQL running.
