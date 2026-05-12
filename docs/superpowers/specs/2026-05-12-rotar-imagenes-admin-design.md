# Módulo Admin de Rotación de Imágenes

**Fecha:** 2026-05-12
**Estado:** Diseño aprobado, pendiente plan de implementación
**Brands afectados:** Jhonson, Nestle, Shell

## 1. Contexto y motivación

Las fotos subidas desde las apps móviles a veces llegan al filesystem en orientación incorrecta ("acostadas") porque el celular guarda los píxeles en orientación nativa del sensor y el backend descartaba la metadata EXIF al recodificar con `ImageIO.read/write` (ver fix aplicado el 2026-05-12 en `ManejadorDeArchivos.base64ToImagen`).

El fix mencionado resuelve el caso de uploads nuevos cuando la foto trae EXIF. Pero:

- Las **fotos viejas** que ya están torcidas en el filesystem no se corrigen solas.
- Pueden seguir apareciendo casos residuales en celulares con ROMs custom que strippean EXIF antes de subir (caso C documentado en la investigación previa).

Hoy existe `ManejadorDeArchivos.rotateImage(String imgPath)` para rotar un archivo individual, pero **no hay UI** para invocarlo. El admin no tiene cómo accionarlo salvo entrando manualmente al backend.

**Objetivo:** dar al admin una pantalla en d-insights-ccr para listar fotos por brand + período y rotar las que estén mal orientadas.

## 2. Alcance

### En scope

- Pantalla nueva en `d-insights-ccr` bajo `pages/admin/imagenes/` que permite:
  - Filtrar fotos por brand (Jhonson/Nestle/Shell), año, mes, y opcionalmente boca (y distribuidor para Nestle).
  - Visualizar todas las fotos del filtro en un grid de thumbnails.
  - Click en una foto abre un modal con la imagen en grande y un botón "Rotar 90° CW".
- Endpoints REST nuevos en `ccr-rest` bajo `/admin/imagenes/*`.
- Creación del `ImagenesShellService` (no existe; alcanza paridad con Jhonson y Nestle).
- Validación anti path-traversal en el endpoint de rotación.

### Fuera de scope (anti-creep)

- Borrar, subir, renombrar o mover imágenes desde el admin.
- Rotación en ángulos distintos a 90° CW.
- Rotación batch (rotar varias en una acción).
- Undo / historial de rotaciones.
- Auditoría de quién rotó qué.
- BebidasPY (desestimado del producto).

## 3. Diferencias entre productos

Esta sección consolida todas las diferencias relevantes para el módulo. Cada sección del spec más abajo hace llamadas inline cuando corresponde.

| Aspecto | Jhonson | Nestle | Shell |
|---|---|---|---|
| Carpeta filesystem | `zoomin-jhonson/{codBoca}/` | `zoomin-nestle/{codDistribuidor}/{codBoca}/` | `zoomin-shell/{codBoca}/` |
| Property | `path.directory.server_path_images_scj` | `path.directory.server_path_images_nestle` | `path.directory.server_path_images_shell` |
| Service backend existente | `ImagenesSCJService` | `ImagenesNestService` | **no existe (crear)** |
| Jerarquía de filtros | brand + boca opcional | brand + **distribuidor obligatorio** + boca opcional | brand + boca opcional |
| Nombre de archivo | `{codBoca}_{anio}_{mes}_{nro}.jpg` | misma convención (verificar al implementar) | misma convención (verificar al implementar) |
| Endpoint actual de listado | `/jhonson/imagenes/boca/{codigo}` | análogo en módulo nestle | no existe |

**Implicancias:**

- El controller admin tendrá un `switch(brand)` que delega a cada service. La lógica de "qué carpetas iterar" vive dentro de cada service para no contaminar el controller.
- Cuando `brand=nestle`, el frontend debe mostrar el dropdown de Distribuidor antes que el de Boca. Para los otros dos brands, ese dropdown queda oculto.
- En la validación anti path-traversal del endpoint `/rotar`, el path recibido debe arrancar con la carpeta del brand correspondiente (lookup por brand).
- Si `ImagenesShellService` se crea siguiendo el patrón existente, conviene revisarlo simultáneamente con el código de los otros dos para detectar oportunidad de refactor futuro (no en este scope).

## 4. Arquitectura

