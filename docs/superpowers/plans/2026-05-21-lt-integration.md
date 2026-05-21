# LT Integration Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Crear el módulo `lt` dentro del REST API para que el proveedor LT pueda insertar/actualizar datos de sucursales, productos, tickets y personas vía 4 endpoints POST autenticados con API key estática.

**Architecture:** Nuevo paquete `py.com.jaimeferreira.ccr.lt` con la estructura estándar entity → repository → service → controller. Nuevo schema `lt` en PostgreSQL. Autenticación por API key estática (Bearer token configurado en properties), separada del JWT existente: el filtro JWT se modifica para saltear rutas `/lt/**`, y se agrega un filtro dedicado `LtApiKeyFilter` que valida la clave y setea el SecurityContext.

**Tech Stack:** Spring Boot 2.7, Spring Security, Spring Data JPA, PostgreSQL (schema `lt`), Jackson (JSON arrays), `BaseEntidad` para auditoría.

---

## Estructura de datos del proveedor

Todos los endpoints reciben un array JSON. Ejemplo real de cada uno:

**GuardarSucursal:**
```json
[{"punto":1,"direccion":"GRAL ELIZARDO AQUINO Y C.A. LOPEZ,","provincia":"SIN ASIGNAR","ciudad":"SIN ASIGNAR","mts2":0}]
```

**GuardarProducto:**
```json
[{"eancode":247573,"descripcion":"JGO 3PZC PLANGANA/COLADOR/TAPER","id_Sector":2,"sector":"NO PERECEDEROS","id_Seccion":14,"seccion":"NO TRADICIONALES","id_Categoria":96,"categoria":"BAZAR","id_Subcategoria":347,"subcategoria":"PLASTICOS","fabricante":"FORTALEZA EMPRENDIMIENTO SRL","marca":"INDEFINIDA","contenido":1,"pesovolumen":0,"unidadMedida":"Unid"}]
```

**GuardarTicket:**
```json
[{"punto":1,"nroTicket":"0010120219942","fecha":"2026-05-05","hora":"19:14:00","eancode":7500435019828,"ean_desc":"2EN1 H&S SUAVE X 375ML","unidades_vendidas":1,"precio_regular":43400,"precio_promocional":43400,"tipo_venta":"P"}]
```

**GuardarPersona:**
```json
[{"punto":1,"nroTicket":"0010010295677","fecha":"2026-05-05","identificacion":"44444401-7","nombreyapellidoempresa":"INNOMINADO"}]
```

---

## Mapa de archivos

| Acción | Archivo |
|--------|---------|
| Crear | `src/main/resources/sql/10-lt-integration-schema.sql` |
| Modificar | `src/main/resources/application-dev.properties` |
| Modificar | `src/main/resources/application-prod.properties` |
| Crear | `src/main/java/.../lt/entity/LtSucursal.java` |
| Crear | `src/main/java/.../lt/entity/LtProducto.java` |
| Crear | `src/main/java/.../lt/entity/LtTicket.java` |
| Crear | `src/main/java/.../lt/entity/LtPersona.java` |
| Crear | `src/main/java/.../lt/dto/SucursalDTO.java` |
| Crear | `src/main/java/.../lt/dto/ProductoDTO.java` |
| Crear | `src/main/java/.../lt/dto/TicketDTO.java` |
| Crear | `src/main/java/.../lt/dto/PersonaDTO.java` |
| Crear | `src/main/java/.../lt/dto/LtResponseDTO.java` |
| Crear | `src/main/java/.../lt/repository/LtSucursalRepository.java` |
| Crear | `src/main/java/.../lt/repository/LtProductoRepository.java` |
| Crear | `src/main/java/.../lt/repository/LtTicketRepository.java` |
| Crear | `src/main/java/.../lt/repository/LtPersonaRepository.java` |
| Crear | `src/main/java/.../lt/service/LtIntegracionService.java` |
| Crear | `src/main/java/.../lt/controller/LtIntegracionController.java` |
| Crear | `src/main/java/.../security/LtApiKeyFilter.java` |
| Modificar | `src/main/java/.../security/JWTAuthorizationFilter.java` |
| Modificar | `src/main/java/.../security/SecurityConfigurer.java` |

Prefijo de paquete: `py.com.jaimeferreira.ccr`

---

## Task 1: SQL Schema y configuración de propiedades

**Files:**
- Create: `src/main/resources/sql/10-lt-integration-schema.sql`
- Modify: `src/main/resources/application-dev.properties`
- Modify: `src/main/resources/application-prod.properties`

- [ ] **Step 1: Crear script SQL**

Archivo `src/main/resources/sql/10-lt-integration-schema.sql`:

