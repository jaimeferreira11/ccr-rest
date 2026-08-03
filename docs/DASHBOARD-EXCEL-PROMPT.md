# Prompt: hoja "Dashboard" con gráficos (template gerencial Nestlé)

> Prompt de referencia para generar (vía Office Script / Excel JS API, ej. Copilot en Excel)
> una hoja "Dashboard" con resumen visual vinculado a las hojas de datos del template
> gerencial. Guardado acá para reutilizar tal cual cuando se necesite regenerar o adaptar
> el dashboard a otro cliente/template.
>
> Complementa [`REPORTE-GERENCIAL-GENERACION.md`](REPORTE-GERENCIAL-GENERACION.md) (pipeline
> de generación del `.xlsx`) e [`INSIGHTS-SPEC.md`](INSIGHTS-SPEC.md) (spec funcional de
> templates). No es código que corra en `ReporteInsService` — es un prompt para ejecutar
> manualmente (Copilot/Office Script) sobre un template ya generado o sobre el `.xlsx` base,
> **antes** de subirlo como template a `TemplateInsService`.

## Prerrequisito — estructura del workbook

Las siguientes hojas deben existir con datos en columnas C:F (labels en C, años 2024/2025/2026
en D5/E5/F5, filas de datos desde fila 6 o 7 hasta antes del "Total general"):

- Reales (C5:F22)
- Acumulados (C5:F21)
- Variaciones (C4:I21 — incluye Var Absoluta y Var Share por año)
- Distribución Física (C5:F22)
- Distribución Ponderada (C5:F21)
- Precios PP (C5:F22)
- Evol Segmentos (C5:F14)
- Evol Canales (C5:F21)

## Prompt completo

