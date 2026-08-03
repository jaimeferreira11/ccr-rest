# LT eancode: BIGINT → TEXT interno (sin tocar contrato con proveedor) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cambiar el almacenamiento interno de `eancode` (tablas `lt.producto` y `lt.ticket`) de `BIGINT` a `VARCHAR`, sin modificar el contrato JSON documentado con el proveedor LT (`docs/LT-INTEGRACION-API.md`, `docs/LT-INTEGRACION-API-v2.md`, `docs/LT-INTEGRACION-API.doc`, postman collection). El proveedor sigue enviando `eancode` como número JSON sin comillas, tal como especifica la doc actual; el cambio es 100% interno (DB + entidad + repositorio).

**Architecture:** Los DTOs (`ProductoDTO`, `TicketDTO`) mantienen `eancode` como `Long` — es el punto de contacto con el proveedor y no cambia. Las entidades JPA (`LtProducto`, `LtTicket`) y las columnas de base de datos pasan a `String`/`VARCHAR`. La conversión `Long → String` ocurre en `LtIntegracionService`, en el único punto donde un DTO se copia a una entidad.

**Tech Stack:** Spring Boot 2.7 (Java 8), Spring Data JPA, PostgreSQL. Sin frameworks de test — verificación manual vía `scripts/*.sh` (smoke tests con curl) y `psql`, siguiendo el patrón ya establecido en `scripts/test-lt-tickets-formatos.sh` y las migraciones previas (`13-lt-ticket-unidades-decimal.sql`, `14-lt-ticket-precios-sin-decimales.sql`).

## Global Constraints

- Java 8 compatible: no `var`, no `Stream.toList()`, no records.
- `ddl-auto=none` — todo cambio de esquema va en un script SQL nuevo en `src/main/resources/sql/`, nunca en anotaciones JPA únicamente.
- No modificar `ProductoDTO.eancode` ni `TicketDTO.eancode` (siguen siendo `Long`) — es el contrato con el proveedor y no se toca.
- No modificar `docs/LT-INTEGRACION-API.md`, `docs/LT-INTEGRACION-API-v2.md`, `docs/LT-INTEGRACION-API.doc` ni el postman collection.
- La migración SQL debe ejecutarse como owner de las tablas (usuario `postgres`), igual que las migraciones 12 y 14 (el usuario de aplicación `ccr` no tiene permiso de `ALTER TABLE`).
- Conversión `Long → String` debe ser null-safe (no debe producir el literal `"null"` si `dto.getEancode()` es `null`).

---

## File Structure

- **Create:** `src/main/resources/sql/15-lt-eancode-text.sql` — migración: `ALTER COLUMN eancode TYPE VARCHAR(20)` en `lt.producto` y `lt.ticket`.
- **Modify:** `src/main/java/py/com/jaimeferreira/ccr/lt/entity/LtProducto.java` — campo `eancode` de `Long` a `String`.
- **Modify:** `src/main/java/py/com/jaimeferreira/ccr/lt/entity/LtTicket.java` — campo `eancode` de `Long` a `String`.
- **Modify:** `src/main/java/py/com/jaimeferreira/ccr/lt/repository/LtProductoRepository.java` — `findByEancode(Long)` → `findByEancode(String)`.
- **Modify:** `src/main/java/py/com/jaimeferreira/ccr/lt/repository/LtTicketRepository.java` — `findByPuntoAndNroTicketAndEancode(Integer, String, Long)` → `(Integer, String, String)`.
- **Modify:** `src/main/java/py/com/jaimeferreira/ccr/lt/service/LtIntegracionService.java` — conversión `Long → String` null-safe en `guardarProductos` y `guardarTickets`.
- **Create:** `scripts/test-lt-eancode-text.sh` — smoke test: postea producto+ticket, verifica en BD que `eancode` es `character varying` y que el valor coincide con el enviado; verifica que el flujo de update-por-eancode (upsert) sigue funcionando.

No se tocan DTOs, controladores, ni documentación del contrato.

---

### Task 1: Migración SQL

