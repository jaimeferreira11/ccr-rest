package py.com.jaimeferreira.ccr.dto;

/**
 * @author Luis Fernando Capdevila Avalos
 *
 */

public class ChangePasswordDTO {
    
    private String usuario;

    private String oldPaswword;

    private String newPassword;

    public String getOldPaswword() {
        return oldPaswword;
    }

    public void setOldPaswword(String oldPaswword) {
        this.oldPaswword = oldPaswword;
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
