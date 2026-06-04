# Auditoría del módulo Insights — Diseño

**Fecha:** 2026-06-04
**Autor:** Jaime Ferreira
**Estado:** Aprobado (Parte 1)

## Objetivo

Registrar una auditoría de las acciones administrativas del módulo Insights, empezando por la gestión de archivos base (template, filtros base, datos base). Cada registro guarda **qué pasó** (evento del catálogo), **quién** lo hizo, **cuándo**, **sobre qué cliente/categoría/tipo**, el **resultado** (éxito o error) y **datos extra** en una columna de detalle.

El trabajo se divide en dos partes:

- **Parte 1 (este spec):** persistencia + registro automático desde el backend.
- **Parte 2 (posterior):** endpoint de consulta paginado/filtrable + pantalla Angular de listado.

## Alcance — Parte 1

### Eventos registrados

| Evento (enum) | Origen | Cliente | Categoría | Tipo |
|---|---|---|---|---|
| `TEMPLATE_SUBIDO` | `subirArchivosBase` (template) | sí | sí | sí (NORMAL/CADENA) |
| `FILTROS_BASE_SUBIDO` | `subirArchivosBase` (filtros) | sí | — | — |
| `DATOS_BASE_SUBIDO` | `subirArchivosBase` (datos) | sí | — | — |
| `DATOS_BASE_ELIMINADO` | `eliminarArchivosBase` | sí | — | sí |

Cada uno se registra tanto en **EXITO** como en **ERROR**.

**Decisión sobre el tipo:** el `tipoReporte` (NORMAL/CADENA) hoy sólo llega al backend al subir un template y al eliminar datos base. La subida de datos base y filtros base no recibe el tipo (filtros base directamente no tiene concepto de tipo). Se registra el tipo **sólo donde existe hoy**; en los demás eventos queda `NULL`. No se modifica el frontend en la Parte 1.

### Fuera de alcance (Parte 1)

- Pantalla de listado y endpoint de consulta (Parte 2).
- Auditoría de otras acciones admin (clientes, categorías, países, estado de plataforma): el catálogo queda extensible para agregarlas después.
- Estadísticas finas del sanitizado de template (filas eliminadas, reducción de tamaño): viven dentro de `TemplateInsService` y no se exponen; se podrán plomear más adelante.

## Modelo de datos

### Tabla `ccr.auditoria_insights`

| Columna | Tipo | Nulo | Notas |
|---|---|---|---|
| `id` | BIGSERIAL | PK | identidad, como `ccr.informe` |
| `evento` | VARCHAR(50) | NOT NULL | código del enum `EventoAuditoriaIns` |
| `resultado` | VARCHAR(20) | NOT NULL | `EXITO` / `ERROR` |
| `usuario` | VARCHAR(200) | NOT NULL | de `SecurityContextHolder` |
| `cod_cliente` | VARCHAR(50) | NOT NULL | todos los eventos actuales son por cliente |
| `cod_categoria` | VARCHAR(50) | NULL | sólo template |
| `tipo_reporte` | VARCHAR(20) | NULL | sólo template y eliminación |
| `fecha_hora` | TIMESTAMP | NOT NULL DEFAULT now() | |
| `detalle` | TEXT | NULL | JSON con datos extra |

Índices: `idx_auditoria_ins_fecha (fecha_hora)`, `idx_auditoria_ins_evento (evento)`, `idx_auditoria_ins_cliente (cod_cliente)` — pensados para la pantalla de la Parte 2.

**`detalle` como TEXT con JSON:** serializado con Jackson (ya en el stack). Se evita `jsonb` porque mapearlo con Hibernate 5 / Java 8 requiere `hibernate-types`; igual queda consultable con cast `detalle::jsonb` en Postgres si hiciera falta.

Contenido típico de `detalle` por evento:

- `TEMPLATE_SUBIDO`: `nombreArchivo`, `sanitizar`, `duracionMs`; en error: `mensajeError`.
- `FILTROS_BASE_SUBIDO`: `nombreArchivo`, `duracionMs`; en error: `mensajeError`.
- `DATOS_BASE_SUBIDO`: `nombreArchivo`, `duracionMs`; en error: `mensajeError`.
- `DATOS_BASE_ELIMINADO`: `resultadoDetalle` (string que devuelve el servicio), `duracionMs`; en error: `mensajeError`.

### DDL

Nuevo script `src/main/resources/sql/11-auditoria-insights.sql`. Se agrega al orden de ejecución documentado en `CLAUDE.md` (después de `10-lt-integration-schema.sql`). Hibernate sigue en `ddl-auto=none`.

## Componentes (capa por capa)

### Enums (catálogo en código)

`py.com.jaimeferreira.ccr.insights.entity`:

