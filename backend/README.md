# clothing-retail backend

Spring Boot (Maven) API for the clothing-retail e-commerce app. Base package `com.clothingretail`, Java 25.

## Spring Boot version

**Spring Boot 4.1.1.** The requirements doc named "4.1" as an aspirational target, but as of this writing (Sept 2026)
Spring Boot 4.1.1 is the actual, current GA release (confirmed against Maven Central's own metadata, not just a
changelog) - so 4.1 wasn't aspirational after all, it's what's used, pinned exactly as `4.1.1` in `pom.xml`. It
requires only Java 17+ and works cleanly on the Java 25 (Homebrew OpenJDK) installed on this machine. All other
dependency versions (Spring Security/Data/Web starters via the BOM, `flyway-core`/`flyway-mysql` 13.5.0,
`mysql-connector-j` 26.7.0, `jjwt-*` 0.13.0, `springdoc-openapi-starter-webmvc-ui` 3.1.0, `h2` 2.4.240) were
likewise checked against Maven Central for the latest release compatible with Spring Boot 4.x/Jakarta EE, not
assumed from memory.

## Local MySQL setup

MySQL 9.6 is installed at `/usr/local/mysql` but the service isn't running yet. Start it, then create the database
and app user:

```bash
# start the server (adjust if you run it a different way)
sudo /usr/local/mysql/support-files/mysql.server start

mysql -u root -p <<'SQL'
CREATE DATABASE clothing_retail;
CREATE USER 'clothing_retail'@'localhost' IDENTIFIED BY 'clothing_retail';
GRANT ALL ON clothing_retail.* TO 'clothing_retail'@'localhost';
FLUSH PRIVILEGES;
SQL
```

These match the app's local-dev defaults (`DB_HOST=localhost`, `DB_PORT=3306`, `DB_NAME=clothing_retail`,
`DB_USERNAME=clothing_retail`, `DB_PASSWORD=clothing_retail`) so no env vars are required for local dev - override
any of them if your setup differs. Flyway applies all migrations (schema + seed data: 5 categories, sub-categories,
sizes, colors, brands, materials, one vendor, and 10 seeded products with variants/images) automatically on startup.

## Running

```bash
cd backend
mvn spring-boot:run
```

The API listens on **http://localhost:8080**, with every endpoint already prefixed `/api/...` (no
`server.servlet.context-path` is set). Swagger UI is at `/swagger-ui.html` once running.

## Running tests

```bash
cd backend
mvn test
```

Tests run against the `test` Spring profile, which points at an in-memory H2 database in MySQL-compatibility mode
(`jdbc:h2:mem:testdb;MODE=MySQL`) with the same Flyway migrations applied - no local MySQL needed for `mvn test`.

## Bootstrap admin account

There is no public admin sign-up endpoint. On first startup, a `CommandLineRunner`
(`auth.AdminBootstrapRunner`) checks whether any `SUPER_ADMIN` exists and, if not, creates one from:

- `ADMIN_BOOTSTRAP_EMAIL` (default: `admin@clothingretail.local`)
- `ADMIN_BOOTSTRAP_PASSWORD` (default: `ChangeMe123!`)

**Change these via env vars for anything beyond local development** - the defaults are committed to source control
and are not secret. Once at least one `SUPER_ADMIN` exists, that account (or any other `SUPER_ADMIN`) can create
further `ADMIN` accounts via `POST /api/admin/admins`.

Similarly, `JWT_SECRET` has a local-dev fallback baked into `application.yml` (documented there with a comment) -
override it in any real deployment.

## Deferred to a later phase

- **File/image upload**: there's no upload endpoint yet. `ProductImage.url` is a plain string field, and admin
  product CRUD accepts image URLs directly - the seed data uses `https://picsum.photos/seed/<sku>-<n>/600/800`
  placeholders. Real upload/object-storage support is future work.
- **Docker**: `docker/backend.Dockerfile` (multi-stage: Maven+JDK build stage, slim JRE run stage, exposes 8080) is
  written but has not been built or run - Docker isn't installed on this machine yet.
