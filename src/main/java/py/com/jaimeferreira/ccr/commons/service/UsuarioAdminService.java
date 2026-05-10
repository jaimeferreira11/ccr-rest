package py.com.jaimeferreira.ccr.commons.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import py.com.jaimeferreira.ccr.commons.dto.UsuarioAdminDTO;
import py.com.jaimeferreira.ccr.commons.entity.UserRole;
import py.com.jaimeferreira.ccr.commons.entity.Usuario;
import py.com.jaimeferreira.ccr.commons.exception.UnknownResourceException;
import py.com.jaimeferreira.ccr.commons.repository.UserRolesRepository;
import py.com.jaimeferreira.ccr.commons.repository.UsuarioRepository;

/**
 * Servicio de administración de usuarios.
 *
 * @author Jaime Ferreira
 */
@Service
public class UsuarioAdminService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UserRolesRepository userRolesRepository;

    public List<UsuarioAdminDTO> findAll() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        return usuarios.stream()
                .map(u -> UsuarioAdminDTO.from(u, userRolesRepository.findByUsuarioIgnoreCase(u.getUsuario())))
                .collect(Collectors.toList());
    }

    public UsuarioAdminDTO findById(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UnknownResourceException("Usuario no encontrado con id: " + id));
        List<UserRole> roles = userRolesRepository.findByUsuarioIgnoreCase(usuario.getUsuario());
        return UsuarioAdminDTO.from(usuario, roles);
    }

    @Transactional
    public UsuarioAdminDTO save(UsuarioAdminDTO dto) {
        Usuario existente = usuarioRepository.findByUsuarioIgnoreCase(dto.getUsuario());
        if (existente != null) {
            throw new IllegalArgumentException("Ya existe un usuario con el nombre: " + dto.getUsuario());
        }

        Usuario usuario = new Usuario();
        usuario.setUsuario(dto.getUsuario().trim().toLowerCase());
        usuario.setPassword(dto.getPassword());
        usuario.setNombreApellido(dto.getNombreApellido().trim());
        usuario.setCodCliente(dto.getCodCliente());
        usuario.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
        usuario.setExterno(dto.getExterno() != null ? dto.getExterno() : false);

        usuario = usuarioRepository.save(usuario);

        sincronizarRoles(usuario.getUsuario(), dto.getRoles());

        List<UserRole> roles = userRolesRepository.findByUsuarioIgnoreCase(usuario.getUsuario());
        return UsuarioAdminDTO.from(usuario, roles);
    }

    @Transactional
    public UsuarioAdminDTO update(Long id, UsuarioAdminDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UnknownResourceException("Usuario no encontrado con id: " + id));

        usuario.setNombreApellido(dto.getNombreApellido().trim());
        usuario.setCodCliente(dto.getCodCliente());

        if (dto.getActivo() != null) {
            usuario.setActivo(dto.getActivo());
        }
        if (dto.getExterno() != null) {
            usuario.setExterno(dto.getExterno());
        }

        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            usuario.setPassword(dto.getPassword());
        }

        usuario = usuarioRepository.save(usuario);

        if (dto.getRoles() != null) {
            sincronizarRoles(usuario.getUsuario(), dto.getRoles());
        }

        List<UserRole> roles = userRolesRepository.findByUsuarioIgnoreCase(usuario.getUsuario());
        return UsuarioAdminDTO.from(usuario, roles);
    }

    @Transactional
    public UsuarioAdminDTO disable(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UnknownResourceException("Usuario no encontrado con id: " + id));

        usuario.setActivo(false);
        usuario = usuarioRepository.save(usuario);

        List<UserRole> roles = userRolesRepository.findByUsuarioIgnoreCase(usuario.getUsuario());
        return UsuarioAdminDTO.from(usuario, roles);
    }

    private void sincronizarRoles(String nombreUsuario, List<String> nuevosRoles) {
        List<UserRole> rolesActuales = userRolesRepository.findByUsuarioIgnoreCase(nombreUsuario);
        userRolesRepository.deleteAll(rolesActuales);

        if (nuevosRoles != null) {
            for (String rol : nuevosRoles) {
                UserRole userRole = new UserRole();
                userRole.setUsuario(nombreUsuario);
                userRole.setRol(rol.trim().toUpperCase());
                userRolesRepository.save(userRole);
            }
        }
    }

}