```java
public enum EventoAuditoriaIns {
    TEMPLATE_SUBIDO("Subida de template"),
    FILTROS_BASE_SUBIDO("Subida de filtros base"),
    DATOS_BASE_SUBIDO("Subida de datos base"),
    DATOS_BASE_ELIMINADO("Eliminación de datos base");
    private final String descripcion; // getter para etiquetas (Parte 2)
}

public enum ResultadoAuditoria { EXITO, ERROR }
```

El enum es la fuente de verdad del catálogo. Su `descripcion` alimenta las etiquetas de la pantalla en la Parte 2.

### Entidad `AuditoriaIns`

`@Entity @Table(name = "auditoria_insights", schema = "ccr")`. Campos espejo de la tabla; `evento` y `resultado` con `@Enumerated(EnumType.STRING)`. `fecha_hora` se setea en el servicio.

### Repository `AuditoriaInsRepository`

`extends JpaRepository<AuditoriaIns, Long>`. La Parte 1 sólo usa `save` (heredado). Los métodos de consulta paginada se agregan en la Parte 2.

### Servicio `AuditoriaInsService`

Un método central **best-effort**: un fallo al auditar **nunca** rompe la operación real (se loguea como `warn`).

```java
public void registrar(EventoAuditoriaIns evento, ResultadoAuditoria resultado,
                      String usuario, String codCliente, String codCategoria,
                      TipoReporte tipoReporte, Map<String, Object> detalle);
```

Construye la entidad, setea `fecha_hora = now()`, serializa `detalle` a JSON con Jackson, y persiste dentro de un try/catch. Si la serialización o el `save` fallan, registra un warning y retorna sin propagar.

## Integración — `AdminPlataformaController`

El registro es 100% backend; el frontend no cambia.

### `subirArchivosBase`

Cada sub-operación (template / filtros / datos) se envuelve en su propio try/catch, midiendo duración:

```java
if (hayTemplate) {
    long t0 = System.currentTimeMillis();
    try {
        // validaciones de tipo/categoría + guardarTemplate(...)
        auditar(TEMPLATE_SUBIDO, EXITO, codClienteNorm, codCategoriaNorm, tipo,
                detalle("nombreArchivo", nombreArchivo, "sanitizar", sanitizar,
                        "duracionMs", System.currentTimeMillis() - t0));
    } catch (RuntimeException e) {
        auditar(TEMPLATE_SUBIDO, ERROR, codClienteNorm, codCategoriaNorm, tipoOrNull,
                detalle("mensajeError", e.getMessage(),
                        "duracionMs", System.currentTimeMillis() - t0));
        throw e; // la respuesta HTTP no cambia
    }
}
```

Análogo para filtros y datos base. Como cada archivo tiene su bloque, un POST que sube template + datos base genera **dos registros**, cada uno con su resultado.

Un helper privado `auditar(...)` obtiene el usuario de `SecurityContextHolder` y delega en `AuditoriaInsService`, para no repetir el boilerplate. Si en el `catch` el tipo aún no fue parseado (p. ej. error "tipo inválido"), se pasa `null`.

### `eliminarArchivosBase`

Mismo patrón con `DATOS_BASE_ELIMINADO`, registrando `tipoReporte` (que sí llega) y el string de resultado del servicio en `detalle`.

## Manejo de errores

- **Auditoría best-effort:** ningún fallo de auditoría afecta la operación de negocio.
- **Resultado fiel:** se audita tanto EXITO como ERROR; en ERROR se preserva el mensaje y se relanza la excepción para que el manejo global de excepciones responda igual que antes.
- Validaciones previas a cualquier operación de archivo (p. ej. "debe adjuntar al menos un archivo", cliente inexistente) **no** generan registro: aún no ocurrió ninguna acción sobre un archivo.

## Pruebas

El proyecto no tiene tests automatizados. Verificación manual:

1. Subir un template (éxito) → 1 fila `TEMPLATE_SUBIDO / EXITO` con cliente, categoría, tipo y `detalle` JSON.
2. Subir template + datos base en un POST → 2 filas (`TEMPLATE_SUBIDO`, `DATOS_BASE_SUBIDO`).
3. Forzar un error (p. ej. tipo inválido o archivo corrupto) → fila con `resultado = ERROR` y `mensajeError` en `detalle`; la respuesta HTTP de error se mantiene.
4. Eliminar datos base → fila `DATOS_BASE_ELIMINADO` con tipo.
5. Confirmar que un fallo simulado de auditoría no rompe la subida.

## Plan de despliegue

1. Ejecutar `11-auditoria-insights.sql` en la base del entorno.
2. Desplegar el backend.
3. (Parte 2) Endpoint de consulta + pantalla Angular.
