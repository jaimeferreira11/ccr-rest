package py.com.jaimeferreira.ccr.insights.dto;

import py.com.jaimeferreira.ccr.insights.entity.EventoAuditoriaIns;

public class EventoCatalogoDTO {

    private String codigo;
    private String descripcion;

    public EventoCatalogoDTO(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public static EventoCatalogoDTO from(EventoAuditoriaIns evento) {
        return new EventoCatalogoDTO(evento.name(), evento.getDescripcion());
    }

    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }
}
