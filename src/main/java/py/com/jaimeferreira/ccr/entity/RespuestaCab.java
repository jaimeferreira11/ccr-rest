
package py.com.jaimeferreira.ccr.entity;

import java.io.Serializable;
import java.sql.Date;
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
@Table(name = "respuesta_cab", schema = "zoomin")
public class RespuestaCab implements Serializable {

    private static final long serialVersionUID = -391070309004835262L;

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

    @NotNull
    @Size(max = 300)
    @Column(name = "PATH_IMAGEN", nullable = false, length = 300)
    private String pathImagen;

//    @NotNull
    // @Temporal(TemporalType.TIMESTAMP)
//    @Column(name = "FECHA_SINC", nullable = false, columnDefinition = "DATE DEFAULT SYSDATE")
//    private Date fechaSinc;

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

    @Transient
    private List<RespuestaDet> detalles;
    
    @Transient
    private String imgBase64String;
    
    @PrePersist
    protected void onCreate() {
//        this.fechaSinc = new Date(System.currentTimeMillis());
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

    public String getPathImagen() {
        return pathImagen;
    }

    public void setPathImagen(String pathImagen) {
        this.pathImagen = pathImagen;
    }

//    public Date getFechaSinc() {
//        return fechaSinc;
//    }
//
//    public void setFechaSinc(Date fechaSinc) {
//        this.fechaSinc = fechaSinc;
//    }

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

    public List<RespuestaDet> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<RespuestaDet> detalles) {
        this.detalles = detalles;
    }

    public String getImgBase64String() {
        return imgBase64String;
    }

    public void setImgBase64String(String imgBase64String) {
        this.imgBase64String = imgBase64String;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
    
    

}
