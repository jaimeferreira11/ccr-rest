package py.com.jaimeferreira.ccr.lt.entity;

import py.com.jaimeferreira.ccr.commons.entity.BaseEntidad;
import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "sucursal", schema = "lt")
public class LtSucursal extends BaseEntidad implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "punto", nullable = false, unique = true)
    private Integer punto;

    @Column(name = "direccion", length = 500)
    private String direccion;

    @Column(name = "provincia", length = 200)
    private String provincia;

    @Column(name = "ciudad", length = 200)
    private String ciudad;

    @Column(name = "mts2", precision = 10, scale = 2)
    private BigDecimal mts2;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "ACTIVO";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getPunto() { return punto; }
    public void setPunto(Integer punto) { this.punto = punto; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getProvincia() { return provincia; }
    public void setProvincia(String provincia) { this.provincia = provincia; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public BigDecimal getMts2() { return mts2; }
    public void setMts2(BigDecimal mts2) { this.mts2 = mts2; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
