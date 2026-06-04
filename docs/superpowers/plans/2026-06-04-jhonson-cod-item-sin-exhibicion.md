# Jhonson — cod_item + texto "Sin Exhibición" — Implementation Plan

> **Ejecución:** inline en esta sesión. **NO commitear** — el usuario revisa los cambios en el working tree de ambos repos. Steps con checkbox (`- [ ]`).

**Goal:** Que `respuesta_det` lleve `cod_item` (código de negocio del ítem, resuelto en el back desde `id_item`, respetando el valor si la app lo manda) y que el detalle de "Sin Exhibición" guarde `desc_item="Sin Exhibición"`, todo funcionando con solo desplegar el backend.

**Architecture:** Migración SQL (columna flexible + backfill), mapear `items.codigo` y agregar `respuesta_det.cod_item` en las entidades, y lógica en `RespuestaCabSCJService.saveRespuestas` (lookup batch de código por `id_item` + override de texto por `id_item==297`). Cambio cosmético en la app (constante de texto).

**Tech Stack:** Spring Boot 2.7 / JPA / Java 8 (ccr-rest); Flutter (jhonson_ccr_app); PostgreSQL schema `jhonson`.

> **Verificado en BD local `localhost:5432/ccr`:** `jhonson.items.codigo` existe (`integer`, ~100/111 poblado, sin UNIQUE); `respuesta_det` aún no tiene `cod_item`.

> **Dos repos:** backend `/Users/jaime/development/workspace-sts/ccr-rest` (rama `feature/jhonson-cod-item`); app `/Users/jaime/development/workspace-flutter/jhonson_ccr_app`.

> **Java 8:** sin `var`, sin `Map.of`, sin records.

---

## PARTE A — Backend (ccr-rest)

### Task 1: Migración SQL

**Files:**
- Create: `src/main/resources/sql/12-jhonson-cod-item.sql`
- Modify: `CLAUDE.md` (orden de ejecución de scripts)

- [ ] **Step 1: Crear el script**

```sql
-- 12-jhonson-cod-item.sql
-- Agrega cod_item (código de negocio del ítem) al detalle de respuestas SCJ
-- y normaliza el texto del ítem especial "Sin Exhibición".
-- Ver docs/superpowers/specs/2026-06-04-jhonson-cod-item-sin-exhibicion-design.md

-- items.codigo ya existe en prod; idempotente para entornos nuevos / sincronizar repo.
ALTER TABLE jhonson.items ADD COLUMN IF NOT EXISTS codigo integer;

-- Nueva columna flexible (sin FK formal): código de negocio del ítem en el detalle.
ALTER TABLE jhonson.respuesta_det ADD COLUMN IF NOT EXISTS cod_item integer;

-- Backfill histórico de cod_item desde items por id_item.
UPDATE jhonson.respuesta_det rd
SET cod_item = i.codigo
FROM jhonson.items i
WHERE rd.id_item = i.id AND rd.cod_item IS NULL;

-- Normalizar texto histórico del ítem especial "Sin Exhibición" (id 297).
UPDATE jhonson.respuesta_det SET desc_item = 'Sin Exhibición' WHERE id_item = 297;
```

- [ ] **Step 2: Agregar al orden en CLAUDE.md**

En `CLAUDE.md`, en la lista numerada de scripts de la sección Database, después de la línea `8. \`11-auditoria-insights.sql\`` agregar:

```
9. `12-jhonson-cod-item.sql`
```

- [ ] **Step 3: NO commitear** (queda para revisión).

---

### Task 2: Mapear `codigo` en `ItemSCJ`

**Files:**
- Modify: `src/main/java/py/com/jaimeferreira/ccr/jhonson/entity/ItemSCJ.java`

- [ ] **Step 1: Agregar el campo**

Después del campo `id` (antes de `leyenda`):

```java
    @Column(name = "codigo")
    private Integer codigo;
```

- [ ] **Step 2: Agregar getter/setter**

Junto a los demás (por ejemplo después de `setId`):

```java
    public Integer getCodigo() {
        return codigo;
    }

    public void setCodigo(Integer codigo) {
        this.codigo = codigo;
    }
```

- [ ] **Step 3: Compilar**

Run: `./mvnw -o -q compile`
Expected: exit 0.

- [ ] **Step 4: NO commitear.**

---

