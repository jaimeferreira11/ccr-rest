---
title: "API de Integración LT ↔ CCR"
subtitle: "Documentación técnica para el proveedor LT"
author: "D-Tech Consulting"
date: "31 de mayo de 2026"
lang: es
---

\vspace{2cm}

|                          |                                                  |
|--------------------------|--------------------------------------------------|
| **Cliente**              | CCR Paraguay                                     |
| **Proveedor**            | LT (sistema POS)                                 |
| **Versión del documento**| 3.0                                              |
| **Fecha de emisión**     | 31 de mayo de 2026                               |
| **Autor**                | D-Tech Consulting                                |
| **Clasificación**        | Confidencial                                     |

\newpage

# 1. Datos de conexión (resumen)

Esta sección resume los datos esenciales para conectarse a la API. El resto del documento desarrolla cada punto en detalle.

| Parámetro              | Valor                                                                  |
|------------------------|------------------------------------------------------------------------|
| **URL base**           | `http://200.108.131.206:8051/ccr-rest-api/lt/api/v1`                   |
| **Autenticación**      | Bearer Token estático en cabecera `Authorization`                      |
| **Token de acceso**    | `ltk_KFYQNxRAls24J46tSwndn68UYPfp9_SfyumbVCa9_es`                      |
| **Cabecera completa**  | `Authorization: Bearer ltk_KFYQNxRAls24J46tSwndn68UYPfp9_SfyumbVCa9_es`|
| **Content-Type**       | `application/json`                                                     |
| **Método HTTP**        | `POST` (todos los endpoints)                                           |
| **Endpoints**          | `/sucursales`, `/productos`, `/tickets`, `/personas`                   |

> **Importante:** el token es **confidencial**. No debe compartirse por canales no seguros ni incluirse en repositorios de código públicos. En caso de compromiso, contactar inmediatamente al equipo CCR para regenerarlo.

\newpage

# 2. Introducción y alcance

Este documento describe la API REST expuesta por **CCR** para recibir cargas de datos transaccionales y maestros desde el sistema POS del proveedor **LT**. La integración permite a CCR consolidar información de ventas, productos, sucursales y compradores para sus reportes analíticos.

La API está orientada a procesos batch: el sistema LT envía lotes JSON vía HTTP POST, y CCR los procesa con lógica de *upsert* (inserción o actualización según la clave única de cada entidad).

## Audiencia

- **Equipo técnico de LT:** implementadores de los procesos de envío.
- **Equipo CCR:** referencia funcional y operativa de la integración.

## Alcance

La especificación cubre los cuatro endpoints disponibles para la carga de datos: `/sucursales`, `/productos`, `/tickets` y `/personas`. Incluye autenticación, formato de mensajes, códigos de respuesta y ejemplos.

## Convenciones

- Todos los textos en formato `código` deben copiarse literalmente.
- Los ejemplos JSON son ilustrativos; los valores reales los provee el sistema LT.
- Las claves marcadas como **Requerido: Sí** son obligatorias para que el registro sea procesado.

\newpage

# 3. URL de servicio

La API está disponible en la siguiente URL base de producción:

```
http://200.108.131.206:8051/ccr-rest-api/lt/api/v1
```

> **Nota:** todos los endpoints documentados se ubican bajo esta URL base. Por ejemplo, `/sucursales` se invoca como `http://200.108.131.206:8051/ccr-rest-api/lt/api/v1/sucursales`.

\newpage

# 4. Autenticación

Todos los endpoints requieren un **Bearer Token estático** en la cabecera HTTP `Authorization`. CCR provee al proveedor LT una única clave de acceso, válida para todos los endpoints.

## Cabecera requerida

```
Authorization: Bearer <api-key>
```

## Clave de acceso provista

```
ltk_KFYQNxRAls24J46tSwndn68UYPfp9_SfyumbVCa9_es
```

> **Importante:** esta clave es **confidencial**. No debe compartirse por canales no seguros (chats públicos, repositorios de código, capturas en correos masivos). Debe almacenarse cifrada o en un gestor de secretos. En caso de compromiso, contactar inmediatamente al equipo CCR para regenerarla.

## Respuesta ante autenticación inválida

Si la cabecera `Authorization` está ausente o la clave es incorrecta, la API responde `HTTP 401 Unauthorized` con el siguiente cuerpo:

```json
{
  "status": "ERROR",
  "mensaje": "API key inválida",
  "registros": 0
}
```

\newpage

# 5. Formato general de mensajes

## Cabeceras obligatorias