**Files:**
- Create: `src/main/resources/sql/15-lt-eancode-text.sql`

**Interfaces:**
- Produces: columnas `lt.producto.eancode` y `lt.ticket.eancode` en tipo `VARCHAR(20)` (antes `BIGINT`). Las constraints `uk_lt_producto_eancode` y `uk_lt_ticket` siguen existiendo sin recrearlas — Postgres las mantiene automáticamente al cambiar el tipo subyacente vía `USING`.

- [ ] **Step 1: Escribir la migración**

```sql
-- Migración 15: eancode pasa de BIGINT a VARCHAR en lt.producto y lt.ticket.
-- Motivo: robustecer el almacenamiento interno frente a futuros cambios de formato
-- (ceros a la izquierda, longitudes variables GTIN-8/12/13/14) sin tocar el contrato
-- JSON vigente con el proveedor LT, que sigue enviando eancode como número entero.
-- Ejecutar como owner de las tablas (postgres), igual que las migraciones 12 y 14.

ALTER TABLE lt.producto
    ALTER COLUMN eancode TYPE VARCHAR(20) USING eancode::text;

ALTER TABLE lt.ticket
    ALTER COLUMN eancode TYPE VARCHAR(20) USING eancode::text;
```

- [ ] **Step 2: Aplicar en la base de dev y verificar el tipo de columna**

Run:
```bash
psql -U postgres -d <db_dev> -f src/main/resources/sql/15-lt-eancode-text.sql
psql -U postgres -d <db_dev> -c "\d lt.producto" -c "\d lt.ticket"
```
Expected: `eancode` aparece como `character varying(20)` en ambas tablas; las constraints `uk_lt_producto_eancode` (UNIQUE) y `uk_lt_ticket` (UNIQUE, punto+nro_ticket+eancode) siguen listadas.

- [ ] **Step 3: Confirmar que los datos existentes se preservaron**

Run:
```bash
psql -U postgres -d <db_dev> -c "SELECT eancode, pg_typeof(eancode) FROM lt.producto LIMIT 5;"
```
Expected: valores idénticos a los previos, ahora como texto (ej. `7500435019828`), `pg_typeof` = `character varying`.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/sql/15-lt-eancode-text.sql
git commit -m "feat(lt): migración eancode BIGINT -> VARCHAR en lt.producto y lt.ticket"
```

---

### Task 2: Entidad `LtProducto`

**Files:**
- Modify: `src/main/java/py/com/jaimeferreira/ccr/lt/entity/LtProducto.java:18-19,68-69`

**Interfaces:**
- Consumes: nada de tasks anteriores (entidad standalone).
- Produces: `LtProducto.getEancode(): String` / `LtProducto.setEancode(String)` — usado por Task 3 (repositorio) y Task 5 (servicio).

- [ ] **Step 1: Cambiar el tipo del campo**

En `LtProducto.java`, reemplazar:

```java
    @Column(name = "eancode", nullable = false, unique = true)
    private Long eancode;
```

por:

```java
    @Column(name = "eancode", nullable = false, unique = true, length = 20)
    private String eancode;
```

- [ ] **Step 2: Cambiar getter/setter**

Reemplazar:

```java
    public Long getEancode() { return eancode; }
    public void setEancode(Long eancode) { this.eancode = eancode; }
```

por:

```java
    public String getEancode() { return eancode; }
    public void setEancode(String eancode) { this.eancode = eancode; }
```

- [ ] **Step 3: Compilar para detectar los usos rotos**

Run: `./mvnw compile -P dev`
Expected: FALLA en `LtProductoRepository.java` (firma `findByEancode(Long)`) y en `LtIntegracionService.java` (`entity.setEancode(dto.getEancode())` pasa `Long` a un setter que ahora espera `String`) — esperado, se corrige en Tasks 3 y 4.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/py/com/jaimeferreira/ccr/lt/entity/LtProducto.java
git commit -m "refactor(lt): LtProducto.eancode de Long a String"
```

---

### Task 3: Entidad `LtTicket`

