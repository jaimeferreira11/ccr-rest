# Generación del Reporte Gerencial (Insights) — Spec técnico

> Cómo `ReporteInsService` convierte un CSV de datos en un `.xlsx` gerencial con Power
> Pivot (modelo VertiPaq + medidas DAX). Documenta el pipeline paso a paso, los internos
> del template Excel, y la **historia de debugging del error `M_YTD MODELO`** para no
> volver a perseguir causas ya descartadas.
>
> Complementa:
> - [`INSIGHTS-SPEC.md`](INSIGHTS-SPEC.md) — spec funcional (tipos de informe, CSVs, API, BD).
> - [`ano-fiscal-spec.md`](ano-fiscal-spec.md) — feature columna "Año Fiscal".
> - [`slicers-independientes.md`](slicers-independientes.md).
>
> Código: `src/main/java/py/com/jaimeferreira/ccr/insights/service/ReporteInsService.java` (~1965 líneas).

---

## 1. Resumen del flujo

```
POST insights/api/v1/... (controller)
  └─ crea InformeIns (estado PROCESANDO) + lanza @Async procesarReporte(...)
        1. Resolver filtros (usuario → base del cliente → classpath)
        2. Concatenar datos_base del cliente + CSV del usuario
        3. Leer + filtrar + validar headers del CSV
        4. Abrir TEMPLATE .xlsx del cliente+categoría (Apache POI / XSSFWorkbook)
        5. Poblar hojas: Calendario, FACT, Total Empresa
        6. Actualizar rangos de tablas + refreshOnLoad de pivots
        7. Guardar a disco (SXSSF, streaming)
        8. forzarRefreshOnLoadDataModel(.xlsx)  ← manipulación XML post-write
        9. Persistir el CSV concatenado como nuevo datos_base
       10. marcarCompletado / marcarError
```

Es **asíncrono** (`@Async`, `ReporteInsService.procesarReporte`, ~línea 317). El estado se
sigue en la tabla `informe` (`PROCESANDO` → `COMPLETADO` | `ERROR`). El proxy `@Async` se
invoca vía `self` (campo `@Lazy @Autowired ReporteInsService self`) para evitar
self-invocation.

---

## 2. Pipeline detallado (`procesarReporte`, línea 317)

| Paso | Código | Qué hace |
|---|---|---|
| Filtros | `resolverFiltros` (~334) | CSV de filtros del usuario; si null usa `filtros_base.csv` del cliente; fallback a `resources/insights/filtros.csv`. Construye `ordenAperturaMap` (APERTURA→Orden_Apertura) y `agrupadorSegmentoMap` (SEGMENTO→AGR_SEGM). |
| Concatenar | `leerDatosBase` + `concatenarCsvData` (~344) | Antepone el `datos_base` acumulado del cliente al CSV nuevo. Libera `csvBytes`/`datosBase` para bajar el pico de heap. |
| Leer/filtrar | `leerYFiltrarCsvData` (~359) | Separador `;`. Aplica filtros, devuelve `List<String[]>` (fila 0 = header). |
| Validar | `validarHeadersCsv` (485) | Detecta columnas faltantes (con alias por acento/case) y orden incorrecto. Descarta filas con < `minCols`. Detecta columna opcional `SUB_MARCA`. |
| Abrir template | `resolverTemplateStream` + `new XSSFWorkbook` (403) | `IOUtils.setByteArrayMaxOverride(500MB)`. Template resuelto por cliente+categoría → cliente → default (`TemplateInsService`). |
| Preparar | `desconectarTablas` (408), `escribirMesInicioFiscal` (409), `limpiarDatosHoja` FACT/Total Empresa (411-412) | Limpia datos viejos del template y setea el named range `MesInicioFiscal`. |
| **Calendario** | `poblarCalendario` (414 / def **729**) | Ver §4. Se hace sobre `XSSFWorkbook` ANTES de envolver con SXSSF. |
| Wrap streaming | `new SXSSFWorkbook(templateWb, 100)` (417) | A partir de acá el write es por streaming (100 filas en memoria). |
| FACT | `poblarFact` (423 / def **557**) | Ver §3. Escribe por NOMBRE de header (no por índice fijo). |
| Total Empresa | `poblarTotalEmpresa` (425) | Análogo a FACT. Libera `dataFiltrada` tras esto. |
| Rangos tablas | `actualizarRangosTablas` (432) | Ajusta el `ref` de las tablas Excel FACT/Total_Empresa/Calendario al nuevo row-count. |
| Pivots | `refrescarTablasDinamicas` (433) | Setea `refreshOnLoad=true` en los `pivotCacheDefinition*.xml`. |
| Ocultar | `ocultarHoja` (434-438) | Oculta FACT, Calendario, Total Empresa, DIM, Hoja1. |
| Guardar | `guardarEnDisco` (443) + `workbook.dispose()` | Escribe el `.xlsx` final. `setForceFormulaRecalculation(true)`. |
| **Refresh modelo** | `forzarRefreshOnLoadDataModel` (453 / def **1135**) | Ver §5. Manipula `xl/connections.xml` con DOM. |
| Persistir base | `guardarDatosBase` (457) | El CSV concatenado pasa a ser el nuevo `datos_base` del cliente. |

