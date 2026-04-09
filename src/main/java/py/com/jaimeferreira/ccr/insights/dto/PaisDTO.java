package py.com.jaimeferreira.ccr.insights.dto;

import java.io.Serializable;

/**
 *
 * @author Jaime Ferreira
 */
public class PaisDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String codigo;
    private String descripcion;
    private Boolean activo;

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

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

}
