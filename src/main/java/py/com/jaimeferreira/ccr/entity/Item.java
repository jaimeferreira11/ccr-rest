
package py.com.jaimeferreira.ccr.entity;

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
@Table(name = "items", schema = "zoomin")
public class Item implements Serializable {

    private static final long serialVersionUID = 5839865051790913667L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @NotNull
    @Column(name = "CODIGO")
    private Long codigo;

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
    @Column(name = "ACTIVO", nullable = false, columnDefinition = "NUMBER(1) DEFAULT 1 CHECK (ACTIVO IN (0,1))")
    private Boolean activo;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
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

}
