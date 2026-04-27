-- 11: Eliminar cabeceras FR (INFALTABLES) y TA (TACTICO) con sus items
-- Solo se usan: SR (Surtido Básico), EA (Exhibiciones Adicionales), FP (Plaza)

BEGIN;

-- 1. Eliminar items de FR y TA
DELETE FROM jhonson.items WHERE cod_cabecera IN ('FR', 'TA');

-- 2. Eliminar respuestas históricas (si existieran)
DELETE FROM jhonson.respuesta_det WHERE cod_cabecera IN ('FR', 'TA');

-- 3. Eliminar cabeceras
DELETE FROM jhonson.cabeceras WHERE codigo IN ('FR', 'TA');

-- 4. Eliminar columnas de comentarios no usadas
ALTER TABLE jhonson.respuesta_cab DROP COLUMN IF EXISTS comentario_fr;
ALTER TABLE jhonson.respuesta_cab DROP COLUMN IF EXISTS comentario_ta;

COMMIT;
