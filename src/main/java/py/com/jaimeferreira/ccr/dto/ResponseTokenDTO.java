package py.com.jaimeferreira.ccr.dto;

import py.com.jaimeferreira.ccr.entity.Usuario;

/**
 * @author Luis Fernando Capdevila Avalos
 *
 */
public class ResponseTokenDTO {

    private Usuario usuario;

    private String token;

    // private String horaExpiracion;

    // private boolean expirado;

    public ResponseTokenDTO() {
        super();
    }

    public ResponseTokenDTO(Usuario usuario, String token) {
        super();
        this.usuario = usuario;
        this.token = token;
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

}
