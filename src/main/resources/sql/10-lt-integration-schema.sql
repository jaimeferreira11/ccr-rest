-- Schema para integración proveedor LT
CREATE SCHEMA IF NOT EXISTS lt;

CREATE TABLE lt.sucursal (
    id BIGSERIAL PRIMARY KEY,
    punto INTEGER NOT NULL,
    direccion VARCHAR(500),
    provincia VARCHAR(200),
    ciudad VARCHAR(200),
    mts2 NUMERIC(10,2),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    nombre_usuario_creacion VARCHAR(100),
    nombre_usuario_actualizacion VARCHAR(100),
    CONSTRAINT uk_lt_sucursal_punto UNIQUE (punto)
);

CREATE TABLE lt.producto (
    id BIGSERIAL PRIMARY KEY,
    eancode BIGINT NOT NULL,
    descripcion VARCHAR(500),
    id_sector INTEGER,
    sector VARCHAR(200),
    id_seccion INTEGER,
    seccion VARCHAR(200),
    id_categoria INTEGER,
    categoria VARCHAR(200),
    id_subcategoria INTEGER,
    subcategoria VARCHAR(200),
    fabricante VARCHAR(200),
    marca VARCHAR(200),
    contenido NUMERIC(10,3),
    pesovolumen NUMERIC(10,3),
    unidad_medida VARCHAR(50),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    nombre_usuario_creacion VARCHAR(100),
    nombre_usuario_actualizacion VARCHAR(100),
    CONSTRAINT uk_lt_producto_eancode UNIQUE (eancode)
);

CREATE TABLE lt.ticket (
    id BIGSERIAL PRIMARY KEY,
    punto INTEGER NOT NULL,
    nro_ticket VARCHAR(50) NOT NULL,
    fecha DATE NOT NULL,
    hora TIME,
    eancode BIGINT NOT NULL,
    ean_desc VARCHAR(500),
    unidades_vendidas INTEGER,
    precio_regular NUMERIC(15,2),
    precio_promocional NUMERIC(15,2),
    tipo_venta VARCHAR(10),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    nombre_usuario_creacion VARCHAR(100),
    nombre_usuario_actualizacion VARCHAR(100),
    CONSTRAINT uk_lt_ticket UNIQUE (punto, nro_ticket, eancode)
);

CREATE TABLE lt.persona (
    id BIGSERIAL PRIMARY KEY,
    punto INTEGER NOT NULL,
    nro_ticket VARCHAR(50) NOT NULL,
    fecha DATE NOT NULL,
    identificacion VARCHAR(50),
    nombre_y_apellido_empresa VARCHAR(500),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    nombre_usuario_creacion VARCHAR(100),
    nombre_usuario_actualizacion VARCHAR(100),
    CONSTRAINT uk_lt_persona UNIQUE (punto, nro_ticket, identificacion)
);

-- Permisos para el usuario de aplicación
GRANT USAGE ON SCHEMA lt TO ccr;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA lt TO ccr;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA lt TO ccr;
ALTER DEFAULT PRIVILEGES IN SCHEMA lt GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ccr;
ALTER DEFAULT PRIVILEGES IN SCHEMA lt GRANT USAGE, SELECT ON SEQUENCES TO ccr;
