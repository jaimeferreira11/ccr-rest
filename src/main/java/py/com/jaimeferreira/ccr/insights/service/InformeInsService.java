package py.com.jaimeferreira.ccr.insights.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.commons.exception.UnknownResourceException;
import py.com.jaimeferreira.ccr.insights.entity.EstadoInforme;
import py.com.jaimeferreira.ccr.insights.entity.InformeIns;
import py.com.jaimeferreira.ccr.insights.repository.InformeInsRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CRUD y consultas sobre los registros de informes generados.
 *
 * @author Jaime Ferreira
 */
@Service
public class InformeInsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InformeInsService.class);

    @Autowired
    private InformeInsRepository repository;

    public List<InformeIns> findUltimos(String usuario) {
        return repository.findByNombreUsuarioCreacionOrderByFechaCreacionDesc(
                usuario, PageRequest.of(0, 10));
    }

    public List<InformeIns> findUltimos(String usuario, EstadoInforme estado) {
        return repository.findByNombreUsuarioCreacionAndEstadoOrderByFechaCreacionDesc(
                usuario, estado, PageRequest.of(0, 10));
    }

    public InformeIns findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new UnknownResourceException("Informe con id " + id + " no encontrado."));
    }

    public InformeIns save(InformeIns informe) {
        return repository.save(informe);
    }

    public void marcarCompletado(Long id, String nombreArchivo, String usuario, long duracionSegundos) {
        LOGGER.info("Marcando informe {} como COMPLETADO en {}s", id, duracionSegundos);
        InformeIns informe = findById(id);
        informe.setEstado(EstadoInforme.COMPLETADO);
        informe.setNombreArchivo(nombreArchivo);
        informe.setDuracionSegundos(duracionSegundos);
        informe.setFechaActualizacion(LocalDateTime.now());
        informe.setNombreUsuarioActualizacion(usuario);
        repository.save(informe);
    }

    public void marcarError(Long id, String mensajeError, String usuario) {
        LOGGER.error("Marcando informe {} como ERROR: {}", id, mensajeError);
        InformeIns informe = findById(id);
        informe.setEstado(EstadoInforme.ERROR);
        informe.setMensajeError(mensajeError);
        informe.setFechaActualizacion(LocalDateTime.now());
        informe.setNombreUsuarioActualizacion(usuario);
        repository.save(informe);
    }
}
