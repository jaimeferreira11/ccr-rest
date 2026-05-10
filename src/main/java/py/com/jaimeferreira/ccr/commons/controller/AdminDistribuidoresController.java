package py.com.jaimeferreira.ccr.commons.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import py.com.jaimeferreira.ccr.commons.exception.UnknownResourceException;
import py.com.jaimeferreira.ccr.jhonson.entity.DistribuidorSCJ;
import py.com.jaimeferreira.ccr.jhonson.repository.DistribuidoresSCJRepository;
import py.com.jaimeferreira.ccr.nestle.entity.DistribuidorNest;
import py.com.jaimeferreira.ccr.nestle.repository.DistribuidoresNestRepository;

/**
 * Controller para administración de distribuidores por esquema.
 *
 * @author Jaime Ferreira
 */
@RestController
@RequestMapping("/api/v1/admin/distribuidores")
public class AdminDistribuidoresController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminDistribuidoresController.class);

    @Autowired
    private DistribuidoresSCJRepository distribuidoresSCJRepository;

    @Autowired
    private DistribuidoresNestRepository distribuidoresNestRepository;

    // ── Distribuidores Johnson ──────────────────────────────────────────

    @GetMapping(value = "/jhonson", produces = "application/json")
    public ResponseEntity<List<DistribuidorSCJ>> getDistribuidoresJhonson() {
        LOGGER.info("Listando todos los distribuidores jhonson (admin)");
        List<DistribuidorSCJ> distribuidores = distribuidoresSCJRepository.findAll(
                org.springframework.data.domain.Sort.by("descripcion").ascending());
        return ResponseEntity.ok(distribuidores);
    }

    @PostMapping(value = "/jhonson", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> crearDistribuidorJhonson(@RequestBody Map<String, Object> req) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' crea distribuidor jhonson: {}", adminUser, req.get("codigo"));

        DistribuidorSCJ dist = new DistribuidorSCJ();
        dist.setCodigo((String) req.get("codigo"));
        dist.setDescripcion((String) req.get("descripcion"));
        dist.setActivo(true);

        dist = distribuidoresSCJRepository.save(dist);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", dist.getId());
        resp.put("mensaje", "Distribuidor creado correctamente.");
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PutMapping(value = "/jhonson/{id}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> actualizarDistribuidorJhonson(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' actualiza distribuidor jhonson: {}", adminUser, id);

        DistribuidorSCJ dist = distribuidoresSCJRepository.findById(id)
                .orElseThrow(() -> new UnknownResourceException("Distribuidor no encontrado con id: " + id));

        dist.setDescripcion((String) req.get("descripcion"));
        distribuidoresSCJRepository.save(dist);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", dist.getId());
        resp.put("mensaje", "Distribuidor actualizado correctamente.");
        return ResponseEntity.ok(resp);
    }

    @PatchMapping(value = "/jhonson/{id}/estado", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> toggleEstadoJhonson(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        Boolean activo = (Boolean) req.get("activo");
        LOGGER.info("Usuario '{}' cambia estado distribuidor jhonson {} a activo={}", adminUser, id, activo);

        DistribuidorSCJ dist = distribuidoresSCJRepository.findById(id)
                .orElseThrow(() -> new UnknownResourceException("Distribuidor no encontrado con id: " + id));

        dist.setActivo(Boolean.TRUE.equals(activo));
        distribuidoresSCJRepository.save(dist);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", dist.getId());
        resp.put("activo", dist.getActivo());
        resp.put("mensaje", activo ? "Distribuidor activado correctamente." : "Distribuidor desactivado correctamente.");
        return ResponseEntity.ok(resp);
    }

    // ── Distribuidores Nestlé ───────────────────────────────────────────

    @GetMapping(value = "/nestle", produces = "application/json")
    public ResponseEntity<List<DistribuidorNest>> getDistribuidoresNestle() {
        LOGGER.info("Listando todos los distribuidores nestle (admin)");
        List<DistribuidorNest> distribuidores = distribuidoresNestRepository.findAll(
                org.springframework.data.domain.Sort.by("descripcion").ascending());
        return ResponseEntity.ok(distribuidores);
    }

    @PostMapping(value = "/nestle", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> crearDistribuidorNestle(@RequestBody Map<String, Object> req) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' crea distribuidor nestle: {}", adminUser, req.get("codigo"));

        DistribuidorNest dist = new DistribuidorNest();
        dist.setCodigo((String) req.get("codigo"));
        dist.setDescripcion((String) req.get("descripcion"));
        dist.setActivo(true);

        dist = distribuidoresNestRepository.save(dist);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", dist.getId());
        resp.put("mensaje", "Distribuidor creado correctamente.");
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PutMapping(value = "/nestle/{id}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> actualizarDistribuidorNestle(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' actualiza distribuidor nestle: {}", adminUser, id);

        DistribuidorNest dist = distribuidoresNestRepository.findById(id)
                .orElseThrow(() -> new UnknownResourceException("Distribuidor no encontrado con id: " + id));

        dist.setDescripcion((String) req.get("descripcion"));
        distribuidoresNestRepository.save(dist);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", dist.getId());
        resp.put("mensaje", "Distribuidor actualizado correctamente.");
        return ResponseEntity.ok(resp);
    }

    @PatchMapping(value = "/nestle/{id}/estado", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> toggleEstadoNestle(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        Boolean activo = (Boolean) req.get("activo");
        LOGGER.info("Usuario '{}' cambia estado distribuidor nestle {} a activo={}", adminUser, id, activo);

        DistribuidorNest dist = distribuidoresNestRepository.findById(id)
                .orElseThrow(() -> new UnknownResourceException("Distribuidor no encontrado con id: " + id));

        dist.setActivo(Boolean.TRUE.equals(activo));
        distribuidoresNestRepository.save(dist);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", dist.getId());
        resp.put("activo", dist.getActivo());
        resp.put("mensaje", activo ? "Distribuidor activado correctamente." : "Distribuidor desactivado correctamente.");
        return ResponseEntity.ok(resp);
    }

}
