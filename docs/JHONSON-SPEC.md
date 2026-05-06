# SCJ (Johnson) - Especificacion Funcional

Documento de referencia para cualquier IA o desarrollador que trabaje sobre el modulo Jhonson.

**Producto:** Jhonson CCR App (recopilacion de datos en punto de venta)  
**Backend:** `ccr-rest` (este repositorio), paquete `py.com.jaimeferreira.ccr.jhonson`  
**App mobile:** Flutter, repo `/development/workspace-flutter/jhonson_ccr_app`  
**D-present (reportes web):** controller `JhonsonDReportsController` (`jhonson/d-reports/api/v1`)  
**Base de datos:** PostgreSQL, schema `jhonson`  
**API base path (app):** `jhonson/api/v1`

---

## 1. Que es la app Jhonson

App mobile para auditores de SC Johnson. Los auditores visitan puntos de venta (bocas),
relevan surtido, exhibiciones y plaza, toman fotos y envian los datos al backend.

El flujo principal es:
1. El auditor se autentica (JWT)
2. Descarga sus bocas asignadas, cabeceras e items
3. Visita cada boca y completa las secciones (SI, SP, SE)
4. Sube respuestas y fotos al backend

---

## 2. Canales

Jhonson solo opera en los canales **Autoservicio** y **Supermercados**. No trabaja con otros canales.

---

## 3. Cabeceras (secciones de auditoria)

Codigos activos en la tabla `jhonson.cabeceras`:

| Codigo | Nombre | Descripcion |
|--------|--------|-------------|
| **SI** | Surtido Basico | Relevamiento de productos presentes en gondola |
| **SP** | Plaza | Medicion de espacio fisico en gondola |
| **SE** | Exhibiciones | Relevamiento de exhibiciones especiales |

> Los codigos anteriores (FI, FO, FP, FR, TA) fueron reemplazados y estan inactivos.

### SI - Surtido Basico
- Se envian **todos los items** en el detalle, aunque el usuario no los marque.
- Los items tienen `categoria`; la app los agrupa y filtra por categoria en pantalla.
- Se pueden adjuntar **multiples fotos**; se requiere **al menos una foto**.
- El usuario puede marcar **Presencia**, **Precio** o **ambos** por item.
- Comentarios opcionales (campo `comentario_si` en `respuesta_cab`).

### SE - Exhibiciones
- Se pueden adjuntar **multiples fotos**; se requiere **al menos una foto**, **excepto** si el usuario marca "Sin Exhibicion".
- Si marca **"Sin Exhibicion"**: se envia un unico registro en el detalle con el item especial "Sin Exhibicion"; no se envian otros items.
- Si **no** marca "Sin Exhibicion": solo llegan los items que el usuario completo.
- Comentarios opcionales (campo `comentario_se` en `respuesta_cab`).

### SP - Plaza
- Si el usuario carga **Total**, tambien debe cargar **Ocupa** (ambos obligatorios juntos).
- **Total** siempre debe ser **mayor o igual** a Ocupa.
- Se envian **todos los items**; los que no tienen datos van con valor `0`.
- Se requiere **al menos una foto**.
- Comentarios opcionales (campo `comentario_sp` en `respuesta_cab`).

### Reglas generales (todas las cabeceras)
- Comentarios son opcionales.
- Limite maximo de fotos definido en constante `AppConstants` de la app (actualmente 20).

---

## 4. Bocas (puntos de venta)

Las bocas son los puntos de venta que los auditores deben visitar.

### Entidad `BocaSCJ` (tabla `jhonson.bocas`)

| Campo | Descripcion |
|-------|-------------|
| `id` | PK autoincremental |
| `cod_boca` | Codigo unico de la boca |
| `nombre` | Nombre del punto de venta |
| `direccion` | Direccion fisica |
| `ciudad` | Ciudad |
| `canal_ccr` | Canal (Autoservicio / Supermercado) |
| `ocasion` | Ocasion de relevamiento |
| `activo` | Flag de boca activa |
| `longitud` / `latitud` | Coordenadas GPS |
| `externo` | Flag de boca externa |
| `cod_distribuidor` | Codigo del distribuidor asociado |
| `fecha_creacion` | Timestamp de creacion |

### Relacion boca-auditor

**Modelo actual (N:M):** Una boca puede tener multiples auditores asignados.
La relacion se gestiona en la tabla intermedia `jhonson.boca_auditor`:

| Campo | Descripcion |
|-------|-------------|
| `id` | PK autoincremental |
| `id_boca` | FK a `jhonson.bocas` |
| `auditor` | Username del auditor |

Constraint: `UNIQUE (id_boca, auditor)` — un auditor no puede estar asignado dos veces a la misma boca.

> **Nota de retrocompatibilidad:** La app mobile espera un campo `auditor` (String) en el JSON
> de cada boca. El backend lo mantiene como campo `@Transient @Deprecated` en `BocaSCJ`,
> populandolo con el username del usuario autenticado en el endpoint `GET /bocas`.
> Esto se puede eliminar cuando se actualice la app mobile.

### Endpoints de bocas

| Endpoint | Descripcion | Filtro |
|----------|-------------|--------|
| `GET /bocas` | Bocas del auditor autenticado | `boca_auditor.auditor = username AND activo = true` |
| `GET /bocas/all` | Todas las bocas activas | `activo = true` |

---

## 5. Endpoints de la app (JhonsonAppController)

| Metodo | Path | Descripcion |
|--------|------|-------------|
| `GET` | `/items` | Lista todos los items activos |
| `GET` | `/cabeceras` | Lista todas las cabeceras activas |
| `GET` | `/bocas` | Bocas asignadas al auditor autenticado |
| `GET` | `/bocas/all` | Todas las bocas activas |
| `GET` | `/respuestas` | Todas las respuestas |
| `POST` | `/respuestas` | Guardar lista de respuestas (cabecera + detalle + imagenes) |
| `PUT` | `/usuarios/change-password` | Cambiar password del usuario |
| `POST` | `/upload-image` | Subir una imagen (base64) |
| `POST` | `/upload-list-image` | Subir multiples imagenes (base64) |

---

## 6. Endpoints de reportes web (JhonsonDReportsController)

| Metodo | Path | Descripcion |
|--------|------|-------------|
| `GET` | `/distribuidores` | Lista distribuidores |
| `GET` | `/bocas` | Todas las bocas activas |
| `GET` | `/bocas/distribuidor/{codigo}` | Bocas por distribuidor |

---

## 7. Modelo de datos (schema `jhonson`)

Tablas principales:
- `bocas` — puntos de venta
- `boca_auditor` — relacion N:M entre bocas y auditores
- `distribuidores` — distribuidores de SCJ
- `usuario_distribuidor` — relacion usuario-distribuidor
- `cabeceras` — secciones de auditoria (SI, SP, SE)
- `items` — items a relevar por cabecera
- `respuesta_cab` — cabecera de respuesta (una por visita a boca)
- `respuesta_det` — detalle de respuesta (una por item relevado)
- `respuesta_imagen` — imagenes adjuntas por cabecera

Scripts DDL en `src/main/resources/sql/`:
1. `creacion_tablas_scj_reports.sql` — tablas base (distribuidores, bocas, reportes)
2. `07-jhonson-app-tablas.sql` — tablas de la app (cabeceras, items, respuestas, auditor en bocas)
3. `08-jhonson-boca-auditor.sql` — migracion a relacion N:M boca-auditor
