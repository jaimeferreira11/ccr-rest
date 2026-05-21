package py.com.jaimeferreira.ccr.lt.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import py.com.jaimeferreira.ccr.lt.dto.*;
import py.com.jaimeferreira.ccr.lt.service.LtIntegracionService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("lt/api/v1")
public class LtIntegracionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LtIntegracionController.class);
    private static final Logger AUDIT  = LoggerFactory.getLogger("lt.audit");

    @Autowired
    private LtIntegracionService service;

    @PostMapping("/sucursales")
    public ResponseEntity<LtResponseDTO> guardarSucursales(@RequestBody List<SucursalDTO> lista,
                                                           HttpServletRequest request) {
        int guardados = service.guardarSucursales(lista);
        AUDIT.info("POST /lt/api/v1/sucursales | ip={} | recibidos={} | guardados={} | status=OK",
                   clientIp(request), lista.size(), guardados);
        return ResponseEntity.ok(new LtResponseDTO("OK", "Sucursales guardadas", guardados));
    }

    @PostMapping("/productos")
    public ResponseEntity<LtResponseDTO> guardarProductos(@RequestBody List<ProductoDTO> lista,
                                                          HttpServletRequest request) {
        int guardados = service.guardarProductos(lista);
        AUDIT.info("POST /lt/api/v1/productos | ip={} | recibidos={} | guardados={} | status=OK",
                   clientIp(request), lista.size(), guardados);
        return ResponseEntity.ok(new LtResponseDTO("OK", "Productos guardados", guardados));
    }

    @PostMapping("/tickets")
    public ResponseEntity<LtResponseDTO> guardarTickets(@RequestBody List<TicketDTO> lista,
                                                        HttpServletRequest request) {
        int guardados = service.guardarTickets(lista);
        AUDIT.info("POST /lt/api/v1/tickets | ip={} | recibidos={} | guardados={} | status=OK",
                   clientIp(request), lista.size(), guardados);
        return ResponseEntity.ok(new LtResponseDTO("OK", "Tickets guardados", guardados));
    }

    @PostMapping("/personas")
    public ResponseEntity<LtResponseDTO> guardarPersonas(@RequestBody List<PersonaDTO> lista,
                                                         HttpServletRequest request) {
        int guardados = service.guardarPersonas(lista);
        AUDIT.info("POST /lt/api/v1/personas | ip={} | recibidos={} | guardados={} | status=OK",
                   clientIp(request), lista.size(), guardados);
        return ResponseEntity.ok(new LtResponseDTO("OK", "Personas guardadas", guardados));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isEmpty())
               ? forwarded.split(",")[0].trim()
               : request.getRemoteAddr();
    }
}
