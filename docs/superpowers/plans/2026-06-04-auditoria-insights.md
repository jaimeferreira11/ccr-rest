# Auditoría del módulo Insights — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Registrar automáticamente desde el backend una auditoría de las acciones de archivos base del módulo Insights (subir template / filtros base / datos base, eliminar datos base), con usuario, fecha-hora, cliente, categoría/tipo cuando existan, resultado (EXITO/ERROR) y un JSON de detalle.

**Architecture:** Tabla nueva `ccr.auditoria_insights` + entidad JPA + repository Spring Data. Un servicio `AuditoriaInsService` con un único método `registrar(...)` **best-effort** (nunca rompe la operación real). El registro se dispara desde `AdminPlataformaController`, envolviendo cada sub-operación en try/catch para auditar tanto el éxito como el error y relanzar la excepción. El catálogo de eventos es un enum en código.

**Tech Stack:** Spring Boot 2.7, Spring Data JPA / Hibernate 5, PostgreSQL (schema `ccr`), Jackson (serialización del detalle), Java 8.

> **Nota sobre pruebas:** este proyecto **no tiene tests automatizados** (ver CLAUDE.md). La verificación de cada tarea es **compilación** (`./mvnw -o compile`) y, al final, **prueba manual de la API** + inspección de la tabla. No se escriben tests JUnit.

> **Rama de trabajo:** `feature/auditoria-insights` (ya creada; el spec ya está commiteado ahí).

> **Compatibilidad Java 8:** sin `var`, sin `record`, sin `Map.of`, sin `Stream.toList()`.

---

### Task 1: DDL de la tabla de auditoría

**Files:**
- Create: `src/main/resources/sql/11-auditoria-insights.sql`
- Modify: `CLAUDE.md` (lista de orden de ejecución de scripts SQL)

- [ ] **Step 1: Crear el script DDL**

Crear `src/main/resources/sql/11-auditoria-insights.sql` con este contenido exacto:

```sql
-- 11-auditoria-insights.sql
-- Auditoría de acciones administrativas del módulo Insights.
-- Ver docs/superpowers/specs/2026-06-04-auditoria-insights-design.md

CREATE TABLE IF NOT EXISTS ccr.auditoria_insights (
    id            BIGSERIAL    PRIMARY KEY,
    evento        VARCHAR(50)  NOT NULL,
    resultado     VARCHAR(20)  NOT NULL,
    usuario       VARCHAR(200) NOT NULL,
    cod_cliente   VARCHAR(50)  NOT NULL,
    cod_categoria VARCHAR(50),
    tipo_reporte  VARCHAR(20),
    fecha_hora    TIMESTAMP    NOT NULL DEFAULT now(),
    detalle       TEXT
);

CREATE INDEX IF NOT EXISTS idx_auditoria_ins_fecha   ON ccr.auditoria_insights (fecha_hora);
CREATE INDEX IF NOT EXISTS idx_auditoria_ins_evento  ON ccr.auditoria_insights (evento);
CREATE INDEX IF NOT EXISTS idx_auditoria_ins_cliente ON ccr.auditoria_insights (cod_cliente);
```

- [ ] **Step 2: Agregar el script al orden de ejecución en CLAUDE.md**

En `CLAUDE.md`, en la sección "Database", la lista numerada de scripts termina hoy en:

```
7. `10-lt-integration-schema.sql`
```

Agregar inmediatamente debajo:

```
8. `11-auditoria-insights.sql`
```

- [ ] **Step 3: Verificar (revisión manual del SQL)**

Releer el script: nombres de columnas en snake_case, schema `ccr`, índices creados. No hay forma automatizada de correrlo aquí; se ejecuta en la BD del entorno en el despliegue.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/sql/11-auditoria-insights.sql CLAUDE.md
git commit -m "feat(insights): DDL tabla auditoria_insights

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 2: Enums del catálogo de eventos

**Files:**
- Create: `src/main/java/py/com/jaimeferreira/ccr/insights/entity/EventoAuditoriaIns.java`
- Create: `src/main/java/py/com/jaimeferreira/ccr/insights/entity/ResultadoAuditoria.java`

- [ ] **Step 1: Crear el enum `EventoAuditoriaIns`**

