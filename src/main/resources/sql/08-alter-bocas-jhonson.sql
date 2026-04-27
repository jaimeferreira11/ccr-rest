-- 08: Ajustar tabla jhonson.bocas para coincidir con producción
-- Eliminar columna mes_ultima_medicion (no existe en prod)
-- Agregar columnas fecha_creacion y Auditor

ALTER TABLE jhonson.bocas DROP COLUMN IF EXISTS mes_ultima_medicion;

ALTER TABLE jhonson.bocas ADD COLUMN IF NOT EXISTS fecha_creacion timestamp NOT NULL DEFAULT now();

ALTER TABLE jhonson.bocas ADD COLUMN IF NOT EXISTS "Auditor" varchar(200) NOT NULL DEFAULT '';