### Task 3: Agregar `codItem` a `RespuestaDetSCJ`

**Files:**
- Modify: `src/main/java/py/com/jaimeferreira/ccr/jhonson/entity/RespuestaDetSCJ.java`

- [ ] **Step 1: Agregar el campo**

Después del campo `idItem` (que NO se elimina), agregar:

```java
    @Column(name = "cod_item")
    private Integer codItem;
```

- [ ] **Step 2: Agregar getter/setter**

Junto a los demás (por ejemplo después de `setIdItem`):

```java
    public Integer getCodItem() {
        return codItem;
    }

    public void setCodItem(Integer codItem) {
        this.codItem = codItem;
    }
```

- [ ] **Step 3: Compilar**

Run: `./mvnw -o -q compile`
Expected: exit 0.

- [ ] **Step 4: NO commitear.**

---

### Task 4: Constante del ítem especial

**Files:**
- Modify: `src/main/java/py/com/jaimeferreira/ccr/jhonson/constants/ConstantsSCJ.java`

- [ ] **Step 1: Agregar la constante**

Dentro de la clase `ConstantsSCJ`, junto a `URL_PROD_IMAGES`:

```java
    /** Id del ítem especial "Sin Exhibición" (cabecera SE). Coincide con AppConstants.idItemSinExhibicion en la app. */
    public static final long ID_ITEM_SIN_EXHIBICION = 297L;
```

- [ ] **Step 2: Compilar**

Run: `./mvnw -o -q compile`
Expected: exit 0.

- [ ] **Step 3: NO commitear.**

---

### Task 5: Lógica en `RespuestaCabSCJService.saveRespuestas`

**Files:**
- Modify: `src/main/java/py/com/jaimeferreira/ccr/jhonson/service/RespuestaCabSCJService.java`

- [ ] **Step 1: Agregar imports**

Junto a los imports existentes:

```java
import java.util.HashMap;
import java.util.Map;

import py.com.jaimeferreira.ccr.jhonson.constants.ConstantsSCJ;
import py.com.jaimeferreira.ccr.jhonson.entity.ItemSCJ;
import py.com.jaimeferreira.ccr.jhonson.repository.ItemsSCJRepository;
```

> `java.util.List`, `java.util.stream.Collectors`, `RespuestaDetSCJ` ya están importados.

- [ ] **Step 2: Inyectar el repositorio de items**

Junto a los demás `@Autowired` de la clase:

```java
    @Autowired
    ItemsSCJRepository itemsRepository;
```

- [ ] **Step 3: Reemplazar el bloque de detalles**

Hoy el bloque es:

```java
            // detalles
            r.getDetalles().stream().forEach(d -> {
                d.setIdRespuestaCab(cab.getId());

                detRepository.save(d);

            });
```

Reemplazarlo por:

```java
            // detalles
            List<RespuestaDetSCJ> detalles = r.getDetalles();

            // Resolver cod_item desde items SOLO para los detalles que la app no envió ya con código.
            // Un único query (findAllById) para evitar N consultas.
            List<Long> idsSinCodigo = detalles.stream()
                    .filter(d -> d.getCodItem() == null && d.getIdItem() != null)
                    .map(RespuestaDetSCJ::getIdItem)
                    .distinct()
                    .collect(Collectors.toList());

            Map<Long, Integer> codigoPorItem = new HashMap<>();
            if (!idsSinCodigo.isEmpty()) {
                for (ItemSCJ item : itemsRepository.findAllById(idsSinCodigo)) {
                    if (item.getCodigo() != null) {
                        codigoPorItem.put(item.getId(), item.getCodigo());
                    }
                }
            }

            detalles.forEach(d -> {
                d.setIdRespuestaCab(cab.getId());

                // cod_item: respetar el que mandó la app; si no vino, resolverlo desde items.
                if (d.getCodItem() == null && d.getIdItem() != null) {
                    d.setCodItem(codigoPorItem.get(d.getIdItem()));
                }

                // "Sin Exhibición": normalizar el texto del ítem especial (id 297).
                if (d.getIdItem() != null && d.getIdItem() == ConstantsSCJ.ID_ITEM_SIN_EXHIBICION) {
                    d.setDescItem("Sin Exhibición");
                }

                detRepository.save(d);
            });
```

- [ ] **Step 4: Compilar**

