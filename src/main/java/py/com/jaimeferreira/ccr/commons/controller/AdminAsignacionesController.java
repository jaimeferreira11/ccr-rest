package py.com.jaimeferreira.ccr.commons.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import py.com.jaimeferreira.ccr.jhonson.entity.BocaAuditorSCJ;
import py.com.jaimeferreira.ccr.jhonson.repository.BocaAuditorSCJRepository;
import py.com.jaimeferreira.ccr.nestle.entity.UsuarioDistribuidorNest;
import py.com.jaimeferreira.ccr.nestle.repository.UsuarioDistribuidorNestRepository;

/**
 * Controller para gestión de asignaciones por esquema.
 * - Nestlé: asignación de distribuidores a usuarios.
 * - Johnson: copia de asignaciones de bocas entre auditores.
 *
 * @author Jaime Ferreira
 */
@RestController
@RequestMapping("/api/v1/admin/asignaciones")
public class AdminAsignacionesController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminAsignacionesController.class);

    @Autowired
    private UsuarioDistribuidorNestRepository usuarioDistribuidorNestRepository;

    @Autowired
    private BocaAuditorSCJRepository bocaAuditorSCJRepository;

    // ── Nestlé: usuario → distribuidores ──────────────────────────────

    @GetMapping(value = "/nestle/{usuario}/distribuidores", produces = "application/json")
    public ResponseEntity<List<Map<String, Object>>> getDistribuidoresUsuarioNestle(@PathVariable String usuario) {
        LOGGER.info("Consultando distribuidores asignados a usuario nestle: {}", usuario);

        List<UsuarioDistribuidorNest> asignaciones = usuarioDistribuidorNestRepository.findByUsuario(usuario.trim().toLowerCase());

        List<Map<String, Object>> result = asignaciones.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("usuario", a.getUsuario());
            m.put("codDistribuidor", a.getCodDistribuidor());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/nestle/{usuario}/distribuidores", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> asignarDistribuidorNestle(
            @PathVariable String usuario, @RequestBody Map<String, String> req) {

        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        String codDistribuidor = req.get("codDistribuidor");
        String usuarioNorm = usuario.trim().toLowerCase();

        LOGGER.info("Usuario '{}' asigna distribuidor '{}' a usuario nestle '{}'", adminUser, codDistribuidor, usuarioNorm);

        // Verificar si ya existe
        List<UsuarioDistribuidorNest> existentes = usuarioDistribuidorNestRepository.findByUsuario(usuarioNorm);
        boolean yaExiste = existentes.stream()
                .anyMatch(a -> a.getCodDistribuidor().equalsIgnoreCase(codDistribuidor));

        if (yaExiste) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("mensaje", "El distribuidor ya está asignado a este usuario.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(resp);
        }

        UsuarioDistribuidorNest asignacion = new UsuarioDistribuidorNest();
        asignacion.setUsuario(usuarioNorm);
        asignacion.setCodDistribuidor(codDistribuidor.trim());
        usuarioDistribuidorNestRepository.save(asignacion);

        Map<String, Object> resp = new HashMap<>();
        resp.put("mensaje", "Distribuidor asignado correctamente.");
        resp.put("usuario", usuarioNorm);
        resp.put("codDistribuidor", codDistribuidor);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @DeleteMapping(value = "/nestle/distribuidores/{id}", produces = "application/json")
    public ResponseEntity<Map<String, Object>> desasignarDistribuidorNestle(@PathVariable Long id) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' desasigna usuario_distribuidor nestle id={}", adminUser, id);

        usuarioDistribuidorNestRepository.deleteById(id);

        Map<String, Object> resp = new HashMap<>();
        resp.put("mensaje", "Distribuidor desasignado correctamente.");
        return ResponseEntity.ok(resp);
    }

    // ── Johnson: copiar bocas entre auditores ─────────────────────────

    @GetMapping(value = "/jhonson/{auditor}/resumen", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getResumenAuditorJhonson(@PathVariable String auditor) {
        LOGGER.info("Consultando resumen de bocas para auditor jhonson: {}", auditor);

        List<BocaAuditorSCJ> asignaciones = bocaAuditorSCJRepository.findByAuditor(auditor.trim().toLowerCase());

        Map<String, Object> resp = new HashMap<>();
        resp.put("auditor", auditor);
        resp.put("totalBocas", asignaciones.size());
        resp.put("idBocas", asignaciones.stream().map(BocaAuditorSCJ::getIdBoca).collect(Collectors.toList()));
        return ResponseEntity.ok(resp);
    }

    @PostMapping(value = "/jhonson/copiar", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> copiarAsignacionesJhonson(@RequestBody Map<String, String> req) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        String origen = req.get("origen").trim().toLowerCase();
        String destino = req.get("destino").trim().toLowerCase();

        LOGGER.info("Usuario '{}' copia bocas de auditor '{}' a '{}'", adminUser, origen, destino);

        if (origen.equals(destino)) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("mensaje", "El usuario origen y destino no pueden ser el mismo.");
            return ResponseEntity.badRequest().body(resp);
        }

        // Bocas del origen
        List<BocaAuditorSCJ> bocasOrigen = bocaAuditorSCJRepository.findByAuditor(origen);
        if (bocasOrigen.isEmpty()) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("mensaje", "El usuario origen no tiene bocas asignadas.");
            return ResponseEntity.badRequest().body(resp);
        }

        // Bocas que ya tiene el destino
        Set<Long> bocasDestinoExistentes = bocaAuditorSCJRepository.findByAuditor(destino)
                .stream().map(BocaAuditorSCJ::getIdBoca).collect(Collectors.toSet());

        // Insertar solo las que no tiene
        int copiadas = 0;
        int omitidas = 0;
        for (BocaAuditorSCJ ba : bocasOrigen) {
            if (bocasDestinoExistentes.contains(ba.getIdBoca())) {
                omitidas++;
            } else {
                BocaAuditorSCJ nueva = new BocaAuditorSCJ();
                nueva.setIdBoca(ba.getIdBoca());
                nueva.setAuditor(destino);
                bocaAuditorSCJRepository.save(nueva);
                copiadas++;
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("mensaje", String.format("Se copiaron %d bocas a '%s'. %d ya estaban asignadas y se omitieron.", copiadas, destino, omitidas));
        resp.put("copiadas", copiadas);
        resp.put("omitidas", omitidas);
        resp.put("origen", origen);
        resp.put("destino", destino);
        return ResponseEntity.ok(resp);
    }

}
