-- ============================================================
-- ccr.categorias
-- Categorias de reporte por cliente para el modulo Insights.
-- No son globales; cada cliente mantiene su propio catalogo.
-- ============================================================
CREATE TABLE ccr.categorias (
    id                      SERIAL PRIMARY KEY,
    codigo                  CHARACTER VARYING(50)  NOT NULL,
    descripcion             CHARACTER VARYING(200) NOT NULL,
    cod_cliente             CHARACTER VARYING(50)  NOT NULL,
    enabled                 BOOLEAN                NOT NULL DEFAULT TRUE,
    fecha_creacion          TIMESTAMP              NOT NULL DEFAULT now(),
    nombre_usuario_creacion CHARACTER VARYING(200),
    CONSTRAINT fk_categorias_cliente
        FOREIGN KEY (cod_cliente)
        REFERENCES ccr.cliente (codigo),
    CONSTRAINT uk_categorias_cliente_codigo
        UNIQUE (cod_cliente, codigo)
);

-- ============================================================
-- Agrega columna cod_categoria a ccr.informe
-- La categoría es obligatoria al generar un informe y determina
-- qué template Excel se usa: template_{tipo}_{codCliente}_{codCategoria}.xlsx
-- ============================================================
CREATE INDEX idx_categorias_cod_cliente
    ON ccr.categorias (cod_cliente);


ALTER TABLE ccr.informe
    ADD COLUMN cod_categoria CHARACTER VARYING(50) NOT NULL DEFAULT '';
