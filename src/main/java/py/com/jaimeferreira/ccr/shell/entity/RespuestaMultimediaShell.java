
package py.com.jaimeferreira.ccr.shell.entity;

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
import javax.validation.constraints.Size;

/**
 *
 * @author Jaime Ferreira
 */
@Entity
@Table(name = "respuesta_multimedia", schema = "shell")
public class RespuestaMultimediaShell implements Serializable {

    private static final long serialVersionUID = -1157409331574920351L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "id_respuesta_cab", nullable = false)
    private Long idRespuestaCab;

    @NotNull
    @Column(name = "path", nullable = false, length = 300)
    private String path;

    @Column(name = "tipo", nullable = false, length = 100)
    private String tipo;

    @NotNull
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Transient
    private String pathBase64String;

    @Size(max = 100)
    @Column(name = "FECHA_CREACION", length = 100)
    private String fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.activo = true;
    }

    public RespuestaMultimediaShell(Long idRespuestaCab, String path, String tipo,
                                    Boolean activo, String fechaCreacion) {
        super();
        this.idRespuestaCab = idRespuestaCab;
        this.path = path;
        this.tipo = tipo;
        this.activo = activo;
        this.fechaCreacion = fechaCreacion;
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public String getPathBase64String() {
        return pathBase64String;
    }

    public void setPathBase64String(String pathBase64String) {
        this.pathBase64String = pathBase64String;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

}
