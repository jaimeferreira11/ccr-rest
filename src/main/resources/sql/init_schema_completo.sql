-- =============================================================================
-- Script consolidado: creación completa del esquema para ccr-rest
-- Schemas: public, bebidas_py, nestle, shell, jhonson
-- =============================================================================

-- =====================
-- SCHEMAS
-- =====================
CREATE SCHEMA IF NOT EXISTS bebidas_py;
CREATE SCHEMA IF NOT EXISTS nestle;
CREATE SCHEMA IF NOT EXISTS shell;
CREATE SCHEMA IF NOT EXISTS jhonson;


-- =============================================================================
-- SCHEMA: public
-- =============================================================================

CREATE TABLE IF NOT EXISTS public.usuarios (
    id               SERIAL PRIMARY KEY,
    usuario          CHARACTER VARYING(200) NOT NULL,
    password         CHARACTER VARYING(200) NOT NULL,
    nombre_apellido  CHARACTER VARYING(300) NOT NULL,
    activo           BOOLEAN NOT NULL DEFAULT TRUE,
    cod_cliente      CHARACTER VARYING(200),
    externo          BOOLEAN NOT NULL DEFAULT FALSE
);

ALTER TABLE public.usuarios ADD CONSTRAINT uq_usuario UNIQUE (usuario);

