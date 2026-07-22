# Diseño: arreglar el YTD fiscal (shift del acumulado)

> **Feedback gerencial (Item 1 de 6)** — PDF "Comentarios gerenciales con dashboard para
> desarrollo". Encara solo el YTD fiscal de la hoja **Acumulados**. Los otros 5 items del
> feedback (MOVIL 3/12, share al padre, renombrar pestañas, agrupador de apertura, gráficos
> sin total empresas) se diseñan por separado.
>
> Complementa: [`../../ano-fiscal-spec.md`](../../ano-fiscal-spec.md),
> [`../../REPORTE-GERENCIAL-GENERACION.md`](../../REPORTE-GERENCIAL-GENERACION.md).

---

## 1. Pedido del cliente y decisión de alcance

> *"YTD significa Year to date, debe incluir información de enero hasta la fecha, y el cambio
> solicitado es que haya la opción de seleccionar un YTD fiscal, que es cuando podés elegir el
> mes que empieza (en la mayoría de las veces el mes 7). En el archivo de FILTROS, esa opción
> sería la columna G ('YTD 1er Mes'), que ahí se selecciona el mes a empezar."*

Decisiones acordadas:
- **Un solo YTD configurable** (no dos vistas coexistiendo, no toggle por slicer). `1` → YTD
  calendario (ene-a-fecha); `7` → YTD fiscal desde julio.
- **Fuente del mes: el combo del front** (`mesInicioFiscal`, default `1`), como hoy. El
  operador elige el mes al generar el reporte.
  - ⚠️ **Divergencia del pedido literal del cliente:** el cliente pidió controlarlo en la
    **columna G del FILTROS**. Se decidió mantener la fuente en el front; **col G se ignora**
    (las entradas que hoy tienen los archivos de FILTROS no afectan el reporte). Si el cliente
    insiste con col G, el cambio es chico (un helper `resolverMesInicioFiscal` que lea el header
    `YTD 1er Mes` por nombre, con el front como fallback) — ver Apéndice A.

Esto reduce el item a **un único arreglo de bug**; no se toca lectura de filtros, ni el modelo
DAX, ni el Angular.

## 2. Estado actual y diagnóstico (el bug)

El valor `mesInicioFiscal` (del front, default `1`) viaja por el service y se usa en **dos
canales independientes**:

### Canal A — `INICIO!B4` (named range `MesInicioFiscal`) → agrupación de Año Fiscal ✅ funciona
`escribirMesInicioFiscal` (`ReporteInsService:717`, llamado en `:425`) escribe el **número
real** en `INICIO!B4` (`:732`) y asegura el named range `MesInicioFiscal → INICIO!$B$4`. La
columna `Año Fiscal` de la hoja **Calendario** lleva la fórmula (`:797`/`:840`):
```excel
[@Año] + IF(AND(MesInicioFiscal>1, [@[Mes Numero]]>=MesInicioFiscal), 1, 0)
```
Excel la calcula al abrir → define a qué año fiscal pertenece cada mes. **Esto ya respeta el
mes elegido** (con 7, jul-2024 → FY2025). `poblarCalendario` recibe `mesInicioFiscal` pero no lo
usa como int; el valor viaja por la fórmula vía el named range.

### Canal B — columna `YTD 1er Mes` en FACT/Total Empresa → shift del YTD ❌ roto
`poblarFact` (`:634`) y `poblarTotalEmpresa` (`:702`) escriben por fila:
```java
setCellIntByHeader(row, headers, "YTD 1er Mes", derivarYtdInt(csv[idxMes], mesInicioFiscal));
```
con `derivarYtdInt` (`:1959`):
```java
return String.valueOf(mesInicioFiscal).equals(mes.trim()) ? Integer.valueOf(1) : null;
```
→ devuelve el **flag `1`** solo en la fila cuyo mes == mesInicioFiscal, `null` en el resto.

La medida del modelo que lo consume:
```DAX
Fecha_YTD = DATEADD('FACT'[Fecha]; -MAXX('FACT';[YTD 1er Mes])+1; MONTH)
```

**Bug:** como `derivarYtdInt` siempre escribe el flag `1` (nunca el número de mes),
`MAXX('FACT';[YTD 1er Mes])` da siempre `1` → el shift del `DATEADD` es siempre `0`. Resultado:
**el YTD fiscal nunca funciona; el acumulado siempre sale calendario, aunque el operador elija
mes 7.** (Riesgo latente extra: si no hay datos del mes de inicio, no se escribe ningún `1`,
`MAXX` da blanco y el shift queda `+1`, rompiendo incluso el calendario.)

Resumen del síntoma del cliente: el reporte **agrupa** bien por año fiscal (Canal A) pero
**acumula** mal (Canal B).

## 3. Insight clave: el DAX ya es correcto

La fórmula `DATEADD('FACT'[Fecha]; -MAXX([YTD 1er Mes])+1; MONTH)` produce el shift correcto
**si la columna lleva el número del mes de inicio (S) en vez del flag**:

