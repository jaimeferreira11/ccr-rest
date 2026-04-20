# Diseño: Selección obligatoria de Categoría en la generación de informes

**Fecha:** 2026-04-20  
**Proyecto:** ccr-rest (backend) + d-insights-ccr (frontend)

---

## Problema

Al generar un informe Insights, el template Excel a usar depende del tipo de reporte (NORMAL o CADENA) y del cliente. Sin embargo, cada cliente puede tener múltiples categorías, y el template correcto varía por categoría. Actualmente no se solicita al usuario que seleccione una categoría, por lo que no se puede resolver el template correcto.

## Solución propuesta

Agregar `codCategoria` como parámetro **obligatorio** en el endpoint de generación de informes. El código de categoría determina cuál template Excel se usa, y se almacena como metadato en el registro del informe.

---

## Cambios de backend (`ccr-rest`)

### 1. Nueva nomenclatura de templates

**Antes:** `template_{tipoReporte}_{codCliente}.xlsx`  
**Después:** `template_{tipoReporte}_{codCliente}_{codCategoria}.xlsx`

Ejemplo: `template_normal_VIERCI_BEBIDAS.xlsx`

Afecta a:
- `TipoReporte.getTemplateFileName(codCliente)` → nuevo método `getTemplateFileName(codCliente, codCategoria)`
- `TemplateInsService.buildNombreArchivo()` y `guardarTemplate()` → reciben `codCategoria`
- `ReporteInsService.resolverTemplateStream()` → pasa `codCategoria`

### 2. Endpoint `POST /insights/api/v1/reportes/generar`

Agrega `@RequestParam("codCategoria") String codCategoria` como campo obligatorio del multipart (junto con `codCliente` y `tipoReporte`). Se valida que no esté vacío.

### 3. Propagación en capa de servicio

- `ReporteInsService.iniciarGeneracion(...)` → recibe `codCategoria`
- `ReporteInsService.procesarReporte(...)` → recibe `codCategoria`, lo usa en `resolverTemplateStream`

### 4. Entidad `InformeIns`

Agrega campo `codCategoria` (`VARCHAR(50) NOT NULL`). Script de migración SQL requerido:

```sql
ALTER TABLE ccr.informe ADD COLUMN cod_categoria VARCHAR(50) NOT NULL DEFAULT '';
```

### 5. Nuevo endpoint público de categorías

`GET /insights/api/v1/categorias/{codCliente}` — retorna las categorías activas del cliente dado. No requiere permisos de admin. Implementado en `InsightsController`.

### 6. Admin — subida de templates

`POST /insights/api/v1/admin/clientes/{codCliente}/archivos-base` agrega `@RequestParam(value = "codCategoria", required = false) String codCategoria`. Este campo es **obligatorio cuando se sube un template**. Afecta a `TemplateInsService.guardarTemplate()`.

---

## Cambios de frontend (`d-insights-ccr`)

### 1. `InsightsService`

Nuevo método:
```ts
getCategoriasByCliente(codCliente: string): Observable<ICategoria[]>
// GET /insights/api/v1/categorias/{codCliente}
```

### 2. `RegistroComponent`

- Agrega campo `categoria` al `FormGroup` con `Validators.required`.
- Agrega propiedad `categorias: ICategoria[]`.
- Al seleccionar cliente (`onPaisChange` → luego al seleccionar cliente): llama `getCategoriasByCliente(codCliente)` para cargar las categorías. Si el cliente cambia, resetea la categoría.
- Nuevo método `onClienteChange(codCliente)` que carga categorías.
- En `generar()`: incluye `codCategoria` en el `FormData`.
- Template HTML: agrega `ng-select` para Categoría entre los campos Cliente y Tipo Gerencial.

### 3. Admin — `subir-archivos-base.component`

- Agrega propiedad `categorias: ICategoria[]`.
- Cuando se selecciona un cliente, carga sus categorías con `getCategoriasByCliente`.
- Agrega campo de selección de categoría en el formulario, obligatorio si se sube template.
- Envía `codCategoria` en el `FormData` cuando hay template.

---

## Flujo de datos

```
[Usuario selecciona País]
        ↓ carga clientes
[Usuario selecciona Cliente]
        ↓ carga categorías del cliente
[Usuario selecciona Categoría] ← NUEVO (obligatorio)
[Usuario selecciona Tipo Gerencial]
[Usuario adjunta CSV de datos]
        ↓ POST /reportes/generar
        { codCliente, codCategoria, tipoReporte, csvData, [csvFiltros] }
        ↓ ReporteInsService.iniciarGeneracion
        ↓ resolverTemplateStream → template_normal_VIERCI_BEBIDAS.xlsx
        ↓ genera Excel y guarda en BD con cod_categoria
```

---

## Consideraciones

- La migración SQL debe aplicarse manualmente antes del despliegue.
- Si un cliente no tiene categorías activas, se mostrará mensaje de advertencia en el frontend.
- El cambio de nomenclatura de templates no afecta templates ya subidos (nombre anterior). Se recomienda re-subir los templates con la nueva nomenclatura desde el admin.
