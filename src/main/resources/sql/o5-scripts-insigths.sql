-- Insertar cliente para uso interno de CCR
INSERT INTO public.clientes
( codigo, nombre, ruc, direccion, contacto, sitio_web, correo, logo, activo)
VALUES('CCR', 'CCR uso interno', NULL, NULL, NULL, NULL, NULL, NULL, true);

CREATE SCHEMA ccr;


-- ============================================================
-- ccr.pais
-- ============================================================
CREATE TABLE ccr.pais (
    id                            SERIAL PRIMARY KEY,
    codigo                        CHARACTER VARYING(50)  NOT NULL,
    descripcion                   CHARACTER VARYING(200) NOT NULL,
    activo                        BOOLEAN                NOT NULL DEFAULT TRUE,
    fecha_creacion                TIMESTAMP NOT NULL default now(),
    nombre_usuario_creacion       CHARACTER VARYING(200)
);

ALTER TABLE ccr.pais
    ADD CONSTRAINT uk_pais_codigo UNIQUE (codigo);


-- ============================================================
-- ccr.cliente
-- ============================================================
CREATE TABLE ccr.cliente (
    id                            SERIAL PRIMARY KEY,
    codigo                        CHARACTER VARYING(50)  NOT NULL,
    descripcion                   CHARACTER VARYING(200) NOT NULL,
    cod_pais                      CHARACTER VARYING(50) NOT NULL,
    enabled                       BOOLEAN                NOT NULL DEFAULT TRUE,
    fecha_creacion                TIMESTAMP NOT NULL default now() ,
    nombre_usuario_creacion       CHARACTER VARYING(200),
    CONSTRAINT fk_cliente_pais FOREIGN KEY (cod_pais)
        REFERENCES ccr.pais (codigo)
);

ALTER TABLE ccr.cliente
    ADD CONSTRAINT uk_cliente_codigo UNIQUE (codigo);


-- ============================================================
-- Datos iniciales
-- Códigos de país según ISO 3166-1 alpha-2:
-- https://www.iso.org/obp/ui/#search/code/
-- ============================================================
INSERT INTO ccr.pais (codigo, descripcion, activo, fecha_creacion, nombre_usuario_creacion)
VALUES ('PY', 'Paraguay', TRUE, now(), 'admin');


-- Privilegios
GRANT USAGE ON SCHEMA ccr TO ccr;
GRANT ALL PRIVILEGES ON ALL TABLES   IN SCHEMA ccr TO ccr;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA ccr TO ccr;
