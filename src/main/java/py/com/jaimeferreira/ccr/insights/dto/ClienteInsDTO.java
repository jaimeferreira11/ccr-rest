package py.com.jaimeferreira.ccr.insights.dto;

import java.io.Serializable;

/**
 *
 * @author Jaime Ferreira
 */
public class ClienteInsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String codigo;
    private String descripcion;
    private String codPais;
    private Boolean enabled;

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCodPais() {
        return codPais;
    }

    public void setCodPais(String codPais) {
        this.codPais = codPais;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

}
