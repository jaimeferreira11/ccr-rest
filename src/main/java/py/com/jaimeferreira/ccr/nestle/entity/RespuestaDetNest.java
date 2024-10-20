
package py.com.jaimeferreira.ccr.nestle.entity;

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
@Table(name = "respuesta_det", schema = "nestle")
public class RespuestaDetNest implements Serializable {

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
    @Column(name = "cod_cabecera", nullable = false, length = 100)
    private String codCabecera;

    @Column(name = "valor_1", length = 200)
    private String valor1;

    @Column(name = "valor_2", length = 200)
    private String valor2;

    @Column(name = "valor_3", length = 200)
    private String valor3;

    @Size(max = 500)
    @Column(name = "COMENTARIO", length = 500)
    private String comentario;

    @Column(name = "sin_datos", nullable = false)
    private Boolean sinDatos;

    @Column(name = "ACTIVO", nullable = false, columnDefinition = "boolean NOT NULL default true")
    private Boolean activo;

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

    public String getCodCabecera() {
        return codCabecera;
    }

    public void setCodCabecera(String codCabecera) {
        this.codCabecera = codCabecera;
    }

    public String getValor1() {
        return valor1;
    }

    public void setValor1(String valor1) {
        this.valor1 = valor1;
    }

    public String getValor2() {
        return valor2;
    }

    public void setValor2(String valor2) {
        this.valor2 = valor2;
    }

    public String getValor3() {
        return valor3;
    }

    public void setValor3(String valor3) {
        this.valor3 = valor3;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public Boolean getSinDatos() {
        return sinDatos;
    }

    public void setSinDatos(Boolean sinDatos) {
        this.sinDatos = sinDatos;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

}
