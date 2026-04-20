package py.com.jaimeferreira.ccr.commons.dto;

import java.util.Collections;
import java.util.Set;

import py.com.jaimeferreira.ccr.commons.entity.Usuario;

/**
 * @author Luis Fernando Capdevila Avalos
 *
 */
public class ResponseTokenDTO {

    private Usuario usuario;

    private String token;

    private Set<String> roles;

    // private String horaExpiracion;

    // private boolean expirado;

    public ResponseTokenDTO() {
        super();
    }

    public ResponseTokenDTO(Usuario usuario, String token) {
        super();
        this.usuario = usuario;
        this.token = token;
        this.roles = Collections.emptySet();
    }

    public ResponseTokenDTO(Usuario usuario, String token, Set<String> roles) {
        super();
        this.usuario = usuario;
        this.token = token;
        this.roles = roles != null ? roles : Collections.emptySet();
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

}
