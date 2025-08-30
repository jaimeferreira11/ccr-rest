
package py.com.jaimeferreira.ccr.nestle.dto;

import java.io.Serializable;

/**
 *
 * @author Jaime Ferreira
 */
public class PreciosDTO implements Serializable{

    private static final long serialVersionUID = 8414726345491165487L;

    private String canal;

    private String precio;

    public String getCanal() {
        return canal;
    }

    public void setCanal(String canal) {
        this.canal = canal;
    }

    public String getPrecio() {
        return precio;
    }

    public void setPrecio(String precio) {
        this.precio = precio;
    }

}
