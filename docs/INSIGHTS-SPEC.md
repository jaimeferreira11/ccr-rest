# d-insights - Especificacion Funcional

Documento de referencia para cualquier IA o desarrollador que trabaje sobre el modulo Insights.

**Producto:** d-insights - Generador de Informes Gerenciales  
**Backend:** `ccr-rest` (este repositorio), paquete `py.com.jaimeferreira.ccr.insights`  
**Frontend:** `/development/workspace-angular/d-insights-ccr` (Angular)  
**Base de datos:** PostgreSQL, schema `ccr`  
**API base path:** `insights/api/v1`

---

## 1. Que es d-insights

d-insights es una plataforma interna que genera informes gerenciales en formato Excel (.xlsx).
El usuario sube archivos CSV con datos de mercado y filtros, selecciona un cliente y una categoria,
y el sistema genera un Excel con tablas dinamicas, hojas ocultas de datos y graficos preconfigurados.

El Excel resultante contiene:
- **Hojas visibles:** INICIO, Reales, Acumulados, Variaciones, Distribucion Fisica, Distribucion Ponderada, Precios PP, Evol Segmentos, Evol Canales (todas con pivot tables y slicers)
- **Hojas ocultas (datos):** FACT, Total Empresa, Calendario, DIM

---

## 2. Tipos de informe

Existen dos tipos, definidos en `TipoReporte` (enum). Cada tipo tiene su propio template Excel
y el CSV de datos tiene **distinta cantidad y orden de columnas**:

| Tipo | Template por defecto | Columnas CSV datos |
|------|---------------------|-------------------|
| **NORMAL** | `template_normal.xlsx` | 13 columnas (indices 0-12) |
| **CADENA** | `template_cadena.xlsx` | 14 columnas (indices 0-13) |

### Headers del CSV de datos por tipo

**NORMAL (13 columnas):**
```
Categoría | Apertura | Empresa | Marca | Segmento | Mes | Año | Distribución Física | Distribución Ponderada | Facturación | Precio | Volumen | Volumen Unidades
```

**CADENA (14 columnas):**
```
Categoría | Apertura Geográfica | Empresa | Marca | Variedad | Segmento | Distribución Física | Distribución Ponderada | Mes | Año | Facturación | Precio | Volumen | Volumen Unidades
```

**Diferencias clave entre NORMAL y CADENA:**
- CADENA incluye la columna `Variedad` (indice 4) que no existe en NORMAL
- El orden de las columnas numéricas difiere: en NORMAL `Mes` y `Año` van antes de los valores; en CADENA van después de `Distribución Ponderada`
- CADENA usa `Apertura Geográfica` como header (vs `Apertura` en NORMAL)
- El CSV de filtros tiene la **misma estructura** para ambos tipos

Ambos tipos generan el mismo resultado funcional en el Excel: hojas FACT y Total Empresa con la
misma estructura de columnas destino. La diferencia es solo el **mapeo de indices CSV → columnas Excel**.

---

## 3. Estructura de los CSVs

### 3.1 CSV de datos

Separador: `;` (punto y coma). Encoding: auto-detectado (ver seccion 10).
Los decimales usan coma: `21,74`.

#### Tipo NORMAL (13 columnas, indices 0-12)

| Indice | Nombre header | Descripcion |
|--------|--------------|-------------|
| 0 | Categoría | Categoria del producto (ej: Arroz, Panificados) |
| 1 | Apertura | Apertura geografica (ej: TOTAL PARAGUAY, AYGAS AUTOSERVICIOS) |
| 2 | Empresa | Nombre de la empresa |
| 3 | Marca | Marca del producto |
| 4 | Segmento | Segmento del producto (ej: ARROZ - DE 251 A 500G, TOT.PROD.) |
| 5 | Mes | Numero de mes (1-12) |
| 6 | Año | Ano (ej: 2024) |
| 7 | Distribución Física | Valor numerico decimal |
| 8 | Distribución Ponderada | Valor numerico decimal |
| 9 | Facturación | Valor numerico decimal |
| 10 | Precio | Valor numerico decimal (NO se usa en las hojas) |
| 11 | Volumen | Valor numerico decimal |
| 12 | Volumen Unidades | Valor numerico decimal |

