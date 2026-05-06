-- =============================================================================
-- 08: Relación N:M entre bocas y auditores (SCJ)
-- Prerequisito: 07-jhonson-app-tablas.sql
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Crear tabla intermedia boca_auditor
-- -----------------------------------------------------------------------------
CREATE TABLE jhonson.boca_auditor (
    id          serial       PRIMARY KEY,
    id_boca     integer      NOT NULL REFERENCES jhonson.bocas(id),
    auditor     varchar(200) NOT NULL,
    UNIQUE (id_boca, auditor)
);

-- -----------------------------------------------------------------------------
-- 2. Migrar datos existentes desde columna auditor de bocas
-- -----------------------------------------------------------------------------
INSERT INTO jhonson.boca_auditor (id_boca, auditor)
SELECT id, auditor FROM jhonson.bocas
WHERE auditor IS NOT NULL AND auditor != '';

-- -----------------------------------------------------------------------------
-- 3. Eliminar columna auditor de bocas
-- -----------------------------------------------------------------------------
ALTER TABLE jhonson.bocas DROP COLUMN auditor;

-- -----------------------------------------------------------------------------
-- 4. Privilegios
-- -----------------------------------------------------------------------------
GRANT ALL PRIVILEGES ON jhonson.boca_auditor TO ccr;
GRANT USAGE, SELECT ON jhonson.boca_auditor_id_seq TO ccr;
