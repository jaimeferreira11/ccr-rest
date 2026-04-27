-- 09: Actualizar cabeceras jhonson
-- Renombrar códigos: FI -> SR (Surtido Básico), FO -> EA (Exhibiciones Adicionales)
-- Actualizar títulos para coincidir con diseño Figma
-- Eliminar columnas despensa y estacion_servicio de cabeceras e items

BEGIN;

-- 1. Insertar nuevos códigos en cabeceras
INSERT INTO jhonson.cabeceras (codigo, titulo, orden, descripcion, activo, autoservicio, despensa, estacion_servicio, supermercado)
VALUES ('SR', 'Surtido Básico', 1, '', true, true, false, false, false);
INSERT INTO jhonson.cabeceras (codigo, titulo, orden, descripcion, activo, autoservicio, despensa, estacion_servicio, supermercado)
VALUES ('EA', 'Exhibiciones Adicionales', 3, '', true, true, false, false, true);

-- 2. Migrar items a nuevos códigos
UPDATE jhonson.items SET cod_cabecera = 'SR' WHERE cod_cabecera = 'FI';
UPDATE jhonson.items SET cod_cabecera = 'EA' WHERE cod_cabecera = 'FO';

-- 3. Migrar respuestas históricas
UPDATE jhonson.respuesta_det SET cod_cabecera = 'SR' WHERE cod_cabecera = 'FI';
UPDATE jhonson.respuesta_det SET cod_cabecera = 'EA' WHERE cod_cabecera = 'FO';

-- 4. Eliminar cabeceras viejas
DELETE FROM jhonson.cabeceras WHERE codigo = 'FI';
DELETE FROM jhonson.cabeceras WHERE codigo = 'FO';

-- 5. Actualizar título de Plaza
UPDATE jhonson.cabeceras SET titulo = 'Plaza' WHERE codigo = 'FP';

COMMIT;

-- 6. Eliminar columnas no usadas (requiere owner/superuser)
-- ALTER TABLE jhonson.cabeceras DROP COLUMN IF EXISTS despensa;
-- ALTER TABLE jhonson.cabeceras DROP COLUMN IF EXISTS estacion_servicio;
-- ALTER TABLE jhonson.items DROP COLUMN IF EXISTS despensa;
-- ALTER TABLE jhonson.items DROP COLUMN IF EXISTS estacion_servicio;