**Ejemplo:**
```
Categoría;Apertura;Empresa;Marca;Segmento;Mes;Año;Distribución Física;Distribución Ponderada;Facturación;Precio;Volumen;Volumen Unidades
Arroz;AYGAS AUTOSERVICIOS;APOLO IMPORT;EL PAIS;ARROZ - DE 251 A 500G;1;2022;21,74;31,75;4697,759;7064;0,665;1,308
```

#### Tipo CADENA (14 columnas, indices 0-13)

| Indice | Nombre header | Descripcion |
|--------|--------------|-------------|
| 0 | Categoría | Categoria del producto |
| 1 | Apertura Geográfica | Apertura geografica |
| 2 | Empresa | Nombre de la empresa |
| 3 | Marca | Marca del producto |
| 4 | Variedad | Variedad del producto (columna exclusiva de CADENA) |
| 5 | Segmento | Segmento del producto |
| 6 | Distribución Física | Valor numerico decimal |
| 7 | Distribución Ponderada | Valor numerico decimal |
| 8 | Mes | Numero de mes (1-12) |
| 9 | Año | Ano (ej: 2024) |
| 10 | Facturación | Valor numerico decimal |
| 11 | Precio | Valor numerico decimal (NO se usa en las hojas) |
| 12 | Volumen | Valor numerico decimal |
| 13 | Volumen Unidades | Valor numerico decimal |

**Ejemplo:**
```
Categoría;Apertura Geográfica;Empresa;Marca;Variedad;Segmento;Distribución Física;Distribución Ponderada;Mes;Año;Facturación;Precio;Volumen;Volumen Unidades
Arroz;TOTAL PARAGUAY;APOLO IMPORT;EL PAIS;GRANO LARGO;ARROZ - DE 251 A 500G;21,74;31,75;1;2022;4697,759;7064;0,665;1,308
```

**Nota importante:** El codigo en `ReporteInsService` usa constantes de indice (CSV_CATEGORIA=0,
CSV_APERTURA=1, CSV_MES=5, etc.) que corresponden al tipo NORMAL. Para CADENA, los mismos indices
apuntan a columnas diferentes debido al orden distinto. La columna extra `CSV[13]` se mapea a
FACT col 17 y Total Empresa col 15.

### 3.2 CSV de filtros

Separador: `;`. Mismo encoding que el CSV de datos.

**Columnas:**

| Nombre header | Presente en datos? | Proposito |
|---------------|-------------------|-----------|
| CLIENTE | NO | Informativo, nombre del cliente |
| PAIS | NO | Informativo, pais del cliente |
| CATEGORIA | NO | Matching contra columna `Categoria` del CSV de datos |
| APERTURA | SI | Matching contra columna `Apertura` del CSV de datos |
| SEGMENTO | SI | Matching contra columna `Segmento` del CSV de datos |
| AGR_SEGM | NO | **Valor de enriquecimiento**: Agrupador de Segmento para el Excel |
| YTD 1er Mes | NO | Informativo (el YTD real se deriva del mes=1 en el codigo) |
| Orden_Apertura | NO | **Valor de enriquecimiento**: orden numerico de la apertura para el Excel |
| *(solo CADENA)* | NO | Columna extra para tipo CADENA |

**Importante — Doble proposito del CSV de filtros:**

1. **Filtrado de filas:** solo las columnas que existen en el CSV de datos (CATEGORIA, APERTURA, SEGMENTO) se usan para hacer match. Las claves que no existen en el CSV de datos se ignoran.

2. **Enriquecimiento del Excel:** las columnas AGR_SEGM y Orden_Apertura se usan para construir mapas de lookup:
   - `APERTURA -> Orden_Apertura` (ej: TOTAL PARAGUAY -> 1, TOT.PY.MIN. -> 2)
   - `SEGMENTO -> AGR_SEGM` (ej: ARROZ - DE 251 A 500G -> POR GRAMAJE, TOT.PROD. -> Tot.Prod.)

