# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build (dev profile, active by default)
./mvnw clean package -P dev

# Build for production
./mvnw clean package -P prod

# Run locally
./mvnw spring-boot:run

# Skip tests during build
./mvnw clean package -DskipTests
```

There are no automated tests in this project.

## Architecture Overview

Spring Boot 2.7 REST API backed by PostgreSQL. Context path: `/ccr-rest-api`. The app is deployed to Tomcat externally (Tomcat starter is excluded from the fat JAR).

### Module structure

The codebase is partitioned by client/brand under `py.com.jaimeferreira.ccr`:

| Package | Purpose |
|---|---|
| `commons` | Shared: auth, JWT, entities, DTOs, exceptions, utils |
| `bebidaspy` | Bebidas PY brand module |
| `nestle` | Nestlé brand module |
| `jhonson` | SC Johnson (SCJ) brand module |
| `shell` | Shell brand module |
| `insights` | Cross-brand analytics/reporting module |
| `security` | JWT filter + CORS filter |

Each brand module follows the same layered pattern: `entity` → `repository` (Spring Data JPA) → `service` → `controller` → `dto`.

### Security

Stateless JWT authentication via `JWTAuthorizationFilter`. All requests require `Authorization: Bearer <token>`. Token is validated against a hardcoded secret key (the same key is in `application-dev.properties`). Login is handled by `PublicController` / `AutenticacionService`.

### Insights module (d-insights)

The most complex module — a standalone product for generating Excel management reports. **Full functional spec in [`docs/INSIGHTS-SPEC.md`](docs/INSIGHTS-SPEC.md).**

Key concepts:
- **`ReporteInsService`**: Async Excel generation (`@Async`). Accepts CSV data + optional CSV filters, merges them into an Excel template (Apache POI), and saves the file to disk.
- **`InformeIns`** (schema `ccr`): Tracks report generation jobs with states `PROCESANDO`, `COMPLETADO`, `ERROR`.
- **`TemplateInsService`**: Manages Excel template files on disk.
- **`Categoria`**: Each client (`ClienteIns`) has categories; each category determines which Excel template is used.
- **`PlataformaConfig` / `PlataformaService`**: Admin-managed feature flags for platform status (maintenance mode, etc.).
- **`InformeCleanupScheduler`**: Scheduled job that purges old generated report files.
- **Frontend (Angular):** separate repo at `development/workspace-angular/d-insights-ccr`.

### File storage

Files (images, generated Excel reports, templates) are stored on the local filesystem under `path.directory.main_imagenes` (`/opt/images/` in dev). Sub-paths are configured per brand in `application-*.properties`.

### Database

PostgreSQL, schema `ccr`. DDL is managed manually via SQL scripts in `src/main/resources/sql/`. Hibernate is set to `ddl-auto=none`. Run scripts in numerical/logical order when setting up a new environment:
1. `creacion_basedatos.sql`
2. `creacion_tablas.sql`
3. Brand-specific tables (`creacion_tablas_scj.sql`, `creacion_tablas_nestle.sql`, etc.)
4. `05-scripts-insigths.sql`
5. `06-categorias-insights.sql`

### Exception handling

Custom exceptions in `commons.exception`: `UnknownResourceException` (404), `ForbiddenException` (403), `UnauthorizedException` (401), `InternalServerErrorException` (500). Use these instead of returning raw HTTP codes from services.

### Base entity

Most entities do NOT extend `BaseEntidad`. `BaseEntidad` provides audit fields (`fechaCreacion`, `fechaActualizacion`, `nombreUsuarioCreacion`, `nombreUsuarioActualizacion`) but many entities declare these fields directly instead.
