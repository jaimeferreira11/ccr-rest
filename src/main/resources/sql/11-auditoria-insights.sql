-- 11-auditoria-insights.sql
-- Auditoría de acciones administrativas del módulo Insights.
-- Ver docs/superpowers/specs/2026-06-04-auditoria-insights-design.md

CREATE TABLE IF NOT EXISTS ccr.auditoria_insights (
    id            BIGSERIAL    PRIMARY KEY,
    evento        VARCHAR(50)  NOT NULL,
    resultado     VARCHAR(20)  NOT NULL,
    usuario       VARCHAR(200) NOT NULL,
    cod_cliente   VARCHAR(50)  NOT NULL,
    cod_categoria VARCHAR(50),
    tipo_reporte  VARCHAR(20),
    fecha_hora    TIMESTAMP    NOT NULL DEFAULT now(),
    detalle       TEXT
);

CREATE INDEX IF NOT EXISTS idx_auditoria_ins_fecha   ON ccr.auditoria_insights (fecha_hora);
CREATE INDEX IF NOT EXISTS idx_auditoria_ins_evento  ON ccr.auditoria_insights (evento);
CREATE INDEX IF NOT EXISTS idx_auditoria_ins_cliente ON ccr.auditoria_insights (cod_cliente);
