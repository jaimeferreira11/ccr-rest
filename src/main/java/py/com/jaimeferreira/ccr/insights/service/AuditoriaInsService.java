package py.com.jaimeferreira.ccr.insights.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.insights.entity.AuditoriaIns;
import py.com.jaimeferreira.ccr.insights.entity.EventoAuditoriaIns;
import py.com.jaimeferreira.ccr.insights.entity.ResultadoAuditoria;
import py.com.jaimeferreira.ccr.insights.entity.TipoReporte;
import py.com.jaimeferreira.ccr.insights.repository.AuditoriaInsRepository;

import java.time.LocalDateTime;
import java.util.Map;

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
