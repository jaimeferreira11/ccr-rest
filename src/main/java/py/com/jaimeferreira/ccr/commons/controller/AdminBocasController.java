package py.com.jaimeferreira.ccr.commons.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import py.com.jaimeferreira.ccr.commons.exception.UnknownResourceException;

import py.com.jaimeferreira.ccr.jhonson.entity.BocaSCJ;
import py.com.jaimeferreira.ccr.jhonson.entity.BocaAuditorSCJ;
import py.com.jaimeferreira.ccr.jhonson.repository.BocasSCJRepository;
import py.com.jaimeferreira.ccr.jhonson.repository.BocaAuditorSCJRepository;
import py.com.jaimeferreira.ccr.nestle.entity.BocaNest;
import py.com.jaimeferreira.ccr.nestle.repository.BocasNestRepository;
import py.com.jaimeferreira.ccr.shell.entity.BocaShell;
import py.com.jaimeferreira.ccr.shell.repository.BocasShellRepository;

/**
 * Controller exclusivo para administración de bocas.
 * Endpoints paginados y gestión de auditores por esquema de producto.
 *
 * @author Jaime Ferreira
 */
@RestController
@RequestMapping("/api/v1/admin/bocas")
public class AdminBocasController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBocasController.class);

    @Autowired
    private BocasSCJRepository bocasSCJRepository;

    @Autowired
    private BocaAuditorSCJRepository bocaAuditorSCJRepository;

    @Autowired
    private BocasNestRepository bocasNestRepository;

    @Autowired
    private BocasShellRepository bocasShellRepository;

    // ── Bocas Johnson (paginado) ────────────────────────────────────────

    @GetMapping(value = "/jhonson", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getBocasJhonson(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String auditor) {

        LOGGER.info("Listando bocas jhonson paginado: page={}, size={}, busqueda={}, activo={}, auditor={}", page, size, busqueda, activo, auditor);

        PageRequest pageable = PageRequest.of(page, size, Sort.by("nombre").ascending().and(Sort.by("id").ascending()));
        Page<BocaSCJ> pagina = bocasSCJRepository.buscarPaginado(
                busqueda != null && busqueda.trim().isEmpty() ? null : busqueda,
                activo,
                auditor != null && auditor.trim().isEmpty() ? null : auditor,
                pageable);

        List<Map<String, Object>> content = pagina.getContent().stream().map(b -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", b.getId());
            m.put("codBoca", b.getCodBoca());
            m.put("nombre", b.getNombre());
            m.put("direccion", b.getDireccion());
            m.put("ciudad", b.getCiudad());
            m.put("canalCcr", b.getCanalCcr());
            m.put("ocasion", b.getOcasion());
            m.put("activo", b.isActivo());
            m.put("externo", b.isExterno());
            m.put("codDistribuidor", b.getCodDistribuidor());
            List<String> auditores = bocaAuditorSCJRepository.findByIdBoca(b.getId())
                    .stream().map(BocaAuditorSCJ::getAuditor).collect(Collectors.toList());
            m.put("auditores", auditores);
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("totalElements", pagina.getTotalElements());
        response.put("totalPages", pagina.getTotalPages());
        response.put("page", pagina.getNumber());
        response.put("size", pagina.getSize());

        return ResponseEntity.ok(response);
    }

    // ── CRUD Bocas Johnson ────────────────────────────────────────────

    @PostMapping(value = "/jhonson", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> crearBoca(@RequestBody Map<String, Object> req) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' crea boca jhonson: {}", adminUser, req.get("codBoca"));

        BocaSCJ boca = new BocaSCJ();
        boca.setCodBoca((String) req.get("codBoca"));
        boca.setNombre((String) req.get("nombre"));
        boca.setDireccion((String) req.get("direccion"));
        boca.setCiudad((String) req.get("ciudad"));
        boca.setCodDistribuidor((String) req.get("codDistribuidor"));
        boca.setCanalCcr((String) req.get("canalCcr"));
        boca.setOcasion((String) req.get("ocasion"));
        boca.setExterno(Boolean.TRUE.equals(req.get("externo")));
        boca.setActivo(true);

        boca = bocasSCJRepository.save(boca);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", boca.getId());
        resp.put("mensaje", "Boca creada correctamente.");
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PutMapping(value = "/jhonson/{id}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> actualizarBoca(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' actualiza boca jhonson: {}", adminUser, id);

        BocaSCJ boca = bocasSCJRepository.findById(id)
                .orElseThrow(() -> new UnknownResourceException("Boca no encontrada con id: " + id));

        boca.setNombre((String) req.get("nombre"));
        boca.setDireccion((String) req.get("direccion"));
        boca.setCiudad((String) req.get("ciudad"));
        boca.setCodDistribuidor((String) req.get("codDistribuidor"));
        boca.setCanalCcr((String) req.get("canalCcr"));
        boca.setOcasion((String) req.get("ocasion"));
        boca.setExterno(Boolean.TRUE.equals(req.get("externo")));

        bocasSCJRepository.save(boca);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", boca.getId());
        resp.put("mensaje", "Boca actualizada correctamente.");
        return ResponseEntity.ok(resp);
    }

    @PatchMapping(value = "/jhonson/{id}/estado", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> toggleEstado(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        Boolean activo = (Boolean) req.get("activo");
        LOGGER.info("Usuario '{}' cambia estado boca jhonson {} a activo={}", adminUser, id, activo);

        BocaSCJ boca = bocasSCJRepository.findById(id)
                .orElseThrow(() -> new UnknownResourceException("Boca no encontrada con id: " + id));

        boca.setActivo(Boolean.TRUE.equals(activo));
        bocasSCJRepository.save(boca);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", boca.getId());
        resp.put("activo", boca.isActivo());
        resp.put("mensaje", activo ? "Boca activada correctamente." : "Boca desactivada correctamente.");
        return ResponseEntity.ok(resp);
    }

    // ── Bocas Nestlé (paginado) ───────────────────────────────────────

    @GetMapping(value = "/nestle", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getBocasNestle(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String codDistribuidor) {

        LOGGER.info("Listando bocas nestle paginado: page={}, size={}, busqueda={}, activo={}, codDistribuidor={}", page, size, busqueda, activo, codDistribuidor);

        PageRequest pageable = PageRequest.of(page, size, Sort.by("nombre").ascending().and(Sort.by("id").ascending()));
        Page<BocaNest> pagina = bocasNestRepository.buscarPaginado(
                busqueda != null && busqueda.trim().isEmpty() ? null : busqueda,
                activo,
                codDistribuidor != null && codDistribuidor.trim().isEmpty() ? null : codDistribuidor,
                pageable);

        List<Map<String, Object>> content = pagina.getContent().stream().map(b -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", b.getId());
            m.put("codBoca", b.getCodBoca());
            m.put("nombre", b.getNombre());
            m.put("direccion", b.getDireccion());
            m.put("ciudad", b.getCiudad());
            m.put("canalCcr", b.getCanalCcr());
            m.put("ocasion", b.getOcasion());
            m.put("activo", Boolean.TRUE.equals(b.getActivo()));
            m.put("codDistribuidor", b.getCodDistribuidor());
            m.put("mesUltimaMedicion", b.getMesUltimaMedicion());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("totalElements", pagina.getTotalElements());
        response.put("totalPages", pagina.getTotalPages());
        response.put("page", pagina.getNumber());
        response.put("size", pagina.getSize());

        return ResponseEntity.ok(response);
    }

    // ── CRUD Bocas Nestlé ──────────────────────────────────────────────

    @PostMapping(value = "/nestle", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> crearBocaNestle(@RequestBody Map<String, Object> req) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' crea boca nestle: {}", adminUser, req.get("codBoca"));

        BocaNest boca = new BocaNest();
        boca.setCodBoca((String) req.get("codBoca"));
        boca.setNombre((String) req.get("nombre"));
        boca.setDireccion((String) req.get("direccion"));
        boca.setCiudad((String) req.get("ciudad"));
        boca.setCodDistribuidor((String) req.get("codDistribuidor"));
        boca.setCanalCcr((String) req.get("canalCcr"));
        boca.setOcasion((String) req.get("ocasion"));
        boca.setActivo(true);

        boca = bocasNestRepository.save(boca);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", boca.getId());
        resp.put("mensaje", "Boca creada correctamente.");
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PutMapping(value = "/nestle/{id}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> actualizarBocaNestle(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' actualiza boca nestle: {}", adminUser, id);

        BocaNest boca = bocasNestRepository.findById(id)
                .orElseThrow(() -> new UnknownResourceException("Boca no encontrada con id: " + id));

        boca.setNombre((String) req.get("nombre"));
        boca.setDireccion((String) req.get("direccion"));
        boca.setCiudad((String) req.get("ciudad"));
        boca.setCodDistribuidor((String) req.get("codDistribuidor"));
        boca.setCanalCcr((String) req.get("canalCcr"));
        boca.setOcasion((String) req.get("ocasion"));

        bocasNestRepository.save(boca);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", boca.getId());
        resp.put("mensaje", "Boca actualizada correctamente.");
        return ResponseEntity.ok(resp);
    }

    @PatchMapping(value = "/nestle/{id}/estado", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> toggleEstadoNestle(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        Boolean activo = (Boolean) req.get("activo");
        LOGGER.info("Usuario '{}' cambia estado boca nestle {} a activo={}", adminUser, id, activo);

        BocaNest boca = bocasNestRepository.findById(id)
                .orElseThrow(() -> new UnknownResourceException("Boca no encontrada con id: " + id));

        boca.setActivo(Boolean.TRUE.equals(activo));
        bocasNestRepository.save(boca);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", boca.getId());
        resp.put("activo", boca.getActivo());
        resp.put("mensaje", activo ? "Boca activada correctamente." : "Boca desactivada correctamente.");
        return ResponseEntity.ok(resp);
    }

    // ── Bocas Shell (paginado) ──────────────────────────────────────────

    @GetMapping(value = "/shell", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getBocasShell(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String zona) {

        LOGGER.info("Listando bocas shell paginado: page={}, size={}, busqueda={}, activo={}, zona={}", page, size, busqueda, activo, zona);

        PageRequest pageable = PageRequest.of(page, size, Sort.by("nombre").ascending().and(Sort.by("id").ascending()));
        Page<BocaShell> pagina = bocasShellRepository.buscarPaginado(
                busqueda != null && busqueda.trim().isEmpty() ? null : busqueda,
                activo,
                zona != null && zona.trim().isEmpty() ? null : zona,
                pageable);

        List<Map<String, Object>> content = pagina.getContent().stream().map(b -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", b.getId());
            m.put("codBoca", b.getCodBoca());
            m.put("nombre", b.getNombre());
            m.put("direccion", b.getDireccion());
            m.put("ciudad", b.getCiudad());
            m.put("zona", b.getZona());
            m.put("activo", Boolean.TRUE.equals(b.getActivo()));
            m.put("longitud", b.getLongitud());
            m.put("latitud", b.getLatitud());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("totalElements", pagina.getTotalElements());
        response.put("totalPages", pagina.getTotalPages());
        response.put("page", pagina.getNumber());
        response.put("size", pagina.getSize());

        return ResponseEntity.ok(response);
    }

    // ── CRUD Bocas Shell ──────────────────────────────────────────────

    @PostMapping(value = "/shell", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> crearBocaShell(@RequestBody Map<String, Object> req) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' crea boca shell: {}", adminUser, req.get("codBoca"));

        BocaShell boca = new BocaShell();
        boca.setCodBoca((String) req.get("codBoca"));
        boca.setNombre((String) req.get("nombre"));
        boca.setDireccion((String) req.get("direccion"));
        boca.setCiudad((String) req.get("ciudad"));
        boca.setZona((String) req.get("zona"));
        boca.setLongitud((String) req.get("longitud"));
        boca.setLatitud((String) req.get("latitud"));
        boca.setActivo(true);

        boca = bocasShellRepository.save(boca);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", boca.getId());
        resp.put("mensaje", "Boca creada correctamente.");
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PutMapping(value = "/shell/{id}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> actualizarBocaShell(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' actualiza boca shell: {}", adminUser, id);

        BocaShell boca = bocasShellRepository.findById(id)
                .orElseThrow(() -> new UnknownResourceException("Boca no encontrada con id: " + id));

        boca.setNombre((String) req.get("nombre"));
        boca.setDireccion((String) req.get("direccion"));
        boca.setCiudad((String) req.get("ciudad"));
        boca.setZona((String) req.get("zona"));
        boca.setLongitud((String) req.get("longitud"));
        boca.setLatitud((String) req.get("latitud"));

        bocasShellRepository.save(boca);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", boca.getId());
        resp.put("mensaje", "Boca actualizada correctamente.");
        return ResponseEntity.ok(resp);
    }

    @PatchMapping(value = "/shell/{id}/estado", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> toggleEstadoShell(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        Boolean activo = (Boolean) req.get("activo");
        LOGGER.info("Usuario '{}' cambia estado boca shell {} a activo={}", adminUser, id, activo);

        BocaShell boca = bocasShellRepository.findById(id)
                .orElseThrow(() -> new UnknownResourceException("Boca no encontrada con id: " + id));

        boca.setActivo(Boolean.TRUE.equals(activo));
        bocasShellRepository.save(boca);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", boca.getId());
        resp.put("activo", boca.getActivo());
        resp.put("mensaje", activo ? "Boca activada correctamente." : "Boca desactivada correctamente.");
        return ResponseEntity.ok(resp);
    }

    // ── Auditores (Johnson) ─────────────────────────────────────────────

    @PostMapping(value = "/jhonson/{idBoca}/auditores", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> asignarAuditor(@PathVariable Long idBoca, @RequestBody Map<String, String> req) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        String auditor = req.get("auditor");
        LOGGER.info("Usuario '{}' asigna auditor '{}' a boca {}", adminUser, auditor, idBoca);

        List<BocaAuditorSCJ> existentes = bocaAuditorSCJRepository.findByIdBoca(idBoca);
        boolean yaExiste = existentes.stream().anyMatch(ba -> ba.getAuditor().equalsIgnoreCase(auditor));
        if (yaExiste) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("mensaje", "El auditor ya está asignado a esta boca.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(resp);
        }

        BocaAuditorSCJ ba = new BocaAuditorSCJ();
        ba.setIdBoca(idBoca);
        ba.setAuditor(auditor.trim().toLowerCase());
        bocaAuditorSCJRepository.save(ba);

        Map<String, Object> resp = new HashMap<>();
        resp.put("mensaje", "Auditor asignado correctamente.");
        resp.put("idBoca", idBoca);
        resp.put("auditor", auditor);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @DeleteMapping(value = "/jhonson/{idBoca}/auditores/{auditor}", produces = "application/json")
    public ResponseEntity<Map<String, Object>> desasignarAuditor(@PathVariable Long idBoca, @PathVariable String auditor) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' desasigna auditor '{}' de boca {}", adminUser, auditor, idBoca);

        List<BocaAuditorSCJ> asignaciones = bocaAuditorSCJRepository.findByIdBoca(idBoca);
        BocaAuditorSCJ target = asignaciones.stream()
                .filter(b -> b.getAuditor().equalsIgnoreCase(auditor))
                .findFirst().orElse(null);

        if (target != null) {
            bocaAuditorSCJRepository.delete(target);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("mensaje", "Auditor desasignado correctamente.");
        resp.put("idBoca", idBoca);
        resp.put("auditor", auditor);
        return ResponseEntity.ok(resp);
    }

}
