
package py.com.jaimeferreira.ccr.nestle.service.filter;

import java.io.Serializable;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 *
 * @author Jaime Ferreira
 */
public class ImagenesFilter implements Serializable {

    private static final long serialVersionUID = 3622619212391173172L;

    @NotNull
    private int mes;
    
    @NotNull
    @NotEmpty
    private String codDistribuidor;
    
    @NotNull
    @NotEmpty
    private String codBoca;

    public int getMes() {
        return mes;
    }

    public void setMes(int mes) {
        this.mes = mes;
    }

    public String getCodDistribuidor() {
        return codDistribuidor;
    }

    public void setCodDistribuidor(String codDistribuidor) {
        this.codDistribuidor = codDistribuidor;
    }

    public String getCodBoca() {
        return codBoca;
    }

    public void setCodBoca(String codBoca) {
        this.codBoca = codBoca;
    }

}
