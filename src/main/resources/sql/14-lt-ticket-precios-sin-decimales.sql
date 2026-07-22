-- Migración 14: precio_regular y precio_promocional pasan de NUMERIC(15,2) a NUMERIC(15,0).
-- Los montos en guaraníes (Gs) no usan decimales; se elimina la parte decimal para que
-- la propia BD rechace/redondee cualquier valor decimal espurio enviado por LT.
-- Nota: el ALTER redondea los valores existentes al entero más cercano.
-- Ejecutar como owner de la tabla lt.ticket (postgres).

ALTER TABLE lt.ticket
    ALTER COLUMN precio_regular TYPE NUMERIC(15,0);

ALTER TABLE lt.ticket
    ALTER COLUMN precio_promocional TYPE NUMERIC(15,0);