**Logica de matching:** OR entre filas del filtro, AND entre columnas no vacias de cada fila.
Las celdas vacias en el filtro significan "cualquier valor". Ejemplo: una fila con solo APERTURA=NORTE
acepta cualquier segmento para esa apertura.

**Ejemplo:**
```
CLIENTE;PAIS;CATEGORIA;APERTURA;SEGMENTO;AGR_SEGM;YTD 1er Mes;Orden_Apertura
A.J.Vierci;Paraguay;Arroz;AYGAS AUTOSERVICIOS;ARROZ - DE 251 A 500G;POR GRAMAJE;1;6
;;;AYGAS DESPENSAS;ARROZ - DE 501 A 1000G;POR GRAMAJE;;7
;;;AYGAS MIN;Tot.Prod.;Tot.Prod.;;4
;;;NORTE;;;;10
;;;TOTAL PARAGUAY;;;;1
```

### 3.3 Comparacion de headers entre filtro y datos

La comparacion de headers es **case-insensitive** y **accent-insensitive** (normalizada via NFD + strip diacriticos + lowercase). Asi, `Categoria` = `CATEGORIA` = `CATEGORiA`.

---

## 4. Estructura del Excel generado

### 4.1 Hoja FACT (oculta)

Fuente de datos principal para los pivot tables. 17 columnas (18 si CADENA):

| Col | Header | Origen |
|-----|--------|--------|
| 0 | Distribucion Fisica | CSV datos[7] |
| 1 | Distribucion Ponderada | CSV datos[8] |
| 2 | Facturacion | CSV datos[9] |
| 3 | Volumen | CSV datos[11] |
| 4 | Volumen Unidades | CSV datos[12] |
| 5 | Apertura Geografica | CSV datos[1] |
| 6 | Categoria | CSV datos[0] |
| 7 | CLIENTE | Label del cliente (de la BD) |
| 8 | Empresa | CSV datos[2] |
| 9 | hash | Vacio (reservado) |
| 10 | Marca | CSV datos[3] |
| 11 | PAIS | Pais del cliente (de la BD) |
| 12 | Segmento | CSV datos[4] |
| 13 | Agrupador Segmento | **Lookup:** filtro SEGMENTO -> AGR_SEGM. Fallback: TOT.PROD. si segmento es TOT.PROD., sino POR TIPO |
| 14 | Orden Apertura | **Lookup:** filtro APERTURA -> Orden_Apertura. Fallback: "0" |
| 15 | YTD 1er Mes | "1" si mes=1 (enero), vacio en caso contrario |
| 16 | Fecha | Date Excel: 1ro del mes (LocalDate(ano, mes, 1)) |
| 17 | *(solo CADENA)* | CSV datos[13] |

Nombre de tabla Excel: `FACT`. Rango actualizado a A1:Q{n+1} (NORMAL) o A1:R{n+1} (CADENA).

### 4.2 Hoja Total Empresa (oculta)

15 columnas (16 si CADENA):

| Col | Header | Origen |
|-----|--------|--------|
| 0 | Distribucion Fisica | CSV datos[7] |
| 1 | Distribucion Ponderada | CSV datos[8] |
| 2 | Apertura Geografica | CSV datos[1] |
| 3 | Categoria | CSV datos[0] |
| 4 | CLIENTE | Label del cliente (de la BD) |
| 5 | Empresa | CSV datos[2] |
| 6 | hash | Vacio |
| 7 | PAIS | Pais del cliente (de la BD) |
| 8 | Segmento | CSV datos[4] |
| 9 | Agrupador Segmento | **Lookup desde filtro** (igual que FACT col 13) |
| 10 | Volumen Unidades | CSV datos[12] |
| 11 | YTD 1er Mes | "1" si mes=1, vacio en caso contrario |
| 12 | Orden Apertura | **Lookup desde filtro** (igual que FACT col 14) |
| 13 | Fecha | Date Excel |
| 14 | Marca | CSV datos[3] |
| 15 | *(solo CADENA)* | CSV datos[13] |

