package py.com.jaimeferreira.ccr.insights.dto;

import py.com.jaimeferreira.ccr.insights.entity.EstadoInforme;
import py.com.jaimeferreira.ccr.insights.entity.InformeIns;
import py.com.jaimeferreira.ccr.insights.entity.TipoReporte;

import java.time.LocalDateTime;

/**
 * DTO de respuesta con los datos de un informe generado.
 *
 * @author Jaime Ferreira
 */
public class InformeDTO {

    private Long id;
    private String codCliente;
    private TipoReporte tipoReporte;
    private String nombreArchivo;
    private EstadoInforme estado;
    private String mensajeError;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private String nombreUsuarioCreacion;
    private Long duracionSegundos;

    public static InformeDTO from(InformeIns informe) {
        InformeDTO dto = new InformeDTO();
        dto.id = informe.getId();
        dto.codCliente = informe.getCodCliente();
        dto.tipoReporte = informe.getTipoReporte();
        dto.nombreArchivo = informe.getNombreArchivo();
        dto.estado = informe.getEstado();
        dto.mensajeError = informe.getMensajeError();
        dto.fechaCreacion = informe.getFechaCreacion();
        dto.fechaActualizacion = informe.getFechaActualizacion();
        dto.nombreUsuarioCreacion = informe.getNombreUsuarioCreacion();
        dto.duracionSegundos = informe.getDuracionSegundos();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getCodCliente() {
        return codCliente;
    }

    public TipoReporte getTipoReporte() {
        return tipoReporte;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public EstadoInforme getEstado() {
        return estado;
    }

    public String getMensajeError() {
        return mensajeError;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public String getNombreUsuarioCreacion() {
        return nombreUsuarioCreacion;
    }

    public Long getDuracionSegundos() {
        return duracionSegundos;
    }
}
