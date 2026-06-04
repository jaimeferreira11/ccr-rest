package py.com.jaimeferreira.ccr.insights.entity;

/**
 * Catálogo de eventos auditables del módulo Insights.
 * El enum es la fuente de verdad del catálogo; la descripción alimenta
 * las etiquetas de la pantalla de listado (Parte 2).
 *
 * @author Jaime Ferreira
 */
public enum EventoAuditoriaIns {

    TEMPLATE_SUBIDO("Subida de template"),
    FILTROS_BASE_SUBIDO("Subida de filtros base"),
    DATOS_BASE_SUBIDO("Subida de datos base"),
    DATOS_BASE_ELIMINADO("Eliminación de datos base");

    private final String descripcion;

    EventoAuditoriaIns(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