Nombre de tabla Excel: `Total_Empresa`. Rango: A1:O{n+1} (NORMAL) o A1:P{n+1} (CADENA).

### 4.3 Hoja Calendario (oculta)

Generada automaticamente a partir de los pares (Ano, Mes) unicos del CSV de datos.

| Col | Header | Valor |
|-----|--------|-------|
| 0 | Fecha | Date Excel (1ro del mes) |
| 1 | Mes Numero | Entero 1-12 |
| 2 | Ano | Entero (ej: 2024) |
| 3 | Mes | Nombre en espanol (enero, febrero...) |

Nombre de tabla Excel: `Calendario`. Rango: A1:D{n+1}.

---

## 5. Flujo de generacion

```
1. Usuario selecciona: Pais -> Cliente -> Categoria -> Tipo (NORMAL/CADENA)
2. Usuario sube: CSV datos (obligatorio) + CSV filtros (opcional)
3. Backend (POST /reportes/generar):
   a. Valida parametros y archivos
   b. Crea registro InformeIns con estado PROCESANDO
   c. Retorna 202 Accepted con el informe
   d. Lanza procesamiento asincrono (@Async)

4. Procesamiento asincrono (ReporteInsService.procesarReporte):
   a. Obtiene datos del cliente y pais desde la BD
   b. Resuelve filtros (prioridad: usuario > filtros_base.csv del cliente > classpath)
   c. Construye mapas de lookup: APERTURA->Orden, SEGMENTO->AGR_SEGM
   d. Lee datos_base.csv del cliente (si existe) y concatena con CSV del usuario
   e. Filtra filas del CSV concatenado segun reglas del filtro
   f. Resuelve template Excel (prioridad: cliente+cat > cliente > default disco > classpath)
   g. Desconecta tablas Power Query del template
   h. Limpia hojas FACT y Total Empresa del template
   i. Pobla Calendario, FACT y Total Empresa
   j. Actualiza rangos de tablas Excel
   k. Oculta hojas de datos, marca recalculo al abrir
   l. Guarda .xlsx en disco
   m. Guarda CSV concatenado como nuevo datos_base.csv del cliente (con backup)
   n. Actualiza InformeIns a COMPLETADO (o ERROR si falla)

5. Frontend hace polling en GET /reportes/{id} hasta estado != PROCESANDO
6. Usuario descarga con GET /reportes/{id}/descargar
```

---

## 6. Archivos en disco por cliente

Directorio: `{path.directory.server}/{path.directory.server_path_clientes_insights}/{COD_CLIENTE}/`

| Archivo | Descripcion |
|---------|-------------|
| `filtros_base.csv` | Filtros base subidos por administracion (fallback si el usuario no sube filtro) |
| `datos_base.csv` | CSV acumulativo: cada generacion exitosa concatena los datos nuevos al base existente |
| `datos_base_backup.csv` | Backup automatico del datos_base.csv antes de cada sobreescritura |

Templates en: `{path.directory.server}/{path.directory.server_path_templates_insights}/`

Nomenclatura de templates:
- `template_{tipo}_{CODCLIENTE}_{CODCATEGORIA}.xlsx` — especifico cliente+categoria (maxima prioridad)
- `template_{tipo}_{CODCLIENTE}.xlsx` — especifico cliente
- `template_{tipo}.xlsx` — default en disco
- `resources/insights/template_{tipo}.xlsx` — fallback classpath

---

## 7. Modelo de datos (schema ccr)

### Tabla `informe`
Registro de cada informe generado. Campos clave:
- `cod_cliente`, `cod_categoria`, `tipo_reporte` (NORMAL/CADENA)
- `estado` (PROCESANDO/COMPLETADO/ERROR)
- `nombre_archivo` (nombre del .xlsx generado)
- `mensaje_error` (detalle si estado=ERROR)
- `duracion_segundos`
- Campos de auditoria: `fecha_creacion`, `nombre_usuario_creacion`

