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
import py.com.jaimeferreira.ccr.jhonson.entity.CabeceraSCJ;
import py.com.jaimeferreira.ccr.jhonson.entity.ItemSCJ;
import py.com.jaimeferreira.ccr.jhonson.repository.CabecerasSCJRepository;
import py.com.jaimeferreira.ccr.jhonson.repository.ItemsSCJRepository;
import py.com.jaimeferreira.ccr.nestle.entity.CabeceraNest;
import py.com.jaimeferreira.ccr.nestle.entity.ItemNest;
import py.com.jaimeferreira.ccr.nestle.repository.CabecerasNestRepository;
import py.com.jaimeferreira.ccr.nestle.repository.ItemsNestRepository;
import py.com.jaimeferreira.ccr.shell.entity.CabeceraShell;
import py.com.jaimeferreira.ccr.shell.entity.ItemShell;
import py.com.jaimeferreira.ccr.shell.repository.CabecerasShellRepository;
import py.com.jaimeferreira.ccr.shell.repository.ItemsShellRepository;

/**
 * Controller para administración de items por esquema.
 *
 * @author Jaime Ferreira
 */
@RestController
@RequestMapping("/api/v1/admin/items")
public class AdminItemsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminItemsController.class);

    @Autowired
    private ItemsSCJRepository itemsSCJRepository;

    @Autowired
    private CabecerasSCJRepository cabecerasSCJRepository;

    @Autowired
    private ItemsNestRepository itemsNestRepository;

    @Autowired
    private CabecerasNestRepository cabecerasNestRepository;

    @Autowired
    private ItemsShellRepository itemsShellRepository;

    @Autowired
    private CabecerasShellRepository cabecerasShellRepository;

    // ── Items Johnson (paginado) ────────────────────────────────────────

    @GetMapping(value = "/jhonson", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getItemsJhonson(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String codCabecera,
            @RequestParam(required = false) Boolean autoservicio,
            @RequestParam(required = false) Boolean supermercado) {

        LOGGER.info("Listando items jhonson paginado: page={}, size={}, busqueda={}, activo={}, codCabecera={}, autoservicio={}, supermercado={}",
                page, size, busqueda, activo, codCabecera, autoservicio, supermercado);

        PageRequest pageable = PageRequest.of(page, size, Sort.by("orden").ascending().and(Sort.by("id").ascending()));
        Page<ItemSCJ> pagina = itemsSCJRepository.buscarPaginado(
                busqueda != null && busqueda.trim().isEmpty() ? null : busqueda,
                activo,
                codCabecera != null && codCabecera.trim().isEmpty() ? null : codCabecera,
                autoservicio,
                supermercado,
                pageable);

        List<Map<String, Object>> content = pagina.getContent().stream().map(item -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", item.getId());
            m.put("leyenda", item.getLeyenda());
            m.put("descripcion", item.getDescripcion());
            m.put("codCabecera", item.getCodCabecera());
            m.put("pregunta", item.getPregunta());
            m.put("activo", Boolean.TRUE.equals(item.getActivo()));
            m.put("autoservicio", Boolean.TRUE.equals(item.getAutoservicio()));
            m.put("supermercado", Boolean.TRUE.equals(item.getSupermercado()));
            m.put("imagen", item.getImagen());
            m.put("categoria", item.getCategoria());
            m.put("orden", item.getOrden());
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

    // ── Cabeceras Johnson (para selects) ────────────────────────────────

    @GetMapping(value = "/jhonson/cabeceras", produces = "application/json")
    public ResponseEntity<List<CabeceraSCJ>> getCabecerasJhonson() {
        LOGGER.info("Listando cabeceras jhonson");
        return ResponseEntity.ok(cabecerasSCJRepository.findByActivoTrueOrderByOrden());
    }

    // ── CRUD Items Johnson ──────────────────────────────────────────────

    @PostMapping(value = "/jhonson", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> crearItemJhonson(@RequestBody Map<String, Object> req) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' crea item jhonson: {}", adminUser, req.get("descripcion"));

        ItemSCJ item = new ItemSCJ();
        item.setDescripcion((String) req.get("descripcion"));
        item.setLeyenda((String) req.get("leyenda"));
        item.setCodCabecera((String) req.get("codCabecera"));
        item.setPregunta((String) req.get("pregunta"));
        item.setImagen((String) req.get("imagen"));
        item.setCategoria((String) req.get("categoria"));
        item.setAutoservicio(Boolean.TRUE.equals(req.get("autoservicio")));
        item.setSupermercado(Boolean.TRUE.equals(req.get("supermercado")));
        item.setOrden(req.get("orden") != null ? ((Number) req.get("orden")).intValue() : null);
        item.setActivo(true);

        item = itemsSCJRepository.save(item);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", item.getId());
        resp.put("mensaje", "Item creado correctamente.");
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PutMapping(value = "/jhonson/{id}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> actualizarItemJhonson(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' actualiza item jhonson: {}", adminUser, id);

        ItemSCJ item = itemsSCJRepository.findById(id)
                .orElseThrow(() -> new UnknownResourceException("Item no encontrado con id: " + id));

        item.setDescripcion((String) req.get("descripcion"));
        item.setLeyenda((String) req.get("leyenda"));
        item.setCodCabecera((String) req.get("codCabecera"));
        item.setPregunta((String) req.get("pregunta"));
        item.setImagen((String) req.get("imagen"));
        item.setCategoria((String) req.get("categoria"));
        item.setAutoservicio(Boolean.TRUE.equals(req.get("autoservicio")));
        item.setSupermercado(Boolean.TRUE.equals(req.get("supermercado")));
        item.setOrden(req.get("orden") != null ? ((Number) req.get("orden")).intValue() : null);

        itemsSCJRepository.save(item);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", item.getId());
        resp.put("mensaje", "Item actualizado correctamente.");
        return ResponseEntity.ok(resp);
    }

    @PatchMapping(value = "/jhonson/{id}/estado", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> toggleEstadoItemJhonson(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        Boolean activo = (Boolean) req.get("activo");
        LOGGER.info("Usuario '{}' cambia estado item jhonson {} a activo={}", adminUser, id, activo);

        ItemSCJ item = itemsSCJRepository.findById(id)
                .orElseThrow(() -> new UnknownResourceException("Item no encontrado con id: " + id));

        item.setActivo(Boolean.TRUE.equals(activo));
        itemsSCJRepository.save(item);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", item.getId());
        resp.put("activo", item.getActivo());
        resp.put("mensaje", activo ? "Item activado correctamente." : "Item desactivado correctamente.");
        return ResponseEntity.ok(resp);
    }

    // ── Items Nestlé (paginado) ─────────────────────────────────────────

    @GetMapping(value = "/nestle", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getItemsNestle(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String codCabecera,
            @RequestParam(required = false) Boolean autoservicio,
            @RequestParam(required = false) Boolean supermercado,
            @RequestParam(required = false) Boolean despensa,
            @RequestParam(required = false) Boolean estacionServicio) {

        LOGGER.info("Listando items nestle paginado: page={}, size={}, busqueda={}, activo={}, codCabecera={}", page, size, busqueda, activo, codCabecera);

        PageRequest pageable = PageRequest.of(page, size, Sort.by("orden").ascending().and(Sort.by("id").ascending()));
        Page<ItemNest> pagina = itemsNestRepository.buscarPaginado(
                busqueda != null && busqueda.trim().isEmpty() ? null : busqueda,
                activo,
                codCabecera != null && codCabecera.trim().isEmpty() ? null : codCabecera,
                autoservicio, supermercado, despensa, estacionServicio,
                pageable);

        List<Map<String, Object>> content = pagina.getContent().stream().map(item -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", item.getId());
            m.put("leyenda", item.getLeyenda());
            m.put("descripcion", item.getDescripcion());
            m.put("codCabecera", item.getCodCabecera());
            m.put("pregunta", item.getPregunta());
            m.put("activo", Boolean.TRUE.equals(item.getActivo()));
            m.put("autoservicio", Boolean.TRUE.equals(item.getAutoservicio()));
            m.put("supermercado", Boolean.TRUE.equals(item.getSupermercado()));
            m.put("despensa", Boolean.TRUE.equals(item.getDespensa()));
            m.put("estacionServicio", Boolean.TRUE.equals(item.getEstacionServicio()));
            m.put("imagen", item.getImagen());
            m.put("categoria", item.getCategoria());
            m.put("orden", item.getOrden());
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

    @GetMapping(value = "/nestle/cabeceras", produces = "application/json")
    public ResponseEntity<List<CabeceraNest>> getCabecerasNestle() {
        LOGGER.info("Listando cabeceras nestle");
        return ResponseEntity.ok(cabecerasNestRepository.findByActivoTrueOrderByOrden());
    }

    @PostMapping(value = "/nestle", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> crearItemNestle(@RequestBody Map<String, Object> req) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' crea item nestle: {}", adminUser, req.get("descripcion"));

        ItemNest item = new ItemNest();
        item.setDescripcion((String) req.get("descripcion"));
        item.setLeyenda((String) req.get("leyenda"));
        item.setCodCabecera((String) req.get("codCabecera"));
        item.setPregunta((String) req.get("pregunta"));
        item.setImagen((String) req.get("imagen"));
        item.setCategoria((String) req.get("categoria"));
        item.setAutoservicio(Boolean.TRUE.equals(req.get("autoservicio")));
        item.setSupermercado(Boolean.TRUE.equals(req.get("supermercado")));
        item.setDespensa(Boolean.TRUE.equals(req.get("despensa")));
        item.setEstacionServicio(Boolean.TRUE.equals(req.get("estacionServicio")));
        item.setOrden(req.get("orden") != null ? ((Number) req.get("orden")).intValue() : null);
        item.setActivo(true);

        item = itemsNestRepository.save(item);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", item.getId());
        resp.put("mensaje", "Item creado correctamente.");
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PutMapping(value = "/nestle/{id}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> actualizarItemNestle(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' actualiza item nestle: {}", adminUser, id);

        ItemNest item = itemsNestRepository.findById(id)
                .orElseThrow(() -> new UnknownResourceException("Item no encontrado con id: " + id));

        item.setDescripcion((String) req.get("descripcion"));
        item.setLeyenda((String) req.get("leyenda"));
        item.setCodCabecera((String) req.get("codCabecera"));
        item.setPregunta((String) req.get("pregunta"));
        item.setImagen((String) req.get("imagen"));
        item.setCategoria((String) req.get("categoria"));
        item.setAutoservicio(Boolean.TRUE.equals(req.get("autoservicio")));
        item.setSupermercado(Boolean.TRUE.equals(req.get("supermercado")));
        item.setDespensa(Boolean.TRUE.equals(req.get("despensa")));
        item.setEstacionServicio(Boolean.TRUE.equals(req.get("estacionServicio")));
        item.setOrden(req.get("orden") != null ? ((Number) req.get("orden")).intValue() : null);

        itemsNestRepository.save(item);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", item.getId());
        resp.put("mensaje", "Item actualizado correctamente.");
        return ResponseEntity.ok(resp);
    }

    @PatchMapping(value = "/nestle/{id}/estado", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> toggleEstadoItemNestle(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        Boolean activo = (Boolean) req.get("activo");
        LOGGER.info("Usuario '{}' cambia estado item nestle {} a activo={}", adminUser, id, activo);

        ItemNest item = itemsNestRepository.findById(id)
                .orElseThrow(() -> new UnknownResourceException("Item no encontrado con id: " + id));

        item.setActivo(Boolean.TRUE.equals(activo));
        itemsNestRepository.save(item);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", item.getId());
        resp.put("activo", item.getActivo());
        resp.put("mensaje", activo ? "Item activado correctamente." : "Item desactivado correctamente.");
        return ResponseEntity.ok(resp);
    }

    // ── Items Shell (paginado) ──────────────────────────────────────────

    @GetMapping(value = "/shell", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getItemsShell(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String codCabecera) {

        LOGGER.info("Listando items shell paginado: page={}, size={}, busqueda={}, activo={}, codCabecera={}", page, size, busqueda, activo, codCabecera);

        PageRequest pageable = PageRequest.of(page, size, Sort.by("nro").ascending().and(Sort.by("id").ascending()));
        Page<ItemShell> pagina = itemsShellRepository.buscarPaginado(
                busqueda != null && busqueda.trim().isEmpty() ? null : busqueda,
                activo,
                codCabecera != null && codCabecera.trim().isEmpty() ? null : codCabecera,
                pageable);

        List<Map<String, Object>> content = pagina.getContent().stream().map(item -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", item.getId());
            m.put("tema", item.getTema());
            m.put("descripcion", item.getDescripcion());
            m.put("leyenda", item.getLeyenda());
            m.put("codCabecera", item.getCodCabecera());
            m.put("tipo", item.getTipo());
            m.put("valorMostrarCondicional", item.getValorMostrarCondicional());
            m.put("preguntaCondicional", item.getPreguntaCondicional());
            m.put("activo", Boolean.TRUE.equals(item.getActivo()));
            m.put("nro", item.getNro());
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

    @GetMapping(value = "/shell/cabeceras", produces = "application/json")
    public ResponseEntity<List<CabeceraShell>> getCabecerasShell() {
        LOGGER.info("Listando cabeceras shell");
        return ResponseEntity.ok(cabecerasShellRepository.findByActivoTrueOrderByOrden());
    }

    @PostMapping(value = "/shell", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> crearItemShell(@RequestBody Map<String, Object> req) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' crea item shell: {}", adminUser, req.get("descripcion"));

        ItemShell item = new ItemShell();
        item.setDescripcion((String) req.get("descripcion"));
        item.setLeyenda((String) req.get("leyenda"));
        item.setTema((String) req.get("tema"));
        item.setCodCabecera((String) req.get("codCabecera"));
        item.setTipo((String) req.getOrDefault("tipo", "SI/NO"));
        item.setValorMostrarCondicional((String) req.get("valorMostrarCondicional"));
        item.setPreguntaCondicional((String) req.get("preguntaCondicional"));
        item.setNro(req.get("nro") != null ? ((Number) req.get("nro")).intValue() : 0);
        item.setActivo(true);

        item = itemsShellRepository.save(item);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", item.getId());
        resp.put("mensaje", "Item creado correctamente.");
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PutMapping(value = "/shell/{id}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> actualizarItemShell(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' actualiza item shell: {}", adminUser, id);

        ItemShell item = itemsShellRepository.findById(id)
                .orElseThrow(() -> new UnknownResourceException("Item no encontrado con id: " + id));

        item.setDescripcion((String) req.get("descripcion"));
        item.setLeyenda((String) req.get("leyenda"));
        item.setTema((String) req.get("tema"));
        item.setCodCabecera((String) req.get("codCabecera"));
        item.setTipo((String) req.getOrDefault("tipo", "SI/NO"));
        item.setValorMostrarCondicional((String) req.get("valorMostrarCondicional"));
        item.setPreguntaCondicional((String) req.get("preguntaCondicional"));
        item.setNro(req.get("nro") != null ? ((Number) req.get("nro")).intValue() : 0);

        itemsShellRepository.save(item);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", item.getId());
        resp.put("mensaje", "Item actualizado correctamente.");
        return ResponseEntity.ok(resp);
    }

    @PatchMapping(value = "/shell/{id}/estado", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> toggleEstadoItemShell(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        String adminUser = SecurityContextHolder.getContext().getAuthentication().getName();
        Boolean activo = (Boolean) req.get("activo");
        LOGGER.info("Usuario '{}' cambia estado item shell {} a activo={}", adminUser, id, activo);

        ItemShell item = itemsShellRepository.findById(id)
                .orElseThrow(() -> new UnknownResourceException("Item no encontrado con id: " + id));

        item.setActivo(Boolean.TRUE.equals(activo));
        itemsShellRepository.save(item);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", item.getId());
        resp.put("activo", item.getActivo());
        resp.put("mensaje", activo ? "Item activado correctamente." : "Item desactivado correctamente.");
        return ResponseEntity.ok(resp);
    }

}
