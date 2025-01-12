
CREATE TABLE public.clientes (
    id SERIAL PRIMARY KEY,
    codigo CHARACTER VARYING(200) NOT NULL UNIQUE,
    nombre CHARACTER VARYING(200) NOT NULL,
    ruc CHARACTER VARYING(200),
    direccion CHARACTER VARYING(300),
    contacto CHARACTER VARYING(300),
    sitio_web CHARACTER VARYING(300),
    correo CHARACTER VARYING(300),
    logo BYTEA,
    activo BOOLEAN DEFAULT TRUE NOT NULL
);

-- Este no para asegurar la retrocompatibilidad
--ALTER TABLE public.usuarios
--ADD COLUMN cod_cliente CHARACTER VARYING(200),
--ADD CONSTRAINT fk_codigo_cliente FOREIGN KEY (cod_cliente)
--REFERENCES public.clientes(codigo);

ALTER TABLE public.usuarios
ADD CONSTRAINT uq_usuario UNIQUE (usuario);

create table public.roles(
 id SERIAL PRIMARY KEY,
 rol CHARACTER VARYING(100) NOT null UNIQUE
);

CREATE TABLE public.user_roles (
  id SERIAL PRIMARY KEY,
  rol CHARACTER VARYING(100) NOT NULL,
  usuario CHARACTER VARYING(200) NOT NULL,
  FOREIGN KEY (rol) REFERENCES public.roles(rol) ON DELETE cascade,
  FOREIGN KEY (usuario) REFERENCES public.usuarios(usuario) ON DELETE CASCADE
);
