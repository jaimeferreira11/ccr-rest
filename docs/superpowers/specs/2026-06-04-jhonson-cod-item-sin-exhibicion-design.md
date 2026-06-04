# Jhonson — `cod_item` en respuesta_det y texto "Sin Exhibición" — Diseño

**Fecha:** 2026-06-04
**Autor:** Jaime Ferreira
**Estado:** Aprobado

## Objetivo

Dos ajustes en la app Jhonson (SCJ), priorizando que se resuelvan en el **backend**:

1. **`cod_item` en `respuesta_det`:** que el detalle de respuestas lleve el **código de negocio del ítem** (`jhonson.items.codigo`), además del `id_item` (que NO se elimina).
2. **Texto "Sin Exhibición":** cuando se marca sin exhibición (cabecera SE), el `desc_item` debe ser **"Sin Exhibición"** en vez del actual **"SIN EXHIBICIONES ADICIONALES"**.

## Contexto (estado actual, verificado)

- La app envía `POST /jhonson/api/v1/respuestas` con `List<RespuestaCabSCJ>`, cada una con `detalles` (`RespuestaDetSCJ`). El backend (`RespuestaCabSCJService.saveRespuestas`) **persiste el detalle tal cual lo manda la app** — no consulta `items` ni deriva nada. `GET /jhonson/api/v1/respuestas` devuelve la entidad (con detalles) tal cual.
- **`jhonson.items.codigo` SÍ existe** en la base (tipo `integer`), poblado en ~100/111 ítems, valores únicos entre los no-nulos, **sin constraint UNIQUE**. Está desincronizado: no figura en el DDL del repo (`07-jhonson-app-tablas.sql`), ni en la entidad `ItemSCJ`, ni en el `ItemModel` de la app.
- `respuesta_det` **no** tiene `cod_item` todavía. Columnas: `id, id_respuesta_cab, id_item, desc_item, cod_cabecera, valor_1, valor_2, valor_3, comentario, sin_datos, activo`.
- El texto "SIN EXHIBICIONES ADICIONALES" nace en la app: `UiHelper.getTextoSinDatos(SE)` (constante hardcodeada). Se envía como `desc_item` con `idItem = 297` (constante `AppConstants.idItemSinExhibicion = 297`). El backend lo guarda verbatim.

## Decisiones

- **`cod_item` flexible:** columna `integer` nullable en `respuesta_det`, **sin FOREIGN KEY formal** (items.codigo no es UNIQUE y hay ítems sin código). Se llena por lógica.
- **No eliminar `id_item`:** se sigue guardando; `cod_item` es adicional.
- **Backend resuelve `cod_item`** desde `id_item` al guardar (solo back ahora). **Si la app ya manda `cod_item` en el payload, se respeta** (no se consulta) — habilita el futuro envío desde el front sin query extra.
- **"Sin Exhibición": ambos lados.** Override en backend (por `id_item == 297`) + corrección de la constante en la app.
- **Histórico:** se backfillea `cod_item` y se normaliza el texto en la misma migración.

## Backend (ccr-rest)

### Migración `src/main/resources/sql/12-jhonson-cod-item.sql`

```sql
-- items.codigo ya existe en prod; idempotente para entornos nuevos / sincronizar repo.
ALTER TABLE jhonson.items ADD COLUMN IF NOT EXISTS codigo integer;

-- Nueva columna flexible (sin FK): código de negocio del ítem en el detalle.
ALTER TABLE jhonson.respuesta_det ADD COLUMN IF NOT EXISTS cod_item integer;

-- Backfill histórico de cod_item desde items por id_item.
UPDATE jhonson.respuesta_det rd
SET cod_item = i.codigo
FROM jhonson.items i
WHERE rd.id_item = i.id AND rd.cod_item IS NULL;

-- Normalizar texto histórico del ítem especial "Sin Exhibición" (id 297).
UPDATE jhonson.respuesta_det SET desc_item = 'Sin Exhibición' WHERE id_item = 297;
```

Se agrega `12-jhonson-cod-item.sql` al orden de ejecución de scripts en `CLAUDE.md`.

### Entidad `ItemSCJ`

Mapear la columna existente:
```java
@Column(name = "codigo")
private Integer codigo;
```
+ getter/setter. Efecto colateral deseado: `GET /jhonson/api/v1/items` pasa a incluir `codigo`, lo que habilita a la app a enviarlo a futuro.

### Entidad `RespuestaDetSCJ`

Agregar (sin tocar `idItem`):
```java
@Column(name = "cod_item")
private Integer codItem;
```
+ getter/setter.

### Constante `ConstantsSCJ`

```java
public static final long ID_ITEM_SIN_EXHIBICION = 297L;
```

### `RespuestaCabSCJService.saveRespuestas`

En el procesamiento de detalles de cada respuesta (antes de persistir cada `RespuestaDetSCJ`):

1. **Resolver `cod_item`:** juntar los `idItem` de los detalles que vengan con `codItem == null`, hacer **un solo** `itemsRepository.findAllById(ids)`, armar un mapa `id → codigo`, y setear `d.setCodItem(map.get(d.getIdItem()))`. Si la app ya mandó `codItem`, no se toca. Ítems sin código quedan en `null` (flexible).
2. **Override "Sin Exhibición":** si `d.getIdItem() != null && d.getIdItem() == ID_ITEM_SIN_EXHIBICION`, `d.setDescItem("Sin Exhibición")`.

Requiere inyectar `ItemsSCJRepository` en el servicio (hoy no está). `findAllById` ya viene de `JpaRepository`.

## App (jhonson_ccr_app, Flutter)

Único cambio: en `lib/app/helpers/ui_helper.dart`, `getTextoSinDatos`, caso SE:
```dart
} else if (codCategoria == CodigoCabeceras.SE.name) {
  return 'Sin Exhibición';   // antes: 'SIN EXHIBICIONES ADICIONALES'
}
```

> El envío de `cod_item` desde la app (para evitar el lookup del back) queda **fuera de alcance** de esta entrega; el backend ya lo soporta cuando llegue.

## Manejo de errores / bordes

- `cod_item` nullable: ítems sin `codigo` (11 hoy) o `idItem` inexistente → `cod_item = null`, no rompe el insert.
- El override de texto se basa en el id estable `297`; aplica aunque la app vieja siga mandando el texto largo (compatibilidad hacia atrás).
- El backfill es seguro/re-ejecutable (`cod_item IS NULL`, y el UPDATE de texto por id puntual).

## Pruebas

Sin tests automatizados. Verificación:

**Backend** (`./mvnw -o -q compile` + manual contra BD local `localhost:5432/ccr`):
1. `POST /jhonson/api/v1/respuestas` con un detalle de un ítem con código → fila en `respuesta_det` con `cod_item` poblado y `id_item` intacto.
2. Detalle con `idItem=297` → `desc_item = 'Sin Exhibición'` y (si el ítem 297 tiene código) `cod_item` resuelto.
3. Payload que ya trae `codItem` → se respeta sin sobrescribir.
4. `GET /jhonson/api/v1/respuestas` → los detalles incluyen `codItem`. `GET /items` → incluye `codigo`.
5. Migración: `cod_item` backfilleado en históricos; textos "Sin Exhibición" normalizados.

**App** (`flutter analyze` / build): marcar sin exhibición en SE → el `descItem` enviado es "Sin Exhibición".

## Despliegue

1. Backend: correr `12-jhonson-cod-item.sql` en la BD del entorno, luego desplegar.
2. App: build + publicar (cambio menor de constante).
