package py.com.jaimeferreira.ccr.insights.dto;

import py.com.jaimeferreira.ccr.insights.entity.AuditoriaIns;

import java.time.LocalDateTime;

/**
 * Vista de un registro de auditoría para el listado de administración.
 *
 * @author Jaime Ferreira
 */
public class AuditoriaDTO {

    private Long id;
    private String evento;
    private String eventoDescripcion;
    private String resultado;
    private String usuario;
    private String codCliente;
    private String codCategoria;
    private String tipoReporte;
    private LocalDateTime fechaHora;
    private String detalle;

    public static AuditoriaDTO from(AuditoriaIns a) {
        AuditoriaDTO dto = new AuditoriaDTO();
        dto.id = a.getId();
        dto.evento = a.getEvento() != null ? a.getEvento().name() : null;
        dto.eventoDescripcion = a.getEvento() != null ? a.getEvento().getDescripcion() : null;
        dto.resultado = a.getResultado() != null ? a.getResultado().name() : null;
        dto.usuario = a.getUsuario();
        dto.codCliente = a.getCodCliente();
        dto.codCategoria = a.getCodCategoria();
        dto.tipoReporte = a.getTipoReporte() != null ? a.getTipoReporte().name() : null;
        dto.fechaHora = a.getFechaHora();
        dto.detalle = a.getDetalle();
        return dto;
    }

    public Long getId() { return id; }
    public String getEvento() { return evento; }
    public String getEventoDescripcion() { return eventoDescripcion; }
    public String getResultado() { return resultado; }
    public String getUsuario() { return usuario; }
    public String getCodCliente() { return codCliente; }
    public String getCodCategoria() { return codCategoria; }
    public String getTipoReporte() { return tipoReporte; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public String getDetalle() { return detalle; }
}