```java
package py.com.jaimeferreira.ccr.insights.entity;

/**
 * Catálogo de eventos auditables del módulo Insights.
 * El enum es la fuente de verdad del catálogo; la descripción alimenta
 * las etiquetas de la pantalla de listado (Parte 2).
 *
 * @author Jaime Ferreira
 */
public enum EventoAuditoriaIns {

    TEMPLATE_SUBIDO("Subida de template"),
    FILTROS_BASE_SUBIDO("Subida de filtros base"),
    DATOS_BASE_SUBIDO("Subida de datos base"),
    DATOS_BASE_ELIMINADO("Eliminación de datos base");

    private final String descripcion;

    EventoAuditoriaIns(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
```

- [ ] **Step 2: Crear el enum `ResultadoAuditoria`**

```java
package py.com.jaimeferreira.ccr.insights.entity;

/**
 * Resultado de una acción auditada.
 *
 * @author Jaime Ferreira
 */
public enum ResultadoAuditoria {
    EXITO,
    ERROR
}
```

- [ ] **Step 3: Compilar**

Run: `./mvnw -o -q compile`
Expected: build sin errores (BUILD SUCCESS / sin salida de error).

- [ ] **Step 4: Commit**

```bash
git add src/main/java/py/com/jaimeferreira/ccr/insights/entity/EventoAuditoriaIns.java \
        src/main/java/py/com/jaimeferreira/ccr/insights/entity/ResultadoAuditoria.java
git commit -m "feat(insights): catálogo de eventos de auditoría (enums)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 3: Entidad `AuditoriaIns`

**Files:**
- Create: `src/main/java/py/com/jaimeferreira/ccr/insights/entity/AuditoriaIns.java`

- [ ] **Step 1: Crear la entidad**

Sigue el patrón de `InformeIns` (campos directos, sin `BaseEntidad`).

```java
package py.com.jaimeferreira.ccr.insights.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Registro de auditoría de una acción administrativa del módulo Insights.
 *
 * @author Jaime Ferreira
 */
