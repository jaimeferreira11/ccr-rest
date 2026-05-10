package py.com.jaimeferreira.ccr.commons.dto;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import py.com.jaimeferreira.ccr.commons.entity.UserRole;
import py.com.jaimeferreira.ccr.commons.entity.Usuario;

/**
 * DTO para administración de usuarios.
 *
 * @author Jaime Ferreira
 */
public class UsuarioAdminDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String usuario;
    private String password;
    private String nombreApellido;
    private Boolean activo;
    private String codCliente;
    private Boolean externo;
    private List<String> roles;

    public static UsuarioAdminDTO from(Usuario entity, List<UserRole> userRoles) {
        UsuarioAdminDTO dto = new UsuarioAdminDTO();
        dto.setId(entity.getId());
        dto.setUsuario(entity.getUsuario());
        dto.setNombreApellido(entity.getNombreApellido());
        dto.setActivo(entity.getActivo());
        dto.setCodCliente(entity.getCodCliente());
        dto.setExterno(entity.getExterno());
        if (userRoles != null) {
            dto.setRoles(userRoles.stream()
                    .map(UserRole::getRol)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNombreApellido() {
        return nombreApellido;
    }

    public void setNombreApellido(String nombreApellido) {
        this.nombreApellido = nombreApellido;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public String getCodCliente() {
        return codCliente;
    }

    public void setCodCliente(String codCliente) {
        this.codCliente = codCliente;
    }

    public Boolean getExterno() {
        return externo;
    }

    public void setExterno(Boolean externo) {
        this.externo = externo;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

}