**Files:**
- Modify: `src/main/java/py/com/jaimeferreira/ccr/lt/entity/LtTicket.java:33-34,64-65`

**Interfaces:**
- Consumes: nada de tasks anteriores.
- Produces: `LtTicket.getEancode(): String` / `LtTicket.setEancode(String)` — usado por Task 4 (repositorio) y Task 5 (servicio).

- [ ] **Step 1: Cambiar el tipo del campo**

En `LtTicket.java`, reemplazar:

```java
    @Column(name = "eancode", nullable = false)
    private Long eancode;
```

por:

```java
    @Column(name = "eancode", nullable = false, length = 20)
    private String eancode;
```

- [ ] **Step 2: Cambiar getter/setter**

Reemplazar:

```java
    public Long getEancode() { return eancode; }
    public void setEancode(Long eancode) { this.eancode = eancode; }
```

por:

```java
    public String getEancode() { return eancode; }
    public void setEancode(String eancode) { this.eancode = eancode; }
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/py/com/jaimeferreira/ccr/lt/entity/LtTicket.java
git commit -m "refactor(lt): LtTicket.eancode de Long a String"
```

---

### Task 4: Repositorios

**Files:**
- Modify: `src/main/java/py/com/jaimeferreira/ccr/lt/repository/LtProductoRepository.java:8`
- Modify: `src/main/java/py/com/jaimeferreira/ccr/lt/repository/LtTicketRepository.java:8`

**Interfaces:**
- Consumes: `LtProducto.eancode: String` (Task 2), `LtTicket.eancode: String` (Task 3).
- Produces: `LtProductoRepository.findByEancode(String): Optional<LtProducto>`, `LtTicketRepository.findByPuntoAndNroTicketAndEancode(Integer, String, String): Optional<LtTicket>` — usados por Task 5.

- [ ] **Step 1: Actualizar `LtProductoRepository`**

Reemplazar:

```java
    Optional<LtProducto> findByEancode(Long eancode);
```

por:

```java
    Optional<LtProducto> findByEancode(String eancode);
```

- [ ] **Step 2: Actualizar `LtTicketRepository`**

Reemplazar:

```java
    Optional<LtTicket> findByPuntoAndNroTicketAndEancode(Integer punto, String nroTicket, Long eancode);
```

por:

```java
    Optional<LtTicket> findByPuntoAndNroTicketAndEancode(Integer punto, String nroTicket, String eancode);
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/py/com/jaimeferreira/ccr/lt/repository/LtProductoRepository.java \
        src/main/java/py/com/jaimeferreira/ccr/lt/repository/LtTicketRepository.java
git commit -m "refactor(lt): repositorios LT usan eancode como String"
```

---

### Task 5: `LtIntegracionService` — conversión Long → String

**Files:**
- Modify: `src/main/java/py/com/jaimeferreira/ccr/lt/service/LtIntegracionService.java:1-11` (imports), `:55,62` (`guardarProductos`), `:88,99` (`guardarTickets`)

**Interfaces:**
- Consumes: `ProductoDTO.getEancode(): Long`, `TicketDTO.getEancode(): Long` (sin cambios — contrato intacto), `LtProductoRepository.findByEancode(String)`, `LtTicketRepository.findByPuntoAndNroTicketAndEancode(Integer, String, String)`, `LtProducto.setEancode(String)`, `LtTicket.setEancode(String)`.
- Produces: comportamiento observable — `guardarProductos`/`guardarTickets` siguen aceptando `eancode` numérico del proveedor sin cambios, persisten como texto.

- [ ] **Step 1: Agregar import**

En `LtIntegracionService.java`, agregar junto a los imports existentes:

```java
import java.util.Objects;
```

- [ ] **Step 2: Convertir en `guardarProductos`**

Reemplazar:

```java
            LtProducto entity = productoRepo.findByEancode(dto.getEancode())
                    .orElseGet(() -> {
                        LtProducto nuevo = new LtProducto();
                        nuevo.setFechaCreacion(LocalDateTime.now());
                        nuevo.setNombreUsuarioCreacion(USUARIO_SISTEMA);
                        return nuevo;
                    });
            entity.setEancode(dto.getEancode());
```

