package py.com.jaimeferreira.ccr.insights.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import py.com.jaimeferreira.ccr.insights.entity.EstadoInforme;
import py.com.jaimeferreira.ccr.insights.entity.InformeIns;
import py.com.jaimeferreira.ccr.insights.repository.InformeInsRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Daemon que detecta informes atascados en PROCESANDO por más de 20 minutos
 * y los marca automáticamente como ERROR.
 *
 * Se ejecuta cada 5 minutos.
 *
 * @author Jaime Ferreira
 */
@Component
public class InformeCleanupScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(InformeCleanupScheduler.class);

    private static final int TIMEOUT_MINUTOS = 20;

    @Autowired
    private InformeInsRepository repository;

    @Scheduled(fixedDelay = 10 * 60 * 1000) // cada 10 minutos
    public void marcarInformesAtascados() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTOS);

        List<InformeIns> atascados = repository.findByEstadoAndFechaCreacionBefore(
                EstadoInforme.PROCESANDO, limite);

        if (atascados.isEmpty()) {
            return;
        }

        LOGGER.warn("Se encontraron {} informe(s) atascados en PROCESANDO por más de {} minutos.",
                atascados.size(), TIMEOUT_MINUTOS);

        for (InformeIns informe : atascados) {
            LOGGER.warn("Marcando informe id={} (cliente={}, creado={}) como ERROR por timeout.",
                    informe.getId(), informe.getCodCliente(), informe.getFechaCreacion());
            informe.setEstado(EstadoInforme.ERROR);
            informe.setMensajeError("Timeout: el proceso superó los " + TIMEOUT_MINUTOS
                    + " minutos sin completarse.");
            informe.setFechaActualizacion(LocalDateTime.now());
            repository.save(informe);
        }
    }
}
