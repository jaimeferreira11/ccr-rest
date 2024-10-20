
package py.com.jaimeferreira.ccr.nestle.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author Jaime Ferreira
 */

@Entity
@Table(name = "cabeceras", schema = "nestle")
public class CabeceraNest implements Serializable {

    private static final long serialVersionUID = 4501196302523221834L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @NotNull
    @Column(name = "CODIGO", nullable = false)
    private String codigo;

    @NotNull
    @Size(max = 200)
    @Column(name = "TITULO", nullable = false, length = 200)
    private String titulo;

    @Size(max = 300)
    @Column(name = "DESCRIPCION", length = 300)
    private String descripcion;

    @NotNull
    @Column(name = "ACTIVO", nullable = false, columnDefinition = "boolean NOT NULL default true")
    private Boolean activo;

    @NotNull
    @Column(name = "autoservicio", nullable = false, columnDefinition = "boolean NOT NULL default false")
    private Boolean autoservicio;

    @NotNull
    @Column(name = "despensa", nullable = false, columnDefinition = "boolean NOT NULL default false")
    private Boolean despensa;

    @NotNull
    @Column(name = "estacion_servicio", nullable = false, columnDefinition = "boolean NOT NULL default false")
    private Boolean estacionServicio;

    @NotNull
    @Column(name = "supermercado", nullable = false, columnDefinition = "boolean NOT NULL default false")
    private Boolean supermercado;
    
    @Column(name = "ORDEN")
    private Integer orden;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
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

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }
    
    

}
