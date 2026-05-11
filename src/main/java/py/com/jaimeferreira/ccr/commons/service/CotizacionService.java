package py.com.jaimeferreira.ccr.commons.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import py.com.jaimeferreira.ccr.commons.entity.Cotizacion;
import py.com.jaimeferreira.ccr.commons.repository.CotizacionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CotizacionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CotizacionService.class);

    private static final String MONEDA_USD = "USD";
    private static final ZoneId ZONE_PY = ZoneId.of("America/Asuncion");

    @Value("${cotizacion.dnit.url:https://www.dnit.gov.py/dna-reference/ddt/ctz/getultimacotizacion}")
    private String dnitUrl;

    @Value("${cotizacion.dolarpy.url:https://dolar.melizeche.com/api/1.0/}")
    private String dolarPyUrl;

    @Autowired
    private CotizacionRepository repository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Obtiene la cotización para una moneda y fecha.
     * Si no existe registro para la fecha exacta, retorna la más reciente anterior.
     *
     * @param moneda código de moneda (ej: "USD")
     * @param fecha  fecha de consulta (null = hoy)
     * @return cotización encontrada, o empty si no hay ningún registro
     */
    public Optional<Cotizacion> obtenerCotizacion(String moneda, LocalDate fecha) {
        if (moneda == null || moneda.isBlank()) {
            moneda = MONEDA_USD;
        }
        if (fecha == null) {
            fecha = LocalDate.now(ZONE_PY);
        }

        // Buscar exacta primero
        Optional<Cotizacion> exacta = repository.findByMonedaAndFecha(moneda.toUpperCase(), fecha);
        if (exacta.isPresent()) {
            return exacta;
        }

        // Fallback: la más reciente anterior o igual a la fecha
        return repository.findTopByMonedaAndFechaLessThanEqualOrderByFechaDesc(moneda.toUpperCase(), fecha);
    }

    /**
     * Intenta obtener la cotización del día desde las APIs externas y la persiste.
     * Primero intenta DNIT, si falla intenta DolarPy.
     *
     * @return true si se obtuvo y persistió la cotización
     */
    public boolean fetchYGuardarCotizacionDiaria() {
        LocalDate hoy = LocalDate.now(ZONE_PY);

        // Si ya existe la cotización del día, no hacer nada
        if (repository.findByMonedaAndFecha(MONEDA_USD, hoy).isPresent()) {
            LOGGER.info("Cotización USD del {} ya existe, se omite fetch.", hoy);
            return true;
        }

        // Intentar DNIT
        BigDecimal valor = fetchFromDnit();
        String fuente = "DNIT";

        // Fallback DolarPy
        if (valor == null) {
            valor = fetchFromDolarPy();
            fuente = "DOLARPY";
        }

        if (valor == null) {
            LOGGER.error("No se pudo obtener cotización USD de ninguna fuente para {}", hoy);
            return false;
        }

        guardarCotizacion(MONEDA_USD, valor, hoy, fuente);
        return true;
    }

    /**
     * Consulta la API de DNIT (cotización impositiva).
     */
    private BigDecimal fetchFromDnit() {
        try {
            LOGGER.info("Consultando cotización DNIT: {}", dnitUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("pFecha", String.valueOf(System.currentTimeMillis()));
            body.put("pMoneda", "DOL");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(dnitUrl, request, String.class);

            JsonNode json = objectMapper.readTree(response.getBody());
            JsonNode cotizacionNode = json.get("cotizacion");

            if (cotizacionNode != null && cotizacionNode.isNumber()) {
                BigDecimal valor = cotizacionNode.decimalValue();
                LOGGER.info("Cotización DNIT obtenida: {} Gs/USD", valor);
                return valor;
            }

            LOGGER.warn("Respuesta DNIT sin campo 'cotizacion': {}", response.getBody());
            return null;

        } catch (Exception e) {
            LOGGER.error("Error al consultar DNIT: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Consulta la API de DolarPy (fallback).
     * Usa el valor referencial_diario del BCP.
     */
    private BigDecimal fetchFromDolarPy() {
        try {
            LOGGER.info("Consultando cotización DolarPy (fallback): {}", dolarPyUrl);

            ResponseEntity<String> response = restTemplate.getForEntity(dolarPyUrl, String.class);
            JsonNode json = objectMapper.readTree(response.getBody());

            JsonNode bcp = json.path("dolarpy").path("bcp");
            JsonNode referencialNode = bcp.path("referencial_diario");

            if (!referencialNode.isMissingNode() && referencialNode.isNumber()) {
                BigDecimal valor = referencialNode.decimalValue();
                LOGGER.info("Cotización DolarPy (BCP referencial) obtenida: {} Gs/USD", valor);
                return valor;
            }

            // Fallback al promedio compra/venta si no hay referencial
            JsonNode compra = bcp.path("compra");
            JsonNode venta = bcp.path("venta");
            if (!compra.isMissingNode() && !venta.isMissingNode()) {
                BigDecimal promedio = compra.decimalValue().add(venta.decimalValue())
                        .divide(BigDecimal.valueOf(2), 4, java.math.RoundingMode.HALF_UP);
                LOGGER.info("Cotización DolarPy (promedio compra/venta) obtenida: {} Gs/USD", promedio);
                return promedio;
            }

            LOGGER.warn("Respuesta DolarPy sin datos BCP: {}", response.getBody());
            return null;

        } catch (Exception e) {
            LOGGER.error("Error al consultar DolarPy: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Carga masiva de cotizaciones. Si ya existe un registro para la misma moneda+fecha,
     * lo actualiza con el nuevo valor.
     *
     * @return cantidad de registros insertados/actualizados
     */
    public int cargarCotizacionesBulk(List<Map<String, Object>> cotizaciones) {
        int count = 0;
        List<String> errores = new ArrayList<>();

        for (int i = 0; i < cotizaciones.size(); i++) {
            Map<String, Object> item = cotizaciones.get(i);
            try {
                String moneda = ((String) item.get("moneda")).trim().toUpperCase();
                BigDecimal valor = new BigDecimal(item.get("valor").toString());
                LocalDate fecha = LocalDate.parse(item.get("fecha").toString());
                String fuente = item.containsKey("fuente") ? (String) item.get("fuente") : "MANUAL";

                Optional<Cotizacion> existente = repository.findByMonedaAndFecha(moneda, fecha);
                if (existente.isPresent()) {
                    Cotizacion cot = existente.get();
                    cot.setValor(valor);
                    cot.setFuente(fuente);
                    cot.setCreatedAt(LocalDateTime.now(ZONE_PY));
                    repository.save(cot);
                } else {
                    guardarCotizacion(moneda, valor, fecha, fuente);
                }
                count++;
            } catch (Exception e) {
                errores.add("Índice " + i + ": " + e.getMessage());
                LOGGER.warn("Error en carga bulk índice {}: {}", i, e.getMessage());
            }
        }

        if (!errores.isEmpty()) {
            LOGGER.warn("Carga bulk completada con {} errores de {} registros", errores.size(), cotizaciones.size());
        }
        LOGGER.info("Carga bulk: {} registros procesados exitosamente", count);
        return count;
    }

    private void guardarCotizacion(String moneda, BigDecimal valor, LocalDate fecha, String fuente) {
        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setMoneda(moneda);
        cotizacion.setValor(valor);
        cotizacion.setFecha(fecha);
        cotizacion.setFuente(fuente);
        cotizacion.setCreatedAt(LocalDateTime.now(ZONE_PY));

        repository.save(cotizacion);
        LOGGER.info("Cotización guardada: {} = {} Gs ({}, fuente: {})", moneda, valor, fecha, fuente);
    }
}