| Cabecera          | Valor                                                                  |
|-------------------|------------------------------------------------------------------------|
| `Content-Type`    | `application/json`                                                     |
| `Authorization`   | `Bearer ltk_KFYQNxRAls24J46tSwndn68UYPfp9_SfyumbVCa9_es`               |

## Características del request

- **Método HTTP:** `POST` en todos los endpoints.
- **Body:** array JSON con uno o más registros.
- **Encoding:** UTF-8.

## Estructura de respuesta exitosa

Una respuesta correcta devuelve HTTP `200 OK` con el siguiente formato:

```json
{
  "status": "OK",
  "mensaje": "Sucursales guardadas",
  "registros": 10
}
```

| Campo       | Tipo   | Descripción                                          |
|-------------|--------|------------------------------------------------------|
| `status`    | string | `"OK"` si todo fue procesado correctamente.          |
| `mensaje`   | string | Descripción legible del resultado.                   |
| `registros` | number | Cantidad de registros procesados.                    |

## Comportamiento de upsert

Si un registro ya existe en la base de datos (identificado por su clave única), se **actualiza**. Si no existe, se **inserta**. **No se generan duplicados al reenviar el mismo batch**, lo que permite reintentos seguros.

\newpage

# 6. Endpoints

## 6.1 POST /sucursales

Carga o actualiza el maestro de sucursales (puntos de venta).

**Clave única:** `punto`

### Body del request

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

### Definición de campos

| Campo       | Tipo    | Requerido | Descripción                          |
|-------------|---------|-----------|--------------------------------------|
| `punto`     | integer | Sí        | ID único de la sucursal.             |
| `direccion` | string  | No        | Dirección física.                    |
| `provincia` | string  | No        | Provincia o departamento.            |
| `ciudad`    | string  | No        | Ciudad.                              |
| `mts2`      | number  | No        | Superficie en metros cuadrados.      |

### Respuesta exitosa

```json
{
  "status": "OK",
  "mensaje": "Sucursales guardadas",
  "registros": 2
}
```

\newpage

## 6.2 POST /productos

Carga o actualiza el catálogo de productos.

**Clave única:** `eancode`

> **Atención:** los campos `id_Sector`, `id_Seccion`, `id_Categoria` e `id_Subcategoria` deben enviarse exactamente con la primera letra en mayúscula (notación camelCase mostrada en el ejemplo). Cualquier variación será ignorada.

### Body del request

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

### Definición de campos

| Campo             | Tipo    | Requerido | Descripción                                                       |
|-------------------|---------|-----------|-------------------------------------------------------------------|
| `eancode`         | long    | Sí        | Código EAN del producto. Puede superar el rango de int (32 bits). |
| `descripcion`     | string  | No        | Descripción del producto.                                         |
| `id_Sector`       | integer | No        | ID del sector.                                                    |
| `sector`          | string  | No        | Nombre del sector.                                                |
| `id_Seccion`      | integer | No        | ID de la sección.                                                 |
| `seccion`         | string  | No        | Nombre de la sección.                                             |
| `id_Categoria`    | integer | No        | ID de la categoría.                                               |
| `categoria`       | string  | No        | Nombre de la categoría.                                           |
| `id_Subcategoria` | integer | No        | ID de la subcategoría.                                            |
| `subcategoria`    | string  | No        | Nombre de la subcategoría.                                        |
| `fabricante`      | string  | No        | Nombre del fabricante.                                            |
| `marca`           | string  | No        | Marca del producto.                                               |
| `contenido`       | number  | No        | Cantidad de contenido.                                            |
| `pesovolumen`     | number  | No        | Peso o volumen.                                                   |
| `unidadMedida`    | string  | No        | Unidad de medida (ej.: `"Unid"`, `"Kg"`, `"Lt"`).                 |

### Respuesta exitosa

```json
{
  "status": "OK",
  "mensaje": "Productos guardados",
  "registros": 1
}
```

\newpage

## 6.3 POST /tickets

Carga o actualiza las líneas de tickets de venta.

**Clave única:** combinación de `punto` + `nroTicket` + `eancode`

### Body del request

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

### Definición de campos

