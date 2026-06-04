# Auditoría del módulo Insights — Parte 2 (consulta + pantalla) — Diseño

**Fecha:** 2026-06-04
**Autor:** Jaime Ferreira
**Estado:** Aprobado
**Depende de:** Parte 1 (`2026-06-04-auditoria-insights-design.md`, ya en `main`)

## Objetivo

Exponer la auditoría del módulo Insights (tabla `ccr.auditoria_insights`, poblada en la Parte 1) y mostrarla en una pantalla de administración **Administración → Auditoría**, paginada y filtrable **por evento** y **por cliente**.

Abarca dos repos:
- **Backend:** `ccr-rest` — endpoints de consulta.
- **Frontend:** `d-insights-ccr` (Angular 18) — pantalla de listado.

## Decisiones

- **Paginación/filtrado server-side**: el endpoint pagina y filtra en la BD (espeja `listarInformes` / `InformePageDTO`). Una auditoría crece sin límite; descargarla entera al navegador no escala.
- **Detalle (JSON)**: se muestra con un botón **“Ver”** que abre un modal (SweetAlert2) con el JSON formateado, para mantener la tabla limpia.
- **Filtros**: sólo **evento** y **cliente** (sin rango de fechas en esta etapa).
- **Controller dedicado**: `AuditoriaInsController`, para no agrandar el ya extenso `AdminPlataformaController`.
- **Catálogo de eventos vía endpoint**: el enum `EventoAuditoriaIns` es la fuente de verdad; un endpoint expone `{codigo, descripcion}` para poblar el dropdown (evita hardcodear los valores/labels en el front).

## Backend (ccr-rest)

### Endpoints

Nuevo controller `py.com.jaimeferreira.ccr.insights.admin.controller.AuditoriaInsController`,
`@RequestMapping("/insights/api/v1/admin/auditoria")`. Misma autenticación JWT que el resto del surface admin (sin anotación de rol extra; igual que `AdminPlataformaController`).

| Método | Ruta | Params | Respuesta |
|---|---|---|---|
| GET | `/insights/api/v1/admin/auditoria` | `evento` (opcional), `codCliente` (opcional), `page` (def 0), `size` (def 10) | `AuditoriaPageDTO` |
| GET | `/insights/api/v1/admin/auditoria/eventos` | — | `List<EventoCatalogoDTO>` |

- El listado se ordena por `fecha_hora` desc.
- `evento` llega como String; se parsea a `EventoAuditoriaIns`. Si es inválido/no vacío y no matchea, se lanza `UnknownResourceException` (mismo criterio que `estado` en `listarInformes`). Si es null/vacío, no filtra por evento.
- `codCliente` se normaliza con `trim().toUpperCase()` antes de filtrar (consistente con el resto del módulo).

### DTOs

`py.com.jaimeferreira.ccr.insights.dto`:

- **`AuditoriaDTO`**: `id`, `evento` (código), `eventoDescripcion` (label del enum), `resultado`, `usuario`, `codCliente`, `codCategoria`, `tipoReporte`, `fechaHora`, `detalle`. Factory `from(AuditoriaIns)`.
- **`AuditoriaPageDTO`**: `content: List<AuditoriaDTO>`, `totalElements: long`. Mismo shape que `InformePageDTO`.
- **`EventoCatalogoDTO`**: `codigo`, `descripcion`. Factory `from(EventoAuditoriaIns)`.

### Repository

Agregar a `AuditoriaInsRepository` (mismo estilo derivado que `InformeInsRepository`):

```java
List<AuditoriaIns> findAllByOrderByFechaHoraDesc(Pageable pageable);
List<AuditoriaIns> findByEventoOrderByFechaHoraDesc(EventoAuditoriaIns evento, Pageable pageable);
List<AuditoriaIns> findByCodClienteOrderByFechaHoraDesc(String codCliente, Pageable pageable);
List<AuditoriaIns> findByEventoAndCodClienteOrderByFechaHoraDesc(EventoAuditoriaIns evento, String codCliente, Pageable pageable);

long countByEvento(EventoAuditoriaIns evento);
long countByCodCliente(String codCliente);
long countByEventoAndCodCliente(EventoAuditoriaIns evento, String codCliente);
// count() ya viene de JpaRepository
```

### Servicio

En `AuditoriaInsService` (donde ya vive `registrar`):

- `AuditoriaPageDTO listar(EventoAuditoriaIns evento, String codCliente, int page, int size)` — ramifica por combinación de filtros (sin filtro / evento / cliente / ambos) usando `PageRequest.of(page, size)`, igual que `InformeInsService.findUltimos`. Mapea a `AuditoriaDTO`.
- `List<EventoCatalogoDTO> listarEventos()` — `EventoAuditoriaIns.values()` mapeado a `EventoCatalogoDTO`.

