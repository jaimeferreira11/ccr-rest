package py.com.jaimeferreira.ccr.insights.entity;

/**
 * Tipos de reporte disponibles en el módulo Insights.
 * Los templates se buscan en resources/insights/ con la convención:
 *   1. template_{tipo}_{codCliente}.xlsx  (específico por cliente)
 *   2. template_{tipo}.xlsx               (default)
 *
 * @author Jaime Ferreira
 */
public enum TipoReporte {

    NORMAL("template_normal"),
    CADENA("template_cadena");

    private final String templateBaseName;

    TipoReporte(String templateBaseName) {
        this.templateBaseName = templateBaseName;
    }

    /** Nombre base del template sin extensión, ej: "template_normal" */
    public String getTemplateBaseName() {
        return templateBaseName;
    }

    /** Nombre de archivo del template por defecto (sin cliente específico) */
    public String getDefaultTemplateFileName() {
        return templateBaseName + ".xlsx";
    }

    /** Nombre de archivo del template específico para un cliente */
    public String getTemplateFileName(String codCliente) {
        return templateBaseName + "_" + codCliente.trim().toUpperCase() + ".xlsx";
    }

    /** Path classpath del template por defecto — usado como último fallback */
    public String getDefaultTemplatePath() {
        return "insights/" + templateBaseName + ".xlsx";
    }

    /** Path classpath del template específico para un cliente */
    public String getClientTemplatePath(String codCliente) {
        return "insights/" + templateBaseName + "_" + codCliente.trim().toUpperCase() + ".xlsx";
    }
}
