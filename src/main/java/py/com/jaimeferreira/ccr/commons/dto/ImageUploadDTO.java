package py.com.jaimeferreira.ccr.commons.dto;

import javax.validation.constraints.NotNull;

/**
 * @author Jaime Ferreira
 *
 */

public class ImageUploadDTO {

    @NotNull
    private String pathImagen;

    @NotNull
    private String imgBase64String;

    @NotNull
    private String fechaCreacion;

    public String getPathImagen() {
        return pathImagen;
    }

    public void setPathImagen(String pathImagen) {
        this.pathImagen = pathImagen;
    }

    public String getImgBase64String() {
        return imgBase64String;
    }

    public void setImgBase64String(String imgBase64String) {
        this.imgBase64String = imgBase64String;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

}
