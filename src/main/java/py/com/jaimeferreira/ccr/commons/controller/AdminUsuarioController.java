package py.com.jaimeferreira.ccr.commons.controller;

import java.util.Collections;
import java.util.List;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import py.com.jaimeferreira.ccr.commons.dto.UsuarioAdminDTO;
import py.com.jaimeferreira.ccr.commons.entity.Cliente;
import py.com.jaimeferreira.ccr.commons.entity.Usuario;
import py.com.jaimeferreira.ccr.commons.repository.ClienteRepository;
import py.com.jaimeferreira.ccr.commons.repository.UsuarioRepository;
import py.com.jaimeferreira.ccr.commons.service.UsuarioAdminService;

/**
 * Endpoints de administración compartidos entre todas las apps.
 * Requiere JWT válido.
 *
 * @author Jaime Ferreira
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminUsuarioController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminUsuarioController.class);

    @Autowired
    private UsuarioAdminService usuarioAdminService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // ── Clientes ─────────────────────────────────────────────────────────

    @GetMapping(value = "/clientes", produces = "application/json")
    public ResponseEntity<List<Cliente>> getClientes() {
        LOGGER.info("Listando clientes (public.clientes) para administracion");
        return ResponseEntity.ok(clienteRepository.findAll());
    }

    @GetMapping(value = "/usuarios/por-cliente/{codCliente}", produces = "application/json")
    public ResponseEntity<List<UsuarioAdminDTO>> getUsuariosPorCliente(@PathVariable String codCliente) {
        LOGGER.info("Listando usuarios por cliente o globales: codCliente={}", codCliente);
        List<Usuario> usuarios = usuarioRepository.findActivosByCodClienteOrGlobal(codCliente);
        List<UsuarioAdminDTO> dtos = usuarios.stream()
                .map(u -> UsuarioAdminDTO.from(u, Collections.emptyList()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // ── Usuarios ─────────────────────────────────────────────────────────

    @GetMapping(value = "/usuarios", produces = "application/json")
    public ResponseEntity<List<UsuarioAdminDTO>> getUsuarios() {
        LOGGER.info("Listando usuarios para administracion");
        return ResponseEntity.ok(usuarioAdminService.findAll());
    }

    @GetMapping(value = "/usuarios/{id}", produces = "application/json")
    public ResponseEntity<UsuarioAdminDTO> getUsuario(@PathVariable Long id) {
        LOGGER.info("Consultando usuario: {}", id);
        return ResponseEntity.ok(usuarioAdminService.findById(id));
    }

    @PostMapping(value = "/usuarios", consumes = "application/json", produces = "application/json")
    public ResponseEntity<UsuarioAdminDTO> crearUsuario(@RequestBody UsuarioAdminDTO req) {
        String usuario = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' crea usuario: {}", usuario, req.getUsuario());
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioAdminService.save(req));
    }

    @PutMapping(value = "/usuarios/{id}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<UsuarioAdminDTO> actualizarUsuario(@PathVariable Long id, @RequestBody UsuarioAdminDTO req) {
        String usuario = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' actualiza usuario: {}", usuario, id);
        return ResponseEntity.ok(usuarioAdminService.update(id, req));
    }

    @DeleteMapping(value = "/usuarios/{id}", produces = "application/json")
    public ResponseEntity<UsuarioAdminDTO> deshabilitarUsuario(@PathVariable Long id) {
        String usuario = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' deshabilita usuario: {}", usuario, id);
        return ResponseEntity.ok(usuarioAdminService.disable(id));
    }

}
