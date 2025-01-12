
alter table public.usuarios add column externo boolean not null default false;

CREATE SCHEMA jhonson;


create table jhonson.distribuidores(
    id serial primary key,
    descripcion character varying(300) not null,
    codigo character varying(100) not null,
    activo boolean default true not null 
);

alter table jhonson.distribuidores add constraint uk_code_distribuidor unique(codigo);

create table jhonson.bocas (
    id serial primary key,
    cod_boca character varying(50) not null,
    nombre character varying(200) not null,
    direccion character varying(200),
    ciudad character varying(200) not null,
    canal_ccr character varying(200),
    ocasion character varying(200),
    activo boolean default true not null,
    longitud character varying(500),
    latitud character varying(500),
    externo boolean not null default false,
    cod_distribuidor character varying(50) not null,
    fecha_creacion timestamp default now() not null,
    foreign key (cod_distribuidor) references jhonson.distribuidores(codigo)
);


create table jhonson.reportes(
    id serial primary key,
    cod_distribuidor character varying(50) not null,
    descripcion character varying(500),
    mes character varying(2) not null, -- 01, 02, 03 ...
    usuario character varying(50) not null,
    pdf boolean not null default false,
    ppt boolean not null default false,
    bocas jsonb not null,
    detalles jsonb not null,
    fecha_creacion timestamp default now() not null,
    fecha_borrado timestamp,
    path_pdf text,
    path_ppt text,
    foreign key (cod_distribuidor) references jhonson.distribuidores(codigo),
    foreign key (usuario) references public.usuarios(usuario)
    
);


-- nuevo
alter table jhonson.reportes add column anio integer;


-- Ejecutar los privilegios al final
GRANT USAGE ON SCHEMA jhonson TO ccr;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ccr;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA jhonson TO ccr; 
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA jhonson TO ccr;