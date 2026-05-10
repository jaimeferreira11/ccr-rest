-- =============================================================================
-- 07: Tablas para la app mobile de SCJ (Johnson)
-- Unificación de scripts 07 al 11 — para instalación desde cero.
-- Prerequisitos: creacion_tablas_scj.sql (schema jhonson, distribuidores, bocas, reportes).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- bocas: agregar columna auditor (requerida por la app)
-- -----------------------------------------------------------------------------
ALTER TABLE jhonson.bocas ADD COLUMN IF NOT EXISTS auditor varchar(200) NOT NULL DEFAULT '';


-- -----------------------------------------------------------------------------
-- cabeceras
-- -----------------------------------------------------------------------------
CREATE TABLE jhonson.cabeceras (
    id              serial          PRIMARY KEY,
    codigo          varchar(100)    NOT NULL,
    titulo          varchar(200)    NOT NULL,
    descripcion     varchar(300),
    activo          boolean         NOT NULL DEFAULT true,
    autoservicio    boolean         NOT NULL DEFAULT false,
    supermercado    boolean         NOT NULL DEFAULT false,
    orden           int
);

ALTER TABLE jhonson.cabeceras ADD CONSTRAINT uk_codigo_cabecera UNIQUE (codigo);


-- -----------------------------------------------------------------------------
-- items
-- -----------------------------------------------------------------------------
CREATE TABLE jhonson.items (
    id              serial          PRIMARY KEY,
    leyenda         varchar(300),
    descripcion     varchar(200)    NOT NULL,
    cod_cabecera    varchar(100)    NOT NULL,
    pregunta        varchar(500),
    activo          boolean         NOT NULL DEFAULT true,
    autoservicio    boolean         NOT NULL DEFAULT false,
    supermercado    boolean         NOT NULL DEFAULT false,
    imagen          varchar(300)    NOT NULL,
    categoria       varchar(200),
    orden           int,
    precios         jsonb           DEFAULT '[]',
    FOREIGN KEY (cod_cabecera) REFERENCES jhonson.cabeceras (codigo)
);


-- -----------------------------------------------------------------------------
-- respuesta_cab
-- -----------------------------------------------------------------------------
CREATE TABLE jhonson.respuesta_cab (
    id              serial          PRIMARY KEY,
    id_boca         integer         NOT NULL,
    cod_boca        varchar(50)     NOT NULL,
    desc_boca       varchar(200)    NOT NULL,
    canal_ccr       varchar(200)    NOT NULL,
    usuario         varchar(200)    NOT NULL,
    longitud        varchar(500)    NOT NULL,
    latitud         varchar(500)    NOT NULL,
    fecha_creacion  varchar(100),
    hora_inicio     varchar(20),
    hora_fin        varchar(20),
    comentario_si   varchar(500),
    comentario_sp   varchar(500),
    comentario_se   varchar(500),
    activo          boolean         NOT NULL DEFAULT true,
    FOREIGN KEY (id_boca) REFERENCES jhonson.bocas (id)
);


-- -----------------------------------------------------------------------------
-- respuesta_imagen
-- -----------------------------------------------------------------------------
CREATE TABLE jhonson.respuesta_imagen (
    id                  serial          PRIMARY KEY,
    id_respuesta_cab    integer         NOT NULL,
    path_imagen         varchar(300)    NOT NULL,
    cod_cabecera        varchar(100)    NOT NULL,
    activo              boolean         NOT NULL DEFAULT true,
    FOREIGN KEY (id_respuesta_cab) REFERENCES jhonson.respuesta_cab (id)
);


-- -----------------------------------------------------------------------------
-- respuesta_det
-- -----------------------------------------------------------------------------
CREATE TABLE jhonson.respuesta_det (
    id                  serial          PRIMARY KEY,
    id_respuesta_cab    integer         NOT NULL,
    id_item             integer         NOT NULL,
    desc_item           varchar(200)    NOT NULL,
    cod_cabecera        varchar(100)    NOT NULL,
    valor_1             varchar(200),
    valor_2             varchar(200),
    valor_3             varchar(200),
    comentario          varchar(500),
    sin_datos           boolean         NOT NULL DEFAULT false,
    activo              boolean         NOT NULL DEFAULT true,
    FOREIGN KEY (id_respuesta_cab) REFERENCES jhonson.respuesta_cab (id),
    FOREIGN KEY (cod_cabecera)     REFERENCES jhonson.cabeceras (codigo)
);


-- -----------------------------------------------------------------------------
-- usuario_distribuidor
-- -----------------------------------------------------------------------------
CREATE TABLE jhonson.usuario_distribuidor (
    id                  serial          PRIMARY KEY,
    usuario             varchar(100)    NOT NULL,
    cod_distribuidor    varchar(100)    NOT NULL,
    FOREIGN KEY (usuario)           REFERENCES public.usuarios (usuario),
    FOREIGN KEY (cod_distribuidor)  REFERENCES jhonson.distribuidores (codigo),
    UNIQUE (usuario, cod_distribuidor)
);


-- -----------------------------------------------------------------------------
-- Privilegios
-- -----------------------------------------------------------------------------
GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA jhonson TO ccr;
GRANT USAGE, SELECT ON ALL SEQUENCES  IN SCHEMA jhonson TO ccr;
