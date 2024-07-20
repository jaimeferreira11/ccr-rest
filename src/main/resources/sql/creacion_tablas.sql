
      
CREATE USER "ccr" WITH ENCRYPTED PASSWORD 'ccr';
ALTER ROLE "ccr" WITH createdb;
CREATE database "ccr";
ALTER DATABASE ccr OWNER TO ccr;

CREATE SCHEMA zoomin;


create table zoomin.bocas (
    id serial primary key,
    cod_boca character varying(50) not null,
    nombre character varying(200) not null,
    direccion character varying(200),
    ciudad character varying(200) not null,
    tipo_boca character varying(200),
    ocasion character varying(200) not null,
    activo boolean default true not null 
);

alter table zoomin.bocas add constraint uk_code_boca unique(cod_boca);

create table zoomin.usuarios(
    id serial primary key,
    usuario character varying(200) not null,
    password character varying(200) not null,
    nombre_apellido character varying(300) not null,
    activo boolean default true not null 
);

create table zoomin.cabeceras(
    id serial primary key,
    codigo character varying(100) not null,
    titulo character varying(100) not null,
    descripcion character varying(300),
    activo boolean default true not null
);

alter table zoomin.cabeceras add constraint uk_codigo_cabecera unique(codigo);

create table zoomin.items(
    id serial primary key,
    codigo integer,
    descripcion character varying(200) not null,
    leyenda character varying(300) not null,
    cod_cabecera character varying(100) not null,
    pregunta character varying(500),
    ocasion character varying(200) not null,
    activo boolean default true not null,
    foreign key (cod_cabecera) references zoomin.cabeceras(codigo)
);

create table zoomin.respuesta_cab(
    id serial primary key,
    id_boca integer not null,
    cod_boca character varying(50) not null,
    desc_boca character varying(200) not null,
    usuario character varying(200) not null,
    longitud character varying(500) not null,
    latitud character varying(500) not null,
    path_imagen character varying(300) not null,
    fecha_sinc timestamp default now() not null,
    fecha_creacion character varying(100),
    hora_inicio character varying(20),
    hora_fin character varying(20),
    activo boolean default true not null,
);

create table zoomin.respuesta_det(
    id serial primary key,
    id_respuesta_cab integer not null,
    id_item integer not null,
    desc_item character varying(200) not null,
    cabecera character varying(200) not null,
    valor character varying(10) not null,
    comentario character varying(500),
    precio character varying(100),
    activo boolean default true not null,
    constraint fk_respuesta_det_cab foreign key (id_respuesta_cab) references zoomin.respuesta_cab(id)
);

create table zoomin.respuesta_imagen(
    id serial primary key,
    id_respuesta_cab integer not null,
    path_imagen character varying(300) not null,
    activo boolean default true not null,
    constraint fk_respuesta_imagen_cab foreign key (id_respuesta_cab) references zoomin.respuesta_cab(id)
);