Nombre de archivo: `{CLIENTE}_{TIPO}_{yyyyMMdd_HHmmss}.xlsx` (`buildNombreArchivo`).

---

## 3. Hoja FACT (`poblarFact`, línea 557)

Escritura **por nombre de header** (`buildHeaderIndexMap`), robusta a reordenamientos del
template. Columnas escritas: `Distribución Fisica, Distribución Ponderada, Facturación,
Volumen, Volumen Unidades, Apertura Geografica, Categoría, CLIENTE, Variedad, Empresa,
hash, Marca, PAIS, Segmento, Agrupador Segmento, Orden Apertura, YTD 1er Mes, Fecha
[, SUB_MARCA]`.

- `Fecha`: `setCellDateByHeader(..., mes, año, ...)` → **día 1 del mes** (misma convención
  que Calendario; la relación del modelo FACT↔Calendario es por `Fecha`).
- `YTD 1er Mes`: `derivarYtdInt(mes, mesInicioFiscal)`.
- Los índices de columna del CSV salen de `TipoReporte` (NORMAL vs CADENA difieren en
  cantidad y orden — ver INSIGHTS-SPEC §3).

---

## 4. Hoja Calendario (`poblarCalendario`, línea 729) — **zona crítica**

Se arma con los pares **(año, mes) únicos presentes en el CSV** (`TreeMap<año, TreeSet<mes>>`),
una fila por mes. Columnas:

| Col | Nombre | Contenido | Origen |
|---|---|---|---|
| A | `Fecha` | `LocalDate.of(año, mes, 1)` como fecha (estilo `yyyy-mm-dd`) | Datos |
| B | `Mes Numero` | int 1-12 | Datos |
| C | `Año` | int | Datos |
| D | `Mes` | nombre en español (`MESES_ES`) | Datos |
| E | `Año Fiscal` | **fórmula** `[@Año]+IF(AND(MesInicioFiscal>1,[@[Mes Numero]]>=MesInicioFiscal),1,0)` escrita como string crudo (`CTCell.addNewF`), **sin valor cacheado** | Calc en Excel al abrir |
| F | `Cotización USD` | `cotizacionService.obtenerCotizacion("USD", últimoDíaMes)` redondeado; blanco si no hay | `CotizacionService` |

⚠️ **Las columnas E (Año Fiscal) y F (Cotización USD) NO existen en el template original.**
Se agregan en runtime (headers en 740-743; `CTTableColumn` a la tabla `Calendario` en
745-772). Esto expande la tabla de **4 columnas (A-D)** a **6 (A-F)**. Introducidas en commit
`23ed1a9` (2026-05-11). **No** son la causa del bug de DAX: el usuario confirmó que estas
columnas no se usan en el modelo del gerencial (ver §6, H1 descartada).

