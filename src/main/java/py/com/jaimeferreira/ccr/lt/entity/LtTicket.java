package py.com.jaimeferreira.ccr.lt.entity;

import py.com.jaimeferreira.ccr.commons.entity.BaseEntidad;
import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "ticket", schema = "lt",
    uniqueConstraints = @UniqueConstraint(columnNames = {"punto", "nro_ticket", "eancode"}))
public class LtTicket extends BaseEntidad implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "punto", nullable = false)
    private Integer punto;

    @Column(name = "nro_ticket", nullable = false, length = 50)
    private String nroTicket;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora")
    private LocalTime hora;

    @Column(name = "eancode", nullable = false)
    private Long eancode;

    @Column(name = "ean_desc", length = 500)
    private String eanDesc;

    @Column(name = "unidades_vendidas")
    private Integer unidadesVendidas;

    @Column(name = "precio_regular", precision = 15, scale = 2)
    private BigDecimal precioRegular;

    @Column(name = "precio_promocional", precision = 15, scale = 2)
    private BigDecimal precioPromocional;

    @Column(name = "tipo_venta", length = 10)
    private String tipoVenta;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "ACTIVO";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getPunto() { return punto; }
    public void setPunto(Integer punto) { this.punto = punto; }
    public String getNroTicket() { return nroTicket; }
    public void setNroTicket(String nroTicket) { this.nroTicket = nroTicket; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHora() { return hora; }
    public void setHora(LocalTime hora) { this.hora = hora; }
    public Long getEancode() { return eancode; }
    public void setEancode(Long eancode) { this.eancode = eancode; }
    public String getEanDesc() { return eanDesc; }
    public void setEanDesc(String eanDesc) { this.eanDesc = eanDesc; }
    public Integer getUnidadesVendidas() { return unidadesVendidas; }
    public void setUnidadesVendidas(Integer unidadesVendidas) { this.unidadesVendidas = unidadesVendidas; }
    public BigDecimal getPrecioRegular() { return precioRegular; }
    public void setPrecioRegular(BigDecimal precioRegular) { this.precioRegular = precioRegular; }
    public BigDecimal getPrecioPromocional() { return precioPromocional; }
    public void setPrecioPromocional(BigDecimal precioPromocional) { this.precioPromocional = precioPromocional; }
    public String getTipoVenta() { return tipoVenta; }
    public void setTipoVenta(String tipoVenta) { this.tipoVenta = tipoVenta; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