```
Necesito que crees una hoja llamada "Dashboard" entre las hojas "INICIO" y "Reales" con
resumen visual de cada hoja de datos. Seguí estos lineamientos exactos:

==================================================
ESTRUCTURA DEL WORKBOOK (debe existir antes)
==================================================
Las siguientes hojas deben existir con datos en columnas C:F (labels en C, años 2024/2025/2026
en D5/E5/F5, filas de datos desde fila 6 o 7 hasta antes del "Total general"):
- Reales (C5:F22)
- Acumulados (C5:F21)
- Variaciones (C4:I21 — incluye Var Absoluta y Var Share por año)
- Distribución Física (C5:F22)
- Distribución Ponderada (C5:F21)
- Precios PP (C5:F22)
- Evol Segmentos (C5:F14)
- Evol Canales (C5:F21)

==================================================
PASO 1 — Crear la hoja Dashboard
==================================================
- Insertar hoja "Dashboard" inmediatamente después de "INICIO"
- Color de pestaña: #1F4E79
- Sin gridlines

==================================================
PASO 2 — Anchos de columna
==================================================
- Columnas A:K = 43px cada una (zona gráficos izquierda)
- Columna L = 12px (gap)
- Columnas M:W = 43px cada una (zona gráficos derecha)

==================================================
PASO 3 — Header (filas 1 a 5)
==================================================
- Fila 1: fondo #D81E05 (rojo Nestlé), altura 3px
- Fila 2: fondo blanco, altura 36px, alineación vertical centro
  · Celda A2: texto " NESTLÉ" — Calibri 14pt bold color #1A1A1A (rango A2:F2)
  · Celda G2: texto "DASHBOARD EJECUTIVO" — Calibri 10pt color #6E6E6E, alineación derecha (rango G2:W2)
- Fila 3: fondo blanco, texto " Período 2024 · 2025 · 2026 | Datos vinculados a hojas fuente" — Calibri 8pt color #9A9A9A, altura 14px
- Fila 4: fondo #E5E7EB (divisor gris), altura 1px
- Fila 5: fondo #FAFBFC, altura 12px (spacer)
- Rango A6:W60: fondo #FAFBFC (background del área de gráficos)

==================================================
PASO 4 — Crear 8 gráficos con tipos variados
==================================================
Todos los gráficos: vinculados DIRECTAMENTE a las hojas fuente (no copiar datos), fondo
blanco, plot area blanco, Calibri 11pt bold color #111827, borde fino color #E5E7EB.
Ancho: 470px | Alto: 240px

Posiciones (top, left):
- Fila 1: top=70  | izq left=5, der left=484
- Fila 2: top=322 | izq left=5, der left=484
- Fila 3: top=574 | izq left=5, der left=484
- Fila 4: top=826 | izq left=5, der left=484

GRÁFICO 1 — Reales (top-left)
- Source: Reales!C5:F22, seriesBy=columns
- Tipo: Doughnut
- Título: "Share de Mercado 2026 — Reales"
- Paleta (multi-color para empresas): ["#1E3A5F","#3B82B5","#5FA8D3","#A8DADC","#E9C46A","#F4A261","#E76F51","#264653","#2A9D8F","#8AB17D","#BC4749","#6A4C93","#90A955","#577590","#4F5D75","#C8B6A6","#7B7263"]

GRÁFICO 2 — Acumulados (top-right)
- Source: Acumulados!C5:F21, seriesBy=columns
- Tipo: barStacked100
- Título: "Acumulados YTD — Composición % por Empresa"
- Paleta: misma multi-color de empresas (igual que Gráfico 1)

GRÁFICO 3 — Variaciones (row 2 left)
- Source: Variaciones!C4:I21, seriesBy=columns
- Tipo: lineMarkers
- Título: "Variaciones — Tendencia por Empresa"
- Paleta (alterna acento + gris para suavizar series de share): ["#1E3A5F","#D3D3D3","#3B82B5","#D3D3D3","#E76F51","#D3D3D3"]

GRÁFICO 4 — Distribución Física (row 2 right)
- Source: Distribución Física!C5:F22, seriesBy=columns
- Tipo: columnClustered
- Título: "Distribución Física — % por Empresa"
- Paleta (3 años, tonos fríos): ["#1E3A5F","#3B82B5","#A8DADC"]

GRÁFICO 5 — Distribución Ponderada (row 3 left)
- Source: Distribución Ponderada!C5:F21, seriesBy=columns
- Tipo: barClustered
- Título: "Distribución Ponderada — % por Empresa"
- Paleta (3 años, tonos cálidos): ["#264653","#2A9D8F","#8AB17D"]

GRÁFICO 6 — Precios PP (row 3 right)
- Source: Precios PP!C5:F22, seriesBy=columns
- Tipo: lineMarkers
- Título: "Precios Promedio — Índice por Empresa"
- Paleta: ["#1E3A5F","#E76F51","#E9C46A"]

GRÁFICO 7 — Evol Segmentos (row 4 left)
- Source: Evol Segmentos!C5:F14, seriesBy=columns
- Tipo: columnStacked
- Título: "Evolución Segmentos — Share % 2024–2026"
- Paleta: ["#1E3A5F","#3B82B5","#5FA8D3","#A8DADC","#E9C46A","#F4A261","#E76F51","#2A9D8F","#8AB17D"]

GRÁFICO 8 — Evol Canales (row 4 right)
- Source: Evol Canales!C5:F21, seriesBy=columns
- Tipo: barStacked100
- Título: "Evolución Canales — Share % 2024–2026"
- Paleta: misma multi-color de empresas (igual que Gráfico 1)

==================================================
NOTAS IMPORTANTES
==================================================
1. Los gráficos deben enlazarse directamente con las hojas fuente — al cambiar los datos en
   las hojas, los gráficos se actualizan solos.
2. NO crear tablas intermedias ni copiar datos al Dashboard.
3. Si el rango fuente tiene una fila "Total general" al final, EXCLUIRLA del rango del
   gráfico (por eso los rangos terminan en fila 21 o 22 según el caso).
4. Cuando crees el gráfico, primero hacé sync(), después seteá top/left/width/height con
   otro sync(), después title, después format.fill/font, y finalmente las series — cada
   bloque con su propio sync() para evitar errores de objetos no cargados.
5. La propiedad "ChartTitle.font" no está accesible directamente — usá "chart.format.font"
   que aplica al título.
6. "ChartLegend.font.size" tampoco está accesible — saltearlo.
7. "ChartSeries.name" no se puede modificar — dejar los nombres por defecto.
8. "ChartBorder.weight" tampoco — solo setear color y lineStyle.
```

## Notas de aplicabilidad

- Paletas y textos ("NESTLÉ", rojo #D81E05) son específicos del template Nestlé. Para otro
  cliente/brand, adaptar colores de marca y nombres de hoja antes de reusar.
- Asume que las hojas fuente (`Reales`, `Acumulados`, etc.) ya están pobladas — no es parte
  del pipeline de `ReporteInsService` (que sólo puebla `Calendario`, `FACT`, `Total Empresa`).
  Este dashboard aplica sobre hojas de resumen adicionales del template, no sobre las que
  genera el backend.
