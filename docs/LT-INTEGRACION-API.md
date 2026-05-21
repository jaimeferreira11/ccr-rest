# API de Integración — Proveedor LT

Documentación técnica para la carga de datos desde el sistema LT al servidor CCR.

---

## URL Base

**Producción:**

```
http://200.108.131.206:8051/ccr-rest-api/lt/api/v1
```

**Desarrollo:**

```
http://localhost:8081/ccr-rest-api/lt/api/v1
```

---

## Autenticación

Todos los endpoints requieren un **Bearer Token** estático en el header `Authorization`.

```
Authorization: Bearer <api-key>
```

### Clave de acceso

```
ltk_KFYQNxRAls24J46tSwndn68UYPfp9_SfyumbVCa9_es
```

> **Importante:** Esta clave es confidencial. No compartirla por canales no seguros ni incluirla en código fuente público. En caso de compromiso, contactar al equipo CCR para regenerarla.

Sin este header, o con una clave incorrecta, la API responde `401 Unauthorized` con el siguiente body:

```json
{
  "status": "ERROR",
  "mensaje": "API key inválida",
  "registros": 0
}
```

### Características de la clave

| Propiedad | Valor |
|-----------|-------|
| Algoritmo | CSPRNG (`secrets.token_urlsafe`) |
| Entropía | 256 bits |
| Longitud | 47 caracteres |
| Prefijo | `ltk_` (identifica el origen) |
| Formato | URL-safe Base64 |

---

## Formato General

- **Content-Type:** `application/json`
- **Método:** `POST` para todos los endpoints
- **Body:** Array JSON con uno o más registros
- **Respuesta exitosa:** HTTP `200 OK` con JSON

### Estructura de respuesta exitosa

