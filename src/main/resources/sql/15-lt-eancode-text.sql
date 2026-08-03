-- Migración 15: eancode pasa de BIGINT a VARCHAR en lt.producto y lt.ticket.
-- Motivo: robustecer el almacenamiento interno frente a futuros cambios de formato
-- (ceros a la izquierda, longitudes variables GTIN-8/12/13/14) sin tocar el contrato
-- JSON vigente con el proveedor LT, que sigue enviando eancode como número entero.
-- Ejecutar como owner de las tablas (postgres), igual que las migraciones 12 y 14.

ALTER TABLE lt.producto
    ALTER COLUMN eancode TYPE VARCHAR(20) USING eancode::text;

ALTER TABLE lt.ticket
    ALTER COLUMN eancode TYPE VARCHAR(20) USING eancode::text;
