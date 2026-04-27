# CCR-sts



## Construir el jar

```mvn clean package -Pprod```

## d-insights: Probar generacion de reportes

Script para generar reportes sin frontend:

```bash
./scripts/test-generar-reporte.sh <usuario> <contrasena> <codCliente> <codCategoria> <tipoReporte> <csvDatos> [csvFiltros]
```

Ejemplo:

```bash
./scripts/test-generar-reporte.sh 4800301 123456 VIERCI ARROZ NORMAL \
  ~/Downloads/Arroz_2024_datos.csv ~/Downloads/A.J.\ Vierci_Arroz_filtro.csv
```

- `tipoReporte`: `NORMAL` o `CADENA`
- `csvFiltros`: opcional, si no se envia se usa el filtro base del cliente
- Variable de entorno `CCR_BASE_URL` para cambiar el host (default: `http://localhost:8080/ccr-rest-api`)

## SCJ (Johnson) — Endpoints

### App mobile (Flutter: `jhonson_ccr_app`)

Base path: `jhonson/api/v1`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/bocas` | Bocas del mes actual |
| `GET` | `/bocas/all` | Todas las bocas activas |
| `GET` | `/bocas/usuario` | Bocas del usuario autenticado (por distribuidor asignado) |
| `GET` | `/cabeceras` | Cabeceras activas ordenadas |
| `GET` | `/items` | Items activos con imagen en base64 |
| `POST` | `/respuestas` | Guardar respuestas (cab + detalles + imágenes) |
| `POST` | `/upload-image` | Subir imagen en base64 |
| `POST` | `/upload-list-image` | Subir lista de imágenes en base64 |
| `PUT` | `/usuarios/change-password` | Cambiar contraseña del usuario |

### d-reports (Web)

Base path: `jhonson/d-reports/api/v1`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/distribuidores` | Distribuidores activos |
| `GET` | `/bocas` | Todas las bocas activas |
| `GET` | `/bocas/distribuidor/{codigo}` | Bocas por distribuidor |
| `GET` | `/imagenes/directorios` | Directorios de imágenes |
| `GET` | `/imagenes/boca/{codigo}` | Imágenes de una boca (filtro mes/anio) |
| `GET` | `/imagenes/boca/{codigo}/meses` | Meses con imágenes por boca |
| `POST` | `/imagenes/externo` | Guardar imagen externa |
| `POST` | `/reportes` | Crear reporte |
| `GET` | `/reportes/last` | Últimos reportes del usuario |
| `GET` | `/reportes/{idReporte}` | Descargar reporte PDF/PPT |