```json
{
  "status": "OK",
  "mensaje": "Sucursales guardadas",
  "registros": 10
}
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `status` | string | `"OK"` si todo fue procesado correctamente |
| `mensaje` | string | Descripción del resultado |
| `registros` | number | Cantidad de registros procesados |

### Comportamiento de upsert

Si un registro ya existe en la base de datos (identificado por su clave única), se **actualiza**. Si no existe, se **inserta**. No se generan duplicados al reenviar el mismo batch.

---

## Endpoints

### POST `/sucursales`

Carga o actualiza el maestro de sucursales (puntos de venta).

**Clave única:** `punto`

#### Body

```json
[
  {
    "punto": 1,
    "direccion": "GRAL ELIZARDO AQUINO Y C.A. LOPEZ",
    "provincia": "CENTRAL",
    "ciudad": "ASUNCION",
    "mts2": 250
  },
  {
    "punto": 2,
    "direccion": "Ruta 3 General Elizardo Aquino",
    "provincia": "SIN ASIGNAR",
    "ciudad": "SIN ASIGNAR",
    "mts2": 0
  }
]
```

#### Campos

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `punto` | integer | Sí | ID único de la sucursal |
| `direccion` | string | No | Dirección física |
| `provincia` | string | No | Provincia / departamento |
| `ciudad` | string | No | Ciudad |
| `mts2` | number | No | Superficie en metros cuadrados |

#### Respuesta

```json
{
  "status": "OK",
  "mensaje": "Sucursales guardadas",
  "registros": 2
}
```

---

### POST `/productos`

Carga o actualiza el catálogo de productos.

**Clave única:** `eancode`

> **Nota:** Los campos `id_Sector`, `id_Seccion`, `id_Categoria`, `id_Subcategoria` deben enviarse con la primera letra en mayúscula, tal como se muestra en el ejemplo.

#### Body

```json
[
  {
    "eancode": 247573,
    "descripcion": "JGO 3PZC PLANGANA/COLADOR/TAPER REF:559688",
    "id_Sector": 2,
    "sector": "NO PERECEDEROS",
    "id_Seccion": 14,
    "seccion": "NO TRADICIONALES",
    "id_Categoria": 96,
    "categoria": "BAZAR",
    "id_Subcategoria": 347,
    "subcategoria": "PLASTICOS",
    "fabricante": "FORTALEZA EMPRENDIMIENTO SRL",
    "marca": "INDEFINIDA",
    "contenido": 1,
    "pesovolumen": 0,
    "unidadMedida": "Unid"
  }
]
```

#### Campos

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `eancode` | long | Sí | Código EAN del producto (puede superar el rango de int) |
| `descripcion` | string | No | Descripción del producto |
| `id_Sector` | integer | No | ID del sector |
| `sector` | string | No | Nombre del sector |
| `id_Seccion` | integer | No | ID de la sección |
| `seccion` | string | No | Nombre de la sección |
| `id_Categoria` | integer | No | ID de la categoría |
| `categoria` | string | No | Nombre de la categoría |
| `id_Subcategoria` | integer | No | ID de la subcategoría |
| `subcategoria` | string | No | Nombre de la subcategoría |
| `fabricante` | string | No | Nombre del fabricante |
| `marca` | string | No | Marca del producto |
| `contenido` | number | No | Cantidad de contenido |
| `pesovolumen` | number | No | Peso o volumen |
| `unidadMedida` | string | No | Unidad de medida (ej: `"Unid"`, `"Kg"`, `"Lt"`) |

---

### POST `/tickets`

Carga o actualiza las líneas de tickets de venta.

**Clave única:** combinación de `punto` + `nroTicket` + `eancode`

#### Body

```json
[
  {
    "punto": 1,
    "nroTicket": "0010120219942",
    "fecha": "2026-05-05",
    "hora": "19:14:00",
    "eancode": 7500435019828,
    "ean_desc": "2EN1 H&S SUAVE Y MANEJ X 375ML",
    "unidades_vendidas": 1,
    "precio_regular": 43400,
    "precio_promocional": 43400,
    "tipo_venta": "P"
  }
]
```

#### Campos

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `punto` | integer | Sí | ID de la sucursal donde se realizó la venta |
| `nroTicket` | string | Sí | Número de ticket |
| `fecha` | string | Sí | Fecha de la venta en formato `yyyy-MM-dd` |
| `hora` | string | No | Hora de la venta en formato `HH:mm:ss` |
| `eancode` | long | Sí | Código EAN del producto vendido |
| `ean_desc` | string | No | Descripción del producto |
| `unidades_vendidas` | integer | No | Cantidad de unidades vendidas |
| `precio_regular` | number | No | Precio regular (sin promoción) |
| `precio_promocional` | number | No | Precio aplicado en la venta |
| `tipo_venta` | string | No | Tipo de venta (ej: `"P"` = promocional) |

---

### POST `/personas`

Carga o actualiza los compradores asociados a tickets.

**Clave única:** combinación de `punto` + `nroTicket` + `identificacion`

#### Body

```json
[
  {
    "punto": 1,
    "nroTicket": "0010010295677",
    "fecha": "2026-05-05",
    "identificacion": "44444401-7",
    "nombreyapellidoempresa": "JUAN PEREZ"
  }
]
```

#### Campos

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `punto` | integer | Sí | ID de la sucursal |
| `nroTicket` | string | Sí | Número de ticket al que pertenece la persona |
| `fecha` | string | Sí | Fecha del ticket en formato `yyyy-MM-dd` |
| `identificacion` | string | No | Cédula o RUC del comprador |
| `nombreyapellidoempresa` | string | No | Nombre completo o razón social |

---

## Códigos de respuesta HTTP

| Código | Descripción |
|--------|-------------|
| `200 OK` | Registros procesados correctamente |
| `400 Bad Request` | JSON malformado o tipos de campo incorrectos |
| `401 Unauthorized` | API key ausente o inválida |
| `500 Internal Server Error` | Error interno del servidor |

### Respuesta 200 (éxito)

```json
{ "status": "OK", "mensaje": "Sucursales guardadas", "registros": 10 }
```

### Respuesta 401 (auth inválida)

```json
{ "status": "ERROR", "mensaje": "API key inválida", "registros": 0 }
```

### Respuesta 400 (JSON inválido)

El error 400 usa el formato estándar de Spring Boot — distinto al formato de negocio de los otros endpoints:

```json
{
  "timestamp": "2026-05-21T22:01:59.741+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "JSON parse error: ...",
  "path": "/ccr-rest-api/lt/api/v1/sucursales"
}
```

---

## Ejemplo completo con curl

```bash
curl -X POST http://200.108.131.206:8051/ccr-rest-api/lt/api/v1/sucursales \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ltk_KFYQNxRAls24J46tSwndn68UYPfp9_SfyumbVCa9_es" \
  -d '[
    {
      "punto": 1,
      "direccion": "GRAL ELIZARDO AQUINO Y C.A. LOPEZ",
      "provincia": "CENTRAL",
      "ciudad": "ASUNCION",
      "mts2": 250
    }
  ]'
```

Respuesta:

```json
{
  "status": "OK",
  "mensaje": "Sucursales guardadas",
  "registros": 1
}
```

---

## Notas técnicas

- Los batches pueden enviarse en cualquier orden. El sistema acepta primero sucursales, luego productos, luego tickets y personas, pero no es obligatorio.
- No hay límite de registros por request, aunque se recomienda no superar los 5.000 registros por llamada para evitar timeouts.
- El campo `eancode` debe enviarse como número entero largo (no string). Valores como `7500435019828` superan el rango de `int` estándar de 32 bits.
- Las fechas deben enviarse siempre en formato ISO `yyyy-MM-dd`. Las horas en `HH:mm:ss`.
- Los campos opcionales pueden omitirse o enviarse como `null`.