Run: `./mvnw -o -q compile`
Expected: exit 0.

- [ ] **Step 5: NO commitear.**

---

## PARTE B — App (jhonson_ccr_app)

### Task 6: Texto "Sin Exhibición" en la app

**Files:**
- Modify: `lib/app/helpers/ui_helper.dart`

- [ ] **Step 1: Cambiar el texto del caso SE en `getTextoSinDatos`**

Hoy:

```dart
    } else if (codCategoria == CodigoCabeceras.SE.name) {
      return 'SIN EXHIBICIONES ADICIONALES';
    } else if (codCategoria == CodigoCabeceras.SP.name) {
```

Cambiar la línea del return SE por:

```dart
    } else if (codCategoria == CodigoCabeceras.SE.name) {
      return 'Sin Exhibición';
    } else if (codCategoria == CodigoCabeceras.SP.name) {
```

> No tocar `getCabeceraTexto` (esa es la etiqueta de la cabecera "EXHIBICIONES ADICIONALES", no el `desc_item`).

- [ ] **Step 2: Verificar análisis estático**

Run (desde `/Users/jaime/development/workspace-flutter/jhonson_ccr_app`): `flutter analyze lib/app/helpers/ui_helper.dart`
Expected: sin errores nuevos.

- [ ] **Step 3: NO commitear.**

---

## Task 7: Verificación (incluye "solo back ⇒ la base toma los ajustes")

**Files:** ninguno.

- [ ] **Step 1: Compilación backend completa**

Run: `./mvnw -o -q compile`
Expected: exit 0.

- [ ] **Step 2: Aplicar la migración en la BD local (dev) y verificar el esquema**

```bash
PW=$(grep -E "datasource.password" src/main/resources/application-dev.properties | sed -E 's/.*password=//' | tr -d ' \r')
PGPASSWORD="$PW" psql -h localhost -p 5432 -U ccr -d ccr -f src/main/resources/sql/12-jhonson-cod-item.sql
PGPASSWORD="$PW" psql -h localhost -p 5432 -U ccr -d ccr -tAc "SELECT column_name FROM information_schema.columns WHERE table_schema='jhonson' AND table_name='respuesta_det' AND column_name='cod_item';"
```
Expected: el segundo comando devuelve `cod_item` (columna creada). El backfill puede afectar 0+ filas.

- [ ] **Step 3: Confirmar que el back-only cubre ambos ajustes (revisión lógica)**

Confirmar en `RespuestaCabSCJService.saveRespuestas` que:
- `cod_item` se setea desde `items` por `id_item` cuando la app no lo manda (la app actual NO lo manda) → **se llena solo con el back**.
- `desc_item` se fuerza a "Sin Exhibición" cuando `id_item==297`, **independiente de lo que mande la app** → **se corrige solo con el back**.

Esto demuestra que **desplegando solo el backend + corriendo la migración**, la base recibe ambos ajustes sin necesidad de actualizar la app. (El cambio de la app es solo para consistencia del texto que muestra/envía.)

- [ ] **Step 4: Dejar TODO sin commitear**

```bash
cd /Users/jaime/development/workspace-sts/ccr-rest && git status --short
cd /Users/jaime/development/workspace-flutter/jhonson_ccr_app && git status --short
```
Expected: archivos modificados/nuevos listados, **sin commits nuevos** (el usuario revisa).

---

## Self-Review (cobertura del spec)

- **cod_item flexible (integer null, sin FK) + backfill** → Task 1. ✓
- **items.codigo idempotente** → Task 1. ✓
- **Mapear ItemSCJ.codigo (y exponerlo en GET /items)** → Task 2. ✓
- **RespuestaDetSCJ.codItem, sin eliminar idItem** → Task 3. ✓
- **Constante 297** → Task 4. ✓
- **saveRespuestas: lookup batch, respeta payload, override texto 297** → Task 5. ✓
- **App: getTextoSinDatos SE → "Sin Exhibición"** → Task 6. ✓
- **Verificar back-only ⇒ base con ajustes** → Task 7. ✓
- **No commitear (revisión del usuario)** → todas las tareas. ✓

Sin placeholders. Nombres/tipos consistentes: `codigo` (Integer), `codItem` (Integer), `ID_ITEM_SIN_EXHIBICION` (long 297), `itemsRepository.findAllById`.