por:

```java
            String eancode = Objects.toString(dto.getEancode(), null);
            LtProducto entity = productoRepo.findByEancode(eancode)
                    .orElseGet(() -> {
                        LtProducto nuevo = new LtProducto();
                        nuevo.setFechaCreacion(LocalDateTime.now());
                        nuevo.setNombreUsuarioCreacion(USUARIO_SISTEMA);
                        return nuevo;
                    });
            entity.setEancode(eancode);
```

`Objects.toString(Object, String)` devuelve el segundo argumento (`null`) si `dto.getEancode()` es `null`, en vez del literal `"null"` que produciría `String.valueOf`.

- [ ] **Step 3: Convertir en `guardarTickets`**

Reemplazar:

```java
            LtTicket entity = ticketRepo
                    .findByPuntoAndNroTicketAndEancode(dto.getPunto(), dto.getNroTicket(), dto.getEancode())
                    .orElseGet(() -> {
                        LtTicket nuevo = new LtTicket();
                        nuevo.setFechaCreacion(LocalDateTime.now());
                        nuevo.setNombreUsuarioCreacion(USUARIO_SISTEMA);
                        return nuevo;
                    });
            entity.setPunto(dto.getPunto());
            entity.setNroTicket(dto.getNroTicket());
            entity.setFecha(dto.getFecha());
            entity.setHora(dto.getHora());
            entity.setEancode(dto.getEancode());
```

por:

```java
            String eancode = Objects.toString(dto.getEancode(), null);
            LtTicket entity = ticketRepo
                    .findByPuntoAndNroTicketAndEancode(dto.getPunto(), dto.getNroTicket(), eancode)
                    .orElseGet(() -> {
                        LtTicket nuevo = new LtTicket();
                        nuevo.setFechaCreacion(LocalDateTime.now());
                        nuevo.setNombreUsuarioCreacion(USUARIO_SISTEMA);
                        return nuevo;
                    });
            entity.setPunto(dto.getPunto());
            entity.setNroTicket(dto.getNroTicket());
            entity.setFecha(dto.getFecha());
            entity.setHora(dto.getHora());
            entity.setEancode(eancode);
```

- [ ] **Step 4: Compilar y confirmar que ya no hay errores**

Run: `./mvnw compile -P dev`
Expected: BUILD SUCCESS (las fallas de Task 2 quedan resueltas).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/py/com/jaimeferreira/ccr/lt/service/LtIntegracionService.java
git commit -m "fix(lt): convertir eancode Long->String de forma null-safe al persistir"
```

---

### Task 6: Verificación end-to-end (smoke test)

**Files:**
- Create: `scripts/test-lt-eancode-text.sh`

**Interfaces:**
- Consumes: endpoints `POST /lt/api/v1/productos` y `POST /lt/api/v1/tickets` (sin cambios de contrato — siguen recibiendo `eancode` numérico).

- [ ] **Step 1: Escribir el smoke test**

```bash
#!/bin/bash
#
# Smoke test: eancode pasa de BIGINT a VARCHAR en lt.producto/lt.ticket,
# sin cambiar el contrato JSON con el proveedor (sigue enviándose como número).
# Verifica:
#   - el endpoint sigue aceptando eancode numérico sin comillas (contrato intacto),
#   - el valor se persiste como texto en la columna,
#   - el upsert por eancode (buscar-y-actualizar) sigue funcionando: reenviar el
#     mismo eancode actualiza la fila existente, no crea un duplicado.
#
# Uso:
#   ./scripts/test-lt-eancode-text.sh
#
# Requiere: curl, jq (opcional), psql apuntando a la misma BD que usa la app.

set -e

