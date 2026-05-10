# Especificación: Año Fiscal dinámico en template Excel

Contexto para IA que trabaja sobre un proyecto Java que genera reportes gerenciales en Excel a partir de un template (`.xlsx`) y un CSV de datos.

---

## 1. Qué es el año fiscal

El **año calendario** siempre va de enero a diciembre. El **año fiscal (FY)** es el período de 12 meses que una empresa usa para sus cuentas y reportes, y **puede empezar en cualquier mes** del año.

Ejemplos comunes:
- Enero–Diciembre → FY = año calendario (caso típico Paraguay)
- Abril–Marzo → empieza en abril (Japón, India)
- Julio–Junio → empieza en julio (Australia, varias multinacionales de bebidas)
- Octubre–Septiembre → empieza en octubre (gobierno EE.UU.)

## 2. Convención de nombre adoptada: "FY por el año que termina"

Decisión: el año fiscal **se nombra por el año calendario en el que termina** (estándar US GAAP / IFRS / multinacional).

| Mes inicio FY | Mes calendario | Año cal. | Año Fiscal |
|---|---|---|---|
| 1 (ene)  | cualquier mes   | 2024 | **2024** |
| 7 (jul)  | jun-2024        | 2024 | **2024** (fin del FY24) |
| 7 (jul)  | jul-2024        | 2024 | **2025** (inicio del FY25) |
| 7 (jul)  | jun-2025        | 2025 | **2025** (fin del FY25) |
| 4 (abr)  | mar-2024        | 2024 | **2024** |
| 4 (abr)  | abr-2024        | 2024 | **2025** |
| 10 (oct) | sep-2024        | 2024 | **2024** |
| 10 (oct) | oct-2024        | 2024 | **2025** |

Regla:
- Si el mes calendario `m >= MesInicioFiscal` y `MesInicioFiscal > 1` → FY = año calendario **+ 1**
- En cualquier otro caso → FY = año calendario
- Caso especial: si `MesInicioFiscal = 1`, FY siempre = año calendario (sin shift)

## 3. Arquitectura en el template Excel

El template actual (`Gerencial Beb del Py-Gaseosas.xlsx`) ya tiene:

- Hoja `INICIO`: panel de configuración. Hoy contiene `Categoria` (A2) y `Cliente` (B2), expuestos como nombres definidos del workbook.
- Hoja `Calendario`: tabla Excel estructurada llamada `Calendario` con columnas:
  - `A: Fecha` (datetime)
  - `B: Mes Numero` (int 1–12)
  - `C: Año` (int, año calendario)
  - `D: Mes` (string, ej. "enero")
- Hoja `FACT`: tabla `FACT` con datos transaccionales (393k+ filas). Se vincula a `Calendario` por `Fecha`.
- Dashboards, Pivot Tables, Slicers (`SegmentaciónDeDatos_*`) consumen estos datos.

### 3.1. Cambios a aplicar al template

#### A) En hoja `INICIO`: agregar variable de configuración

| Celda | Contenido |
|---|---|
| `A4` | `Mes Inicio Fiscal` (etiqueta) |
| `B4` | número 1–12 (default `1` = enero) |

Crear **nombre definido a nivel workbook**: `MesInicioFiscal` → `=INICIO!$B$4`.

#### B) En tabla `Calendario`: agregar columna `Año Fiscal`

- Nueva columna `E` con header `Año Fiscal`.
- Extender el `ref` de la tabla `Calendario` de `A1:D{n}` a `A1:E{n}`.
- Registrar la nueva `tableColumn` en `<tableColumns count="5">`.
- Fórmula por fila (sintaxis EN, que es la que Excel guarda internamente):
  ```excel
  =[@Año]+IF(AND(MesInicioFiscal>1,[@[Mes Numero]]>=MesInicioFiscal),1,0)
  ```
  Equivalente en sintaxis ES (la que muestra Excel en español):
  ```excel
  =[@Año]+SI(Y(MesInicioFiscal>1;[@[Mes Numero]]>=MesInicioFiscal);1;0)
  ```

  > Nota: el XML interno de `.xlsx` **siempre** usa la versión EN con `,` como separador. Apache POI y openpyxl trabajan con la sintaxis EN. Sólo cambia la presentación en la UI de Excel.

  Fórmula alternativa sin referencias estructuradas (por si la tabla no se preserva — usar referencias directas a celda):
  ```excel
  =C2+IF(AND(MesInicioFiscal>1,B2>=MesInicioFiscal),1,0)
  ```

#### C) Marcar la fórmula como "calculated column" de la tabla

En el XML de la tabla (`xl/tables/tableN.xml`), la columna `Año Fiscal` debe tener:
```xml
<tableColumn id="5" name="Año Fiscal">
  <calculatedColumnFormula>[@Año]+IF(AND(MesInicioFiscal&gt;1,[@[Mes Numero]]&gt;=MesInicioFiscal),1,0)</calculatedColumnFormula>
</tableColumn>
```
Esto hace que Excel auto-extienda la fórmula a nuevas filas cuando Java agregue datos.

## 4. Implementación en Java (Apache POI)

### 4.1. Escribir el mes de inicio fiscal en cada generación de reporte

```java
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

try (XSSFWorkbook wb = new XSSFWorkbook(new FileInputStream(templatePath))) {
    Sheet inicio = wb.getSheet("INICIO");
    Row row4 = inicio.getRow(3); // 0-indexed
    if (row4 == null) row4 = inicio.createRow(3);
    Cell cell = row4.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK); // B4
    cell.setCellValue(mesInicioFiscal); // int 1..12

    // Forzar recálculo de fórmulas al abrir
    wb.setForceFormulaRecalculation(true);

    try (FileOutputStream out = new FileOutputStream(outputPath)) {
        wb.write(out);
    }
}
```