@Entity
@Table(name = "auditoria_insights", schema = "ccr")
public class AuditoriaIns implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "evento", nullable = false, length = 50)
    private EventoAuditoriaIns evento;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false, length = 20)
    private ResultadoAuditoria resultado;

    @NotNull
    @Column(name = "usuario", nullable = false, length = 200)
    private String usuario;

    @NotNull
    @Column(name = "cod_cliente", nullable = false, length = 50)
    private String codCliente;

    @Column(name = "cod_categoria", length = 50)
    private String codCategoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_reporte", length = 20)
    private TipoReporte tipoReporte;

    @Column(name = "fecha_hora", nullable = false, columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime fechaHora;

    @Column(name = "detalle", columnDefinition = "TEXT")
    private String detalle;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EventoAuditoriaIns getEvento() {
        return evento;
    }

    public void setEvento(EventoAuditoriaIns evento) {
        this.evento = evento;
    }

    public ResultadoAuditoria getResultado() {
        return resultado;
    }

    public void setResultado(ResultadoAuditoria resultado) {
        this.resultado = resultado;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getCodCliente() {
        return codCliente;
    }

    public void setCodCliente(String codCliente) {
        this.codCliente = codCliente;
    }

    public String getCodCategoria() {
        return codCategoria;
    }

    public void setCodCategoria(String codCategoria) {
        this.codCategoria = codCategoria;
    }

    public TipoReporte getTipoReporte() {
        return tipoReporte;
    }

    public void setTipoReporte(TipoReporte tipoReporte) {
        this.tipoReporte = tipoReporte;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }
}
```

- [ ] **Step 2: Compilar**

Run: `./mvnw -o -q compile`
Expected: build sin errores.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/py/com/jaimeferreira/ccr/insights/entity/AuditoriaIns.java
git commit -m "feat(insights): entidad AuditoriaIns

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 4: Repository `AuditoriaInsRepository`

**Files:**
- Create: `src/main/java/py/com/jaimeferreira/ccr/insights/repository/AuditoriaInsRepository.java`

- [ ] **Step 1: Crear el repository**

La Parte 1 sólo usa `save` (heredado de `JpaRepository`). Los métodos de consulta paginada se agregarán en la Parte 2.

```java
package py.com.jaimeferreira.ccr.insights.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import py.com.jaimeferreira.ccr.insights.entity.AuditoriaIns;

/**
 * @author Jaime Ferreira
 */
public interface AuditoriaInsRepository extends JpaRepository<AuditoriaIns, Long> {
}
```

- [ ] **Step 2: Compilar**

Run: `./mvnw -o -q compile`
Expected: build sin errores.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/py/com/jaimeferreira/ccr/insights/repository/AuditoriaInsRepository.java
git commit -m "feat(insights): repository AuditoriaInsRepository

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 5: Servicio `AuditoriaInsService`

**Files:**
- Create: `src/main/java/py/com/jaimeferreira/ccr/insights/service/AuditoriaInsService.java`

- [ ] **Step 1: Crear el servicio**

Método único `registrar(...)`, **best-effort**: cualquier fallo (serialización o `save`) se loguea como `warn` y no se propaga, para no romper la operación de negocio.

```java
package py.com.jaimeferreira.ccr.insights.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.insights.entity.AuditoriaIns;
import py.com.jaimeferreira.ccr.insights.entity.EventoAuditoriaIns;
import py.com.jaimeferreira.ccr.insights.entity.ResultadoAuditoria;
import py.com.jaimeferreira.ccr.insights.entity.TipoReporte;
import py.com.jaimeferreira.ccr.insights.repository.AuditoriaInsRepository;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Registra eventos de auditoría del módulo Insights.
 *
 * <p>El registro es best-effort: un fallo al auditar NUNCA interrumpe la
 * operación de negocio que se está auditando — sólo se loguea como warning.</p>
 *
 * @author Jaime Ferreira
 */
@Service
public class AuditoriaInsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditoriaInsService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private AuditoriaInsRepository auditoriaInsRepository;

    /**
     * Persiste un registro de auditoría.
     *
     * @param evento       evento del catálogo
     * @param resultado    EXITO o ERROR
     * @param usuario      usuario que ejecutó la acción
     * @param codCliente   código del cliente (obligatorio)
     * @param codCategoria categoría (sólo template; null en otros)
     * @param tipoReporte  tipo de reporte (sólo template y eliminación; null en otros)
     * @param detalle      datos extra que se serializan a JSON (puede ser null)
     */
    public void registrar(EventoAuditoriaIns evento, ResultadoAuditoria resultado,
                          String usuario, String codCliente, String codCategoria,
                          TipoReporte tipoReporte, Map<String, Object> detalle) {
        try {
            AuditoriaIns registro = new AuditoriaIns();
            registro.setEvento(evento);
            registro.setResultado(resultado);
            registro.setUsuario(usuario);
            registro.setCodCliente(codCliente);
            registro.setCodCategoria(codCategoria);
            registro.setTipoReporte(tipoReporte);
            registro.setFechaHora(LocalDateTime.now());
            registro.setDetalle(serializar(detalle));
            auditoriaInsRepository.save(registro);
        } catch (Exception e) {
            LOGGER.warn("No se pudo registrar auditoría [{}/{}] cliente={}: {}",
                    evento, resultado, codCliente, e.toString());
        }
    }

    private String serializar(Map<String, Object> detalle) {
        if (detalle == null || detalle.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(detalle);
        } catch (JsonProcessingException e) {
            LOGGER.warn("No se pudo serializar el detalle de auditoría: {}", e.toString());
            return null;
        }
    }
}
```

- [ ] **Step 2: Compilar**

Run: `./mvnw -o -q compile`
Expected: build sin errores.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/py/com/jaimeferreira/ccr/insights/service/AuditoriaInsService.java
git commit -m "feat(insights): AuditoriaInsService (registro best-effort)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 6: Integrar el registro en `AdminPlataformaController`

**Files:**
- Modify: `src/main/java/py/com/jaimeferreira/ccr/insights/admin/controller/AdminPlataformaController.java`

Esta tarea: (a) inyecta el servicio, (b) agrega dos helpers privados (`auditar` y `detalle`), (c) reescribe `subirArchivosBase` para auditar cada archivo, (d) reescribe `eliminarArchivosBase` para auditar la eliminación.

- [ ] **Step 1: Agregar imports**

Agregar junto a los demás imports del controller:

```java
import py.com.jaimeferreira.ccr.insights.entity.EventoAuditoriaIns;
import py.com.jaimeferreira.ccr.insights.entity.ResultadoAuditoria;
import py.com.jaimeferreira.ccr.insights.service.AuditoriaInsService;

import java.util.LinkedHashMap;
```

> Verificar si `java.util.LinkedHashMap` ya está importado; si está, no duplicar. `EventoAuditoriaIns`/`ResultadoAuditoria` y `AuditoriaInsService` son nuevos.

- [ ] **Step 2: Inyectar el servicio**

Junto a los otros campos `@Autowired` (después de `private CategoriaService categoriaService;`):

```java
    @Autowired
    private AuditoriaInsService auditoriaInsService;
