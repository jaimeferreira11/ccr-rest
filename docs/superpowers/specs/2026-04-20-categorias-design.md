# Diseno - Categorias de reporte por cliente en Insights

## Problema

El modulo `insights` necesita administrar categorias de reporte asociadas a cada cliente. Las categorias no son globales: cada cliente mantiene su propio conjunto. La persistencia debe vivir en el esquema `ccr` porque aplica solo a `insights`, y el front necesita un ABM administrativo separado.

## Objetivo

Agregar soporte completo para administrar categorias de reporte por cliente con:

- script SQL manual en el esquema `ccr`
- CRUD administrativo en backend
- pantalla ABM en frontend Angular
- baja logica mediante `enabled = false`

## Alcance

Incluye:

- nueva tabla `ccr.categorias`
- nueva capa JPA en `py.com.jaimeferreira.ccr.insights`
- endpoints admin bajo `/insights/api/v1/admin/categorias`
- nueva pantalla admin de categorias con filtro por cliente

No incluye:

- consumo de categorias en endpoints publicos
- integracion de categorias en la generacion de informes
- borrado fisico de registros

## Modelo de datos

### Tabla

`ccr.categorias`

### Columnas

| Columna | Tipo | Regla |
|---|---|---|
| `id` | `bigserial` o equivalente | PK |
| `codigo` | `varchar(50)` | obligatorio |
| `descripcion` | `varchar(200)` | obligatoria |
| `enabled` | `boolean` | obligatorio, default `true` |
| `cod_cliente` | `varchar(50)` | obligatorio, FK a `ccr.cliente(codigo)` |
| `fecha_creacion` | `timestamp` | default `now()` |
| `nombre_usuario_creacion` | `varchar(200)` | opcional |

### Restricciones

- foreign key `cod_cliente -> ccr.cliente(codigo)`
- unique compuesto por `(cod_cliente, codigo)`
- indice por `cod_cliente`

### Semantica

- un cliente puede tener muchas categorias
- una categoria pertenece a un solo cliente
- el mismo `codigo` puede repetirse entre clientes distintos
- el mismo `codigo` no puede repetirse dentro del mismo cliente

## Diseno backend

### Paquetes

Dentro de `py.com.jaimeferreira.ccr.insights`:

- `entity/Categoria`
- `dto/CategoriaDTO`
- `repository/CategoriaRepository`
- `service/CategoriaService`

La implementacion debe seguir el patron ya usado por `ClienteIns` y `Pais`.

### Entidad

La entidad `Categoria` mapeara a `ccr.categorias` con:

- `id`
- `codigo`
- `descripcion`
- `enabled`
- `cliente` como `@ManyToOne` a `ClienteIns`, uniendo por `cod_cliente -> codigo`
- `fechaCreacion`
- `nombreUsuarioCreacion`

No se agregan campos de actualizacion porque el patron actual de `insights` para catalogos simples tampoco los exige.

### Repository

El repository debe cubrir al menos:

- listado general ordenado por cliente y codigo
- listado por cliente
- busqueda por id
- busqueda por cliente y codigo
- listado activos por cliente si luego se necesita en flujos publicos

### Service

Responsabilidades del servicio:

- normalizar `codigo` a mayusculas
- validar requeridos (`codigo`, `descripcion`, `codCliente`)
- validar existencia del cliente
- evitar duplicados dentro del mismo cliente
- alta con `enabled = true` por defecto
- actualizacion parcial de descripcion, cliente y estado si aplica
- baja logica seteando `enabled = false`

### Endpoints admin

Base: `/insights/api/v1/admin/categorias`

| Metodo | Ruta | Uso |
|---|---|---|
| `GET` | `/categorias` | lista categorias; acepta `codCliente` opcional como filtro |
| `GET` | `/categorias/{id}` | obtiene detalle |
| `POST` | `/categorias` | crea categoria |
| `PUT` | `/categorias/{id}` | actualiza categoria |
| `DELETE` | `/categorias/{id}` | baja logica |

### Contrato DTO

Campos del DTO:

- `id`
- `codigo`
- `descripcion`
- `enabled`
- `codCliente`
- opcional `clienteDescripcion` para simplificar la tabla del front

## Diseno frontend

### Pantalla

Nueva pantalla:

- `src/app/pages/admin/categorias/categorias-admin.component.ts`

Se implementa como una pantalla admin separada, con el mismo patron visual y funcional ya usado en:

- `clientes-admin`
- `paises-admin`

### Comportamiento

- carga inicial de clientes y categorias
- filtro por texto
- filtro por estado
- filtro por cliente
- paginacion
- modal de alta y edicion
- accion de deshabilitar

### Formulario

Campos editables:

- cliente
- codigo
- descripcion

Campos no editables o internos:

- `id`
- `enabled` manejado por sistema

### Servicio Angular

Agregar en `InsightsService`:

- `getAdminCategorias(codCliente?)`
- `createAdminCategoria(payload)`
- `updateAdminCategoria(id, payload)`
- `disableAdminCategoria(id)`

### Modelo Angular

Nueva interfaz `ICategoria` con:

- `id`
- `codigo`
- `descripcion`
- `enabled`
- `codCliente`
- `clienteDescripcion?`

### Navegacion

Agregar entrada de menu o ruta admin con nombre visible `Categorias`.

## Manejo de errores

Mensajes explicitos para:

- cliente requerido
- codigo requerido
- descripcion requerida
- cliente inexistente
- categoria inexistente
- codigo duplicado dentro del cliente

La baja logica de una categoria ya deshabilitada no necesita borrar datos ni romper el historial.

## Validacion esperada

La solucion debe permitir:

1. crear varias categorias para un mismo cliente
2. reutilizar un mismo codigo en clientes distintos
3. impedir codigo duplicado dentro del mismo cliente
4. editar descripcion de una categoria existente
5. deshabilitar una categoria sin borrarla fisicamente

## Consideraciones de implementacion

- Hibernate no gestiona el esquema; el SQL debe ir en `src/main/resources/sql/`
- el cambio queda aislado en `insights`, aunque la tabla viva en `ccr`
- el frontend de soporte esta en `/Users/jaime/development/workspace-angular/d-insights-ccr`
- no se agregan endpoints publicos hasta que el flujo funcional los necesite
