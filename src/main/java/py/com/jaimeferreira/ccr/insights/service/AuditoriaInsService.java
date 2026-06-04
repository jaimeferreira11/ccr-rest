package py.com.jaimeferreira.ccr.insights.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.insights.dto.AuditoriaDTO;
import py.com.jaimeferreira.ccr.insights.dto.AuditoriaPageDTO;
import py.com.jaimeferreira.ccr.insights.dto.EventoCatalogoDTO;
import py.com.jaimeferreira.ccr.insights.entity.AuditoriaIns;
import py.com.jaimeferreira.ccr.insights.entity.EventoAuditoriaIns;
import py.com.jaimeferreira.ccr.insights.entity.ResultadoAuditoria;
import py.com.jaimeferreira.ccr.insights.entity.TipoReporte;
import py.com.jaimeferreira.ccr.insights.repository.AuditoriaInsRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Registra eventos de auditoría del módulo Insights.
 *
 * <p>El registro es best-effort: un fallo al auditar NUNCA interrumpe la
 * operación de negocio que se está auditando — sólo se loguea como warning.</p>
 *
 * @author Jaime Ferreira
 */
@Service
public class AuditoriaInsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditoriaInsService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private AuditoriaInsRepository auditoriaInsRepository;

    /**
     * Persiste un registro de auditoría.
     *
     * @param evento       evento del catálogo
     * @param resultado    EXITO o ERROR
     * @param usuario      usuario que ejecutó la acción
     * @param codCliente   código del cliente (obligatorio)
     * @param codCategoria categoría (sólo template; null en otros)
     * @param tipoReporte  tipo de reporte (sólo template y eliminación; null en otros)
     * @param detalle      datos extra que se serializan a JSON (puede ser null)
     */
    public void registrar(EventoAuditoriaIns evento, ResultadoAuditoria resultado,
                          String usuario, String codCliente, String codCategoria,
                          TipoReporte tipoReporte, Map<String, Object> detalle) {
        try {
            AuditoriaIns registro = new AuditoriaIns();
            registro.setEvento(evento);
            registro.setResultado(resultado);
            registro.setUsuario(usuario);
            registro.setCodCliente(codCliente);
            registro.setCodCategoria(codCategoria);
            registro.setTipoReporte(tipoReporte);
            registro.setFechaHora(LocalDateTime.now());
            registro.setDetalle(serializar(detalle));
            auditoriaInsRepository.save(registro);
        } catch (Exception e) {
            LOGGER.warn("No se pudo registrar auditoría [{}/{}] cliente={}: {}",
                    evento, resultado, codCliente, e.toString());
        }
    }

    /**
     * Lista registros de auditoría paginados y filtrados (server-side), ordenados
     * por fecha-hora descendente. Ramifica por combinación de filtros, igual que
     * {@code InformeInsService.findUltimos}.
     *
     * @param evento     filtro por evento (null = todos)
     * @param codCliente filtro por cliente normalizado (null/vacío = todos)
     * @param page       página base 0
     * @param size       tamaño de página
     */
    public AuditoriaPageDTO listar(EventoAuditoriaIns evento, String codCliente, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        boolean tieneCliente = codCliente != null && !codCliente.isEmpty();
        boolean tieneEvento = evento != null;

        long totalElements;
        List<AuditoriaIns> registros;

        if (tieneEvento && tieneCliente) {
            totalElements = auditoriaInsRepository.countByEventoAndCodCliente(evento, codCliente);
            registros = auditoriaInsRepository.findByEventoAndCodClienteOrderByFechaHoraDesc(evento, codCliente, pageable);
        } else if (tieneCliente) {
            totalElements = auditoriaInsRepository.countByCodCliente(codCliente);
            registros = auditoriaInsRepository.findByCodClienteOrderByFechaHoraDesc(codCliente, pageable);
        } else if (tieneEvento) {
            totalElements = auditoriaInsRepository.countByEvento(evento);
            registros = auditoriaInsRepository.findByEventoOrderByFechaHoraDesc(evento, pageable);
        } else {
            totalElements = auditoriaInsRepository.count();
            registros = auditoriaInsRepository.findAllByOrderByFechaHoraDesc(pageable);
        }

        List<AuditoriaDTO> content = registros.stream()
                .map(AuditoriaDTO::from)
                .collect(Collectors.toList());
        return new AuditoriaPageDTO(content, totalElements);
    }

    /**
     * Catálogo de eventos auditables (para poblar el filtro de la pantalla).
     */
    public List<EventoCatalogoDTO> listarEventos() {
        return Arrays.stream(EventoAuditoriaIns.values())
                .map(EventoCatalogoDTO::from)
                .collect(Collectors.toList());
    }

    private String serializar(Map<String, Object> detalle) {
        if (detalle == null || detalle.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(detalle);
        } catch (JsonProcessingException e) {
            LOGGER.warn("No se pudo serializar el detalle de auditoría: {}", e.toString());
            return null;
        }
    }
}
