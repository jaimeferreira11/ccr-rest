
CREATE SCHEMA nestle;

-- Ejecutar los privilegios al final
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ccr;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA nestle TO ccr; 

create table public.usuarios(
    id serial primary key,
    usuario character varying(200) not null,
    password character varying(200) not null,
    nombre_apellido character varying(300) not null,
    activo boolean default true not null 
);



create table nestle.bocas (
    id serial primary key,
    cod_boca character varying(50) not null,
    nombre character varying(200) not null,
    direccion character varying(200),
    ciudad character varying(200) not null,
    canal_ccr character varying(200) not null,
    ocasion character varying(200),
    activo boolean default true not null,
    longitud character varying(500),
    latitud character varying(500),
    fecha_creacion timestamp default now() not null,
    mes_ultima_medicion character varying(200) not null
);

alter table nestle.bocas add constraint uk_code_boca unique(cod_boca);

ALTER TABLE nestle.bocas ADD CONSTRAINT chk_mes_valido CHECK (mes_ultima_medicion IN (
    'ENERO', 'FEBRERO', 'MARZO', 'ABRIL', 'MAYO', 'JUNIO',
    'JULIO', 'AGOSTO', 'SETIEMBRE', 'OCTUBRE', 'NOVIEMBRE', 'DICIEMBRE', 'SEPTIEMBRE'
));

ALTER TABLE nestle.bocas ADD CONSTRAINT chk_boca_canal CHECK (canal_ccr IN (
    'AUTOSERVICIO', 'DESPENSA', 'ESTACION DE SERVICIO', 'SUPERMERCADO'
));


create table nestle.cabeceras(
    id serial primary key,
    codigo character varying(100) not null,
    titulo character varying(100) not null,
    orden int,
    descripcion character varying(300),
    activo boolean default true not null
    autoservicio boolean not null default false,
    despensa boolean not null default false,
    estacion_servicio boolean not null default false,
    supermercado boolean not null default false
);

alter table nestle.cabeceras add constraint uk_codigo_cabecera unique(codigo);


create table nestle.items(
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
    foreign key (cod_cabecera) references nestle.cabeceras(codigo)
);





create table nestle.respuesta_cab(
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
    foreign key (id_boca) references nestle.bocas(id)
);



create table nestle.respuesta_imagen(
    id serial primary key,
    id_respuesta_cab integer not null,
    cod_cabecera character varying(100) not null,
    path_imagen character varying(300) not null,
    activo boolean default true not null,
    fecha_creacion timestamp default now() not null,
    foreign key (id_respuesta_cab) references nestle.respuesta_cab(id)
);





create table nestle.respuesta_det(
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
    foreign key (id_respuesta_cab) references nestle.respuesta_cab(id),
    foreign key (cod_cabecera) references nestle.cabeceras(codigo)
);



create table nestle.distribuidores(
    id serial primary key,
    descripcion character varying(300) not null,
    codigo character varying(100) not null,
    activo boolean default true not null 
);

alter table nestle.distribuidores add constraint uk_code_distribuidor unique(codigo);

alter table nestle.bocas add column cod_distribuidor character varying(100);

alter table nestle.bocas add foreign key (cod_distribuidor) references nestle.distribuidores(codigo);



-- Ejecutar los privilegios al final
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ccr;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA nestle TO ccr;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA nestle TO ccr;

