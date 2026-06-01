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

There are no automated tests in this project. Manual API smoke scripts live in `scripts/` (`test-generar-reporte.sh`, `test-jhonson-app.sh`).

## Architecture Overview

Spring Boot 2.7 REST API backed by PostgreSQL. Context path: `/ccr-rest-api`. Target runtime is Java 8 — keep new code 1.8-compatible (no `var`, no `Stream.toList()`, no records). The app is deployed to Tomcat externally (Tomcat starter is excluded from the fat JAR).

### Module structure

The codebase is partitioned by client/brand under `py.com.jaimeferreira.ccr`:

| Package | Purpose |
|---|---|
| `commons` | Shared: auth, JWT, entities, DTOs, exceptions, utils, async config, cotización service |
| `bebidaspy` | Bebidas PY brand module (deprioritized — kept but not actively developed) |
| `nestle` | Nestlé brand module |
| `jhonson` | SC Johnson (SCJ) brand module |
| `shell` | Shell brand module |
| `insights` | Cross-brand analytics/reporting module (d-insights) — has its own `admin` subpackage |
| `lt` | Integración con proveedor POS externo "LT" — ingesta de sucursales/productos/tickets/personas |
| `security` | JWT filter, CORS filter, LT API-key filter |

Each brand module follows the same layered pattern: `entity` → `repository` (Spring Data JPA) → `service` → `controller` → `dto`.

### Security

Two parallel authentication mechanisms, switched by URL path:

- **JWT (default)**: Stateless via `JWTAuthorizationFilter`. All requests require `Authorization: Bearer <token>`. Token is validated against a hardcoded secret key (same key in `application-dev.properties`). Login is handled by `PublicController` / `AutenticacionService`.
- **Static API key for `/lt/**`**: Handled by `LtApiKeyFilter`. The JWT filter explicitly skips paths starting with `/lt/`. The expected key is configured via `lt.api.key` in `application-*.properties` and supplied by the external LT system as `Authorization: Bearer <api-key>`.

### Insights module (d-insights)

The most complex module — a standalone product for generating Excel management reports. **Full functional spec in [`docs/INSIGHTS-SPEC.md`](docs/INSIGHTS-SPEC.md).** Additional specs: [`docs/ano-fiscal-spec.md`](docs/ano-fiscal-spec.md), [`docs/slicers-independientes.md`](docs/slicers-independientes.md).

Key concepts:
- **`ReporteInsService`**: Async Excel generation (`@Async`). Accepts CSV data + optional CSV filters, merges them into an Excel template (Apache POI), and saves the file to disk.
- **`TipoReporte` enum** (`NORMAL`, `CADENA`): Each variant defines a CSV column-index map and a template base name. CSV headers are validated (with accent/case-insensitive alias matching) before processing. Column counts differ (NORMAL=13 base, CADENA=14 base, +1 optional SUB_MARCA).
- **`InformeIns`** (schema `ccr`): Tracks report generation jobs with states `PROCESANDO`, `COMPLETADO`, `ERROR`.
- **`TemplateInsService`**: Manages Excel template files on disk. Templates are resolved per cliente+categoría, falling back to per-cliente, then to the default.
- **`Categoria`**: Each `ClienteIns` has categories; each category determines which Excel template is used. Categoría is mandatory at report generation time.
- **`insights.admin` subpackage**: Admin-only surface, mounted under separate controllers (`AdminPlataformaController`, `PublicPlataformaController`). Provides:
  - `PlataformaConfig` / `PlataformaService` — feature flags for platform status (e.g. maintenance mode), enforced by `PlataformaStatusFilter`.
  - `LogStreamService` — server-sent log tailing for the admin UI.
- **`InformeCleanupScheduler`**: Scheduled job that purges old generated report files.
- **Excel refresh on Windows**: `scripts/refresh-excel.vbs` is a VBScript (COM Automation) helper invoked on the deployment host to run `Workbook.RefreshAll` for power-pivot/DAX rebuild — must run from Windows with Excel installed; PowerShell variants hit COM threading issues from a service.
- **Frontend (Angular):** separate repo at `development/workspace-angular/d-insights-ccr`.

### Admin image rotation (cross-brand)

`AdminImagenesController` (under `commons` or brand-specific admin packages) exposes endpoints to list and rotate brand-uploaded images (`/binario` to serve, `/rotar` to rotate). The flow uses `ImagenPathValidator` to block path traversal and `NOFOLLOW_LINKS` to block symlinks. EXIF orientation is normalized when rotating. Brand-specific services: `ImagenesNestService`, `ImagenesSCJService`, `ImagenesShellService`.

### Cotización (currency rates)

`commons.service.CotizacionService` + `CotizacionScheduler` fetch USD/PYG rates on a schedule and expose them via a public endpoint. The rate is also surfaced as a "Cotización USD" column in the Calendario sheet of generated Insights reports.

### LT integration

`lt/api/v1/{sucursales,productos,tickets,personas}` — POST endpoints that accept JSON arrays from the LT POS provider and persist them. All endpoints require the static API key (see Security above). Tables live under schema `ccr` (see `10-lt-integration-schema.sql`). API contract for the external provider is documented in [`docs/LT-INTEGRACION-API.md`](docs/LT-INTEGRACION-API.md). Audit logs are emitted to a dedicated `lt.audit` logger.

### File storage

Files (images, generated Excel reports, templates) are stored on the local filesystem under `path.directory.main_imagenes` (`/opt/images/` in dev). Sub-paths are configured per brand in `application-*.properties`.

### Database

PostgreSQL, schema `ccr`. DDL is managed manually via SQL scripts in `src/main/resources/sql/`. Hibernate is set to `ddl-auto=none`. For a new environment, run scripts in this order:
1. `creacion_basedatos.sql`
2. `creacion_tablas.sql`
3. Brand-specific tables (`creacion_tablas_scj_reports.sql`, `creacion_tablas_nestle.sql`, `creacion_tablas_shell.sql`)
4. `05-scripts-insigths.sql`, `06-categorias-insights.sql`
5. `07-jhonson-app-tablas.sql`, `08-jhonson-boca-auditor.sql`
6. `09-cotizacion.sql`
7. `10-lt-integration-schema.sql`

`init_schema_completo.sql` is a consolidated bootstrap script; `multi_cliente_tablas.sql` and `dpresent_tablas_nestle.sql` cover the multi-cliente / d-present surface.

### Exception handling

Custom exceptions in `commons.exception`: `UnknownResourceException` (404), `ForbiddenException` (403), `UnauthorizedException` (401), `InternalServerErrorException` (500). Use these instead of returning raw HTTP codes from services.

### Base entity

Most entities do NOT extend `BaseEntidad`. `BaseEntidad` provides audit fields (`fechaCreacion`, `fechaActualizacion`, `nombreUsuarioCreacion`, `nombreUsuarioActualizacion`) but many entities declare these fields directly instead.
