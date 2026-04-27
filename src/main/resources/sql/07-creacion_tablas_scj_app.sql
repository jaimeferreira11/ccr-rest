
-- Tablas para la app mobile de SCJ (Johnson)
-- Ejecutar sobre schema jhonson ya existente

-- Agregar mes_ultima_medicion a bocas (necesario para filtro mes actual en app)
ALTER TABLE jhonson.bocas ADD COLUMN IF NOT EXISTS mes_ultima_medicion character varying(200);

create table jhonson.cabeceras(
    id serial primary key,
    codigo character varying(100) not null,
    titulo character varying(100) not null,
    orden int,
    descripcion character varying(300),
    activo boolean default true not null,
    autoservicio boolean not null default false,
    despensa boolean not null default false,
    estacion_servicio boolean not null default false,
    supermercado boolean not null default false
);

alter table jhonson.cabeceras add constraint uk_codigo_cabecera unique(codigo);


create table jhonson.items(
    id serial primary key,
    codigo int,
    descripcion character varying(200) not null,
    leyenda character varying(300),
    cod_cabecera character varying(100) not null,
    pregunta character varying(500),
    autoservicio boolean not null default false,
    despensa boolean not null default false,
    estacion_servicio boolean not null default false,
    supermercado boolean not null default false,
    categoria character varying(200),
    activo boolean default true not null,
    imagen character varying(300),
    orden int,
    precios jsonb default '[]',
    foreign key (cod_cabecera) references jhonson.cabeceras(codigo)
);


create table jhonson.respuesta_cab(
    id serial primary key,
    id_boca integer not null,
    cod_boca character varying(50) not null,
    desc_boca character varying(200) not null,
    canal_ccr character varying(200) not null,
    usuario character varying(200) not null,
    longitud character varying(500) not null,
    latitud character varying(500) not null,
    fecha_sinc timestamp default now() not null,
    fecha_creacion character varying(100),
    hora_inicio character varying(20),
    hora_fin character varying(20),
    comentario_fi character varying(500),
    comentario_fo character varying(500),
    comentario_fp character varying(500),
    comentario_fr character varying(500),
    activo boolean default true not null,
    foreign key (id_boca) references jhonson.bocas(id)
);


create table jhonson.respuesta_imagen(
    id serial primary key,
    id_respuesta_cab integer not null,
    cod_cabecera character varying(100) not null,
    path_imagen character varying(300) not null,
    activo boolean default true not null,
    fecha_creacion timestamp default now() not null,
    foreign key (id_respuesta_cab) references jhonson.respuesta_cab(id)
);


create table jhonson.respuesta_det(
    id serial primary key,
    id_respuesta_cab integer not null,
    id_item integer not null,
    desc_item character varying(200) not null,
    cod_cabecera character varying(100) not null,
    valor_1 character varying(200),
    valor_2 character varying(200),
    valor_3 character varying(200),
    comentario character varying(500),
    sin_datos boolean not null default false,
    activo boolean default true not null,
    foreign key (id_respuesta_cab) references jhonson.respuesta_cab(id),
    foreign key (cod_cabecera) references jhonson.cabeceras(codigo)
);


create table jhonson.usuario_distribuidor (
    id serial primary key,
    usuario character varying(100) not null,
    cod_distribuidor character varying(100) not null,
    foreign key (usuario) references public.usuarios(usuario),
    foreign key (cod_distribuidor) references jhonson.distribuidores(codigo),
    unique (usuario, cod_distribuidor)
);


-- Ejecutar los privilegios al final
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA jhonson TO ccr;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA jhonson TO ccr;
