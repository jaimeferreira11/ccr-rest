package py.com.jaimeferreira.ccr.insights.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 *
 * @author Jaime Ferreira
 */
@Entity
@Table(name = "cliente", schema = "ccr")
public class ClienteIns implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "codigo", nullable = false, unique = true, length = 50)
    private String codigo;

    @NotNull
    @Column(name = "descripcion", nullable = false, length = 200)
    private String descripcion;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cod_pais", referencedColumnName = "codigo", nullable = false)
    private Pais pais;

    @NotNull
    @Column(name = "enabled", nullable = false, columnDefinition = "boolean default true")
    private Boolean enabled;

    @Column(name = "fecha_creacion", nullable = false, columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime fechaCreacion;

    @Column(name = "nombre_usuario_creacion", length = 200)
    private String nombreUsuarioCreacion;

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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Pais getPais() {
        return pais;
    }

    public void setPais(Pais pais) {
        this.pais = pais;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getNombreUsuarioCreacion() {
        return nombreUsuarioCreacion;
    }

    public void setNombreUsuarioCreacion(String nombreUsuarioCreacion) {
        this.nombreUsuarioCreacion = nombreUsuarioCreacion;
    }

}