**Contigüidad (fix implementado 2026-05-31):** `poblarCalendario` genera un **rango mensual
CONTIGUO (sin huecos)** desde el primer mes con datos hasta un **tope**:
- si el front envía `mesReporte` (1-12) → tope = **(año en curso, `mesReporte`)**; nunca por
  debajo del último mes con datos (para no dejar filas FACT huérfanas);
- si no viene → tope = último mes con datos.

Itera por índice absoluto de mes (`anio*12 + (mes-1)`) para cruzar años sin huecos. Antes
generaba sólo los meses presentes en el CSV; si faltaba un mes intermedio (o el rango quedaba
parcial) rompía `'FACT'[Fecha_YTD]` (`DATEADD`) con `#ERROR`. Ver §6. Los meses agregados sin
datos quedan con medidas en blanco (correcto para una dimensión calendario). **No se toca la
fórmula DAX del template** (decisión de negocio: el reporte generado debe ser idéntico al
existente).

---

## 5. Template Excel y modelo de datos (Power Pivot)

Estructura interna del `.xlsx` (ver también la memoria
`reference-insights-template-xlsx-structure`):

- **`xl/connections.xml` — 9 conexiones:**
  - `id=1` `Consulta - Calendario` — Power Query que lee la hoja Calendario (`$Workbook$`).
  - `id=2` `Consulta - FACT` — idem hoja FACT.
  - `id=3` `Consulta - Total Empresa` — idem.
  - `id=4` `ThisWorkbookDataModel` — **conexión OLAP al modelo VertiPaq** (`<dbPr command="Model"/>` + `<x15:connection model="1"/>`).
  - `id=5..9` `WorksheetConnection_*` — **fantasma**, apuntan a workbooks externos inexistentes (`Dashboard CCR Palermo*.xlsx`, `Dashboard Template CCR.xlsx`). Inertes salvo que algún pivot/slicer las referencie.
- **`xl/model/item.data`** — binario VertiPaq (~1.19 MB). **POI NO puede manipularlo.** Contiene el esquema del modelo, relaciones, marca de tabla de fechas y las **medidas DAX**.
- Medidas DAX (extraídas con `strings`): usan time-intelligence sobre `Calendario[Fecha]` —
  `SAMEPERIODLASTYEAR`/`SINPERIOD`, `PARALLELPERIOD(Calendario[Fecha],-12,MONTH)`, YTD.
  Cadena de dependencia del error: **`M_MAESTRA_ACUM` → `M_YTD MODELO`**.

### `forzarRefreshOnLoadDataModel` (línea 1135)

Abre el `.xlsx` como `FileSystem` ZIP, parsea `xl/connections.xml` con DOM, ubica la conexión
del Data Model (match por `dbPr command="Model"` **o** `x15:connection model="1"`) y le setea
`refreshOnLoad="1"`. No fatal si falla.

**Intención:** que al abrir, Excel reconstruya VertiPaq desde las queries internas (1-3) que
leen las hojas con la data nueva. **Caveat:** primera apertura muestra barra "Habilitar
contenido" (una sola vez por archivo).

> Reemplazó al refresh server-side vía Excel COM (`scripts/refresh-excel.vbs`), descartado por
> fallar crónicamente bajo cuenta de servicio Windows (`RPC_E_SERVERFAULT`). El VBS y
> `refrescarModeloDatos` siguen en el repo pero **no se invocan**.

---

## 6. Failure mode: error DAX `M_YTD MODELO` al abrir el reporte

