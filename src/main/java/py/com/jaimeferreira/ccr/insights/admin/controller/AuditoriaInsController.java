package py.com.jaimeferreira.ccr.insights.admin.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import py.com.jaimeferreira.ccr.commons.exception.UnknownResourceException;
import py.com.jaimeferreira.ccr.insights.dto.AuditoriaPageDTO;
import py.com.jaimeferreira.ccr.insights.dto.EventoCatalogoDTO;
import py.com.jaimeferreira.ccr.insights.entity.EventoAuditoriaIns;
import py.com.jaimeferreira.ccr.insights.service.AuditoriaInsService;

import java.util.List;

/**
 * Endpoints de consulta de la auditoría del módulo Insights.
 * Requieren JWT válido (mismo surface admin que AdminPlataformaController).
 *
 * @author Jaime Ferreira
 */
@RestController
@RequestMapping("/insights/api/v1/admin/auditoria")
public class AuditoriaInsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditoriaInsController.class);

    @Autowired
    private AuditoriaInsService auditoriaInsService;

    /**
     * Lista paginada y filtrable de registros de auditoría.
     *
     * @param evento     filtro por código de evento (opcional)
     * @param codCliente filtro por código de cliente (opcional)
     * @param page       página base 0 (default 0)
     * @param size       tamaño de página (default 10)
     */
    @GetMapping(produces = "application/json")
    public ResponseEntity<AuditoriaPageDTO> listar(
            @RequestParam(value = "evento", required = false) String evento,
            @RequestParam(value = "codCliente", required = false) String codCliente,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        LOGGER.info("Listando auditoría insights. evento={}, codCliente={}, page={}, size={}",
                evento, codCliente, page, size);

        EventoAuditoriaIns eventoEnum = parseEvento(evento);
        String codClienteNorm = (codCliente != null && !codCliente.trim().isEmpty())
                ? codCliente.trim().toUpperCase() : null;

        return ResponseEntity.ok(auditoriaInsService.listar(eventoEnum, codClienteNorm, page, size));
    }

    /**
     * Catálogo de eventos auditables (para el filtro de la pantalla).
     */
    @GetMapping(value = "/eventos", produces = "application/json")
    public ResponseEntity<List<EventoCatalogoDTO>> listarEventos() {
        LOGGER.info("Listando catálogo de eventos de auditoría insights.");
        return ResponseEntity.ok(auditoriaInsService.listarEventos());
    }

    private EventoAuditoriaIns parseEvento(String evento) {
        if (evento == null || evento.trim().isEmpty()) {
            return null;
        }
        try {
            return EventoAuditoriaIns.valueOf(evento.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new UnknownResourceException("Evento de auditoría inválido: " + evento);
        }
    }
}
