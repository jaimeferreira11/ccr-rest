# Separar Slicers compartidos en el template Excel

## Problema

Las hojas **Reales, Acumulados, Distribución Física y Distribución Ponderada** comparten el mismo slicer cache para Mes (`SegmentaciónDeDatos_Mes11`) y Año (`SegmentaciónDeDatos_Año11`). Al filtrar por mes/año en cualquiera de estas hojas, la selección se propaga a las otras tres.

Las demás hojas (Variaciones, Precios PP, Evol Segmentos, Evol Canales) ya tienen caches independientes.

## Objetivo

Que cada hoja tenga slicers de Mes y Año independientes, sin afectar a las demás.

## Paso a paso en Excel

Repetir los pasos 1–5 para cada una de las tres hojas que necesitan independizarse: **Acumulados**, **Distribución Física** y **Distribución Ponderada**. La hoja **Reales** se queda con los slicers actuales.

### 1. Ir a la hoja

Hacer clic en la pestaña de la hoja (ej: Acumulados).

### 2. Eliminar los slicers existentes de Mes y Año

- Hacer clic sobre el slicer de **Mes** para seleccionarlo (aparece el borde con handles).
- Presionar `Supr` (Delete) para eliminarlo.
- Repetir con el slicer de **Año**.

> Nota: esto solo elimina el control visual de esa hoja. El slicer cache compartido sigue existiendo mientras lo use otra hoja (Reales).

### 3. Hacer clic en cualquier celda de la Tabla Dinámica de esa hoja

Esto activa las opciones de Tabla Dinámica en la cinta de opciones. La tabla dinámica generalmente ocupa el rango principal de datos de la hoja.

### 4. Insertar nuevos slicers

- Ir a la cinta: **Analizar** (o **Análisis de tabla dinámica**) → **Insertar segmentación de datos**.
- En el diálogo que aparece, marcar:
  - `Mes` (de la tabla Calendario)
  - `Año` (de la tabla Calendario)
- Hacer clic en **Aceptar**.

> Excel crea automáticamente slicer caches nuevos e independientes para esta pivot table.

### 5. Posicionar y dar formato a los slicers

- Arrastrar los slicers nuevos a la misma posición donde estaban los anteriores.
- Ajustar el tamaño y número de columnas para que coincidan con el estilo del resto del informe.
- (Opcional) En la pestaña **Segmentación de datos** de la cinta, elegir el mismo estilo visual que usan las demás hojas.

### 6. Verificar independencia

- Seleccionar un mes en la hoja **Reales**.
- Ir a **Acumulados** y verificar que su slicer de Mes NO cambió.
- Repetir la verificación con Distribución Física y Distribución Ponderada.

### 7. Guardar el template

Guardar el archivo `.xlsx` del template. No se necesita ningún cambio en el código Java.

## Resultado esperado

| Hoja | Cache Mes | Cache Año | Independiente |
|---|---|---|---|
| Reales | SegmentaciónDeDatos_Mes11 (original) | SegmentaciónDeDatos_Año11 (original) | Si |
| Acumulados | nuevo cache propio | nuevo cache propio | Si |
| Distribución Física | nuevo cache propio | nuevo cache propio | Si |
| Distribución Ponderada | nuevo cache propio | nuevo cache propio | Si |
| Variaciones | SegmentaciónDeDatos_Mes11411 | SegmentaciónDeDatos_Año11411 | Si (ya era) |
| Precios PP | SegmentaciónDeDatos_Mes111 | SegmentaciónDeDatos_Año111 | Si (ya era) |
| Evol Segmentos | SegmentaciónDeDatos_Mes1121 | SegmentaciónDeDatos_Año1121 | Si (ya era) |
| Evol Canales | SegmentaciónDeDatos_Mes112 | SegmentaciónDeDatos_Año112 | Si (ya era) |
