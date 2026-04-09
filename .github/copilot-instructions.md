# CCR REST API — Copilot Instructions

## Build & Run

```bash
# Development build (default profile)
./mvnw clean package

# Production build
./mvnw clean package -Pprod

# Run with dev profile
./mvnw spring-boot:run
```

- Dev artifact: `target/ccr-rest-dev.jar` (port 8080)
- Prod artifact: `target/ccr-rest.jar` (port 8051)
- Context path: `/ccr-rest-api`
- No automated tests exist in this project.

## Architecture

This is a multi-brand field sales reporting API (PDV — Punto de Venta). Each brand is an isolated sub-package under `py.com.jaimeferreira.ccr`, all sharing a common `commons` package.

**Brand packages and their API base paths:**

| Package     | API Base Path     | Brand          |
|-------------|-------------------|----------------|
| `bebidaspy` | `api/v1`          | Bebidas PY     |
| `nestle`    | `nestle/api/v1`   | Nestlé         |
| `jhonson`   | `jhonson/api/v1`  | Johnson (SCJ)  |
| `shell`     | `shell/api/v1`    | Shell          |

Every brand package follows the same layered structure:
```
{brand}/
  controller/   — @RestController
  service/      — @Service (@Component + @Service in some cases)
  repository/   — Spring Data JPA repositories
  entity/       — JPA entities
  dto/          — Request/response objects
  constants/    — Static constants (e.g., image URLs)
```

**Shared infrastructure (`commons`):**
- `entity/BaseEntidad` — abstract audit base for all entities
- `exception/` — custom exception hierarchy
- `security/` — JWT filter and utils
- `util/ManejadorDeArchivos` — image read/write/watermark utility
- `service/AutenticacionService` — user authentication

## Key Conventions

### Entities
All entities that need audit fields must extend `BaseEntidad`, which provides `fechaCreacion`, `fechaActualizacion`, `nombreUsuarioCreacion`, `nombreUsuarioActualizacion`.

### Exception Handling
Define errors in `EnumErrors` (code + message), then throw using the appropriate subclass:
- `CustomGeneralException` — base
- `UnknownResourceException` — 404-style not found
- `ForbiddenException`, `UnauthorizedException`, `InternalServerErrorException`

### Logging
Every class that logs must declare:
```java
private static final Logger LOGGER = LoggerFactory.getLogger(ClassName.class);
```

### Security
- `/auth/**` and `/public/**` are open; everything else requires a JWT Bearer token.
- JWT is stateless (no HTTP session). Tokens are generated via `JWTAuthorizationUtils` and validated by `JWTAuthorizationFilter`.
- Login endpoint: `POST /ccr-rest-api/auth/login`

### Database
- `ddl-auto=none` — the schema is **never** auto-managed by Hibernate.
- Schema changes must be scripted in `src/main/resources/sql/` and applied manually.
- Database: PostgreSQL (`ccr` database, `ccr` user).

### Image Storage
Images are stored on disk under paths configured by `path.directory.*` properties. Use `ManejadorDeArchivos` (autowired) to read/write images — never construct paths manually. The `directoryMainImages` property is the root for most operations. External images are identified by the string `"externo"` in their path.

### Reports
JasperReports is used for PDF/PPTX generation. `.jrxml` source files are in `src/main/resources/jasper/` and compiled at runtime via `JasperCompileManager`. Services inject `JdbcTemplate` directly for report data queries.

### Runtime Profile Check
Services that behave differently between dev and prod inject `env.active` via `@Value("${env.active}")` (`"dev"` or `"prod"`).

### Adding a New Brand
1. Create a new package under `py.com.jaimeferreira.ccr.{brand}` with the full layered structure.
2. Add a `constants/` class with brand-specific image URLs.
3. Add `path.directory.server_path_images_{brand}` properties to both `application-dev.properties` and `application-prod.properties`.
4. Add the corresponding SQL schema script to `src/main/resources/sql/`.
