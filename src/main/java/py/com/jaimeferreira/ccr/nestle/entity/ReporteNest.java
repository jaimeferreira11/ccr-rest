package py.com.jaimeferreira.ccr.nestle.entity;

import java.sql.Timestamp;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;

import py.com.jaimeferreira.ccr.jhonson.dto.ReporteDetalleDTO;

/**
 *
 * @author Jaime Ferreira
 */

@Entity
@Table(name = "reportes", schema = "nestle")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class ReporteNest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cod_distribuidor", length = 50, nullable = false)
    private String codDistribuidor;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "mes", length = 2, nullable = false)
    private String mes;

    @Column(name = "anio")
    private Integer anio;

    @Column(name = "usuario", length = 50, nullable = false)
    private String usuario;

    @Column(name = "pdf", nullable = false, columnDefinition = "boolean default false")
    private boolean pdf = false;

    @Column(name = "ppt", nullable = false, columnDefinition = "boolean default false")
    private boolean ppt = false;

    // @Column(name = "bocas", columnDefinition = "jsonb", nullable = false)
    // private String bocas;

    @Type(type = "jsonb")
    @Column(name = "bocas", columnDefinition = "jsonb", nullable = false)
    private List<String> bocas;

    // @Column(name = "detalles", columnDefinition = "jsonb", nullable = false)
    // private String detalles;

    @Type(type = "jsonb")
    @Column(name = "detalles", columnDefinition = "jsonb", nullable = false)
    private List<ReporteDetalleDTO> detalles;

    @Column(name = "fecha_creacion", nullable = false, columnDefinition = "timestamp default now()")
    private Timestamp fechaCreacion;

    @Column(name = "fecha_borrado")
    private Timestamp fechaBorrado;

    // @Column(name = "path_pdf")
    // private String pathPdf;
    //
    // @Column(name = "path_ppt")
    // private String pathPpt;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = new Timestamp(System.currentTimeMillis());
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodDistribuidor() {
        return codDistribuidor;
    }

    public void setCodDistribuidor(String codDistribuidor) {
        this.codDistribuidor = codDistribuidor;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getMes() {
        return mes;
    }

    public void setMes(String mes) {
        this.mes = mes;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public boolean isPdf() {
        return pdf;
    }

    public void setPdf(boolean pdf) {
        this.pdf = pdf;
    }

    public boolean isPpt() {
        return ppt;
    }

    public void setPpt(boolean ppt) {
        this.ppt = ppt;
    }

    public List<String> getBocas() {
        return bocas;
    }

    public void setBocas(List<String> bocas) {
        this.bocas = bocas;
    }

    public List<ReporteDetalleDTO> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<ReporteDetalleDTO> detalles) {
        this.detalles = detalles;
    }

    public Timestamp getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Timestamp fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Timestamp getFechaBorrado() {
        return fechaBorrado;
    }

    public void setFechaBorrado(Timestamp fechaBorrado) {
        this.fechaBorrado = fechaBorrado;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    //
    // public String getPathPdf() {
    // return pathPdf;
    // }
    //
    // public void setPathPdf(String pathPdf) {
    // this.pathPdf = pathPdf;
    // }
    //
    // public String getPathPpt() {
    // return pathPpt;
    // }
    //
    // public void setPathPpt(String pathPpt) {
    // this.pathPpt = pathPpt;
    // }

}
