-- Tabla de cotizaciones de monedas (schema public, reutilizable entre módulos)
CREATE TABLE IF NOT EXISTS public.cotizacion (
    id          SERIAL PRIMARY KEY,
    moneda      CHARACTER VARYING(10)   NOT NULL,
    valor       NUMERIC(18,4)           NOT NULL,
    fecha       DATE                    NOT NULL,
    fuente      CHARACTER VARYING(50),
    created_at  TIMESTAMP               NOT NULL DEFAULT now(),
    CONSTRAINT uk_cotizacion_moneda_fecha UNIQUE (moneda, fecha)
);
