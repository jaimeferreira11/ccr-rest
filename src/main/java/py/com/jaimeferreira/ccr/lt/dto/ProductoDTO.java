package py.com.jaimeferreira.ccr.lt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class ProductoDTO {
    private Long eancode;
    private String descripcion;

    @JsonProperty("id_Sector")
    private Integer idSector;
    private String sector;

    @JsonProperty("id_Seccion")
    private Integer idSeccion;
    private String seccion;

    @JsonProperty("id_Categoria")
    private Integer idCategoria;
    private String categoria;

    @JsonProperty("id_Subcategoria")
    private Integer idSubcategoria;
    private String subcategoria;

    private String fabricante;
    private String marca;
    private BigDecimal contenido;
    private BigDecimal pesovolumen;
    private String unidadMedida;

    public Long getEancode() { return eancode; }
    public void setEancode(Long eancode) { this.eancode = eancode; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Integer getIdSector() { return idSector; }
    public void setIdSector(Integer idSector) { this.idSector = idSector; }
    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }
    public Integer getIdSeccion() { return idSeccion; }
    public void setIdSeccion(Integer idSeccion) { this.idSeccion = idSeccion; }
    public String getSeccion() { return seccion; }
    public void setSeccion(String seccion) { this.seccion = seccion; }
    public Integer getIdCategoria() { return idCategoria; }
    public void setIdCategoria(Integer idCategoria) { this.idCategoria = idCategoria; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public Integer getIdSubcategoria() { return idSubcategoria; }
    public void setIdSubcategoria(Integer idSubcategoria) { this.idSubcategoria = idSubcategoria; }
    public String getSubcategoria() { return subcategoria; }
    public void setSubcategoria(String subcategoria) { this.subcategoria = subcategoria; }
    public String getFabricante() { return fabricante; }
    public void setFabricante(String fabricante) { this.fabricante = fabricante; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public BigDecimal getContenido() { return contenido; }
    public void setContenido(BigDecimal contenido) { this.contenido = contenido; }
    public BigDecimal getPesovolumen() { return pesovolumen; }
    public void setPesovolumen(BigDecimal pesovolumen) { this.pesovolumen = pesovolumen; }
    public String getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(String unidadMedida) { this.unidadMedida = unidadMedida; }
}
