package py.com.jaimeferreira.ccr.lt.entity;

import py.com.jaimeferreira.ccr.commons.entity.BaseEntidad;
import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "persona", schema = "lt",
    uniqueConstraints = @UniqueConstraint(columnNames = {"punto", "nro_ticket", "identificacion"}))
public class LtPersona extends BaseEntidad implements Serializable {

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

    @Column(name = "identificacion", length = 50)
    private String identificacion;

    @Column(name = "nombre_y_apellido_empresa", length = 500)
    private String nombreYApellidoEmpresa;

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
    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String identificacion) { this.identificacion = identificacion; }
    public String getNombreYApellidoEmpresa() { return nombreYApellidoEmpresa; }
    public void setNombreYApellidoEmpresa(String nombreYApellidoEmpresa) { this.nombreYApellidoEmpresa = nombreYApellidoEmpresa; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