```sql
-- Schema para integración proveedor LT
CREATE SCHEMA IF NOT EXISTS lt;

CREATE TABLE lt.sucursal (
    id BIGSERIAL PRIMARY KEY,
    punto INTEGER NOT NULL,
    direccion VARCHAR(500),
    provincia VARCHAR(200),
    ciudad VARCHAR(200),
    mts2 NUMERIC(10,2),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    nombre_usuario_creacion VARCHAR(100),
    nombre_usuario_actualizacion VARCHAR(100),
    CONSTRAINT uk_lt_sucursal_punto UNIQUE (punto)
);

CREATE TABLE lt.producto (
    id BIGSERIAL PRIMARY KEY,
    eancode BIGINT NOT NULL,
    descripcion VARCHAR(500),
    id_sector INTEGER,
    sector VARCHAR(200),
    id_seccion INTEGER,
    seccion VARCHAR(200),
    id_categoria INTEGER,
    categoria VARCHAR(200),
    id_subcategoria INTEGER,
    subcategoria VARCHAR(200),
    fabricante VARCHAR(200),
    marca VARCHAR(200),
    contenido NUMERIC(10,3),
    pesovolumen NUMERIC(10,3),
    unidad_medida VARCHAR(50),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    nombre_usuario_creacion VARCHAR(100),
    nombre_usuario_actualizacion VARCHAR(100),
    CONSTRAINT uk_lt_producto_eancode UNIQUE (eancode)
);

CREATE TABLE lt.ticket (
    id BIGSERIAL PRIMARY KEY,
    punto INTEGER NOT NULL,
    nro_ticket VARCHAR(50) NOT NULL,
    fecha DATE NOT NULL,
    hora TIME,
    eancode BIGINT NOT NULL,
    ean_desc VARCHAR(500),
    unidades_vendidas INTEGER,
    precio_regular NUMERIC(15,2),
    precio_promocional NUMERIC(15,2),
    tipo_venta VARCHAR(10),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    nombre_usuario_creacion VARCHAR(100),
    nombre_usuario_actualizacion VARCHAR(100),
    CONSTRAINT uk_lt_ticket UNIQUE (punto, nro_ticket, eancode)
);

CREATE TABLE lt.persona (
    id BIGSERIAL PRIMARY KEY,
    punto INTEGER NOT NULL,
    nro_ticket VARCHAR(50) NOT NULL,
    fecha DATE NOT NULL,
    identificacion VARCHAR(50),
    nombre_y_apellido_empresa VARCHAR(500),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    nombre_usuario_creacion VARCHAR(100),
    nombre_usuario_actualizacion VARCHAR(100),
    CONSTRAINT uk_lt_persona UNIQUE (punto, nro_ticket, identificacion)
);
```

- [ ] **Step 2: Agregar API key a application-dev.properties**

Al final del archivo `src/main/resources/application-dev.properties`:

```properties
# LT Integration — API key para el proveedor LT
lt.api.key=LT-DEV-KEY-2026
```

- [ ] **Step 3: Agregar API key a application-prod.properties**

Al final del archivo `src/main/resources/application-prod.properties`:

```properties
# LT Integration — API key para el proveedor LT
lt.api.key=CAMBIAR_POR_CLAVE_SEGURA_EN_PROD
```

- [ ] **Step 4: Ejecutar SQL en la base de datos**

```bash
psql -U ccr -d ccr -f src/main/resources/sql/10-lt-integration-schema.sql
```

Verificar con: `\dt lt.*`  
Esperado: 4 tablas — `lt.sucursal`, `lt.producto`, `lt.ticket`, `lt.persona`

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/sql/10-lt-integration-schema.sql \
        src/main/resources/application-dev.properties \
        src/main/resources/application-prod.properties
git commit -m "feat(lt): crear schema lt y configurar api key"
```

---

## Task 2: Entidades JPA

**Files:**
- Create: `src/main/java/py/com/jaimeferreira/ccr/lt/entity/LtSucursal.java`
- Create: `src/main/java/py/com/jaimeferreira/ccr/lt/entity/LtProducto.java`
- Create: `src/main/java/py/com/jaimeferreira/ccr/lt/entity/LtTicket.java`
- Create: `src/main/java/py/com/jaimeferreira/ccr/lt/entity/LtPersona.java`

Todas extienden `BaseEntidad` (que ya tiene fechaCreacion, fechaActualizacion, nombreUsuarioCreacion, nombreUsuarioActualizacion) y agregan el campo `estado`.

- [ ] **Step 1: Crear LtSucursal.java**

```java
package py.com.jaimeferreira.ccr.lt.entity;

