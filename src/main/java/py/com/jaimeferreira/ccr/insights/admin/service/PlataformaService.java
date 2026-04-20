package py.com.jaimeferreira.ccr.insights.admin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.insights.admin.entity.PlataformaConfig;
import py.com.jaimeferreira.ccr.insights.admin.repository.PlataformaConfigRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Servicio que gestiona el estado activo/suspendido de la plataforma.
 * Cachea el estado en memoria y lo refresca desde la BD cada 2 minutos.
 *
 * @author Jaime Ferreira
 */
@Service
public class PlataformaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlataformaService.class);

    private static final Long CONFIG_ID = 1L;

    @Autowired
    private PlataformaConfigRepository repository;

    private final AtomicBoolean activaCached = new AtomicBoolean(true);
    private final AtomicReference<String> mensajeCached =
            new AtomicReference<>("La Plataforma queda suspendida por falta administrativa.");

    @PostConstruct
    public void init() {
        refrescarCache();
    }

    /** Refresca el estado desde la BD cada 10 segundos. */
    @Scheduled(fixedDelay = 10 * 1000)
    public void refrescarCache() {
        repository.findById(CONFIG_ID).ifPresent(config -> {
            activaCached.set(Boolean.TRUE.equals(config.getActiva()));
            if (config.getMensajeSuspension() != null) {
                mensajeCached.set(config.getMensajeSuspension());
            }
        });
    }

    public boolean isActiva() {
        return activaCached.get();
    }

    public String getMensajeSuspension() {
        return mensajeCached.get();
    }

    /**
     * Lee el estado directamente desde la BD, sin cache.
     * Usar en endpoints donde se requiere el valor más reciente.
     */
    public PlataformaConfig getEstadoFromDB() {
        return repository.findById(CONFIG_ID)
                .orElseThrow(() -> new IllegalStateException("Configuración de plataforma no encontrada."));
    }

    /**
     * Habilita o deshabilita la plataforma y persiste en BD.
     */
    public PlataformaConfig setEstado(boolean activa, String usuario) {
        PlataformaConfig config = repository.findById(CONFIG_ID)
                .orElseThrow(() -> new IllegalStateException("Configuración de plataforma no encontrada."));

        config.setActiva(activa);
        config.setFechaActualizacion(LocalDateTime.now());
        config.setNombreUsuarioActualizacion(usuario);
        config = repository.save(config);

        // Actualizar cache inmediatamente
        activaCached.set(activa);

        LOGGER.warn("Plataforma {} por usuario '{}'.", activa ? "HABILITADA" : "SUSPENDIDA", usuario);
        return config;
    }
}