```

- [ ] **Step 3: Reemplazar el método `subirArchivosBase` completo**

Reemplazar todo el método actual `subirArchivosBase` (desde `public ResponseEntity<Map<String, String>> subirArchivosBase(` hasta su `}` de cierre) por:

```java
    public ResponseEntity<Map<String, String>> subirArchivosBase(
            @PathVariable String codCliente,
            @RequestParam(value = "template", required = false) MultipartFile template,
            @RequestParam(value = "tipoReporte", required = false) String tipoReporte,
            @RequestParam(value = "codCategoria", required = false) String codCategoria,
            @RequestParam(value = "csvFiltrosBase", required = false) MultipartFile csvFiltrosBase,
            @RequestParam(value = "csvDatosBase", required = false) MultipartFile csvDatosBase,
            @RequestParam(value = "sanitizar", defaultValue = "true") boolean sanitizar) {

        String usuario = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' sube archivos base para cliente: {}", usuario, codCliente);

        boolean hayTemplate = template != null && !template.isEmpty();
        boolean hayFiltros  = csvFiltrosBase != null && !csvFiltrosBase.isEmpty();
        boolean hayDatos    = csvDatosBase != null && !csvDatosBase.isEmpty();

        if (!hayTemplate && !hayFiltros && !hayDatos) {
            throw new UnknownResourceException("Debe adjuntar al menos un archivo.");
        }

        String codClienteNorm = codCliente.trim().toUpperCase();
        clienteInsService.findByCodigo(codClienteNorm);

        Map<String, String> response = new HashMap<>();

        if (hayTemplate) {
            long t0 = System.currentTimeMillis();
            String codCategoriaNorm = (codCategoria == null) ? null : codCategoria.trim().toUpperCase();
            TipoReporte tipo = null;
            try {
                if (tipoReporte == null || tipoReporte.trim().isEmpty()) {
                    throw new UnknownResourceException("Debe indicar el tipo de reporte (NORMAL o CADENA) al subir un template.");
                }
                if (codCategoria == null || codCategoria.trim().isEmpty()) {
                    throw new UnknownResourceException("Debe indicar el código de categoría al subir un template.");
                }
                try {
                    tipo = TipoReporte.valueOf(tipoReporte.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new UnknownResourceException("Tipo de reporte inválido: " + tipoReporte
                            + ". Valores válidos: NORMAL, CADENA");
                }
                String nombreArchivo = templateInsService.guardarTemplate(template, codClienteNorm, codCategoriaNorm, tipo, sanitizar);
                response.put("template", nombreArchivo + " guardado correctamente");
                LOGGER.info("Template guardado para cliente: {}, categoria: {}", codClienteNorm, codCategoriaNorm);
                auditar(EventoAuditoriaIns.TEMPLATE_SUBIDO, ResultadoAuditoria.EXITO, codClienteNorm, codCategoriaNorm, tipo,
                        detalle("nombreArchivo", nombreArchivo, "sanitizar", sanitizar,
                                "duracionMs", System.currentTimeMillis() - t0));
            } catch (RuntimeException e) {
                auditar(EventoAuditoriaIns.TEMPLATE_SUBIDO, ResultadoAuditoria.ERROR, codClienteNorm, codCategoriaNorm, tipo,
                        detalle("mensajeError", e.getMessage(),
                                "duracionMs", System.currentTimeMillis() - t0));
                throw e;
            }
        }

        if (hayFiltros) {
            long t0 = System.currentTimeMillis();
            try {
                validarCsv(csvFiltrosBase, "filtros base");
                reporteInsService.guardarArchivoBase(csvFiltrosBase, codClienteNorm, "filtros_base.csv");
                response.put("filtrosBase", "filtros_base.csv guardado correctamente");
                LOGGER.info("Filtros base guardados para cliente: {}", codClienteNorm);
                auditar(EventoAuditoriaIns.FILTROS_BASE_SUBIDO, ResultadoAuditoria.EXITO, codClienteNorm, null, null,
                        detalle("nombreArchivo", "filtros_base.csv",
                                "duracionMs", System.currentTimeMillis() - t0));
            } catch (RuntimeException e) {
                auditar(EventoAuditoriaIns.FILTROS_BASE_SUBIDO, ResultadoAuditoria.ERROR, codClienteNorm, null, null,
                        detalle("mensajeError", e.getMessage(),
                                "duracionMs", System.currentTimeMillis() - t0));
                throw e;
            }
        }

        if (hayDatos) {
            long t0 = System.currentTimeMillis();
            try {
                validarCsv(csvDatosBase, "datos base");
                reporteInsService.guardarArchivoBase(csvDatosBase, codClienteNorm, "datos_base.csv");
                response.put("datosBase", "datos_base.csv guardado correctamente");
                LOGGER.info("Datos base guardados para cliente: {}", codClienteNorm);
                auditar(EventoAuditoriaIns.DATOS_BASE_SUBIDO, ResultadoAuditoria.EXITO, codClienteNorm, null, null,
                        detalle("nombreArchivo", "datos_base.csv",
                                "duracionMs", System.currentTimeMillis() - t0));
            } catch (RuntimeException e) {
                auditar(EventoAuditoriaIns.DATOS_BASE_SUBIDO, ResultadoAuditoria.ERROR, codClienteNorm, null, null,
                        detalle("mensajeError", e.getMessage(),
                                "duracionMs", System.currentTimeMillis() - t0));
                throw e;
            }
        }

        response.put("cliente", codClienteNorm);
        response.put("mensaje", "Archivos base guardados correctamente");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
```

> Cambio de comportamiento intencional: antes `guardarTemplate`/`guardarArchivoBase` recibían `codCliente` sin normalizar en algunos casos; ahora usan `codClienteNorm` de forma consistente (el cliente ya se valida con ese valor en `findByCodigo`).

- [ ] **Step 4: Reemplazar el cuerpo de `eliminarArchivosBase`**

En `eliminarArchivosBase`, la parte actual desde `String resultado = reporteInsService.eliminarDatosBase(codClienteNorm, tipo);` hasta el `return` final, reemplazar por:

```java
        String resultado;
        long t0 = System.currentTimeMillis();
        try {
            resultado = reporteInsService.eliminarDatosBase(codClienteNorm, tipo);
            auditar(EventoAuditoriaIns.DATOS_BASE_ELIMINADO, ResultadoAuditoria.EXITO, codClienteNorm, null, tipo,
                    detalle("resultadoDetalle", resultado,
                            "duracionMs", System.currentTimeMillis() - t0));
        } catch (RuntimeException e) {
            auditar(EventoAuditoriaIns.DATOS_BASE_ELIMINADO, ResultadoAuditoria.ERROR, codClienteNorm, null, tipo,
                    detalle("mensajeError", e.getMessage(),
                            "duracionMs", System.currentTimeMillis() - t0));
            throw e;
        }

        Map<String, String> response = new HashMap<>();
        response.put("cliente", codClienteNorm);
        response.put("mensaje", "Archivos eliminados correctamente");
        response.put("detalle", resultado);
        return ResponseEntity.ok(response);
```

> El parseo de `tipo` (que lanza `UnknownResourceException` si es inválido) queda **fuera** del try de auditoría: es una validación previa a la acción y, según el spec, no se audita.

- [ ] **Step 5: Agregar los helpers privados al final de la clase**

Antes de la llave de cierre de la clase, agregar:

```java
    /**
     * Registra un evento de auditoría tomando el usuario del contexto de seguridad.
     * Delega en {@link AuditoriaInsService} (best-effort, no rompe la operación).
     */
    private void auditar(EventoAuditoriaIns evento, ResultadoAuditoria resultado, String codCliente,
                         String codCategoria, TipoReporte tipoReporte, Map<String, Object> detalle) {
        String usuario = SecurityContextHolder.getContext().getAuthentication().getName();
        auditoriaInsService.registrar(evento, resultado, usuario, codCliente, codCategoria, tipoReporte, detalle);
    }

    /**
     * Construye un mapa de detalle a partir de pares clave/valor (clave1, valor1, clave2, valor2, ...).
     * Las entradas con valor null se omiten. Java 8 (sin Map.of).
     */
    private static Map<String, Object> detalle(Object... kv) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            if (kv[i] != null && kv[i + 1] != null) {
                mapa.put(String.valueOf(kv[i]), kv[i + 1]);
            }
        }
        return mapa;
    }