```
┌─────────────────────────────────────────────────────┐
│  d-insights-ccr (Angular)                           │
│  pages/admin/imagenes/                              │
│    ├── imagenes-admin.component (filtros + grid)    │
│    ├── imagen-preview-modal     (lightbox + rotar)  │
│    └── imagenes-admin.service   (HTTP)              │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP (JWT)
                       ▼
┌─────────────────────────────────────────────────────┐
│  ccr-rest (Spring Boot)                             │
│  AdminImagenesController                            │
│    ├── GET  /admin/imagenes/listar                  │
│    └── POST /admin/imagenes/rotar                   │
│         │                                           │
│         │ switch(brand)                             │
│         ▼                                           │
│  ImagenesSCJService    (existe; agregar 2 métodos)  │
│  ImagenesNestService   (existe; agregar 2 métodos)  │
│  ImagenesShellService  (CREAR desde cero)           │
│         │                                           │
│         └─► ManejadorDeArchivos.rotateImage()       │
│                  (ya existe, sobrescribe el JPG)    │
└─────────────────────────────────────────────────────┘
```

**Decisiones clave:**

- Un solo controller nuevo (`AdminImagenesController`) → cero impacto en los controllers existentes.
- Los services existentes reciben métodos nuevos; no se modifican los métodos en uso por las apps móviles.
- Las rutas `/admin/*` quedan cubiertas por el `JWTAuthorizationFilter` existente.

## 5. Backend en detalle

### 5.1 Endpoints

```
GET  /ccr-rest-api/admin/imagenes/listar
       Query params:
         brand            (jhonson|nestle|shell)   required
         anio             (4 dígitos)              required
         mes              (1-12)                   required
         codBoca                                   optional
         codDistribuidor                           optional, REQUIRED si brand=nestle
       Responses:
         200 → List<ImagenAdminDTO>
         400 → si falta brand/anio/mes, o brand inválido, o codDistribuidor falta para Nestle

POST /ccr-rest-api/admin/imagenes/rotar
       Body JSON: { brand: string, path: string }
       Responses:
         200 → { path, urlPublica }    (urlPublica con cache-buster)
         400 → si path inválido (traversal, prefijo inesperado, etc)
         404 → si el archivo no existe
         500 → si falló la rotación; el archivo original queda intacto
```

### 5.2 DTO nuevo

```java
public class ImagenAdminDTO {
    private String brand;            // "jhonson" | "nestle" | "shell"
    private String codBoca;          // ej "40"
    private String codDistribuidor;  // SOLO Nestle, null para los demás
    private String fileName;         // ej "40_2026_05_2.jpg"
    private String pathRelativo;     // ej "zoomin-jhonson/40/40_2026_05_2.jpg"
    private String urlPublica;       // URL completa para que el browser cargue la imagen
    private Integer anio;            // 2026 (puede ser null si el nombre no matchea)
    private Integer mes;             // 5    (idem)
}
```

### 5.3 Cambios en services existentes

**`ImagenesSCJService` (Jhonson)** — agregar:

```java
List<ImagenAdminDTO> findAllByMes(int anio, int mes, String codBocaOpcional);
void rotarImagen(String pathRelativo); // delega a ManejadorDeArchivos.rotateImage
```

- `findAllByMes` itera las carpetas de bocas bajo `zoomin-jhonson/` (o filtra una si vino `codBoca`), arma DTOs.
- `rotarImagen` valida que el path arranque con `zoomin-jhonson/` antes de delegar.

**`ImagenesNestService` (Nestle)** — agregar:

```java
List<ImagenAdminDTO> findAllByMes(int anio, int mes, String codDistribuidor, String codBocaOpcional);
void rotarImagen(String pathRelativo);
```

- `codDistribuidor` obligatorio (validado a nivel controller; aquí se asume no-null).
- Iterar bajo `zoomin-nestle/{codDistribuidor}/` (y filtrar boca si vino).
- `rotarImagen` valida prefijo `zoomin-nestle/`.

### 5.4 Service nuevo

**`ImagenesShellService` (CREAR)** — sigue el patrón de Jhonson:

```java
List<ImagenAdminDTO> findAllByMes(int anio, int mes, String codBocaOpcional);
void rotarImagen(String pathRelativo);
```

Estructura idéntica a Jhonson (Shell **no tiene** jerarquía de distribuidor). Carpeta base `zoomin-shell/{codBoca}/`. Property `path.directory.server_path_images_shell` ya existe en `application-*.properties`.

### 5.5 Seguridad y validación

**Anti path-traversal** en `/admin/imagenes/rotar`:

1. Rechazar paths que contengan `..`, `~`, o caracteres null.
2. Rechazar paths absolutos (que arranquen con `/`).
3. Rechazar paths que **no arranquen con la carpeta del brand** correspondiente.
4. Rechazar paths que no terminen en `.jpg`.
5. Loguear todos los rechazos a nivel WARN con el usuario JWT y el path solicitado.

