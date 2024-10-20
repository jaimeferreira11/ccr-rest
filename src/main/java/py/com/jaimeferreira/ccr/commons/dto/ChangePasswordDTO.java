package py.com.jaimeferreira.ccr.commons.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * @author jaime Ferreira
 *
 */

public class ChangePasswordDTO {

    @NotNull
    private String usuario;

    @NotNull
    private String oldPassword;

    @NotNull
    private String newPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

}
