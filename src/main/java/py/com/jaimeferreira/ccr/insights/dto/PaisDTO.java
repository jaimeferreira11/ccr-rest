package py.com.jaimeferreira.ccr.insights.dto;

import java.io.Serializable;

import py.com.jaimeferreira.ccr.insights.entity.Pais;

/**
 *
 * @author Jaime Ferreira
 */
public class PaisDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String codigo;
    private String descripcion;
    private Boolean activo;

    public static PaisDTO from(Pais pais) {
        PaisDTO dto = new PaisDTO();
        dto.setId(pais.getId());
        dto.setCodigo(pais.getCodigo());
        dto.setDescripcion(pais.getDescripcion());
        dto.setActivo(pais.getActivo());
        return dto;
    }

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

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

}