### Tabla `categorias`
Cada cliente tiene N categorias. La categoria determina que template se usa.
- `codigo` (ej: ARROZ), `descripcion`, `cod_cliente` (FK), `enabled`

### Otras tablas
- `pais` — catalogo de paises (codigo, descripcion)
- `cliente_ins` — clientes de insights (codigo, descripcion, cod_pais FK)
- `plataforma_config` — flag de mantenimiento/suspension de la plataforma

---

## 8. API endpoints

### Endpoints de usuario (`insights/api/v1`)

| Metodo | Path | Descripcion |
|--------|------|-------------|
| GET | `/paises` | Lista paises activos |
| GET | `/clientes/{codPais}` | Lista clientes de un pais |
| GET | `/categorias/{codCliente}` | Lista categorias activas de un cliente |
| POST | `/reportes/generar` | Genera informe (multipart: csvData, csvFiltros?, codCliente, codCategoria, tipoReporte) |
| GET | `/reportes` | Ultimos 10 informes del usuario (filtro por estado opcional) |
| GET | `/reportes/{id}` | Estado/detalle de un informe |
| GET | `/reportes/{id}/descargar` | Descarga el .xlsx si estado=COMPLETADO |

### Endpoints de administracion (`insights/api/v1/admin`)

| Metodo | Path | Descripcion |
|--------|------|-------------|
| GET/PUT | `/plataforma` | Ver/cambiar estado de la plataforma |
| CRUD | `/paises`, `/clientes`, `/categorias` | ABM de paises, clientes y categorias |
| POST | `/clientes/{codCliente}/archivos-base` | Subir template, filtros_base.csv, datos_base.csv |

---

## 9. Reglas de negocio importantes

1. **Todos los codigos se normalizan a UPPERCASE** (codCliente, codCategoria, etc.)
2. **La comparacion de filtros vs headers es case-insensitive y accent-insensitive** (NFD + strip diacriticos + lowercase)
3. **datos_base.csv es acumulativo**: cada informe exitoso concatena sus datos al base. Si los datos se corrompen, se puede restaurar desde `datos_base_backup.csv`
4. **Las tablas del template se desconectan de Power Query** antes de poblar (evita error DataSource.NotFound al abrir)
5. **Las pivot tables necesitan recalculo manual** al abrir el Excel (ForceFormulaRecalculation=true, pero el refresh de pivots lo hace Excel al abrir)
6. **El scheduler InformeCleanupScheduler** limpia archivos Excel antiguos del disco periodicamente

---

## 10. Encoding de los CSVs

El encoding se auto-detecta en `ReporteInsService.detectarEncoding()` con la siguiente prioridad:

1. **UTF-8 con BOM** (bytes iniciales `EF BB BF`) → UTF-8
2. **UTF-8 sin BOM** (secuencias multibyte validas, ej: `C3 AD` = í) → UTF-8
3. **MacRoman** (bytes 0x80-0x9F aparecen DENTRO de palabras, entre letras ASCII; ej: `r[0x92]a` = "ría") → MacRoman
4. **cp1252** (fallback) → Windows-1252

Esta heuristica funciona porque:
- En **MacRoman** (Mac), los acentos (á=0x87, é=0x8E, í=0x92, ó=0x97, ú=0x9C, ñ=0x96) usan bytes 0x80-0x9F y siempre aparecen dentro de palabras.
- En **cp1252** (Windows), esos mismos bytes son comillas tipograficas y guiones que aparecen entre palabras, no dentro. Los acentos en cp1252 estan en 0xE0-0xFF.
- En **UTF-8**, los acentos usan 2 bytes (ej: í = `0xC3 0xAD`) que son auto-validantes.

El Excel de salida (.xlsx) siempre es UTF-8 internamente (formato OOXML/XML) — no tiene problema de encoding.
