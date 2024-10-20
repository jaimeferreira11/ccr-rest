package py.com.jaimeferreira.ccr.commons.exception;

import java.io.Serializable;

/**
 * 
 * @author Luis Capdevila [luis_capde@hotmail.com]
 *
 */

public class CustomGeneralException  extends Exception implements Serializable {


    private static final long serialVersionUID = 5867280576687816765L;

    private EnumErrors errorCodigoMensaje; 
    
    private String contenidoError;

    public CustomGeneralException() {
        super();
    }

    public CustomGeneralException(EnumErrors errorCodigoMensaje, String contenidoError) {
        super();
        this.errorCodigoMensaje = errorCodigoMensaje;
        this.contenidoError = contenidoError;
    }

    public EnumErrors getOmedicsErrorCodigoMensaje() {
        return errorCodigoMensaje;
    }

    public void setOmedicsErrorCodigoMensaje(EnumErrors omedicsErrorCodigoMensaje) {
        this.errorCodigoMensaje = omedicsErrorCodigoMensaje;
    }

    public String getContenidoError() {
        return contenidoError;
    }

    public void setContenidoError(String contenidoError) {
        this.contenidoError = contenidoError;
    }

    @Override
    public String toString() {
        return "CustomGeneralException [code=" + errorCodigoMensaje + ", message="
                + contenidoError + "]";
    }
    
    
    

}
