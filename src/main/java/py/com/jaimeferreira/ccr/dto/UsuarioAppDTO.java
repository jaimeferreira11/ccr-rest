package py.com.jaimeferreira.ccr.dto;

import py.com.jaimeferreira.ccr.entity.Usuario;

/**
 * @author Jaime Ferreira
 *
 */

public class UsuarioAppDTO {

    private Usuario usuario;

    private String token;

    public UsuarioAppDTO() {
        super();
    }

    public UsuarioAppDTO(Usuario usuario, String token) {
        this.usuario = usuario;
        this.token = token;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

}
