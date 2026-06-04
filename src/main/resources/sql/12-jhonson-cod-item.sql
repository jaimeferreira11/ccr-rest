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
