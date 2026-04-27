package py.com.jaimeferreira.ccr.insights.dto;

import java.io.Serializable;

import py.com.jaimeferreira.ccr.insights.entity.Categoria;

/**
 *
 * @author Jaime Ferreira
 */
public class CategoriaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String codigo;
    private String descripcion;
    private String codCliente;
    private String clienteDescripcion;
    private Boolean enabled;

    public static CategoriaDTO from(Categoria categoria) {
        CategoriaDTO dto = new CategoriaDTO();
        dto.setId(categoria.getId());
        dto.setCodigo(categoria.getCodigo());
        dto.setDescripcion(categoria.getDescripcion());
        dto.setEnabled(categoria.getEnabled());
        dto.setCodCliente(categoria.getCliente() != null ? categoria.getCliente().getCodigo() : null);
        dto.setClienteDescripcion(categoria.getCliente() != null ? categoria.getCliente().getDescripcion() : null);
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

    public String getCodCliente() {
        return codCliente;
    }

    public void setCodCliente(String codCliente) {
        this.codCliente = codCliente;
    }

    public String getClienteDescripcion() {
        return clienteDescripcion;
    }

    public void setClienteDescripcion(String clienteDescripcion) {
        this.clienteDescripcion = clienteDescripcion;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

}
