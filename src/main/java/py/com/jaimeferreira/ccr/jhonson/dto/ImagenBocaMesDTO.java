
package py.com.jaimeferreira.ccr.jhonson.dto;

import java.io.Serializable;

/**
 *
 * @author Jaime Ferreira
 */
public class ImagenBocaMesDTO implements Serializable {

    private static final long serialVersionUID = -6073659007857740562L;

    private String codBoca;

    private String mes;

    private int cantidad;

    private ImagenBocaMesDTO(Builder builder) {
        this.codBoca = builder.codBoca;
        this.mes = builder.mes;
        this.cantidad = builder.cantidad;
    }

    public String getCodBoca() {
        return codBoca;
    }

    public String getMes() {
        return mes;
    }

    public int getCantidad() {
        return cantidad;
    }

    // Builder
    public static class Builder {
        private String codBoca;
        private String mes;
        private int cantidad;

        public Builder codBoca(String codBoca) {
            this.codBoca = codBoca;
            return this;
        }

        public Builder mes(String mes) {
            this.mes = mes;
            return this;
        }

        public Builder cantidad(int cantidad) {
            this.cantidad = cantidad;
            return this;
        }

        public ImagenBocaMesDTO build() {
            return new ImagenBocaMesDTO(this);
        }
    }

}