BASE_URL="${CCR_BASE_URL:-http://localhost:8081/ccr-rest-api}"
API_KEY="${LT_API_KEY:-ltk_KFYQNxRAls24J46tSwndn68UYPfp9_SfyumbVCa9_es}"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}POST $BASE_URL/lt/api/v1/productos (eancode numérico, sin comillas)${NC}"
curl -sS -X POST "$BASE_URL/lt/api/v1/productos" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '[{"eancode": 7790099887766, "descripcion": "SMK eancode text", "fabricante": "Test", "marca": "Test"}]' \
  | (jq . 2>/dev/null || cat)

echo
echo -e "${YELLOW}Reenviando el MISMO eancode con descripción distinta (debe actualizar, no duplicar)${NC}"
curl -sS -X POST "$BASE_URL/lt/api/v1/productos" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '[{"eancode": 7790099887766, "descripcion": "SMK eancode text - actualizado", "fabricante": "Test", "marca": "Test"}]' \
  | (jq . 2>/dev/null || cat)

echo
echo -e "${YELLOW}POST $BASE_URL/lt/api/v1/tickets (eancode numérico, sin comillas)${NC}"
curl -sS -X POST "$BASE_URL/lt/api/v1/tickets" \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d '[{"punto": 5, "nroTicket": "SMK-EAN-1", "fecha": "2026-08-03", "hora": "10:00:00", "eancode": 7790099887766, "ean_desc": "SMK eancode text", "unidades_vendidas": 1, "precio_regular": "1000", "precio_promocional": "900", "tipo_venta": "P"}]' \
  | (jq . 2>/dev/null || cat)

echo
echo -e "${GREEN}Verificá en BD:${NC}"
cat <<'SQL'

  -- 1 sola fila (no duplicado), descripcion actualizada, eancode como texto
  SELECT eancode, pg_typeof(eancode), descripcion
  FROM lt.producto
  WHERE eancode = '7790099887766';

  SELECT nro_ticket, eancode, pg_typeof(eancode)
  FROM lt.ticket
  WHERE nro_ticket = 'SMK-EAN-1';

  Esperado:
    lt.producto: 1 fila, eancode = '7790099887766' (character varying), descripcion = 'SMK eancode text - actualizado'
    lt.ticket:   1 fila, eancode = '7790099887766' (character varying)
SQL
```

- [ ] **Step 2: Dar permiso de ejecución**

Run: `chmod +x scripts/test-lt-eancode-text.sh`

- [ ] **Step 3: Levantar la app y correr el smoke test**

Run:
```bash
./mvnw spring-boot:run &
sleep 15
./scripts/test-lt-eancode-text.sh
```
Expected: ambos POST devuelven 200/OK con `guardados=1`; las queries SQL confirman una sola fila por eancode (no duplicado tras el segundo POST) y `pg_typeof` = `character varying`.

- [ ] **Step 4: Confirmar que el contrato JSON no cambió**

Comparar el payload usado en el smoke test (`"eancode": 7790099887766`, sin comillas) contra los ejemplos de `docs/LT-INTEGRACION-API.md:154,210` — mismo formato, ningún cambio requerido del lado del proveedor.

- [ ] **Step 5: Commit**

```bash
git add scripts/test-lt-eancode-text.sh
git commit -m "test(lt): smoke test de eancode BIGINT->VARCHAR sin romper contrato LT"
```

---

## Self-Review Notes

- **Cobertura:** Task 1 cubre la migración DB; Tasks 2-3 las entidades; Task 4 los repositorios; Task 5 el único punto de conversión Long→String (`LtIntegracionService`); Task 6 verifica end-to-end que el contrato con el proveedor no cambió y que el upsert por eancode sigue funcionando. `LtSucursal` y `LtPersona` no tienen `eancode` — no requieren cambios.
- **Sin placeholders:** cada step tiene el código completo a pegar, no hay "TODO" ni "similar a la Task N".
- **Consistencia de tipos:** `String eancode` fluye igual en Task 2 (entidad) → Task 4 (repositorio) → Task 5 (servicio, vía `Objects.toString`). `ProductoDTO`/`TicketDTO` quedan con `Long eancode` sin tocar, confirmado explícitamente en Global Constraints y en Task 5.