> ### 🎯 CAUSA RAÍZ REAL (2026-06-01) — supera todo lo de abajo
>
> El verdadero bug es que **las hojas FACT y Total Empresa salían VACÍAS** en el reporte
> generado (solo el header). Verificado en un archivo recién generado sin abrir en Excel:
> FACT `dim A1:R1` (1 fila), Total Empresa `A1:P1` (1 fila), Calendario 27 filas (OK).
>
> **Mecanismo:** `poblarFact`/`poblarTotalEmpresa` leían el header con
> `buildHeaderIndexMap(sheet)` → `sheet.getRow(0)` sobre una hoja **envuelta en SXSSF**, que
> **devuelve `null`** (SXSSF no da acceso aleatorio a las filas existentes del template). Header
> vacío → `if (headers.isEmpty()) return;` → **salían sin escribir ni una fila**. Calendario
> funcionaba porque se escribe sobre el `XSSFWorkbook` ANTES de envolver (ahí `getRow(0)` sí anda).
>
> Con FACT vacío, al abrir el reporte Excel reconstruye el modelo sin datos / con la fecha rota
> → `'FACT'[Fecha_YTD]` (DATEADD/EDATE) da `#ERROR` → cascada a `M_YTD MODELO` → `M_MAESTRA_ACUM`.
> **Por eso todo el análisis DAX de abajo eran SÍNTOMAS, no la causa.**
>
> **Fix (commit pendiente):** en `procesarReporte`, capturar los header maps de FACT y Total
> Empresa desde el `XSSFWorkbook` ANTES del `new SXSSFWorkbook(...)`, y pasarlos a
> `poblarFact`/`poblarTotalEmpresa` (que ya no llaman `buildHeaderIndexMap` sobre la hoja SXSSF).
>
> **Bug previo relacionado (mismo día):** `configurarPoiTempDir` no verificaba que el temp dir
> fuera escribible; en dev (Mac, `path.directory.server=/opt`, root-only) `mkdirs()` fallaba
> silencioso y SXSSF reventaba con `NoSuchFileException: /opt/poi-tmp/poi-sxssf-sheet*.xml`.
> Fix: verificar `isDirectory()/canWrite()` y caer a `java.io.tmpdir`. (Prod Windows
> `c:\ccr_zoomin\poi-tmp` es escribible; excluirlo del antivirus/limpiador de temp.)
>
> Lo de abajo queda como historial del camino recorrido.

### (Histórico) Failure mode visto: error DAX `M_YTD MODELO` al abrir

### Síntoma
Al abrir el `.xlsx` generado, Excel muestra:
> *"La medida 'FACT'[M_MAESTRA_ACUM] depende de otra medida denominada 'FACT'[M_YTD MODELO]
> que tiene un error de dependencia."*

Reportado históricamente como *"con CSV grande"*.

### ❌ Causas DESCARTADAS (no volver a perseguir)
1. **Cache VertiPaq desactualizado / row-count del template.** Era la teoría asumida (ver
   comentario engañoso en `ReporteInsService` ~línea 1120 y la memoria
   `project-dax-refresh-fix`). **FALSIFICADA**: un *"Datos → Actualizar todo"* manual y
   completo en Excel **NO** corrige el error (confirmado por el usuario, 2026-05-31). Si fuera
   cache, un RefreshAll lo reconstruiría.
2. **`refreshOnLoad` no se aplica.** **FALSIFICADO**: el archivo generado SÍ tiene
   `refreshOnLoad="1"` en la conexión `id=4`. El flag está; no alcanza.
3. **Timing del refresh / heap / VBS COM.** Lo que atacó el commit `1b149e8` (timeout 120→300s,
   `CalculateUntilAsyncQueriesDone`, liberar CSV). Todo eso es del camino de refresh, ya
   falsificado como causa.

**Conclusión:** NO es un problema de refresh ni de cache. Es **estructural / de datos en el
modelo**: la reconstrucción del modelo a partir de la data que escribe POI produce un
`M_YTD MODELO` en error, y por eso ningún refresh lo arregla.

### Evidencia (comparación archivo-bueno vs archivo-roto, 2026-05-31)

