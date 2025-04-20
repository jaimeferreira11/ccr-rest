
package py.com.jaimeferreira.ccr.shell.entity;

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
@Table(name = "items", schema = "shell")
public class ItemShell implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Size(max = 200)
    @Column(name = "tema", length = 200)
    private String tema;

    @NotNull
    @Size(max = 200)
    @Column(name = "descripcion", nullable = false, length = 200)
    private String descripcion;

    @Size(max = 300)
    @Column(name = "leyenda", length = 300)
    private String leyenda;

    @NotNull
    @Size(max = 100)
    @Column(name = "cod_cabecera", nullable = false, length = 100)
    private String codCabecera;

    @NotNull
    @Size(max = 100)
    @Column(name = "tipo", nullable = false, length = 100, columnDefinition = "character varying(100) default 'SI/NO'")
    private String tipo = "SI/NO";

    @Size(max = 50)
    @Column(name = "valor_mostrar_condicional", length = 50)
    private String valorMostrarCondicional;

    @Size(max = 500)
    @Column(name = "pregunta_condicional", length = 500)
    private String preguntaCondicional;

    @NotNull
    @Column(name = "activo", nullable = false, columnDefinition = "boolean default true")
    private Boolean activo = true;

    @NotNull
    @Column(name = "nro", nullable = false)
    private Integer nro;

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getLeyenda() {
        return leyenda;
    }

    public void setLeyenda(String leyenda) {
        this.leyenda = leyenda;
    }

    public String getCodCabecera() {
        return codCabecera;
    }

    public void setCodCabecera(String codCabecera) {
        this.codCabecera = codCabecera;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getValorMostrarCondicional() {
        return valorMostrarCondicional;
    }

    public void setValorMostrarCondicional(String valorMostrarCondicional) {
        this.valorMostrarCondicional = valorMostrarCondicional;
    }

    public String getPreguntaCondicional() {
        return preguntaCondicional;
    }

    public void setPreguntaCondicional(String preguntaCondicional) {
        this.preguntaCondicional = preguntaCondicional;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Integer getNro() {
        return nro;
    }

    public void setNro(Integer nro) {
        this.nro = nro;
    }
}