| Campo                | Tipo    | Requerido | Descripción                                       |
|----------------------|---------|-----------|---------------------------------------------------|
| `punto`              | integer | Sí        | ID de la sucursal donde se realizó la venta.      |
| `nroTicket`          | string  | Sí        | Número de ticket.                                 |
| `fecha`              | string  | Sí        | Fecha de la venta en formato `yyyy-MM-dd`.        |
| `hora`               | string  | No        | Hora de la venta en formato `HH:mm:ss`.           |
| `eancode`            | long    | Sí        | Código EAN del producto vendido.                  |
| `ean_desc`           | string  | No        | Descripción del producto.                         |
| `unidades_vendidas`  | integer | No        | Cantidad de unidades vendidas.                    |
| `precio_regular`     | number  | No        | Precio regular (sin promoción).                   |
| `precio_promocional` | number  | No        | Precio aplicado en la venta.                      |
| `tipo_venta`         | string  | No        | Tipo de venta (ej.: `"P"` = promocional).         |

### Respuesta exitosa

```json
{
  "status": "OK",
  "mensaje": "Tickets guardados",
  "registros": 1
}
```

\newpage

## 6.4 POST /personas

Carga o actualiza los compradores asociados a tickets.

**Clave única:** combinación de `punto` + `nroTicket` + `identificacion`

### Body del request

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

### Definición de campos

| Campo                    | Tipo    | Requerido | Descripción                                       |
|--------------------------|---------|-----------|---------------------------------------------------|
| `punto`                  | integer | Sí        | ID de la sucursal.                                |
| `nroTicket`              | string  | Sí        | Número de ticket al que pertenece la persona.     |
| `fecha`                  | string  | Sí        | Fecha del ticket en formato `yyyy-MM-dd`.         |
| `identificacion`         | string  | No        | Cédula o RUC del comprador.                       |
| `nombreyapellidoempresa` | string  | No        | Nombre completo o razón social.                   |

### Respuesta exitosa

```json
{
  "status": "OK",
  "mensaje": "Personas guardadas",
  "registros": 1
}
```

\newpage

# 7. Códigos de respuesta HTTP

| Código                      | Descripción                                          |
|-----------------------------|------------------------------------------------------|
| `200 OK`                    | Registros procesados correctamente.                  |
| `400 Bad Request`           | JSON malformado o tipos de campo incorrectos.        |
| `401 Unauthorized`          | API key ausente o inválida.                          |
| `500 Internal Server Error` | Error interno del servidor.                          |

## 7.1 Respuesta 200 (éxito)

```json
{ "status": "OK", "mensaje": "Sucursales guardadas", "registros": 10 }
```

## 7.2 Respuesta 401 (autenticación inválida)

```json
{ "status": "ERROR", "mensaje": "API key inválida", "registros": 0 }
```

## 7.3 Respuesta 400 (JSON inválido)

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

\newpage

# 8. Ejemplo completo con curl

A continuación se muestra una invocación completa al endpoint `/sucursales`, útil para probar la integración antes de la primera carga real.

## Request

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

## Response

```json
{
  "status": "OK",
  "mensaje": "Sucursales guardadas",
  "registros": 1
}
```

\newpage

# 9. Notas técnicas y recomendaciones

- **Orden de envío:** los batches pueden enviarse en cualquier orden. Se recomienda, no obstante, enviar primero `sucursales` y `productos`, y luego `tickets` y `personas`, para garantizar integridad referencial visual al consultar los datos.
- **Tamaño de batch:** no hay un límite estricto, pero se recomienda **no superar 5.000 registros por llamada** para evitar timeouts y minimizar el impacto en caso de error.
- **Reintentos:** dado el comportamiento de upsert, los batches son idempotentes. En caso de error de red o timeout, el mismo batch puede reenviarse sin riesgo de duplicación.
- **Tipos numéricos:** el campo `eancode` debe enviarse como **número entero largo** (no string). Valores como `7500435019828` superan el rango de `int` estándar de 32 bits.
- **Fechas y horas:** deben enviarse siempre en formato ISO 8601: fechas como `yyyy-MM-dd` y horas como `HH:mm:ss`.
- **Campos opcionales:** pueden omitirse del JSON o enviarse como `null`.
- **Zona horaria:** las fechas y horas se interpretan en zona horaria de Paraguay (America/Asuncion).
- **Encoding:** usar siempre UTF-8 para evitar problemas con acentos y caracteres especiales en descripciones y nombres.

\newpage

# 10. Soporte y contacto

Para consultas técnicas, reporte de incidentes o solicitudes de regeneración de la clave de acceso, contactar al equipo de integración:

| Contacto             | Datos                                       |
|----------------------|---------------------------------------------|
| Responsable técnico  | D-Tech Consulting                           |
| Email                | jaimeferreira11@gmail.com                   |
| Horario de soporte   | Lunes a viernes, 08:00 – 18:00 (GMT-3)      |

---

*Documento generado el 31 de mayo de 2026 · Versión 2.0 · D-Tech Consulting para CCR Paraguay · Confidencial*
