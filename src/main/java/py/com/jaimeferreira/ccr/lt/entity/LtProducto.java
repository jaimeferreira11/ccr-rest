package py.com.jaimeferreira.ccr.lt.entity;

import py.com.jaimeferreira.ccr.commons.entity.BaseEntidad;
import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "producto", schema = "lt")
public class LtProducto extends BaseEntidad implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "eancode", nullable = false, unique = true)
    private Long eancode;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "id_sector")
    private Integer idSector;

    @Column(name = "sector", length = 200)
    private String sector;

    @Column(name = "id_seccion")
    private Integer idSeccion;

    @Column(name = "seccion", length = 200)
    private String seccion;

    @Column(name = "id_categoria")
    private Integer idCategoria;

    @Column(name = "categoria", length = 200)
    private String categoria;

    @Column(name = "id_subcategoria")
    private Integer idSubcategoria;

    @Column(name = "subcategoria", length = 200)
    private String subcategoria;

    @Column(name = "fabricante", length = 200)
    private String fabricante;

    @Column(name = "marca", length = 200)
    private String marca;

    @Column(name = "contenido", precision = 10, scale = 3)
    private BigDecimal contenido;

    @Column(name = "pesovolumen", precision = 10, scale = 3)
    private BigDecimal pesovolumen;

    @Column(name = "unidad_medida", length = 50)
    private String unidadMedida;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "ACTIVO";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
