package py.com.jaimeferreira.ccr.commons.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import py.com.jaimeferreira.ccr.commons.dto.CotizacionDTO;
import py.com.jaimeferreira.ccr.commons.entity.Cotizacion;
import py.com.jaimeferreira.ccr.commons.service.CotizacionService;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Endpoint público para consultar cotizaciones de monedas.
 * Accesible sin autenticación (ruta /public/**).
 */
@RestController
@RequestMapping("/public/cotizacion")
public class PublicCotizacionController {

    @Autowired
    private CotizacionService cotizacionService;

    /**
     * GET /public/cotizacion?moneda=USD&fecha=2026-05-11
     *
     * @param moneda código de moneda (default: USD)
     * @param fecha  fecha de consulta en formato yyyy-MM-dd (default: hoy)
     * @return cotización encontrada o 404 si no hay registros
     */
    @GetMapping(produces = "application/json")
    public ResponseEntity<CotizacionDTO> getCotizacion(
            @RequestParam(value = "moneda", defaultValue = "USD") String moneda,
            @RequestParam(value = "fecha", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        Optional<Cotizacion> cotizacion = cotizacionService.obtenerCotizacion(moneda, fecha);

        return cotizacion
                .map(c -> ResponseEntity.ok(CotizacionDTO.from(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /public/cotizacion/fetch — fuerza la consulta de cotización del día desde APIs externas.
     *
     * @return resultado indicando si se obtuvo la cotización o no
     */
    @PostMapping(value = "/fetch", produces = "application/json")
    public ResponseEntity<Map<String, Object>> fetchCotizacion() {
        boolean ok = cotizacionService.fetchYGuardarCotizacionDiaria();

        if (ok) {
            Optional<Cotizacion> cotizacion = cotizacionService.obtenerCotizacion(null, null);
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "cotizacion", cotizacion.map(CotizacionDTO::from).orElse(null)
            ));
        }

        return ResponseEntity.ok(Map.of(
                "ok", false,
                "mensaje", "No se pudo obtener cotización de ninguna fuente externa"
        ));
    }

    /**
     * POST /public/cotizacion/bulk — carga masiva de cotizaciones históricas.
     *
     * Body: [{"moneda":"USD","valor":7350.00,"fecha":"2025-01-31","fuente":"DNIT"}, ...]
     * El campo "fuente" es opcional (default: "MANUAL").
     * Si ya existe un registro para la misma moneda+fecha, se actualiza.
     */
    @PostMapping(value = "/bulk", produces = "application/json")
    public ResponseEntity<Map<String, Object>> cargarBulk(
            @RequestBody List<Map<String, Object>> cotizaciones) {

        int procesados = cotizacionService.cargarCotizacionesBulk(cotizaciones);

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "procesados", procesados,
                "total", cotizaciones.size()
        ));
    }
}
