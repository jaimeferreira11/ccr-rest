
package py.com.jaimeferreira.ccr.shell.entity;

import java.io.Serializable;
import java.util.List;

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
@Table(name = "respuesta_cab", schema = "shell")
public class RespuestaCabShell implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @NotNull
    @Column(name = "ID_BOCA", nullable = false)
    private Long idBoca;

    @NotNull
    @Size(max = 50)
    @Column(name = "COD_BOCA", nullable = false, length = 50)
    private String codBoca;

    @NotNull
    @Size(max = 200)
    @Column(name = "DESC_BOCA", nullable = false, length = 200)
    private String descBoca;

    @NotNull
    @Size(max = 200)
    @Column(name = "USUARIO", nullable = false, length = 200)
    private String usuario;

    @NotNull
    @Size(max = 500)
    @Column(name = "LONGITUD", nullable = false, length = 500)
    private String longitud;

    @NotNull
    @Size(max = 500)
    @Column(name = "LATITUD", nullable = false, length = 500)
    private String latitud;

    @Size(max = 100)
    @Column(name = "FECHA_CREACION", length = 100)
    private String fechaCreacion;

    @Size(max = 20)
    @Column(name = "HORA_INICIO", length = 20)
    private String horaInicio;

    @Size(max = 20)
    @Column(name = "HORA_FIN", length = 20)
    private String horaFin;

    @Column(name = "ACTIVO", nullable = false, columnDefinition = "boolean NOT NULL default true")
    private Boolean activo;

    @Column(name = "sanitario_clausurado", nullable = false, columnDefinition = "boolean default false")
    private Boolean sanitarioClausuado;

    @Transient
    private List<RespuestaDetShell> detalles;

    @Transient
    private List<RespuestaMultimediaShell> multimedia;

    public List<RespuestaMultimediaShell> getMultimedia() {
        return multimedia;
    }

    public void setMultimedia(List<RespuestaMultimediaShell> multimedia) {
        this.multimedia = multimedia;
    }

    @PrePersist
    protected void onCreate() {
        // this.fechaSinc = new Date(System.currentTimeMillis());
        this.activo = true;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdBoca() {
        return idBoca;
    }

    public void setIdBoca(Long idBoca) {
        this.idBoca = idBoca;
    }

    public String getCodBoca() {
        return codBoca;
    }

    public void setCodBoca(String codBoca) {
        this.codBoca = codBoca;
    }

    public String getDescBoca() {
        return descBoca;
    }

    public void setDescBoca(String descBoca) {
        this.descBoca = descBoca;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getLongitud() {
        return longitud;
    }

    public void setLongitud(String longitud) {
        this.longitud = longitud;
    }

    public String getLatitud() {
        return latitud;
    }

    public void setLatitud(String latitud) {
        this.latitud = latitud;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(String horaInicio) {
        this.horaInicio = horaInicio;
    }

    public String getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(String horaFin) {
        this.horaFin = horaFin;
    }

    public List<RespuestaDetShell> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<RespuestaDetShell> detalles) {
        this.detalles = detalles;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Boolean getSanitarioClausuado() {
        return sanitarioClausuado;
    }

    public void setSanitarioClausuado(Boolean sanitarioClausuado) {
        this.sanitarioClausuado = sanitarioClausuado;
    }

}
