
CREATE SCHEMA shell;


INSERT INTO public.clientes (id, codigo, nombre, activo) VALUES(nextval('cliente_id_seq'::regclass), 'SHELL', 'Shell', true);


create table shell.bocas (
    id serial primary key,
    cod_boca character varying(50) not null,
    nombre character varying(200) not null,
    direccion character varying(200),
    ciudad character varying(200) not null,
    zona character varying(200),
    activo boolean default true not null,
    longitud character varying(600),
    latitud character varying(600),
    fecha_creacion timestamp default now() not null
);

alter table shell.bocas add constraint uk_code_boca unique(cod_boca);


create table shell.cabeceras(
    id serial primary key,
    codigo character varying(100),
    titulo character varying(100) not null,
    orden int,
    descripcion character varying(300),
    activo boolean default true not null
);

alter table shell.cabeceras add constraint uk_code_cabecera unique(codigo);

INSERT INTO shell.cabeceras
(id, codigo, titulo, orden, descripcion, activo)
VALUES(nextval('shell.cabeceras_id_seq'::regclass), 'ATENCION', 'Atención', 1, '', true);

INSERT INTO shell.cabeceras
(id, codigo, titulo, orden, descripcion, activo)
VALUES(nextval('shell.cabeceras_id_seq'::regclass), 'SANITARIOS', 'Sanitarios', 2, '', true);



create table shell.items(
    id serial primary key,
    tema character varying(200),
    descripcion character varying(200) not null,
    leyenda character varying(300),
    cod_cabecera character varying(100) not null,
    tipo character varying(100) not null default 'SI/NO', --SI/NO , TEXTO, FECHA, FECHA_HORA
    valor_mostrar_condicional character varying(50), -- CON QUE Valor se muestra la pregunta condicional
    pregunta_condicional character varying(500),
    activo boolean default true not null,
    nro int not null,
    foreign key (cod_cabecera) references shell.cabeceras(codigo),
    check (tipo in ('SI/NO', 'TEXTO' , 'FECHA', 'FECHA_HORA'))
);

COMMENT ON COLUMN shell.items.tipo IS 'Tipo de respuesta esperada. Valores posibles: ''SI/NO'', , ''FECHA'', ''FECHA_HORA''';
COMMENT ON COLUMN shell.items.valor_mostrar_condicional IS 'Valor que activa la visualización de la pregunta condicional (por ejemplo: ''SI'')';



create table shell.respuesta_cab(
    id serial primary key,
    id_boca integer not null,
    cod_boca character varying(50) not null,
    desc_boca character varying(200) not null,
    usuario character varying(200) not null,
    longitud character varying(500) not null,
    latitud character varying(500) not null,
    fecha_sinc timestamp default now() not null,
    fecha_creacion character varying(100),
    hora_inicio character varying(20),
    hora_fin character varying(20),
    activo boolean default true not null,
    sanitario_clausurado boolean default false,
    foreign key (id_boca) references shell.bocas(id)
);


create table shell.respuesta_det(
    id serial primary key,
    id_respuesta_cab integer not null,
    id_item integer not null,
    desc_item character varying(200) not null,
    cod_cabecera character varying(100) not null,
    valor_1 character varying(200),
    valor_2 character varying(200),
    valor_3 character varying(200),
    comentario character varying(500),
    activo boolean default true not null,
    foreign key (id_respuesta_cab) references shell.respuesta_cab(id),
    foreign key (cod_cabecera) references shell.cabeceras(codigo)
);


create table shell.respuesta_multimedia(
    id serial primary key,
    id_respuesta_cab integer not null,
    path character varying(300) not null,
    tipo character varying(300) not null,
    activo boolean default true not null,
    fecha_creacion timestamp default now() not null,
    foreign key (id_respuesta_cab) references shell.respuesta_cab(id),
    check (tipo in ('IMAGEN', 'VIDEO'))
);




-- Ejecutar los privilegios al final
GRANT USAGE ON SCHEMA shell TO ccr;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ccr;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA shell TO ccr;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA shell TO ccr;



