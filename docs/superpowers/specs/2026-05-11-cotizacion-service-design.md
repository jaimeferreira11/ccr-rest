# Servicio de Cotizaciones

## Problema

Los reportes de d-insights muestran facturación en miles de Gs. Se necesita poder convertir a USD usando la cotización oficial del BCP. La cotización debe obtenerse automáticamente y estar disponible como API pública reutilizable por cualquier módulo.

## Decisiones de diseño

- **Una sola tabla** `cotizacion` en schema `public` (reutilizable entre módulos).
- Columna `moneda` (VARCHAR) para soportar múltiples monedas a futuro. Solo USD por ahora.
- Sin tabla separada de monedas (YAGNI).
- Código en paquete `commons` (entity, repository, service, controller).

## Tabla

```sql
CREATE TABLE public.cotizacion (
    id          SERIAL PRIMARY KEY,
    moneda      VARCHAR(10)    NOT NULL,
    valor       NUMERIC(18,4)  NOT NULL,
    fecha       DATE           NOT NULL,
    fuente      VARCHAR(50),
    created_at  TIMESTAMP      NOT NULL DEFAULT now(),
    CONSTRAINT uk_cotizacion_moneda_fecha UNIQUE (moneda, fecha)
);
```

## Fuentes de datos

1. **DNIT (primaria)**: `POST https://www.dnit.gov.py/dna-reference/ddt/ctz/getultimacotizacion`
   - Body: `{"pFecha": "<epoch_ms>", "pMoneda": "DOL"}`
   - Response: `{"cotizacion": 6159.41, ...}`
   - Cotización impositiva oficial.

2. **DolarPy (fallback)**: `GET https://dolar.melizeche.com/api/1.0/`
   - Response: `{"dolarpy": {"bcp": {"referencial_diario": 6107.60, ...}}}`
   - Proyecto open-source, scraping del BCP cada 10 min.

## Scheduler

- **7:00 AM** (lunes a viernes): intenta DNIT, si falla intenta DolarPy.
- **14:00 PM** (lunes a viernes): solo ejecuta si NO hay cotización del día (retry).
- Configurado con `@Scheduled(cron = "...")`.
- Zona horaria: America/Asuncion.

## API pública

`GET /public/cotizacion?moneda=USD&fecha=2026-05-11`

- `moneda`: default `USD`
- `fecha`: default hoy (formato `yyyy-MM-dd`)

### Response

```json
{
    "moneda": "USD",
    "valor": 6159.41,
    "fecha": "2026-05-11",
    "fuente": "DNIT"
}
```

Si no hay registro para la fecha solicitada, retorna el más reciente con su fecha real. Si no hay ningún registro, retorna 404.

## Componentes

| Componente | Paquete | Responsabilidad |
|---|---|---|
| `Cotizacion` (entity) | `commons.entity` | Mapeo JPA a `public.cotizacion` |
| `CotizacionRepository` | `commons.repository` | Queries Spring Data |
| `CotizacionService` | `commons.service` | Fetch APIs externas, lógica de consulta, persistencia |
| `CotizacionScheduler` | `commons.service` | Jobs 7:00 y 14:00 |
| `CotizacionDTO` | `commons.dto` | Response del endpoint |
| `PublicCotizacionController` | `commons.controller` | Endpoint público |
