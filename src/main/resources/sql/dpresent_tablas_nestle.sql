create table nestle.distribuidores(
    id serial primary key,
    descripcion character varying(300) not null,
    codigo character varying(100) not null,
    activo boolean default true not null 
);

alter table nestle.distribuidores add constraint uk_code_distribuidor unique(codigo);



alter table nestle.distribuidores add column cod_distribuidor character varying(50);
alter table nestle.distribuidores add foreign key (cod_distribuidor) references nestle.distribuidores(codigo);


create table nestle.reportes(
    id serial primary key,
    cod_distribuidor character varying(50) not null,
    descripcion character varying(500),
    mes character varying(2) not null, -- 01, 02, 03 ...
    anio integer not null,
    usuario character varying(50) not null,
    pdf boolean not null default false,
    ppt boolean not null default false,
    bocas jsonb not null,
    detalles jsonb not null,
    fecha_creacion timestamp default now() not null,
    fecha_borrado timestamp,
    path_pdf text,
    path_ppt text,
    foreign key (cod_distribuidor) references nestle.distribuidores(codigo),
    foreign key (usuario) references public.usuarios(usuario)
    
);




-- Ejecutar los privilegios al final
GRANT USAGE ON SCHEMA nestle TO ccr;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ccr;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA nestle TO ccr; 
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA nestle TO ccr;