**Autorización:** las rutas `/admin/*` ya están cubiertas por el `JWTAuthorizationFilter`. **Pregunta abierta:** ¿hay rol "admin" diferenciado de usuario normal? Si no existe el mecanismo de roles, agregarlo está fuera de scope; las rutas quedan accesibles a cualquier usuario autenticado (riesgo aceptado para este iteration; documentado en sección 9).

### 5.6 Cache busting

El backend genera `urlPublica` con query param `?v={timestamp}` en cada DTO. Después de rotar, el response incluye una `urlPublica` con un timestamp nuevo. Esto evita que el browser sirva la versión vieja del cache.

## 6. Frontend en detalle

### 6.1 Estructura de archivos

```
src/app/pages/admin/imagenes/
  ├── imagenes-admin.component.{ts,html,scss}
  ├── imagen-preview-modal/
  │   └── imagen-preview-modal.component.{ts,html,scss}
  └── services/
      └── imagenes-admin.service.ts
```

Registrar la ruta en `pages/admin/admin-routing.module.ts` como `imagenes`. Agregar item en el menú lateral (verificar layout actual de `pages/layouts/`).

### 6.2 `imagenes-admin.component`

**Layout:**

```
┌───────────────────────────────────────────────────┐
│  Filtros                                          │
│  [Brand ▾]  [Año ▾]  [Mes ▾]                      │
│  [Distribuidor ▾]*  [Boca ▾]**    [Buscar]        │
│  * Distribuidor solo aparece si brand=Nestle      │
│  ** Boca es opcional para todos los brands        │
└───────────────────────────────────────────────────┘
┌───────────────────────────────────────────────────┐
│  Resultados: N imágenes                           │
│  Grid responsive (4-6 columnas según viewport)    │
│  Thumbnails con loading="lazy"                    │
└───────────────────────────────────────────────────┘
```

**Comportamiento:**

- Form reactivo. `brand`, `anio`, `mes` obligatorios. `codDistribuidor` obligatorio si `brand === 'nestle'`.
- Cuando se selecciona brand:
  - Jhonson/Shell: ocultar dropdown de distribuidor; cargar bocas del brand.
  - Nestle: mostrar dropdown distribuidor; al elegir distribuidor, cargar bocas de ese distribuidor.
- Botón "Buscar" llama a `imagenes-admin.service.listar(filtro)`. Mientras está en curso: spinner.
- Resultado: grid de tiles con `<img loading="lazy" src="{urlPublica}">` + caption con `fileName`.
- Click en tile → abre `ImagenPreviewModalComponent` con el `ImagenAdminDTO`.

### 6.3 `imagen-preview-modal`

```
┌──────────────────────────────────────────────────┐
│  Boca 40 — 40_2026_05_2.jpg              [X]     │
├──────────────────────────────────────────────────┤
│             [   IMAGEN GRANDE   ]                │
├──────────────────────────────────────────────────┤
│              [ ↻ Rotar 90° CW ]                  │
└──────────────────────────────────────────────────┘
```

**Comportamiento:**

- Header con `codBoca` + `fileName` (en Nestle agregar `codDistribuidor`).
- Imagen grande centrada, max 80vh.
- Botón "Rotar 90° CW":
  - Disabled + spinner durante el request.
  - On success: actualizar el `src` con la nueva `urlPublica` (con cache-buster nuevo); el modal sigue abierto.
  - On error: snackbar rojo; modal sigue abierto para reintentar.
- Emite evento `onRotated(dto)` al padre para que refresque solo ese tile del grid (no toda la lista).

### 6.4 Service Angular

```typescript
@Injectable({ providedIn: 'root' })
export class ImagenesAdminService {
  listar(filtro: ImagenesFiltro): Observable<ImagenAdminDTO[]>;
  rotar(brand: string, path: string): Observable<{ path: string, urlPublica: string }>;
}
```

Usa el `HttpClient` con la base URL del proyecto (revisar cómo está configurada en `core/services/` para seguir el patrón).

### 6.5 Estados de UX

| Estado | Comportamiento |
|---|---|
| Sin búsqueda inicial | Mensaje "Aplicá filtros para ver imágenes." |
| Búsqueda en curso | Spinner sobre el grid |
| 0 resultados | "No hay imágenes con esos filtros." |
| Error al listar | Snackbar rojo "No se pudo cargar el listado." |
| Rotación en curso | Botón del modal deshabilitado + spinner |
| Rotación exitosa | Snackbar verde "Imagen rotada." |
| Rotación falló | Snackbar rojo, modal abierto |
| Thumbnail no carga | Placeholder + nombre de archivo |