| | Template (pre-POI) | **Gerencial (funciona)** | Generado (roto) |
|---|---|---|---|
| `xl/model/item.data` | 1191936 | **1196032** (VertiPaq reconstruido y guardado) | 1191936 (= template, nunca reconstruido) |
| Tabla Calendario | vacía | **4 cols** `Fecha, Mes Numero, Año, Mes` (`A1:D26`); celdas de hoja vacías (datos en el modelo) | **6 cols** (+`Año Fiscal`, `Cotización USD`) (`A1:F27`); `Año Fiscal` = fórmula **sin valor cacheado** |
| `refreshOnLoad` Data Model | no | no (no lo necesita: modelo ya válido y guardado) | sí (`=1`) |

El archivo que funciona tiene el modelo VertiPaq **reconstruido y persistido** y un Calendario
con **sólo las 4 columnas originales**.

### ✅ ROOT CAUSE CONFIRMADO (2026-05-31, vía Power Pivot)

La columna calculada **`'FACT'[Fecha_YTD]` está en `#ERROR` en todas las filas** en el
archivo roto (verificado en Power Pivot). Su fórmula:

```DAX
Fecha_YTD = DATEADD('FACT'[Fecha]; -MAXX('FACT';[YTD 1er Mes])+1; MONTH)
```