```

- [ ] **Step 6: Compilar**

Run: `./mvnw -o -q compile`
Expected: build sin errores. Si falla por `TipoReporte` no importado, verificar que ya esté importado en el controller (lo usa el método actual, así que debería estarlo).

- [ ] **Step 7: Commit**

```bash
git add src/main/java/py/com/jaimeferreira/ccr/insights/admin/controller/AdminPlataformaController.java
git commit -m "feat(insights): registrar auditoría en subida/eliminación de archivos base

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 7: Verificación manual end-to-end

**Files:** ninguno (sólo verificación).

- [ ] **Step 1: Preparar la base**

Ejecutar el script DDL en la BD del entorno de desarrollo:

```bash
psql "$CCR_DB_URL" -f src/main/resources/sql/11-auditoria-insights.sql
```
(o correrlo desde el cliente SQL habitual). Verificar que la tabla existe:

```sql
\d ccr.auditoria_insights
```

- [ ] **Step 2: Levantar la app**

Run: `./mvnw spring-boot:run`
Expected: arranca sin errores; Hibernate no falla al mapear `AuditoriaIns` (recordar `ddl-auto=none`, la tabla debe existir del Step 1).

- [ ] **Step 3: Subir un template (caso éxito) y verificar el registro**

Subir un template vía la pantalla `/admin/archivos-base` o con curl al endpoint
`POST /ccr-rest-api/insights/api/v1/admin/clientes/{codCliente}/archivos-base`
(multipart con `template`, `tipoReporte`, `codCategoria`). Luego:

