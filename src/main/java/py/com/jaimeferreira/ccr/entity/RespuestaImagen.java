
package py.com.jaimeferreira.ccr.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

/**
 *
 * @author Jaime Ferreira
 */
@Entity
@Table(name = "respuesta_imagen", schema = "zoomin")
public class RespuestaImagen implements Serializable {

    private static final long serialVersionUID = 3498915881238784777L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "id_respuesta_cab", nullable = false)
    private Long idRespuestaCab;

    @NotNull
    @Column(name = "path_imagen", nullable = false, length = 300)
    private String pathImagen;

    @NotNull
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Transient
    private String imgBase64String;

    @PrePersist
    protected void onCreate() {
        this.activo = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdRespuestaCab() {
        return idRespuestaCab;
    }

    public void setIdRespuestaCab(Long idRespuestaCab) {
        this.idRespuestaCab = idRespuestaCab;
    }

    public String getPathImagen() {
        return pathImagen;
    }

    public void setPathImagen(String pathImagen) {
        this.pathImagen = pathImagen;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public String getImgBase64String() {
        return imgBase64String;
    }

    public void setImgBase64String(String imgBase64String) {
        this.imgBase64String = imgBase64String;
    }

    // Getters and Setters

}