import py.com.jaimeferreira.ccr.commons.entity.BaseEntidad;
import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "sucursal", schema = "lt")
public class LtSucursal extends BaseEntidad implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "punto", nullable = false, unique = true)
    private Integer punto;

    @Column(name = "direccion", length = 500)
    private String direccion;

    @Column(name = "provincia", length = 200)
    private String provincia;

    @Column(name = "ciudad", length = 200)
    private String ciudad;

    @Column(name = "mts2", precision = 10, scale = 2)
    private BigDecimal mts2;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "ACTIVO";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getPunto() { return punto; }
    public void setPunto(Integer punto) { this.punto = punto; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getProvincia() { return provincia; }
    public void setProvincia(String provincia) { this.provincia = provincia; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public BigDecimal getMts2() { return mts2; }
    public void setMts2(BigDecimal mts2) { this.mts2 = mts2; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
```

- [ ] **Step 2: Crear LtProducto.java**

```java
package py.com.jaimeferreira.ccr.lt.entity;

import py.com.jaimeferreira.ccr.commons.entity.BaseEntidad;
import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "producto", schema = "lt")
public class LtProducto extends BaseEntidad implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "eancode", nullable = false, unique = true)
    private Long eancode;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "id_sector")
    private Integer idSector;

    @Column(name = "sector", length = 200)
    private String sector;

    @Column(name = "id_seccion")
    private Integer idSeccion;

    @Column(name = "seccion", length = 200)
    private String seccion;

    @Column(name = "id_categoria")
    private Integer idCategoria;

    @Column(name = "categoria", length = 200)
    private String categoria;

    @Column(name = "id_subcategoria")
    private Integer idSubcategoria;

    @Column(name = "subcategoria", length = 200)
    private String subcategoria;

    @Column(name = "fabricante", length = 200)
    private String fabricante;

    @Column(name = "marca", length = 200)
    private String marca;

    @Column(name = "contenido", precision = 10, scale = 3)
    private BigDecimal contenido;

    @Column(name = "pesovolumen", precision = 10, scale = 3)
    private BigDecimal pesovolumen;

    @Column(name = "unidad_medida", length = 50)
    private String unidadMedida;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "ACTIVO";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEancode() { return eancode; }
    public void setEancode(Long eancode) { this.eancode = eancode; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Integer getIdSector() { return idSector; }
    public void setIdSector(Integer idSector) { this.idSector = idSector; }
    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }
    public Integer getIdSeccion() { return idSeccion; }
    public void setIdSeccion(Integer idSeccion) { this.idSeccion = idSeccion; }
    public String getSeccion() { return seccion; }
    public void setSeccion(String seccion) { this.seccion = seccion; }
    public Integer getIdCategoria() { return idCategoria; }
    public void setIdCategoria(Integer idCategoria) { this.idCategoria = idCategoria; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public Integer getIdSubcategoria() { return idSubcategoria; }
    public void setIdSubcategoria(Integer idSubcategoria) { this.idSubcategoria = idSubcategoria; }
    public String getSubcategoria() { return subcategoria; }
    public void setSubcategoria(String subcategoria) { this.subcategoria = subcategoria; }
    public String getFabricante() { return fabricante; }
    public void setFabricante(String fabricante) { this.fabricante = fabricante; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public BigDecimal getContenido() { return contenido; }
    public void setContenido(BigDecimal contenido) { this.contenido = contenido; }
    public BigDecimal getPesovolumen() { return pesovolumen; }
    public void setPesovolumen(BigDecimal pesovolumen) { this.pesovolumen = pesovolumen; }
    public String getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(String unidadMedida) { this.unidadMedida = unidadMedida; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
```

- [ ] **Step 3: Crear LtTicket.java**

```java
package py.com.jaimeferreira.ccr.lt.entity;

import py.com.jaimeferreira.ccr.commons.entity.BaseEntidad;
import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "ticket", schema = "lt",
    uniqueConstraints = @UniqueConstraint(columnNames = {"punto", "nro_ticket", "eancode"}))
public class LtTicket extends BaseEntidad implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "punto", nullable = false)
    private Integer punto;

    @Column(name = "nro_ticket", nullable = false, length = 50)
    private String nroTicket;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora")
    private LocalTime hora;

    @Column(name = "eancode", nullable = false)
    private Long eancode;

    @Column(name = "ean_desc", length = 500)
    private String eanDesc;

    @Column(name = "unidades_vendidas")
    private Integer unidadesVendidas;

    @Column(name = "precio_regular", precision = 15, scale = 2)
    private BigDecimal precioRegular;

    @Column(name = "precio_promocional", precision = 15, scale = 2)
    private BigDecimal precioPromocional;

    @Column(name = "tipo_venta", length = 10)
    private String tipoVenta;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "ACTIVO";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getPunto() { return punto; }
    public void setPunto(Integer punto) { this.punto = punto; }
    public String getNroTicket() { return nroTicket; }
    public void setNroTicket(String nroTicket) { this.nroTicket = nroTicket; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHora() { return hora; }
    public void setHora(LocalTime hora) { this.hora = hora; }
    public Long getEancode() { return eancode; }
    public void setEancode(Long eancode) { this.eancode = eancode; }
    public String getEanDesc() { return eanDesc; }
    public void setEanDesc(String eanDesc) { this.eanDesc = eanDesc; }
    public Integer getUnidadesVendidas() { return unidadesVendidas; }
    public void setUnidadesVendidas(Integer unidadesVendidas) { this.unidadesVendidas = unidadesVendidas; }
    public BigDecimal getPrecioRegular() { return precioRegular; }
    public void setPrecioRegular(BigDecimal precioRegular) { this.precioRegular = precioRegular; }
    public BigDecimal getPrecioPromocional() { return precioPromocional; }
    public void setPrecioPromocional(BigDecimal precioPromocional) { this.precioPromocional = precioPromocional; }
    public String getTipoVenta() { return tipoVenta; }
    public void setTipoVenta(String tipoVenta) { this.tipoVenta = tipoVenta; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
```

- [ ] **Step 4: Crear LtPersona.java**

```java
package py.com.jaimeferreira.ccr.lt.entity;

import py.com.jaimeferreira.ccr.commons.entity.BaseEntidad;
import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "persona", schema = "lt",
    uniqueConstraints = @UniqueConstraint(columnNames = {"punto", "nro_ticket", "identificacion"}))
public class LtPersona extends BaseEntidad implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "punto", nullable = false)
    private Integer punto;

    @Column(name = "nro_ticket", nullable = false, length = 50)
    private String nroTicket;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "identificacion", length = 50)
    private String identificacion;

    @Column(name = "nombre_y_apellido_empresa", length = 500)
    private String nombreYApellidoEmpresa;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "ACTIVO";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getPunto() { return punto; }
    public void setPunto(Integer punto) { this.punto = punto; }
    public String getNroTicket() { return nroTicket; }
    public void setNroTicket(String nroTicket) { this.nroTicket = nroTicket; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String identificacion) { this.identificacion = identificacion; }
    public String getNombreYApellidoEmpresa() { return nombreYApellidoEmpresa; }
    public void setNombreYApellidoEmpresa(String nombreYApellidoEmpresa) { this.nombreYApellidoEmpresa = nombreYApellidoEmpresa; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
```

- [ ] **Step 5: Compilar para verificar que no hay errores**

```bash
./mvnw compile -DskipTests -q
```

Esperado: BUILD SUCCESS (sin errores de compilación)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/py/com/jaimeferreira/ccr/lt/entity/
git commit -m "feat(lt): agregar entidades JPA para schema lt"
```

---

## Task 3: DTOs de entrada y respuesta

**Files:**
- Create: `src/main/java/py/com/jaimeferreira/ccr/lt/dto/SucursalDTO.java`
- Create: `src/main/java/py/com/jaimeferreira/ccr/lt/dto/ProductoDTO.java`
- Create: `src/main/java/py/com/jaimeferreira/ccr/lt/dto/TicketDTO.java`
- Create: `src/main/java/py/com/jaimeferreira/ccr/lt/dto/PersonaDTO.java`
- Create: `src/main/java/py/com/jaimeferreira/ccr/lt/dto/LtResponseDTO.java`

Los nombres de campos en los DTOs deben coincidir exactamente con los JSON del proveedor (ver ejemplos de payload arriba). Nota: `id_Sector`, `id_Seccion`, etc. usan mayúsculas en el JSON — usar `@JsonProperty` para mapearlos.

- [ ] **Step 1: Crear SucursalDTO.java**

```java
package py.com.jaimeferreira.ccr.lt.dto;

import java.math.BigDecimal;

public class SucursalDTO {
    private Integer punto;
    private String direccion;
    private String provincia;
    private String ciudad;
    private BigDecimal mts2;

    public Integer getPunto() { return punto; }
    public void setPunto(Integer punto) { this.punto = punto; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getProvincia() { return provincia; }
    public void setProvincia(String provincia) { this.provincia = provincia; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public BigDecimal getMts2() { return mts2; }
    public void setMts2(BigDecimal mts2) { this.mts2 = mts2; }
}
```

- [ ] **Step 2: Crear ProductoDTO.java**

Los campos `id_Sector`, `id_Seccion`, `id_Categoria`, `id_Subcategoria` vienen con mayúscula en el JSON del proveedor, usar `@JsonProperty`.

```java
package py.com.jaimeferreira.ccr.lt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class ProductoDTO {
    private Long eancode;
    private String descripcion;

    @JsonProperty("id_Sector")
    private Integer idSector;
    private String sector;

    @JsonProperty("id_Seccion")
    private Integer idSeccion;
    private String seccion;

    @JsonProperty("id_Categoria")
    private Integer idCategoria;
    private String categoria;

    @JsonProperty("id_Subcategoria")
    private Integer idSubcategoria;
    private String subcategoria;

    private String fabricante;
    private String marca;
    private BigDecimal contenido;
    private BigDecimal pesovolumen;
    private String unidadMedida;

    public Long getEancode() { return eancode; }
    public void setEancode(Long eancode) { this.eancode = eancode; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Integer getIdSector() { return idSector; }
    public void setIdSector(Integer idSector) { this.idSector = idSector; }
    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }
    public Integer getIdSeccion() { return idSeccion; }
    public void setIdSeccion(Integer idSeccion) { this.idSeccion = idSeccion; }
    public String getSeccion() { return seccion; }
    public void setSeccion(String seccion) { this.seccion = seccion; }
    public Integer getIdCategoria() { return idCategoria; }
    public void setIdCategoria(Integer idCategoria) { this.idCategoria = idCategoria; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public Integer getIdSubcategoria() { return idSubcategoria; }
    public void setIdSubcategoria(Integer idSubcategoria) { this.idSubcategoria = idSubcategoria; }
    public String getSubcategoria() { return subcategoria; }
    public void setSubcategoria(String subcategoria) { this.subcategoria = subcategoria; }
    public String getFabricante() { return fabricante; }
    public void setFabricante(String fabricante) { this.fabricante = fabricante; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public BigDecimal getContenido() { return contenido; }
    public void setContenido(BigDecimal contenido) { this.contenido = contenido; }
    public BigDecimal getPesovolumen() { return pesovolumen; }
    public void setPesovolumen(BigDecimal pesovolumen) { this.pesovolumen = pesovolumen; }
    public String getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(String unidadMedida) { this.unidadMedida = unidadMedida; }
}
```

- [ ] **Step 3: Crear TicketDTO.java**

`fecha` y `hora` vienen como strings — usar `@JsonFormat` para parsear.

```java
package py.com.jaimeferreira.ccr.lt.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class TicketDTO {
    private Integer punto;
    private String nroTicket;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime hora;

    private Long eancode;
    private String ean_desc;
    private Integer unidades_vendidas;
    private BigDecimal precio_regular;
    private BigDecimal precio_promocional;
    private String tipo_venta;

    public Integer getPunto() { return punto; }
    public void setPunto(Integer punto) { this.punto = punto; }
    public String getNroTicket() { return nroTicket; }
    public void setNroTicket(String nroTicket) { this.nroTicket = nroTicket; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHora() { return hora; }
    public void setHora(LocalTime hora) { this.hora = hora; }
    public Long getEancode() { return eancode; }
    public void setEancode(Long eancode) { this.eancode = eancode; }
    public String getEan_desc() { return ean_desc; }
    public void setEan_desc(String ean_desc) { this.ean_desc = ean_desc; }
    public Integer getUnidades_vendidas() { return unidades_vendidas; }
    public void setUnidades_vendidas(Integer unidades_vendidas) { this.unidades_vendidas = unidades_vendidas; }
    public BigDecimal getPrecio_regular() { return precio_regular; }
    public void setPrecio_regular(BigDecimal precio_regular) { this.precio_regular = precio_regular; }
    public BigDecimal getPrecio_promocional() { return precio_promocional; }
    public void setPrecio_promocional(BigDecimal precio_promocional) { this.precio_promocional = precio_promocional; }
    public String getTipo_venta() { return tipo_venta; }
    public void setTipo_venta(String tipo_venta) { this.tipo_venta = tipo_venta; }
}
```

- [ ] **Step 4: Crear PersonaDTO.java**

```java
package py.com.jaimeferreira.ccr.lt.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public class PersonaDTO {
    private Integer punto;
    private String nroTicket;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;

    private String identificacion;
    private String nombreyapellidoempresa;

    public Integer getPunto() { return punto; }
    public void setPunto(Integer punto) { this.punto = punto; }
    public String getNroTicket() { return nroTicket; }
    public void setNroTicket(String nroTicket) { this.nroTicket = nroTicket; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String identificacion) { this.identificacion = identificacion; }
    public String getNombreyapellidoempresa() { return nombreyapellidoempresa; }
    public void setNombreyapellidoempresa(String nombreyapellidoempresa) { this.nombreyapellidoempresa = nombreyapellidoempresa; }
}
```

- [ ] **Step 5: Crear LtResponseDTO.java**

```java
package py.com.jaimeferreira.ccr.lt.dto;

public class LtResponseDTO {
    private String status;
    private String mensaje;
    private int registros;

    public LtResponseDTO(String status, String mensaje, int registros) {
        this.status = status;
        this.mensaje = mensaje;
        this.registros = registros;
    }

    public String getStatus() { return status; }
    public String getMensaje() { return mensaje; }
    public int getRegistros() { return registros; }
}
```

- [ ] **Step 6: Compilar**

```bash
./mvnw compile -DskipTests -q
```

Esperado: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/py/com/jaimeferreira/ccr/lt/dto/
git commit -m "feat(lt): agregar DTOs de entrada y respuesta"
```

---

## Task 4: Repositories

**Files:**
- Create: `src/main/java/py/com/jaimeferreira/ccr/lt/repository/LtSucursalRepository.java`
- Create: `src/main/java/py/com/jaimeferreira/ccr/lt/repository/LtProductoRepository.java`
- Create: `src/main/java/py/com/jaimeferreira/ccr/lt/repository/LtTicketRepository.java`
- Create: `src/main/java/py/com/jaimeferreira/ccr/lt/repository/LtPersonaRepository.java`

- [ ] **Step 1: Crear LtSucursalRepository.java**

```java
package py.com.jaimeferreira.ccr.lt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import py.com.jaimeferreira.ccr.lt.entity.LtSucursal;
import java.util.Optional;

public interface LtSucursalRepository extends JpaRepository<LtSucursal, Long> {
    Optional<LtSucursal> findByPunto(Integer punto);
}
```

- [ ] **Step 2: Crear LtProductoRepository.java**

```java
package py.com.jaimeferreira.ccr.lt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import py.com.jaimeferreira.ccr.lt.entity.LtProducto;
import java.util.Optional;

public interface LtProductoRepository extends JpaRepository<LtProducto, Long> {
    Optional<LtProducto> findByEancode(Long eancode);
}
```

- [ ] **Step 3: Crear LtTicketRepository.java**

```java
package py.com.jaimeferreira.ccr.lt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import py.com.jaimeferreira.ccr.lt.entity.LtTicket;
import java.util.Optional;

public interface LtTicketRepository extends JpaRepository<LtTicket, Long> {
    Optional<LtTicket> findByPuntoAndNroTicketAndEancode(Integer punto, String nroTicket, Long eancode);
}
```

- [ ] **Step 4: Crear LtPersonaRepository.java**

```java
package py.com.jaimeferreira.ccr.lt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import py.com.jaimeferreira.ccr.lt.entity.LtPersona;
import java.util.Optional;

public interface LtPersonaRepository extends JpaRepository<LtPersona, Long> {
    Optional<LtPersona> findByPuntoAndNroTicketAndIdentificacion(Integer punto, String nroTicket, String identificacion);
}
```

- [ ] **Step 5: Compilar**

```bash
./mvnw compile -DskipTests -q
```

Esperado: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/py/com/jaimeferreira/ccr/lt/repository/
git commit -m "feat(lt): agregar repositories para entidades LT"
```

---

## Task 5: Service

**Files:**
- Create: `src/main/java/py/com/jaimeferreira/ccr/lt/service/LtIntegracionService.java`

El service implementa lógica de upsert (insert si no existe, update si ya existe), seteando los campos de auditoría de `BaseEntidad` + `estado`.

- [ ] **Step 1: Crear LtIntegracionService.java**

```java
package py.com.jaimeferreira.ccr.lt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import py.com.jaimeferreira.ccr.lt.dto.*;
import py.com.jaimeferreira.ccr.lt.entity.*;
import py.com.jaimeferreira.ccr.lt.repository.*;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LtIntegracionService {

    private static final String USUARIO_SISTEMA = "LT-API";

    @Autowired
    private LtSucursalRepository sucursalRepo;

    @Autowired
    private LtProductoRepository productoRepo;

    @Autowired
    private LtTicketRepository ticketRepo;

    @Autowired
    private LtPersonaRepository personaRepo;

    @Transactional
    public int guardarSucursales(List<SucursalDTO> lista) {
        for (SucursalDTO dto : lista) {
            LtSucursal entity = sucursalRepo.findByPunto(dto.getPunto())
                    .orElseGet(() -> {
                        LtSucursal nueva = new LtSucursal();
                        nueva.setFechaCreacion(LocalDateTime.now());
                        nueva.setNombreUsuarioCreacion(USUARIO_SISTEMA);
                        return nueva;
                    });
            entity.setPunto(dto.getPunto());
            entity.setDireccion(dto.getDireccion());
            entity.setProvincia(dto.getProvincia());
            entity.setCiudad(dto.getCiudad());
            entity.setMts2(dto.getMts2());
            entity.setFechaActualizacion(LocalDateTime.now());
            entity.setNombreUsuarioActualizacion(USUARIO_SISTEMA);
            sucursalRepo.save(entity);
        }
        return lista.size();
    }

    @Transactional
    public int guardarProductos(List<ProductoDTO> lista) {
        for (ProductoDTO dto : lista) {
            LtProducto entity = productoRepo.findByEancode(dto.getEancode())
                    .orElseGet(() -> {
                        LtProducto nuevo = new LtProducto();
                        nuevo.setFechaCreacion(LocalDateTime.now());
                        nuevo.setNombreUsuarioCreacion(USUARIO_SISTEMA);
                        return nuevo;
                    });
            entity.setEancode(dto.getEancode());
            entity.setDescripcion(dto.getDescripcion());
            entity.setIdSector(dto.getIdSector());
            entity.setSector(dto.getSector());
            entity.setIdSeccion(dto.getIdSeccion());
            entity.setSeccion(dto.getSeccion());
            entity.setIdCategoria(dto.getIdCategoria());
            entity.setCategoria(dto.getCategoria());
            entity.setIdSubcategoria(dto.getIdSubcategoria());
            entity.setSubcategoria(dto.getSubcategoria());
            entity.setFabricante(dto.getFabricante());
            entity.setMarca(dto.getMarca());
            entity.setContenido(dto.getContenido());
            entity.setPesovolumen(dto.getPesovolumen());
            entity.setUnidadMedida(dto.getUnidadMedida());
            entity.setFechaActualizacion(LocalDateTime.now());
            entity.setNombreUsuarioActualizacion(USUARIO_SISTEMA);
            productoRepo.save(entity);
        }
        return lista.size();
    }

    @Transactional
    public int guardarTickets(List<TicketDTO> lista) {
        for (TicketDTO dto : lista) {
            LtTicket entity = ticketRepo
                    .findByPuntoAndNroTicketAndEancode(dto.getPunto(), dto.getNroTicket(), dto.getEancode())
                    .orElseGet(() -> {
                        LtTicket nuevo = new LtTicket();
                        nuevo.setFechaCreacion(LocalDateTime.now());
                        nuevo.setNombreUsuarioCreacion(USUARIO_SISTEMA);
                        return nuevo;
                    });
            entity.setPunto(dto.getPunto());
            entity.setNroTicket(dto.getNroTicket());
            entity.setFecha(dto.getFecha());
            entity.setHora(dto.getHora());
            entity.setEancode(dto.getEancode());
            entity.setEanDesc(dto.getEan_desc());
            entity.setUnidadesVendidas(dto.getUnidades_vendidas());
            entity.setPrecioRegular(dto.getPrecio_regular());
            entity.setPrecioPromocional(dto.getPrecio_promocional());
            entity.setTipoVenta(dto.getTipo_venta());
            entity.setFechaActualizacion(LocalDateTime.now());
            entity.setNombreUsuarioActualizacion(USUARIO_SISTEMA);
            ticketRepo.save(entity);
        }
        return lista.size();
    }

    @Transactional
    public int guardarPersonas(List<PersonaDTO> lista) {
        for (PersonaDTO dto : lista) {
            LtPersona entity = personaRepo
                    .findByPuntoAndNroTicketAndIdentificacion(dto.getPunto(), dto.getNroTicket(), dto.getIdentificacion())
                    .orElseGet(() -> {
                        LtPersona nueva = new LtPersona();
                        nueva.setFechaCreacion(LocalDateTime.now());
                        nueva.setNombreUsuarioCreacion(USUARIO_SISTEMA);
                        return nueva;
                    });
            entity.setPunto(dto.getPunto());
            entity.setNroTicket(dto.getNroTicket());
            entity.setFecha(dto.getFecha());
            entity.setIdentificacion(dto.getIdentificacion());
            entity.setNombreYApellidoEmpresa(dto.getNombreyapellidoempresa());
            entity.setFechaActualizacion(LocalDateTime.now());
            entity.setNombreUsuarioActualizacion(USUARIO_SISTEMA);
            personaRepo.save(entity);
        }
        return lista.size();
    }
}
```

- [ ] **Step 2: Compilar**

```bash
./mvnw compile -DskipTests -q
```

Esperado: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/py/com/jaimeferreira/ccr/lt/service/LtIntegracionService.java
git commit -m "feat(lt): agregar service con lógica upsert para los 4 endpoints"
```

---

## Task 6: Seguridad — LtApiKeyFilter + cambios en SecurityConfigurer y JWTFilter

**Files:**
- Create: `src/main/java/py/com/jaimeferreira/ccr/security/LtApiKeyFilter.java`
- Modify: `src/main/java/py/com/jaimeferreira/ccr/security/JWTAuthorizationFilter.java` (línea ~47)
- Modify: `src/main/java/py/com/jaimeferreira/ccr/security/SecurityConfigurer.java`

**Por qué este enfoque:** El filtro JWT existente intenta parsear como JWT cualquier header `Authorization: Bearer ...`. Si el proveedor LT envía su API key estática con ese header, el filtro JWT lanzaría `MalformedJwtException`. La solución es: (1) modificar el JWT filter para saltear rutas `/lt/`, y (2) agregar `LtApiKeyFilter` que valida la clave estática para esas rutas.

- [ ] **Step 1: Crear LtApiKeyFilter.java**

```java
package py.com.jaimeferreira.ccr.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Configuration
public class LtApiKeyFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LtApiKeyFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${lt.api.key}")
    private String ltApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!request.getServletPath().startsWith("/lt/")) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            if (ltApiKey.equals(token)) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        "lt-provider", null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_LT"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
                chain.doFilter(request, response);
                return;
            }
        }

        LOGGER.warn("LT API key inválida o ausente para: {}", request.getServletPath());
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "API key inválida");
    }
}
```

- [ ] **Step 2: Modificar JWTAuthorizationFilter.java — saltear rutas /lt/**

En `src/main/java/py/com/jaimeferreira/ccr/security/JWTAuthorizationFilter.java`, dentro de `doFilterInternal`, agregar al inicio del método (antes del `try`):

```java
// Las rutas /lt/ usan su propio filtro de API key; el JWT no aplica aquí
if (request.getServletPath().startsWith("/lt/")) {
    chain.doFilter(request, response);
    return;
}
```

El método queda así:

```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
    // Las rutas /lt/ usan su propio filtro de API key; el JWT no aplica aquí
    if (request.getServletPath().startsWith("/lt/")) {
        chain.doFilter(request, response);
        return;
    }
    try {
        if (existeJWTToken(request, response)) {
            // ... resto del código existente sin cambios
```

- [ ] **Step 3: Modificar SecurityConfigurer.java — registrar LtApiKeyFilter**

En `src/main/java/py/com/jaimeferreira/ccr/security/SecurityConfigurer.java`:

1. Agregar el campo inyectado (después de `plataformaStatusFilter`):
```java
@Autowired
private LtApiKeyFilter ltApiKeyFilter;
```

2. Agregar el filtro en el método `configure(HttpSecurity http)`, antes de `http.addFilterBefore(plataformaStatusFilter, ...)`:
```java
http.addFilterBefore(ltApiKeyFilter, UsernamePasswordAuthenticationFilter.class);
```

El método `configure(HttpSecurity)` completo queda así:

```java
@Override
protected void configure(HttpSecurity http) throws Exception {
    http.csrf().disable()
        .authorizeRequests()
        .antMatchers("/auth/**", "/public/**")
        .permitAll()
        .anyRequest()
        .authenticated()
        .and()
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

    http.addFilterBefore(ltApiKeyFilter, UsernamePasswordAuthenticationFilter.class);
    http.addFilterBefore(plataformaStatusFilter, UsernamePasswordAuthenticationFilter.class);
    http.addFilterBefore(authFiltroToken, UsernamePasswordAuthenticationFilter.class);
}
```

- [ ] **Step 4: Compilar**

```bash
./mvnw compile -DskipTests -q
```

Esperado: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/py/com/jaimeferreira/ccr/security/LtApiKeyFilter.java \
        src/main/java/py/com/jaimeferreira/ccr/security/JWTAuthorizationFilter.java \
        src/main/java/py/com/jaimeferreira/ccr/security/SecurityConfigurer.java
git commit -m "feat(lt): agregar LtApiKeyFilter y adaptar SecurityConfigurer"
```

---

## Task 7: Controller

**Files:**
- Create: `src/main/java/py/com/jaimeferreira/ccr/lt/controller/LtIntegracionController.java`

URL base: `/lt/api/v1` → rutas completas: `/ccr-rest-api/lt/api/v1/sucursales`, etc.

- [ ] **Step 1: Crear LtIntegracionController.java**

```java
package py.com.jaimeferreira.ccr.lt.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import py.com.jaimeferreira.ccr.lt.dto.*;
import py.com.jaimeferreira.ccr.lt.service.LtIntegracionService;

import java.util.List;

@RestController
@RequestMapping("lt/api/v1")
public class LtIntegracionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LtIntegracionController.class);

    @Autowired
    private LtIntegracionService service;

    @PostMapping("/sucursales")
    public ResponseEntity<LtResponseDTO> guardarSucursales(@RequestBody List<SucursalDTO> lista) {
        LOGGER.info("LT /sucursales — recibidos {} registros", lista.size());
        int guardados = service.guardarSucursales(lista);
        return ResponseEntity.ok(new LtResponseDTO("OK", "Sucursales guardadas", guardados));
    }

    @PostMapping("/productos")
    public ResponseEntity<LtResponseDTO> guardarProductos(@RequestBody List<ProductoDTO> lista) {
        LOGGER.info("LT /productos — recibidos {} registros", lista.size());
        int guardados = service.guardarProductos(lista);
        return ResponseEntity.ok(new LtResponseDTO("OK", "Productos guardados", guardados));
    }

    @PostMapping("/tickets")
    public ResponseEntity<LtResponseDTO> guardarTickets(@RequestBody List<TicketDTO> lista) {
        LOGGER.info("LT /tickets — recibidos {} registros", lista.size());
        int guardados = service.guardarTickets(lista);
        return ResponseEntity.ok(new LtResponseDTO("OK", "Tickets guardados", guardados));
    }

    @PostMapping("/personas")
    public ResponseEntity<LtResponseDTO> guardarPersonas(@RequestBody List<PersonaDTO> lista) {
        LOGGER.info("LT /personas — recibidos {} registros", lista.size());
        int guardados = service.guardarPersonas(lista);
        return ResponseEntity.ok(new LtResponseDTO("OK", "Personas guardadas", guardados));
    }
}
```

- [ ] **Step 2: Compilar y empaquetar**

```bash
./mvnw clean package -DskipTests -P dev
```

Esperado: BUILD SUCCESS

- [ ] **Step 3: Levantar la app y smoke test manual**

```bash
./mvnw spring-boot:run
```

Probar autenticación inválida (esperar 401):
```bash
curl -s -o /dev/null -w "%{http_code}" \
  -X POST http://localhost:8081/ccr-rest-api/lt/api/v1/sucursales \
  -H "Content-Type: application/json" \
  -d '[{"punto":1,"direccion":"TEST","provincia":"TEST","ciudad":"TEST","mts2":0}]'
# Esperado: 401
```

Probar con API key correcta (esperar 200):
```bash
curl -s -w "\n%{http_code}" \
  -X POST http://localhost:8081/ccr-rest-api/lt/api/v1/sucursales \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer LT-DEV-KEY-2026" \
  -d '[{"punto":1,"direccion":"GRAL ELIZARDO AQUINO","provincia":"CENTRAL","ciudad":"ASUNCION","mts2":100}]'
# Esperado: {"status":"OK","mensaje":"Sucursales guardadas","registros":1} + 200
```

Verificar en BD:
```sql
SELECT * FROM lt.sucursal;
```

- [ ] **Step 4: Commit final**

```bash
git add src/main/java/py/com/jaimeferreira/ccr/lt/controller/LtIntegracionController.java
git commit -m "feat(lt): agregar controller con los 4 endpoints de integración LT"
```

---

## Endpoints expuestos al proveedor

| Método | URL completa | Descripción |
|--------|-------------|-------------|
| POST | `/ccr-rest-api/lt/api/v1/sucursales` | Carga/actualiza sucursales |
| POST | `/ccr-rest-api/lt/api/v1/productos` | Carga/actualiza catálogo de productos |
| POST | `/ccr-rest-api/lt/api/v1/tickets` | Carga/actualiza tickets de venta |
| POST | `/ccr-rest-api/lt/api/v1/personas` | Carga/actualiza compradores |

**Auth:** `Authorization: Bearer <lt.api.key>`  
**Content-Type:** `application/json`  
**Body:** Array JSON (mismo formato que los logs de envíos del proveedor)

---

## Self-Review

**Spec coverage:**
- ✅ URL base `/lt/api/v1`
- ✅ Autenticación Bearer token
- ✅ 4 endpoints: /sucursales, /productos, /tickets, /personas
- ✅ Respuesta JSON
- ✅ Schema separado `lt` en PostgreSQL
- ✅ Campos de auditoría (BaseEntidad: fechaCreacion, fechaActualizacion, nombreUsuarioCreacion, nombreUsuarioActualizacion) + campo `estado`
- ✅ Upsert: no duplica si el proveedor reenvía los mismos datos

**Placeholder scan:** Ninguno — todos los pasos tienen código completo.

**Type consistency:** Los tipos usados en DTOs, entities, repositories y service son consistentes en todos los tasks.