## 7. Error handling y casos borde

### 7.1 Backend

| Caso | Comportamiento |
|---|---|
| `brand` inválido | 400 + `"brand no soportado"` |
| Nestle sin `codDistribuidor` | 400 + `"codDistribuidor es obligatorio para Nestle"` |
| `anio` o `mes` mal formados | 400 + mensaje específico |
| Carpeta del brand/boca no existe en disco | `[]` (vacío), no error |
| Path con `..`, `~`, prefijo inesperado o no `.jpg` | 400 + log WARN con usuario y path |
| Archivo no existe al rotar | 404 |
| Falla la lectura/rotación (ImageIO) | 500; archivo original queda intacto |
| Falla la escritura (permisos, disco) | 500; archivo original queda intacto |

### 7.2 Frontend

| Caso | Comportamiento |
|---|---|
| Doble click rápido en "Rotar" | Botón deshabilitado durante el request → no duplica |
| Pierde sesión durante una operación | Interceptor existente redirige a login (verificar que exista) |
| Navegación con request en vuelo | `takeUntil(destroy$)` cancela |

### 7.3 Casos borde de datos

- **Archivos con nombres "raros"** (no matchean `{codBoca}_{anio}_{mes}_{nro}.jpg`): se listan igual, los campos `anio`/`mes`/`codBoca` pueden venir `null`.
- **Archivo borrado entre listar y rotar**: 404 en el rotar, el frontend muestra error y opcionalmente refresca el listado.

## 8. Plan de migración / impacto en código existente

**Cero migración de datos.** Los archivos viejos siguen en disco como están; el admin podrá arreglarlos uno a uno con el nuevo módulo.

**Cero cambios disruptivos en código existente:**

- Los endpoints actuales de imágenes (`/jhonson/imagenes/...`, equivalentes Nestle) siguen funcionando exactamente igual; las apps móviles no se ven afectadas.
- Los métodos existentes de `ImagenesSCJService` y `ImagenesNestService` no se modifican — solo se suman dos métodos por service.
- `ManejadorDeArchivos.rotateImage(String)` se reutiliza sin modificación.

## 9. Open questions / riesgos asumidos

1. **Rol "admin" en JWT:** no se verificó si existe diferenciación de roles. Asumimos que todo usuario autenticado puede acceder a `/admin/*`. Si esto no es aceptable, definir un mecanismo de roles es scope aparte.
2. **Convención de nombres en Nestle y Shell:** asumimos misma convención que Jhonson (`{codBoca}_{anio}_{mes}_{nro}.jpg`). Verificar al implementar examinando archivos reales del filesystem.
3. **Cantidad de fotos por filtro:** un mes de Jhonson puede tener 500+ fotos. Lazy-loading de `<img>` debería alcanzar; si la performance es problema, agregar paginación server-side es scope aparte.
4. **Auditoría:** no se registra quién rotó qué. Si el negocio lo necesita, agregar tabla `auditoria_rotaciones` es scope aparte.

## 10. Resumen de archivos a tocar/crear

### Crear

- `ccr-rest/src/main/java/py/com/jaimeferreira/ccr/commons/controller/AdminImagenesController.java` (o ubicación equivalente)
- `ccr-rest/src/main/java/py/com/jaimeferreira/ccr/commons/dto/ImagenAdminDTO.java`
- `ccr-rest/src/main/java/py/com/jaimeferreira/ccr/shell/service/ImagenesShellService.java`
- `d-insights-ccr/src/app/pages/admin/imagenes/imagenes-admin.component.{ts,html,scss}`
- `d-insights-ccr/src/app/pages/admin/imagenes/imagen-preview-modal/imagen-preview-modal.component.{ts,html,scss}`
- `d-insights-ccr/src/app/pages/admin/imagenes/services/imagenes-admin.service.ts`

### Modificar

- `ccr-rest/src/main/java/py/com/jaimeferreira/ccr/jhonson/service/ImagenesSCJService.java` (agregar 2 métodos)
- `ccr-rest/src/main/java/py/com/jaimeferreira/ccr/nestle/service/ImagenesNestService.java` (agregar 2 métodos)
- `d-insights-ccr/src/app/pages/admin/admin-routing.module.ts` (nueva ruta)
- `d-insights-ccr/src/app/pages/admin/admin.module.ts` (registrar componentes)
- Menú lateral (path por verificar) — agregar item "Rotar Imágenes"
