package py.com.jaimeferreira.ccr.insights.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.commons.exception.UnknownResourceException;
import py.com.jaimeferreira.ccr.insights.dto.InformeDTO;
import py.com.jaimeferreira.ccr.insights.dto.InformePageDTO;
import py.com.jaimeferreira.ccr.insights.entity.EstadoInforme;
import py.com.jaimeferreira.ccr.insights.entity.InformeIns;
import py.com.jaimeferreira.ccr.insights.repository.InformeInsRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

    public InformePageDTO findUltimos(String usuario, String codCliente, EstadoInforme estado, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        boolean tieneCliente = codCliente != null && !codCliente.isEmpty();
        boolean tieneEstado = estado != null;

        long totalElements;
        List<InformeIns> informes;

        if (tieneCliente && tieneEstado) {
            totalElements = repository.countByNombreUsuarioCreacionAndCodClienteAndEstado(usuario, codCliente, estado);
            informes = repository.findByNombreUsuarioCreacionAndCodClienteAndEstadoOrderByFechaCreacionDesc(
                    usuario, codCliente, estado, pageable);
        } else if (tieneCliente) {
            totalElements = repository.countByNombreUsuarioCreacionAndCodCliente(usuario, codCliente);
            informes = repository.findByNombreUsuarioCreacionAndCodClienteOrderByFechaCreacionDesc(
                    usuario, codCliente, pageable);
        } else if (tieneEstado) {
            totalElements = repository.countByNombreUsuarioCreacionAndEstado(usuario, estado);
            informes = repository.findByNombreUsuarioCreacionAndEstadoOrderByFechaCreacionDesc(
                    usuario, estado, pageable);
        } else {
            totalElements = repository.countByNombreUsuarioCreacion(usuario);
            informes = repository.findByNombreUsuarioCreacionOrderByFechaCreacionDesc(usuario, pageable);
        }

        List<InformeDTO> content = informes.stream().map(InformeDTO::from).collect(Collectors.toList());
        return new InformePageDTO(content, totalElements);
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

    public void eliminar(Long id) {
        InformeIns informe = findById(id);
        if (informe.getEstado() != EstadoInforme.ERROR) {
            throw new UnknownResourceException(
                    "Solo se pueden eliminar informes en estado ERROR. Estado actual: " + informe.getEstado());
        }
        LOGGER.info("Eliminando informe id={} en estado ERROR", id);
        repository.deleteById(id);
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
