package py.com.jaimeferreira.ccr.commons.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job programado para obtener la cotización del dólar automáticamente.
 *
 * - 7:00 AM (lun-vie): intento principal (DNIT + fallback DolarPy).
 * - 14:00 PM (lun-vie): reintento si la cotización del día no se obtuvo a la mañana.
 *
 * Zona horaria: America/Asuncion.
 */
@Component
public class CotizacionScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CotizacionScheduler.class);

    @Autowired
    private CotizacionService cotizacionService;

    @Scheduled(cron = "0 0 7 * * MON-FRI", zone = "America/Asuncion")
    public void fetchCotizacionManana() {
        LOGGER.info("=== Job cotización 7:00 AM ===");
        boolean ok = cotizacionService.fetchYGuardarCotizacionDiaria();
        if (ok) {
            LOGGER.info("Cotización obtenida exitosamente en el turno de la mañana.");
        } else {
            LOGGER.warn("No se pudo obtener cotización a las 7:00. Se reintentará a las 14:00.");
        }
    }

    @Scheduled(cron = "0 0 14 * * MON-FRI", zone = "America/Asuncion")
    public void fetchCotizacionReintento() {
        LOGGER.info("=== Job cotización 14:00 PM (reintento) ===");
        boolean ok = cotizacionService.fetchYGuardarCotizacionDiaria();
        if (ok) {
            LOGGER.info("Cotización obtenida/verificada en el turno de la tarde.");
        } else {
            LOGGER.error("No se pudo obtener cotización en ningún turno del día.");
        }
    }
}
