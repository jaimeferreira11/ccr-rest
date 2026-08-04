# Export CSV `lt.ticket` — Diseño

Fecha: 2026-08-04

## Contexto

El módulo `lt` (integración con proveedor POS externo "LT") hoy es solo de ingesta: expone endpoints POST bajo `/lt/**` (protegidos por API key estática, `LtApiKeyFilter`) que reciben datos del proveedor y los persisten en tablas del schema `ccr`/`lt`. No existe ningún endpoint de lectura/export.

Se necesita un endpoint administrativo para descargar en CSV los datos cargados, empezando por `lt.ticket` (entidad `LtTicket`). El endpoint es de uso interno/admin, no del proveedor LT, así que debe vivir fuera de `/lt/**` (que está atado al API key estático) y en su lugar seguir la convención del paquete `insights.admin` (JWT estándar).

Este es el primer export de una serie — el mismo patrón se reutilizará después para `lt.sucursal`, `lt.producto`, `lt.persona`.

## Requisitos (dados por el usuario)

- Delimitador CSV: `;`
- Números con parte decimal real: coma como separador decimal, 3 dígitos (ej. `1,500`, `1,250`)
- Números sin parte decimal: enteros planos, sin separador (ej. `1`, `10`, `100`)
- Esa lógica de formato aplica a todo campo numérico que pueda tener decimales (los 3 `BigDecimal` de `LtTicket`)
- Todas las columnas de negocio del ticket van en el CSV
- Corte por el campo `fecha`, aceptando un rango (`desde`/`hasta`)
- Empezar por `lt.ticket`; luego el resto de tablas del módulo y un front que consuma el API
- Vive en el paquete `insights.admin` (uso admin-only)

## Decisiones tomadas durante brainstorming

1. **Formato numérico dinámico por valor real** (no por columna/scale de DB): si `BigDecimal.stripTrailingZeros().scale() <= 0` → entero plano; si no → `0,000` (coma + 3 decimales, sin agrupador de miles). Los precios (`precio_regular`, `precio_promocional`) están en guaraníes y su scale en DB es 0, así que bajo esta regla siempre salen como enteros — no hace falta un caso especial por columna. `unidades_vendidas` sí puede tener decimales reales (kg, l, m) y usa la misma lógica.
2. **Parámetros de fecha**: `desde` y `hasta`, formato ISO `yyyy-MM-dd`, ambos **obligatorios**, con validación `desde <= hasta` (400 si no se cumple). Sin filtros adicionales (ej. `punto`) en esta primera versión — YAGNI, se agrega después si hace falta.
3. **Encoding**: UTF-8 con BOM, para que Excel en Windows abra tildes/`ñ` correctamente al doble-click.
4. **Volumen**: se asume chico/medio (miles de filas por rango) — se arma el CSV completo en memoria (`ByteArrayOutputStream`), sin streaming ni paginación de query. Si el volumen crece mucho a futuro, revisar.
5. **Columnas incluidas**: las 12 columnas propias de `LtTicket` (`id`, `punto`, `nro_ticket`, `fecha`, `hora`, `eancode`, `ean_desc`, `unidades_vendidas`, `precio_regular`, `precio_promocional`, `tipo_venta`, `estado`). Las 4 columnas de auditoría heredadas de `BaseEntidad` (`fecha_creacion`, `fecha_actualizacion`, `nombre_usuario_creacion`, `nombre_usuario_actualizacion`) quedan **afuera** — son metadata técnica casi siempre vacía en este flujo automático, no dato de negocio.
6. **Nombres de columna en el header CSV**: snake_case igual a la columna DB (`nro_ticket`, no `nroTicket`), para que el CSV sea 1:1 legible contra la tabla `lt.ticket`.
7. **Ubicación del endpoint**: fuera de `/lt/**` (que usa `LtApiKeyFilter` con API key estática del proveedor). Va bajo `/insights/api/v1/admin/lt/...`, protegido por el filtro JWT estándar (`anyRequest().authenticated()`), igual que `AuditoriaInsController` / `AdminPlataformaController`.

## Arquitectura

```
LtExportController (insights.admin.controller)
    -> LtExportService (insights.admin.service)
        -> LtTicketRepository (lt.repository)   [ya existe, se le agrega 1 método]
        -> CsvNumberFormatter (util de formato)  [nuevo]
```