| `YTD 1er Mes` (todas las filas) | `MAXX` | shift `-(S-1)` | Resultado |
|---|---|---|---|
| `1` | 1 | `0` | YTD calendario (idéntico a hoy) |
| `7` | 7 | `-6` | julio → "enero" → **YTD fiscal** |

→ **No se toca el modelo DAX ni `xl/model/item.data`.** Solo cambia qué número escribe Java en
la columna `YTD 1er Mes`.

## 4. Cambio de código (único)

`ReporteInsService.derivarYtdInt` (`:1959`) pasa a devolver el **número del mes de inicio en
todas las filas**, en vez del flag:

```java
// antes: Integer 1 solo si mes == mesInicioFiscal; null si no
// después: el número de mes de inicio (S) siempre
private Integer derivarYtdInt(String mes, int mesInicioFiscal) {
    return Integer.valueOf(mesInicioFiscal);
}
```

- El parámetro `mes` queda sin uso → se puede simplificar la firma y los dos call sites
  (`:634`, `:702`) a escribir `mesInicioFiscal` directamente, o dejar el helper para no tocar
  más líneas. Decisión menor, a resolver en el plan.
- Hace `MAXX = S` de forma robusta, sin depender de que exista una fila del mes de inicio.
- Para `mesInicioFiscal = 1` (default / calendario): cada fila lleva `1`, `MAXX = 1`, shift `0`
  → **comportamiento idéntico al de hoy**. Cambio aditivo y seguro.
- Aplica igual a `poblarFact` y `poblarTotalEmpresa`.

**No se toca:** `escribirMesInicioFiscal` (Canal A ya correcto), `poblarCalendario`, la lectura
de filtros, el controller, ni el Angular.

## 5. Riesgos y validación (en Excel/Windows)

1. **Contigüidad de `DATEADD`.** Con shift real (`-6`) podría reaparecer el `#ERROR` histórico
   de `Fecha_YTD` si la tabla de fechas no es contigua. El fix de calendario contiguo
   (`poblarCalendario` con tope, ver `REPORTE-GERENCIAL-GENERACION.md §4/§6`) ya debería
   cubrirlo. **Validar:** generar un reporte con mes inicio `7`, abrirlo y confirmar que
   `'FACT'[Fecha_YTD]` no da `#ERROR` y que `M_YTD MODELO` / `M_MAESTRA_ACUM` evalúan bien.
2. **Otras medidas que usen `[YTD 1er Mes]`.** Según `REPORTE-GERENCIAL-GENERACION.md §5`, la
   única dependencia es `Fecha_YTD` vía `MAXX`. Si alguna medida lo **suma o cuenta**, escribir
   S en todas las filas (en vez del flag en una) cambiaría sus totales. **Confirmar en Power
   Pivot** que no exista tal uso (riesgo bajo).
3. **Regresión calendario.** Generar un reporte con mes inicio `1` y confirmar que el Acumulado
   YTD es idéntico al actual.

## 6. Casos de prueba

| Front `mesInicioFiscal` | `YTD 1er Mes` escrito | `MAXX` | Shift `Fecha_YTD` | YTD |
|---|---|---|---|---|
| 1 | 1 (todas las filas) | 1 | 0 | calendario (idéntico a hoy) |
| 7 | 7 | 7 | -6 | fiscal desde julio |
| 4 | 4 | 4 | -3 | fiscal desde abril |

## 7. Resumen

Cambio de **una línea, bajo riesgo, sin tocar el modelo DAX**: `derivarYtdInt` escribe el número
del mes de inicio (del combo del front) en la columna `YTD 1er Mes` de FACT/Total Empresa en
todas las filas, en vez de un flag `1`. La medida `Fecha_YTD` existente convierte eso en el
shift de meses correcto. El Canal A (Año Fiscal) ya funcionaba y no se toca. Validación final en
Excel/Windows.

---

## Apéndice A — Si más adelante se quiere la fuente en col G del FILTROS

Los filtros se leen **por NOMBRE de header, no por posición** (`leerFiltrosDesdeBytes` /
`leerFiltrosDesdeResources`), así que "no siempre es G" no es problema: donde existe, la columna
se llama `YTD 1er Mes` (NORMAL y CADENA — confirmado en archivos de prod: Beb del Py, BIMBO, LA
FORTUNA, NESTLE; VIERCI usa un FILTROS mínimo de 3 cols sin esa columna). Implementación:

- Helper `resolverMesInicioFiscal(filtros, mesInicioFiscalFront)`: primer valor no vacío del
  header `YTD 1er Mes` (vía `findFilterValue`, matcheo normalizado, agnóstico a posición);
  parsea 1–12; si no hay columna/vacía/inválido → fallback al `mesInicioFiscalFront` (front,
  default 1). Invocar en `procesarReporte` tras `resolverFiltros` y antes de
  `escribirMesInicioFiscal`/`poblar*`.
- Encoding: `leerFiltrosDesdeBytes` usa `detectarEncoding()` (auto, ISO-8859-1 OK); `YTD 1er
  Mes` es ASCII → sin riesgo de acentos.
