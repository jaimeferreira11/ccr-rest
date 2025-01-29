
package py.com.jaimeferreira.ccr.nestle.dto;

import java.io.Serializable;

/**
 *
 * @author Jaime Ferreira
 */
public class ReporteBocaDTO implements Serializable {

    private static final long serialVersionUID = -4655601115347611997L;
    private String codBoca;
    private String nombre;

    public String getCodBoca() {
        return codBoca;
    }

    public void setCodBoca(String codBoca) {
        this.codBoca = codBoca;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

}