- **`LtExportController`**: `@RestController`, `@RequestMapping("/insights/api/v1/admin/lt")`. Un endpoint: `GET /tickets/export`.
- **`LtExportService`**: arma la lista de `LtTicket` vía repository y produce los bytes del CSV (incluye BOM).
- **`LtTicketRepository`**: se agrega `List<LtTicket> findByFechaBetweenOrderByFechaAscPuntoAscNroTicketAsc(LocalDate desde, LocalDate hasta)`.
- **`CsvNumberFormatter`**: clase utilitaria con un método estático `formatDecimal(BigDecimal valor)` (sin estado, reutilizable para los próximos exports de `lt.producto`, `lt.sucursal`, etc.).

## Contrato del endpoint

```
GET /insights/api/v1/admin/lt/tickets/export?desde=2026-08-01&hasta=2026-08-04
```

- **Query params**: `desde`, `hasta` — `@RequestParam @DateTimeFormat(iso = DATE)` `LocalDate`, ambos obligatorios (Spring devuelve 400 automáticamente si faltan).
- **Validación adicional**: si `desde` > `hasta` → 400 Bad Request con mensaje descriptivo.
- **Auth**: JWT estándar (Bearer token), sin anotación de seguridad especial — cubierto por la regla global `anyRequest().authenticated()` ya que la ruta no matchea `/lt/**`, `/auth/**` ni `/public/**`.
- **Respuesta exitosa**: `200 OK`, `ResponseEntity<ByteArrayResource>`
  - `Content-Type: text/csv;charset=UTF-8`
  - `Content-Disposition: attachment; filename="lt_tickets_{desde}_{hasta}.csv"`
  - Body: bytes del CSV (BOM UTF-8 + contenido)
- **Sin resultados**: 200 OK, CSV con solo la fila de header (no es un error).

## Formato CSV

- Delimitador: `;` (mismo usado ya en `ReporteInsService` para lectura, vía opencsv).
- Librería: `com.opencsv.CSVWriter` (ya en `pom.xml`, versión 5.9 — hoy solo se usa el lado de lectura).
- BOM UTF-8 (`EF BB BF`) escrito antes del contenido del `CSVWriter`.
- **Header** (fila 1, snake_case):
  `id;punto;nro_ticket;fecha;hora;eancode;ean_desc;unidades_vendidas;precio_regular;precio_promocional;tipo_venta;estado`
- **Formato por columna**:
  - `id`, `punto`: `String.valueOf(...)` (enteros, sin formato especial)
  - `nro_ticket`, `eancode`, `ean_desc`, `tipo_venta`, `estado`: texto tal cual, null → celda vacía
  - `fecha`: `yyyy-MM-dd`
  - `hora`: `HH:mm:ss`, null → celda vacía
  - `unidades_vendidas`, `precio_regular`, `precio_promocional`: `CsvNumberFormatter.formatDecimal(...)`, null → celda vacía

### `CsvNumberFormatter.formatDecimal(BigDecimal valor)`

```
si valor == null            -> ""
si valor.stripTrailingZeros().scale() <= 0
                             -> valor.toBigInteger().toString()          (ej. "100")
si no                       -> DecimalFormat("0.000") con
                                DecimalFormatSymbols.setDecimalSeparator(',')
                                y setGroupingUsed(false)                 (ej. "1,500")
```

## Manejo de errores

- `desde > hasta` → `400 Bad Request`, cuerpo JSON simple (mismo estilo de error que ya usa el proyecto para 400s explícitos; si no hay un `@ControllerAdvice` genérico para `IllegalArgumentException`, se construye el `ResponseEntity.badRequest()` directamente en el controller).
- Params faltantes/mal formados: manejado por Spring (`MissingServletRequestParameterException` / `MethodArgumentTypeMismatchException`) → 400 con el manejo ya existente en el proyecto (si existe `@ControllerAdvice` global) o el default de Spring.
- No se contempla ningún otro caso de error propio de negocio (no hay estados intermedios, no hay archivos en disco involucrados).

## Testing

Sin tests automatizados (no hay suite en el proyecto). Se agrega un smoke script manual `scripts/test-lt-export.sh` (curl contra el endpoint, con un token válido) siguiendo el estilo de `scripts/test-generar-reporte.sh`, para verificar manualmente: descarga exitosa, header BOM+`;`, formato de números, filtro de fecha, caso `desde>hasta` → 400.

## Fuera de alcance (próximos pasos, no en este spec)

- Exports de `lt.sucursal`, `lt.producto`, `lt.persona` (mismo patrón, se hacen después).
- Front Angular que consuma este endpoint (se aborda en un spec separado una vez el API esté validado).
- Filtro opcional por `punto` u otros campos (se agrega si surge la necesidad).
