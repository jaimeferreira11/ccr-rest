-- Migración 13: unidades_vendidas pasa de INTEGER a NUMERIC(15,3)
-- para admitir cantidades con decimales (ventas por peso/fracción).
-- Ejecutar como owner de la tabla lt.ticket (postgres).

ALTER TABLE lt.ticket
    ALTER COLUMN unidades_vendidas TYPE NUMERIC(15,3);
