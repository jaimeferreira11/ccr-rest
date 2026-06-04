package py.com.jaimeferreira.ccr.insights.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Registro de auditoría de una acción administrativa del módulo Insights.
 *
 * @author Jaime Ferreira
 */
@Entity
@Table(name = "auditoria_insights", schema = "ccr")
public class AuditoriaIns implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "evento", nullable = false, length = 50)
    private EventoAuditoriaIns evento;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false, length = 20)
    private ResultadoAuditoria resultado;

    @NotNull
    @Column(name = "usuario", nullable = false, length = 200)
    private String usuario;

    @NotNull
    @Column(name = "cod_cliente", nullable = false, length = 50)
    private String codCliente;

    @Column(name = "cod_categoria", length = 50)
    private String codCategoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_reporte", length = 20)
    private TipoReporte tipoReporte;

    @Column(name = "fecha_hora", nullable = false, columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime fechaHora;

    @Column(name = "detalle", columnDefinition = "TEXT")
    private String detalle;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EventoAuditoriaIns getEvento() {
        return evento;
    }

    public void setEvento(EventoAuditoriaIns evento) {
        this.evento = evento;
    }

    public ResultadoAuditoria getResultado() {
        return resultado;
    }

    public void setResultado(ResultadoAuditoria resultado) {
        this.resultado = resultado;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getCodCliente() {
        return codCliente;
    }

    public void setCodCliente(String codCliente) {
        this.codCliente = codCliente;
    }

    public String getCodCategoria() {
        return codCategoria;
    }

    public void setCodCategoria(String codCategoria) {
        this.codCategoria = codCategoria;
    }

    public TipoReporte getTipoReporte() {
        return tipoReporte;
    }

    public void setTipoReporte(TipoReporte tipoReporte) {
        this.tipoReporte = tipoReporte;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }
}