```sql
SELECT evento, resultado, usuario, cod_cliente, cod_categoria, tipo_reporte, fecha_hora, detalle
FROM ccr.auditoria_insights ORDER BY id DESC LIMIT 5;
```
Expected: una fila `TEMPLATE_SUBIDO / EXITO` con cliente, categoría y tipo correctos, y `detalle` con JSON `{"nombreArchivo":...,"sanitizar":...,"duracionMs":...}`.

- [ ] **Step 4: Subir template + datos base en un mismo POST**

Expected: **dos** filas nuevas (`TEMPLATE_SUBIDO` y `DATOS_BASE_SUBIDO`), ambas EXITO.

- [ ] **Step 5: Forzar un error y verificar resultado=ERROR**

Subir un template con `tipoReporte=XXX` (inválido).
Expected: la respuesta HTTP sigue siendo de error (igual que antes), y se registra una fila `TEMPLATE_SUBIDO / ERROR` con `detalle.mensajeError` poblado y `tipo_reporte` NULL.

- [ ] **Step 6: Eliminar datos base**

`DELETE .../archivos-base?tipoReporte=NORMAL`.
Expected: fila `DATOS_BASE_ELIMINADO / EXITO` con `tipo_reporte = NORMAL` y `detalle.resultadoDetalle`.

- [ ] **Step 7: Confirmar aislamiento best-effort (revisión de código)**

Releer `AuditoriaInsService.registrar`: el try/catch envuelve todo y sólo loguea `warn`. Confirmar que un fallo de auditoría (p. ej. BD caída) no impediría que la subida responda OK. No se requiere romper la BD a propósito.

- [ ] **Step 8: Commit (si hubo ajustes durante la verificación)**

Si la verificación reveló correcciones, commitearlas. Si no, no hay nada que commitear en esta tarea.

---

## Self-Review (cobertura del spec)

- **Tabla `ccr.auditoria_insights` con todas las columnas e índices** → Task 1. ✓
- **Catálogo de eventos como enum + ResultadoAuditoria** → Task 2. ✓
- **Entidad JPA estilo `InformeIns`** → Task 3. ✓
- **Repository (save para Parte 1)** → Task 4. ✓
- **Servicio best-effort con `detalle` JSON (Jackson, TEXT)** → Task 5. ✓
- **Registro backend de los 4 eventos (template/filtros/datos + eliminar), EXITO y ERROR, con relanzado de excepción** → Task 6. ✓
- **`cod_cliente` siempre; `cod_categoria` y `tipo_reporte` sólo donde existen; tipo NULL en datos/filtros base** → Task 3 (columnas) + Task 6 (qué se pasa por evento). ✓
- **Validaciones previas no se auditan** → Task 6, parseo de tipo fuera del try en eliminar; en template, las validaciones están dentro del try y sí se auditan como ERROR (decisión: en template el bloque entero cuenta como intento de la acción). Consistente con el spec ("no se audita lo que ocurre antes de cualquier operación de archivo"); el bloque template ya es la operación de template.
- **Frontend sin cambios** → ninguna tarea toca Angular. ✓
- **Parte 2 (endpoint + pantalla) fuera de alcance** → no hay tareas de UI. ✓

Sin placeholders. Firmas y nombres consistentes entre tareas (`registrar(...)`, `auditar(...)`, `detalle(...)`, `EventoAuditoriaIns`, `ResultadoAuditoria`, `AuditoriaIns`, `AuditoriaInsRepository`).