CREATE TABLE IF NOT EXISTS public.roles (
    id  SERIAL PRIMARY KEY,
    rol CHARACTER VARYING(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS public.user_roles (
    id      SERIAL PRIMARY KEY,
    rol     CHARACTER VARYING(100) NOT NULL,
    usuario CHARACTER VARYING(200) NOT NULL,
    CONSTRAINT fk_user_roles_rol     FOREIGN KEY (rol)     REFERENCES public.roles(rol)     ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_usuario FOREIGN KEY (usuario) REFERENCES public.usuarios(usuario) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.clientes (
    id         SERIAL PRIMARY KEY,
    codigo     CHARACTER VARYING(200) NOT NULL UNIQUE,
    nombre     CHARACTER VARYING(200) NOT NULL,
    ruc        CHARACTER VARYING(200),
    direccion  CHARACTER VARYING(300),
    contacto   CHARACTER VARYING(300),
    sitio_web  CHARACTER VARYING(300),
    correo     CHARACTER VARYING(300),
    logo       BYTEA,
    activo     BOOLEAN NOT NULL DEFAULT TRUE
);


-- =============================================================================
-- SCHEMA: bebidas_py
-- =============================================================================

CREATE TABLE IF NOT EXISTS bebidas_py.bocas (
    id        SERIAL PRIMARY KEY,
    cod_boca  CHARACTER VARYING(50)  NOT NULL,
    nombre    CHARACTER VARYING(200) NOT NULL,
    direccion CHARACTER VARYING(200),
    ciudad    CHARACTER VARYING(200) NOT NULL,
    tipo_boca CHARACTER VARYING(200),
    ocasion   CHARACTER VARYING(200) NOT NULL,
    activo    BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE bebidas_py.bocas ADD CONSTRAINT uk_bebidas_cod_boca UNIQUE (cod_boca);

CREATE TABLE IF NOT EXISTS bebidas_py.cabeceras (
    id          SERIAL PRIMARY KEY,
    codigo      CHARACTER VARYING(100) NOT NULL,
    titulo      CHARACTER VARYING(200) NOT NULL,
    descripcion CHARACTER VARYING(300),
    activo      BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE bebidas_py.cabeceras ADD CONSTRAINT uk_bebidas_codigo_cabecera UNIQUE (codigo);

CREATE TABLE IF NOT EXISTS bebidas_py.items (
    id           SERIAL PRIMARY KEY,
    codigo       BIGINT,
    descripcion  CHARACTER VARYING(200) NOT NULL,
    leyenda      CHARACTER VARYING(300),
    cod_cabecera CHARACTER VARYING(100) NOT NULL,
    pregunta     CHARACTER VARYING(500),
    ocasion      CHARACTER VARYING(200) NOT NULL,
    activo       BOOLEAN NOT NULL DEFAULT TRUE,
    imagen       CHARACTER VARYING(300) NOT NULL,
    CONSTRAINT fk_bebidas_items_cabecera FOREIGN KEY (cod_cabecera) REFERENCES bebidas_py.cabeceras(codigo)
);

CREATE TABLE IF NOT EXISTS bebidas_py.respuesta_cab (
    id             SERIAL PRIMARY KEY,
    id_boca        BIGINT NOT NULL,
    cod_boca       CHARACTER VARYING(50)  NOT NULL,
    desc_boca      CHARACTER VARYING(200) NOT NULL,
    usuario        CHARACTER VARYING(200) NOT NULL,
    longitud       CHARACTER VARYING(500) NOT NULL,
    latitud        CHARACTER VARYING(500) NOT NULL,
    fecha_creacion CHARACTER VARYING(100),
    hora_inicio    CHARACTER VARYING(20),
    hora_fin       CHARACTER VARYING(20),
    activo         BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS bebidas_py.respuesta_det (
    id               SERIAL PRIMARY KEY,
    id_respuesta_cab BIGINT NOT NULL,
    id_item          BIGINT NOT NULL,
    desc_item        CHARACTER VARYING(200) NOT NULL,
    cabecera         CHARACTER VARYING(200) NOT NULL,
    valor            CHARACTER VARYING(10)  NOT NULL,
    comentario       CHARACTER VARYING(500),
    precio           CHARACTER VARYING(100),
    activo           BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_bebidas_det_cab FOREIGN KEY (id_respuesta_cab) REFERENCES bebidas_py.respuesta_cab(id)
);

CREATE TABLE IF NOT EXISTS bebidas_py.respuesta_imagen (
    id               SERIAL PRIMARY KEY,
    id_respuesta_cab BIGINT NOT NULL,
    path_imagen      CHARACTER VARYING(300) NOT NULL,
    activo           BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_bebidas_imagen_cab FOREIGN KEY (id_respuesta_cab) REFERENCES bebidas_py.respuesta_cab(id)
);


-- =============================================================================
-- SCHEMA: nestle
-- =============================================================================

CREATE TABLE IF NOT EXISTS nestle.distribuidores (
    id          SERIAL PRIMARY KEY,
    descripcion CHARACTER VARYING(300) NOT NULL,
    codigo      CHARACTER VARYING(100) NOT NULL,
    activo      BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE nestle.distribuidores ADD CONSTRAINT uk_nestle_cod_distribuidor UNIQUE (codigo);

CREATE TABLE IF NOT EXISTS nestle.bocas (
    id                  SERIAL PRIMARY KEY,
    cod_boca            CHARACTER VARYING(50)  NOT NULL,
    nombre              CHARACTER VARYING(200) NOT NULL,
    direccion           CHARACTER VARYING(200),
    ciudad              CHARACTER VARYING(200) NOT NULL,
    canal_ccr           CHARACTER VARYING(200) NOT NULL,
    ocasion             CHARACTER VARYING(200),
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    longitud            CHARACTER VARYING(500),
    latitud             CHARACTER VARYING(500),
    fecha_creacion      TIMESTAMP NOT NULL DEFAULT NOW(),
    mes_ultima_medicion CHARACTER VARYING(200) NOT NULL DEFAULT 'ENERO',
    cod_distribuidor    CHARACTER VARYING(100),
    CONSTRAINT fk_nestle_bocas_distribuidor FOREIGN KEY (cod_distribuidor) REFERENCES nestle.distribuidores(codigo),
    CONSTRAINT chk_nestle_mes_valido CHECK (mes_ultima_medicion IN (
        'ENERO','FEBRERO','MARZO','ABRIL','MAYO','JUNIO',
        'JULIO','AGOSTO','SETIEMBRE','SEPTIEMBRE','OCTUBRE','NOVIEMBRE','DICIEMBRE'
    )),
    CONSTRAINT chk_nestle_canal CHECK (canal_ccr IN (
        'AUTOSERVICIO','DESPENSA','ESTACION DE SERVICIO','SUPERMERCADO'
    ))
);

ALTER TABLE nestle.bocas ADD CONSTRAINT uk_nestle_cod_boca UNIQUE (cod_boca);

CREATE TABLE IF NOT EXISTS nestle.cabeceras (
    id                SERIAL PRIMARY KEY,
    codigo            CHARACTER VARYING(100) NOT NULL,
    titulo            CHARACTER VARYING(200) NOT NULL,
    orden             INTEGER,
    descripcion       CHARACTER VARYING(300),
    activo            BOOLEAN NOT NULL DEFAULT TRUE,
    autoservicio      BOOLEAN NOT NULL DEFAULT FALSE,
    despensa          BOOLEAN NOT NULL DEFAULT FALSE,
    estacion_servicio BOOLEAN NOT NULL DEFAULT FALSE,
    supermercado      BOOLEAN NOT NULL DEFAULT FALSE
);

ALTER TABLE nestle.cabeceras ADD CONSTRAINT uk_nestle_codigo_cabecera UNIQUE (codigo);

CREATE TABLE IF NOT EXISTS nestle.items (
    id                SERIAL PRIMARY KEY,
    codigo            INTEGER,
    descripcion       CHARACTER VARYING(200) NOT NULL,
    leyenda           CHARACTER VARYING(300),
    cod_cabecera      CHARACTER VARYING(100) NOT NULL,
    pregunta          CHARACTER VARYING(500),
    autoservicio      BOOLEAN NOT NULL DEFAULT FALSE,
    despensa          BOOLEAN NOT NULL DEFAULT FALSE,
    estacion_servicio BOOLEAN NOT NULL DEFAULT FALSE,
    supermercado      BOOLEAN NOT NULL DEFAULT FALSE,
    categoria         CHARACTER VARYING(200),
    activo            BOOLEAN NOT NULL DEFAULT TRUE,
    imagen            CHARACTER VARYING(300) NOT NULL,
    orden             INTEGER,
    precios           JSONB DEFAULT '[]',
    CONSTRAINT fk_nestle_items_cabecera FOREIGN KEY (cod_cabecera) REFERENCES nestle.cabeceras(codigo)
);

CREATE TABLE IF NOT EXISTS nestle.respuesta_cab (
    id              SERIAL PRIMARY KEY,
    id_boca         BIGINT NOT NULL,
    cod_boca        CHARACTER VARYING(50)  NOT NULL,
    desc_boca       CHARACTER VARYING(200) NOT NULL,
    canal_ccr       CHARACTER VARYING(200) NOT NULL,
    usuario         CHARACTER VARYING(200) NOT NULL,
    longitud        CHARACTER VARYING(500) NOT NULL,
    latitud         CHARACTER VARYING(500) NOT NULL,
    fecha_sinc      TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_creacion  CHARACTER VARYING(100),
    hora_inicio     CHARACTER VARYING(20),
    hora_fin        CHARACTER VARYING(20),
    comentario_fi   CHARACTER VARYING(500),
    comentario_fo   CHARACTER VARYING(500),
    comentario_fp   CHARACTER VARYING(500),
    comentario_fr   CHARACTER VARYING(500),
    comentario_ta   CHARACTER VARYING(500),
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_nestle_rcab_boca FOREIGN KEY (id_boca) REFERENCES nestle.bocas(id)
);

CREATE TABLE IF NOT EXISTS nestle.respuesta_det (
    id               SERIAL PRIMARY KEY,
    id_respuesta_cab BIGINT NOT NULL,
    id_item          BIGINT NOT NULL,
    desc_item        CHARACTER VARYING(200) NOT NULL,
    cod_cabecera     CHARACTER VARYING(100) NOT NULL,
    valor_1          CHARACTER VARYING(200),
    valor_2          CHARACTER VARYING(200),
    valor_3          CHARACTER VARYING(200),
    comentario       CHARACTER VARYING(500),
    sin_datos        BOOLEAN NOT NULL DEFAULT FALSE,
    activo           BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_nestle_rdet_cab     FOREIGN KEY (id_respuesta_cab) REFERENCES nestle.respuesta_cab(id),
    CONSTRAINT fk_nestle_rdet_cabecera FOREIGN KEY (cod_cabecera)     REFERENCES nestle.cabeceras(codigo)
);

CREATE TABLE IF NOT EXISTS nestle.respuesta_imagen (
    id               SERIAL PRIMARY KEY,
    id_respuesta_cab BIGINT NOT NULL,
    cod_cabecera     CHARACTER VARYING(100) NOT NULL,
    path_imagen      CHARACTER VARYING(300) NOT NULL,
    activo           BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion   TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_nestle_rimagen_cab FOREIGN KEY (id_respuesta_cab) REFERENCES nestle.respuesta_cab(id)
);

CREATE TABLE IF NOT EXISTS nestle.reportes (
    id               SERIAL PRIMARY KEY,
    cod_distribuidor CHARACTER VARYING(50)  NOT NULL,
    descripcion      CHARACTER VARYING(500),
    mes              CHARACTER VARYING(2)   NOT NULL,
    anio             INTEGER               NOT NULL,
    usuario          CHARACTER VARYING(50)  NOT NULL,
    pdf              BOOLEAN NOT NULL DEFAULT FALSE,
    ppt              BOOLEAN NOT NULL DEFAULT FALSE,
    bocas            JSONB NOT NULL,
    detalles         JSONB NOT NULL,
    fecha_creacion   TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_borrado    TIMESTAMP,
    path_pdf         TEXT,
    path_ppt         TEXT,
    CONSTRAINT fk_nestle_reportes_distribuidor FOREIGN KEY (cod_distribuidor) REFERENCES nestle.distribuidores(codigo),
    CONSTRAINT fk_nestle_reportes_usuario      FOREIGN KEY (usuario)          REFERENCES public.usuarios(usuario)
);

CREATE TABLE IF NOT EXISTS nestle.usuario_distribuidor (
    id               SERIAL PRIMARY KEY,
    usuario          CHARACTER VARYING(100) NOT NULL,
    cod_distribuidor CHARACTER VARYING(100) NOT NULL,
    CONSTRAINT fk_nest_ud_usuario      FOREIGN KEY (usuario)          REFERENCES public.usuarios(usuario),
    CONSTRAINT fk_nest_ud_distribuidor FOREIGN KEY (cod_distribuidor) REFERENCES nestle.distribuidores(codigo),
    CONSTRAINT uq_nestle_usuario_dist  UNIQUE (usuario, cod_distribuidor)
);


-- =============================================================================
-- SCHEMA: shell
-- =============================================================================

CREATE TABLE IF NOT EXISTS shell.bocas (
    id             SERIAL PRIMARY KEY,
    cod_boca       CHARACTER VARYING(50)  NOT NULL,
    nombre         CHARACTER VARYING(200) NOT NULL,
    direccion      CHARACTER VARYING(200),
    ciudad         CHARACTER VARYING(200) NOT NULL,
    zona           CHARACTER VARYING(200),
    activo         BOOLEAN NOT NULL DEFAULT TRUE,
    longitud       CHARACTER VARYING(600),
    latitud        CHARACTER VARYING(600),
    fecha_creacion TIMESTAMP NOT NULL DEFAULT NOW()
);

ALTER TABLE shell.bocas ADD CONSTRAINT uk_shell_cod_boca UNIQUE (cod_boca);

CREATE TABLE IF NOT EXISTS shell.cabeceras (
    id          SERIAL PRIMARY KEY,
    codigo      CHARACTER VARYING(100),
    titulo      CHARACTER VARYING(100) NOT NULL,
    orden       INTEGER,
    descripcion CHARACTER VARYING(300),
    activo      BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE shell.cabeceras ADD CONSTRAINT uk_shell_codigo_cabecera UNIQUE (codigo);

CREATE TABLE IF NOT EXISTS shell.items (
    id                       SERIAL PRIMARY KEY,
    tema                     CHARACTER VARYING(200),
    descripcion              CHARACTER VARYING(200) NOT NULL,
    leyenda                  CHARACTER VARYING(300),
    cod_cabecera             CHARACTER VARYING(100) NOT NULL,
    tipo                     CHARACTER VARYING(100) NOT NULL DEFAULT 'SI/NO',
    valor_mostrar_condicional CHARACTER VARYING(50),
    pregunta_condicional     CHARACTER VARYING(500),
    activo                   BOOLEAN NOT NULL DEFAULT TRUE,
    nro                      INTEGER NOT NULL,
    CONSTRAINT fk_shell_items_cabecera FOREIGN KEY (cod_cabecera) REFERENCES shell.cabeceras(codigo),
    CONSTRAINT chk_shell_tipo CHECK (tipo IN ('SI/NO', 'TEXTO', 'FECHA', 'FECHA_HORA'))
);

CREATE TABLE IF NOT EXISTS shell.respuesta_cab (
    id                   SERIAL PRIMARY KEY,
    id_boca              BIGINT NOT NULL,
    cod_boca             CHARACTER VARYING(50)  NOT NULL,
    desc_boca            CHARACTER VARYING(200) NOT NULL,
    usuario              CHARACTER VARYING(200) NOT NULL,
    longitud             CHARACTER VARYING(500) NOT NULL,
    latitud              CHARACTER VARYING(500) NOT NULL,
    fecha_sinc           TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_creacion       CHARACTER VARYING(100),
    hora_inicio          CHARACTER VARYING(20),
    hora_fin             CHARACTER VARYING(20),
    activo               BOOLEAN NOT NULL DEFAULT TRUE,
    sanitario_clausurado BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_shell_rcab_boca FOREIGN KEY (id_boca) REFERENCES shell.bocas(id)
);

CREATE TABLE IF NOT EXISTS shell.respuesta_det (
    id               SERIAL PRIMARY KEY,
    id_respuesta_cab BIGINT NOT NULL,
    id_item          BIGINT NOT NULL,
    desc_item        CHARACTER VARYING(200) NOT NULL,
    cod_cabecera     CHARACTER VARYING(100) NOT NULL,
    valor_1          CHARACTER VARYING(200),
    valor_2          CHARACTER VARYING(200),
    valor_3          CHARACTER VARYING(200),
    comentario       CHARACTER VARYING(500),
    activo           BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_shell_rdet_cab     FOREIGN KEY (id_respuesta_cab) REFERENCES shell.respuesta_cab(id),
    CONSTRAINT fk_shell_rdet_cabecera FOREIGN KEY (cod_cabecera)    REFERENCES shell.cabeceras(codigo)
);

CREATE TABLE IF NOT EXISTS shell.respuesta_multimedia (
    id               SERIAL PRIMARY KEY,
    id_respuesta_cab BIGINT NOT NULL,
    path             CHARACTER VARYING(300) NOT NULL,
    tipo             CHARACTER VARYING(100) NOT NULL,
    activo           BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion   TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_shell_multimedia_cab FOREIGN KEY (id_respuesta_cab) REFERENCES shell.respuesta_cab(id),
    CONSTRAINT chk_shell_multimedia_tipo CHECK (tipo IN ('IMAGEN', 'VIDEO'))
);


-- =============================================================================
-- SCHEMA: jhonson
-- =============================================================================

CREATE TABLE IF NOT EXISTS jhonson.distribuidores (
    id          SERIAL PRIMARY KEY,
    descripcion CHARACTER VARYING(300) NOT NULL,
    codigo      CHARACTER VARYING(100) NOT NULL,
    activo      BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE jhonson.distribuidores ADD CONSTRAINT uk_jhonson_cod_distribuidor UNIQUE (codigo);

CREATE TABLE IF NOT EXISTS jhonson.bocas (
    id               SERIAL PRIMARY KEY,
    cod_boca         CHARACTER VARYING(50)  NOT NULL,
    nombre           CHARACTER VARYING(200) NOT NULL,
    direccion        CHARACTER VARYING(200),
    ciudad           CHARACTER VARYING(200) NOT NULL,
    canal_ccr        CHARACTER VARYING(200),
    ocasion          CHARACTER VARYING(200),
    activo           BOOLEAN NOT NULL DEFAULT TRUE,
    longitud         CHARACTER VARYING(500),
    latitud          CHARACTER VARYING(500),
    externo          BOOLEAN NOT NULL DEFAULT FALSE,
    cod_distribuidor CHARACTER VARYING(50) NOT NULL,
    fecha_creacion   TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_jhonson_bocas_dist FOREIGN KEY (cod_distribuidor) REFERENCES jhonson.distribuidores(codigo)
);

CREATE TABLE IF NOT EXISTS jhonson.reportes (
    id               SERIAL PRIMARY KEY,
    cod_distribuidor CHARACTER VARYING(50)  NOT NULL,
    descripcion      CHARACTER VARYING(500),
    mes              CHARACTER VARYING(2)   NOT NULL,
    anio             INTEGER,
    usuario          CHARACTER VARYING(50)  NOT NULL,
    pdf              BOOLEAN NOT NULL DEFAULT FALSE,
    ppt              BOOLEAN NOT NULL DEFAULT FALSE,
    bocas            JSONB NOT NULL,
    detalles         JSONB NOT NULL,
    fecha_creacion   TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_borrado    TIMESTAMP,
    path_pdf         TEXT,
    path_ppt         TEXT,
    CONSTRAINT fk_jhonson_reportes_dist    FOREIGN KEY (cod_distribuidor) REFERENCES jhonson.distribuidores(codigo),
    CONSTRAINT fk_jhonson_reportes_usuario FOREIGN KEY (usuario)          REFERENCES public.usuarios(usuario)
);


-- =============================================================================
-- PRIVILEGIOS para usuario ccr
-- =============================================================================

GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA public     TO ccr;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public     TO ccr;

GRANT USAGE ON SCHEMA bebidas_py TO ccr;
GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA bebidas_py TO ccr;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA bebidas_py TO ccr;

GRANT USAGE ON SCHEMA nestle TO ccr;
GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA nestle     TO ccr;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA nestle     TO ccr;

GRANT USAGE ON SCHEMA shell TO ccr;
GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA shell      TO ccr;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA shell      TO ccr;

GRANT USAGE ON SCHEMA jhonson TO ccr;
GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA jhonson    TO ccr;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA jhonson    TO ccr;
