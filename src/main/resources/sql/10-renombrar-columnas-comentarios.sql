-- 10: Renombrar columnas de comentarios en respuesta_cab
-- comentario_fi -> comentario_sr (Surtido Básico)
-- comentario_fo -> comentario_ea (Exhibiciones Adicionales)

ALTER TABLE jhonson.respuesta_cab RENAME COLUMN comentario_fi TO comentario_sr;
ALTER TABLE jhonson.respuesta_cab RENAME COLUMN comentario_fo TO comentario_ea;