Cadena: `Fecha_YTD` (#ERROR) → `M_YTD MODELO` (la medida la usa → "error de dependencia") →
`M_MAESTRA_ACUM` (depende de M_YTD MODELO → mensaje al usuario).

**Mecanismo:** `DATEADD` es time-intelligence y exige un **rango de fechas contiguo / años
completos**. Datos de este reporte: 2024 + 2025 + **2026 incompleto** (sólo ene/feb) → DATEADD
rompe. En el archivo que funcionaba los datos eran de **un solo año (2024)** → DATEADD OK. Esto
explica el histórico "con CSV grande" (abarca varios años con el último parcial).

Notas:
- `mesInicioFiscal=1` SÍ está presente → `MAXX('FACT';[YTD 1er Mes])=1` → shift 0. No es el mes
  fiscal faltante; es `DATEADD` en sí con rango no-contiguo/años incompletos.
- `Fecha_YTD` vive en el modelo (`item.data`), **POI no la edita**. Las opciones de fix son
  template-DAX o completar la data:
  - **Fix A (data, en Java/`poblarCalendario`+FACT):** generar el Calendario/Fechas con **años
    completos** (rellenar ene-dic de cada año del rango) para que la tabla de fechas sea
    contigua. Fixea TODA la time-intelligence de una.
  - **Fix B (template DAX):** reemplazar `DATEADD('FACT'[Fecha]; k; MONTH)` por
    `EDATE('FACT'[Fecha]; k)` en la columna `Fecha_YTD` (y donde aplique). `EDATE` es escalar,
    no exige contigüidad. Requiere editar el modelo en Power Pivot y re-guardar el template.

### Fix aplicado: **A — rango contiguo con tope en `poblarCalendario`** (2026-05-31)

Decisión: NO se cambia la fórmula DAX (Fix B/EDATE descartado por ahora) porque la instrucción
de negocio es que el reporte generado sea **idéntico al existente**. Se ataca por los datos:

- `poblarCalendario(... , Integer mesReporte)` genera un rango **mensual contiguo** desde el
  primer mes con datos hasta el tope (ver §4). Esto elimina huecos en la tabla de fechas, que
  es lo que hace fallar a `DATEADD`.
- Nuevo parámetro **`mesReporte`** (opcional, 1-12) que viaja:
  `InsightsController` (`@RequestParam mesReporte`, valida 1-12) → `iniciarGeneracion` →
  `procesarReporte` → `poblarCalendario`. Si el front no lo manda, default = último mes con
  datos (comportamiento seguro, igual forma que el gerencial que funciona, que llegaba a su
  último mes con datos sin meses futuros).
- Tope = `(año en curso, mesReporte)`, acotado para nunca quedar por debajo del último mes con
  datos. Evita rellenar meses futuros innecesarios (preocupación del usuario con 12/2026).

Compila (Java 8). **Front hecho** (repo Angular `d-insights-ccr`,
`pages/registro/registro.component.{ts,html}`): nuevo select "Mes del reporte" (control
`mesReporte`, default = mes actual) que se envía como `mesReporte` en el multipart de
`/reportes/generar`. Build dev OK.

**Pendiente de validación en Excel/Windows:** generar un reporte de LA FORTUNA, abrir,
confirmar que `'FACT'[Fecha_YTD]` ya no da `#ERROR` y que `M_YTD MODELO`/`M_MAESTRA_ACUM`
evalúan bien. **Importante:** `DATEADD` opera sobre `'FACT'[Fecha]`, no sobre Calendario —
si tras esto el `#ERROR` persiste, la causa es un hueco en los meses de **FACT** (no de
Calendario) y/o `DATEADD` resolviendo sin pasar por la tabla de fechas; en ese caso, conseguir
el texto exacto del `#ERROR` y reconsiderar Fix B (EDATE) como última opción.

### Hipótesis vivas — históricas (superadas por el root cause de arriba)

- **H1 — Columnas agregadas a Calendario. ❌ DESCARTADA por el usuario (2026-05-31):** las
  columnas `Año Fiscal` / `Cotización USD` **no se usan** en el modelo del gerencial que se
  genera, así que su presencia no puede ser la causa. La diferencia 4-vs-6 columnas entre el
  archivo bueno y el roto es ruido de versión, no causal.

- **H2 — Tabla de fechas con año/rango incompleto (LÍDER).** El Calendario sólo cubre los
  meses con datos; con CSV grande abarca un **año final parcial** (p.ej. 2026 sólo ene/feb) y/o
  varios años. La time-intelligence (YTD, `SAMEPERIODLASTYEAR`, `PARALLELPERIOD -12 MONTH`)
  exige una tabla de fechas que cubra **años completos y contiguos**; un año parcial o un
  hueco la rompe. **A favor:** explica el *"con CSV grande"* y persiste tras RefreshAll (la
  data en error reaparece en cada refresh). **Pendiente:** confirmar con el error real.

- **H3 — Fechas huérfanas FACT↔Calendario.** Filas de FACT cuyo `Fecha` (día 1 del mes) no
  matchea ninguna fila de Calendario, rompiendo la relación. Menos probable (ambos derivan de
  los mismos pares año/mes), pero a verificar.

Todas las vivas son consistentes con "RefreshAll no lo arregla".

### ✅ Próximo paso diagnóstico decisivo (requiere Excel/Windows)
Abrir el archivo roto en **Power Pivot** (Administrar modelo) y leer el **error real de
`M_YTD MODELO`** (no el mensaje en cascada de `M_MAESTRA_ACUM`):
- Si menciona **tabla de fechas / contigüidad / año incompleto** → **H2**. Fix: generar
  Calendario con **años completos** (rellenar ene-dic de cada año del rango).
- Si menciona la columna **`Año Fiscal`** / una columna de Calendario / un error de carga de
  la tabla → **H1**. Fix: no agregar columnas calculadas sin valor cacheado al modelo
  (precalcular `Año Fiscal` en Java y escribir valores, no fórmula; o excluir esas columnas de
  la Power Query del modelo).

> No commitear un "fix" hasta confirmar la hipótesis con ese error real de Power Pivot. El
> patrón histórico de este bug es atacar el síntoma (refresh) en vez de la causa (modelo/datos).

---

## 7. Salidas y estados

- Archivo final: `{path.directory.server}/.../{CLIENTE}_{TIPO}_{timestamp}.xlsx`.
- `datos_base` del cliente: se sobreescribe con el CSV concatenado al completar.
- `InformeIns`: `PROCESANDO` → `COMPLETADO` (con duración) | `ERROR` (con mensaje).
- `InformeCleanupScheduler` purga archivos viejos.
- Hitos logueados con `logHito` (`[informe id=N] [+Xs] <hito>`).
