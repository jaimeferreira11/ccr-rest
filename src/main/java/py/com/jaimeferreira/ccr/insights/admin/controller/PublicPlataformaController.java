package py.com.jaimeferreira.ccr.insights.admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import py.com.jaimeferreira.ccr.insights.admin.entity.PlataformaConfig;
import py.com.jaimeferreira.ccr.insights.admin.service.PlataformaService;

import java.util.HashMap;
import java.util.Map;

/**
 * Endpoint público para consultar el estado de la plataforma SIN JWT.
 * Permite al frontend verificar si está suspendida antes del login.
 *
 * @author Jaime Ferreira
 */
@RestController
@RequestMapping("/public/plataforma")
public class PublicPlataformaController {

    @Autowired
    private PlataformaService plataformaService;

    /** GET /public/plataforma/estado — accesible sin autenticación. Siempre lee desde BD. */
    @GetMapping(value = "/estado", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getEstado() {
        PlataformaConfig config = plataformaService.getEstadoFromDB();
        Map<String, Object> resp = new HashMap<>();
        resp.put("activa", config.getActiva());
        resp.put("mensaje", config.getMensajeSuspension());
        return ResponseEntity.ok(resp);
    }
}
