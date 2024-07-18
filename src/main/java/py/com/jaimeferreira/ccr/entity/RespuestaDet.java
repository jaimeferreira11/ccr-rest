
package py.com.jaimeferreira.ccr.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author Jaime Ferreira
 */
@Entity
@Table(name = "respuesta_det", schema = "zoomin")
public class RespuestaDet implements Serializable {

    private static final long serialVersionUID = -662454929291270552L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @NotNull
    @Column(name = "ID_RESPUESTA_CAB", nullable = false)
    private Long idRespuestaCab;

    @NotNull
    @Column(name = "ID_ITEM", nullable = false)
    private Long idItem;

    @NotNull
    @Size(max = 200)
    @Column(name = "DESC_ITEM", nullable = false, length = 200)
    private String descItem;

    @NotNull
    @Size(max = 200)
    @Column(name = "CABECERA", nullable = false, length = 200)
    private String cabecera;

    @NotNull
    @Size(max = 10)
    @Column(name = "VALOR", nullable = false, length = 10)
    private String valor;

    @Size(max = 500)
    @Column(name = "COMENTARIO", length = 500)
    private String comentario;

    @Size(max = 100)
    @Column(name = "PRECIO", length = 100)
    private String precio;

    @Column(name = "ACTIVO", nullable = false, columnDefinition = "boolean NOT NULL default true")
    private Boolean activo;

    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "ID_RESOUESTA_CAB", insertable = false, updatable = false)
    // private Long respuestaCab;

    @PrePersist
    protected void onCreate() {
        this.activo = true;
    }

    // Getters and Setters

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

    public Long getIdItem() {
        return idItem;
    }

    public void setIdItem(Long idItem) {
        this.idItem = idItem;
    }

    public String getDescItem() {
        return descItem;
    }

    public void setDescItem(String descItem) {
        this.descItem = descItem;
    }

    public String getCabecera() {
        return cabecera;
    }

    public void setCabecera(String cabecera) {
        this.cabecera = cabecera;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public String getPrecio() {
        return precio;
    }

    public void setPrecio(String precio) {
        this.precio = precio;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

}
