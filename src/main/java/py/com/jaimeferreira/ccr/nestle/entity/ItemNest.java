
package py.com.jaimeferreira.ccr.nestle.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author Jaime Ferreira
 */

@Entity
@Table(name = "items", schema = "nestle")
public class ItemNest implements Serializable {

    private static final long serialVersionUID = 5839865051790913667L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Size(max = 300)
    @Column(name = "LEYENDA", length = 300)
    private String leyenda;

    @NotNull
    @Size(max = 200)
    @Column(name = "DESCRIPCION", nullable = false, length = 200)
    private String descripcion;

    @NotNull
    @Size(max = 100)
    @Column(name = "COD_CABECERA", nullable = false, length = 100)
    private String codCabecera;

    @Size(max = 500)
    @Column(name = "PREGUNTA", length = 500)
    private String pregunta;

    @NotNull
    @Column(name = "ACTIVO", nullable = false, columnDefinition = "boolean NOT NULL default true")
    private Boolean activo;

    @Column(name = "autoservicio", nullable = false)
    private Boolean autoservicio = false;

    @Column(name = "despensa", nullable = false)
    private Boolean despensa = false;

    @Column(name = "estacion_servicio", nullable = false)
    private Boolean estacionServicio = false;

    @Column(name = "supermercado", nullable = false)
    private Boolean supermercado = false;

    @NotNull
    @Size(max = 300)
    @Column(name = "IMAGEN", nullable = false, length = 300)
    private String imagen;

    @Column(name = "categoria")
    private String categoria;

    @Column(name = "ORDEN")
    private Integer orden;

    @Transient
    private String imgBase64String;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCodCabecera() {
        return codCabecera;
    }

    public void setCodCabecera(String codCabecera) {
        this.codCabecera = codCabecera;
    }

    public String getPregunta() {
        return pregunta;
    }

    public void setPregunta(String pregunta) {
        this.pregunta = pregunta;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public String getLeyenda() {
        return leyenda;
    }

    public void setLeyenda(String leyenda) {
        this.leyenda = leyenda;
    }

    public Boolean getAutoservicio() {
        return autoservicio;
    }

    public void setAutoservicio(Boolean autoservicio) {
        this.autoservicio = autoservicio;
    }

    public Boolean getDespensa() {
        return despensa;
    }

    public void setDespensa(Boolean despensa) {
        this.despensa = despensa;
    }

    public Boolean getEstacionServicio() {
        return estacionServicio;
    }

    public void setEstacionServicio(Boolean estacionServicio) {
        this.estacionServicio = estacionServicio;
    }

    public Boolean getSupermercado() {
        return supermercado;
    }

    public void setSupermercado(Boolean supermercado) {
        this.supermercado = supermercado;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public String getImgBase64String() {
        return imgBase64String;
    }

    public void setImgBase64String(String imgBase64String) {
        this.imgBase64String = imgBase64String;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

}