## Frontend (d-insights-ccr, Angular 18)

### Modelos

`src/app/models/auditoria.interface.ts`:

```typescript
export interface IAuditoria {
  id: number;
  evento: string;
  eventoDescripcion: string;
  resultado: 'EXITO' | 'ERROR';
  usuario: string;
  codCliente: string;
  codCategoria?: string;
  tipoReporte?: string;
  fechaHora: string;
  detalle?: string; // JSON string
}
export interface IAuditoriaPage { content: IAuditoria[]; totalElements: number; }
export interface IEventoCatalogo { codigo: string; descripcion: string; }
```

### Servicio

En `InsightsService` (`core/services/insights.service.ts`):

- `getAdminAuditoria(filtros: { evento?: string; codCliente?: string; page: number; size: number }): Observable<IAuditoriaPage>` → GET `${ADMIN_URL}/auditoria` con query params (omitiendo los vacíos).
- `getAdminAuditoriaEventos(): Observable<IEventoCatalogo[]>` → GET `${ADMIN_URL}/auditoria/eventos`.

El `AuthInterceptor` adjunta el JWT automáticamente; no se toca.

### Componente

`src/app/pages/admin/auditoria/auditoria-admin.component.{ts,html,scss}` (standalone), mirando `clientes-admin.component` pero **server-side**:

- Estado: `eventos: IEventoCatalogo[]`, `clientes: ICliente[]`, `eventoFiltro`, `clienteFiltro`, `paginaActual` (1-based en UI, 0-based al backend), `tamanioPagina`, `items: IAuditoria[]`, `totalElements`.
- En `OnInit`: carga catálogo de eventos + clientes (para los dropdowns) y la primera página.
- `onFiltrosChange()` → resetea a página 1 y recarga. `cambiarPagina(n)` → recarga esa página.
- Tabla: **fecha-hora, evento (descripción), resultado (badge: verde EXITO / rojo ERROR), usuario, cliente, categoría, tipo, [Ver]**.
- Filtros: `ng-select` de evento (bindLabel `descripcion`, bindValue `codigo`) y de cliente (bindLabel `descripcion`, bindValue `codigo`), ambos clearable.
- Botón **“Ver”**: abre `Swal` con el `detalle` parseado y formateado (`JSON.stringify(JSON.parse(detalle), null, 2)` dentro de `<pre>`); si `detalle` es vacío, muestra “Sin detalle”.
- Paginación: misma UI de botones que `clientes`, pero `totalPaginas = Math.ceil(totalElements / tamanioPagina)` con datos del server.

### Ruta

En `admin-routing.module.ts`: `{ path: "auditoria", component: AuditoriaAdminComponent }` → ruta final `/admin/auditoria` (el módulo admin se carga lazy bajo `AdminRoleGuard`).

### Menú

En `pages/layouts/sidebar/menu.ts`: ítem **“Auditoría”** con `link: "/admin/auditoria"`, dentro del grupo **Administración** (`adminOnly: true`), junto a los demás ítems admin existentes. Etiqueta i18n.

### i18n

Agregar las claves de etiqueta (menú y, si aplica, títulos de pantalla) a `src/assets/i18n/es.json` y a los demás locales presentes en `assets/i18n/`.

## Manejo de errores

- Backend: filtros inválidos → `UnknownResourceException` (manejo global existente). Sin resultados → página vacía (`content: []`, `totalElements: 0`).
- Frontend: errores HTTP → toast de error (patrón `BaseComponent`/`showToastError`); loader durante las cargas (`showLoader`/`hideLoader`).

## Pruebas

Sin tests automatizados (ver CLAUDE.md). Verificación manual:

**Backend:**
1. `GET /insights/api/v1/admin/auditoria` sin filtros → primera página ordenada por fecha desc, `totalElements` correcto.
2. Con `?evento=TEMPLATE_SUBIDO`, con `?codCliente=NESTLE`, y ambos combinados → resultados filtrados y conteos correctos.
3. `?evento=XXX` inválido → error 404/contrato existente.
4. `GET .../auditoria/eventos` → los 4 eventos con descripción.

**Frontend:**
1. `Administración → Auditoría` aparece en el menú (sólo admin) y carga la tabla.
2. Filtrar por evento y por cliente recarga desde el backend y pagina bien.
3. “Ver” abre el modal con el JSON formateado.
4. `ng build` sin errores.

## Plan de despliegue

1. Backend: desplegar (no requiere DDL; la tabla ya existe de la Parte 1).
2. Frontend: build + deploy de `d-insights-ccr`.
