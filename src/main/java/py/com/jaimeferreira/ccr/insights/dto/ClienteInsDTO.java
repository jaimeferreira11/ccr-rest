package py.com.jaimeferreira.ccr.insights.dto;

import java.io.Serializable;

import py.com.jaimeferreira.ccr.insights.entity.ClienteIns;

/**
 *
 * @author Jaime Ferreira
 */
public class ClienteInsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String codigo;
    private String descripcion;
    private String codPais;
    private Boolean enabled;

    public static ClienteInsDTO from(ClienteIns cliente) {
        ClienteInsDTO dto = new ClienteInsDTO();
        dto.setId(cliente.getId());
        dto.setCodigo(cliente.getCodigo());
        dto.setDescripcion(cliente.getDescripcion());
        dto.setEnabled(cliente.getEnabled());
        dto.setCodPais(cliente.getPais() != null ? cliente.getPais().getCodigo() : null);
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

    public String getCodPais() {
        return codPais;
    }

    public void setCodPais(String codPais) {
        this.codPais = codPais;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

}