### 4.2. Crear/verificar el nombre definido `MesInicioFiscal` (una sola vez en el template, pero idempotente está bien)

```java
import org.apache.poi.ss.usermodel.Name;

String NAME = "MesInicioFiscal";
Name existing = wb.getName(NAME);
if (existing == null) {
    Name name = wb.createName();
    name.setNameName(NAME);
    name.setRefersToFormula("INICIO!$B$4");
}
```

### 4.3. Poblar Calendario desde el CSV

Cuando Java escribe filas en la tabla `Calendario`, debe:

1. Escribir solo las columnas de datos (`Fecha`, `Mes Numero`, `Año`, `Mes`).
2. Para la columna `Año Fiscal`, dejar que Excel la calcule por la `calculatedColumnFormula` de la tabla — **o** escribir explícitamente la fórmula en cada fila si POI no extiende automáticamente:
   ```java
   cellAnoFiscal.setCellFormula("[@Año]+IF(AND(MesInicioFiscal>1,[@[Mes Numero]]>=MesInicioFiscal),1,0)");
   ```
3. Actualizar el `ref` de la `XSSFTable`:
   ```java
   XSSFTable tabla = sheet.getTables().stream()
       .filter(t -> "Calendario".equals(t.getName()))
       .findFirst().orElseThrow();
   tabla.setCellReferences(new AreaReference("A1:E" + lastRow, SpreadsheetVersion.EXCEL2007));
   tabla.updateReferences();
   ```

### 4.4. Asegurar que Pivot Tables y Slicers se refresquen

Las Pivot Tables que consumen `Calendario` van a ver la nueva columna `Año Fiscal` recién después de un refresh. Para que Excel refresque al abrir:

```java
// En cada pivot table que use la tabla Calendario:
for (XSSFPivotTable pt : sheet.getPivotTables()) {
    pt.getCTPivotTableDefinition().setUpdatedVersion((short) 6);
    // refreshOnLoad = true
    pt.getPivotCacheDefinition().getCTPivotCacheDefinition()
      .setRefreshOnLoad(true);
}
```

### 4.5. ⚠️ Riesgos conocidos al manipular el workbook

Este template usa:
- **Slicers** (`<extLst>` con `x14:slicerList`) — Apache POI 5.x los preserva pero **no** los manipula. Está OK siempre que no se eliminen.
- **Drawings/Shapes** en algunas hojas — POI maneja charts e images bien; otras figuras pueden romperse. Evitar borrar/recrear hojas que las contienen.
- **DataModel / PowerPivot** (`xl/model/`) — si existe, POI lo preserva como blob pero no lo modifica.
- **Tabla `Calendario` está marcada como conexión** (`_xlcn.WorksheetConnection_...`) — al modificarla, regenerar la conexión puede no ser necesario, pero verificar que Pivot Tables siguen apuntando a `Calendario[]`.

**Recomendación**: trabajar siempre sobre una **copia** del template (`Files.copy(template, output)`), modificar `output`, jamás el template original.

## 5. Testing / validación

Casos de prueba a verificar después de generar el reporte:

```
Input MesInicioFiscal = 1  (enero, default Paraguay)
  Fecha 2024-01-01 → Año Fiscal 2024
  Fecha 2024-12-01 → Año Fiscal 2024
  Fecha 2025-06-01 → Año Fiscal 2025

Input MesInicioFiscal = 7  (julio)
  Fecha 2024-06-01 → Año Fiscal 2024
  Fecha 2024-07-01 → Año Fiscal 2025
  Fecha 2025-06-01 → Año Fiscal 2025
  Fecha 2025-07-01 → Año Fiscal 2026

Input MesInicioFiscal = 4  (abril)
  Fecha 2024-03-01 → Año Fiscal 2024
  Fecha 2024-04-01 → Año Fiscal 2025

Input MesInicioFiscal = 10  (octubre)
  Fecha 2024-09-01 → Año Fiscal 2024
  Fecha 2024-10-01 → Año Fiscal 2025
```

Validación en Excel: abrir el archivo generado, cambiar manualmente `INICIO!B4` y verificar que toda la columna `Año Fiscal` de `Calendario` recalcula, y que las pivots/slicers que filtran por año fiscal cambian.

## 6. (Opcional) Etiqueta legible "FY2025"

Si en algún reporte se necesita mostrar `FY2025` en vez de `2025`, agregar una columna adicional `Etiqueta FY`:

```excel
="FY"&[@[Año Fiscal]]
```

O para formato corto `FY25`:
```excel
="FY"&RIGHT([@[Año Fiscal]],2)
```

## 7. Resumen para el IA del proyecto Java

Tarea: Modificar el generador de reportes para soportar año fiscal dinámico parametrizado.

Entregables:
1. Una sola vez sobre el template `.xlsx`: agregar celda `INICIO!B4`, nombre definido `MesInicioFiscal`, columna `Año Fiscal` en tabla `Calendario` con fórmula calculada.
2. En el código Java: aceptar parámetro `int mesInicioFiscal` (1–12, default 1) por config/CLI/request, escribirlo en `INICIO!B4` antes de guardar el output, llamar `setForceFormulaRecalculation(true)`.
3. Al poblar `Calendario` desde CSV, no escribir la columna `Año Fiscal` (la calcula Excel) o escribirla con la fórmula estructurada.
4. Tests unitarios con los casos de la sección 5.

No tocar manualmente las fórmulas ya existentes en Dashboard ni los slicers — operan sobre la tabla `Calendario` y van a heredar la columna nueva al refresh.
