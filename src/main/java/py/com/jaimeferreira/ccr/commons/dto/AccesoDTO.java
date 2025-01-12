package py.com.jaimeferreira.ccr.commons.dto;

import java.util.Set;

/**
 * @author Luis Fernando Capdevila Avalos
 *
 */

public class AccesoDTO {

    private String usuario;

    private String contrasena;

    private String codCliente;

    private Set<String> roles;

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getCodCliente() {
        return codCliente;
    }

    public void setCodCliente(String codCliente) {
        this.codCliente = codCliente;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

}
