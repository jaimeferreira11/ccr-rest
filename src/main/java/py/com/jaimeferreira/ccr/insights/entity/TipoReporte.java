package py.com.jaimeferreira.ccr.insights.entity;

/**
 * Tipos de reporte disponibles en el módulo Insights.
 *
 * Cada tipo define el mapeo de índices del CSV de datos a los campos lógicos.
 * NORMAL y CADENA tienen distinta cantidad y orden de columnas:
 *
 * <pre>
 * NORMAL (13 cols, +1 opcional): Categoría | Apertura | Empresa | Marca | Segmento | Mes | Año |
 *                   Dist.Física | Dist.Ponderada | Facturación | Precio | Volumen | Vol.Unidades [| SUB_MARCA]
 *
 * CADENA (14 cols, +1 opcional): Categoría | Apertura Geográfica | Empresa | Marca | Variedad | Segmento |
 *                   Dist.Física | Dist.Ponderada | Mes | Año | Facturación | Precio | Volumen | Vol.Unidades [| SUB_MARCA]
 * </pre>
 *
 * @author Jaime Ferreira
 */
public enum TipoReporte {

    NORMAL("template_normal",
           /* categoria */       0,
           /* apertura */        1,
           /* empresa */         2,
           /* marca */           3,
           /* segmento */        4,
           /* mes */             5,
           /* ano */             6,
           /* distFisica */      7,
           /* distPonderada */   8,
           /* facturacion */     9,
           /* volumen */         11,
           /* volumenUnidades */ 12,
           /* extra */           -1),

    CADENA("template_cadena",
           /* categoria */       0,
           /* apertura */        1,
           /* empresa */         2,
           /* marca */           3,
           /* segmento */        5,
           /* mes */             8,
           /* ano */             9,
           /* distFisica */      6,
           /* distPonderada */   7,
           /* facturacion */     10,
           /* volumen */         12,
           /* volumenUnidades */ 13,
           /* extra (Variedad) */ 4);

    private final String templateBaseName;

    // Índices de columnas en el CSV de datos (0-based)
    private final int idxCategoria;
    private final int idxApertura;
    private final int idxEmpresa;
    private final int idxMarca;
    private final int idxSegmento;
    private final int idxMes;
    private final int idxAno;
    private final int idxDistFisica;
    private final int idxDistPonderada;
    private final int idxFacturacion;
    private final int idxVolumen;
    private final int idxVolumenUnidades;
    /** Columna extra que se escribe en FACT col 17 / Total Empresa col 15. -1 si no aplica. */
    private final int idxExtra;

    TipoReporte(String templateBaseName,
                int idxCategoria, int idxApertura, int idxEmpresa, int idxMarca,
                int idxSegmento, int idxMes, int idxAno,
                int idxDistFisica, int idxDistPonderada, int idxFacturacion,
                int idxVolumen, int idxVolumenUnidades, int idxExtra) {
        this.templateBaseName = templateBaseName;
        this.idxCategoria = idxCategoria;
        this.idxApertura = idxApertura;
        this.idxEmpresa = idxEmpresa;
        this.idxMarca = idxMarca;
        this.idxSegmento = idxSegmento;
        this.idxMes = idxMes;
        this.idxAno = idxAno;
        this.idxDistFisica = idxDistFisica;
        this.idxDistPonderada = idxDistPonderada;
        this.idxFacturacion = idxFacturacion;
        this.idxVolumen = idxVolumen;
        this.idxVolumenUnidades = idxVolumenUnidades;
        this.idxExtra = idxExtra;
    }

    public int getIdxCategoria()        { return idxCategoria; }
    public int getIdxApertura()         { return idxApertura; }
    public int getIdxEmpresa()          { return idxEmpresa; }
    public int getIdxMarca()            { return idxMarca; }
    public int getIdxSegmento()         { return idxSegmento; }
    public int getIdxMes()              { return idxMes; }
    public int getIdxAno()              { return idxAno; }
    public int getIdxDistFisica()       { return idxDistFisica; }
    public int getIdxDistPonderada()    { return idxDistPonderada; }
    public int getIdxFacturacion()      { return idxFacturacion; }
    public int getIdxVolumen()          { return idxVolumen; }
    public int getIdxVolumenUnidades()  { return idxVolumenUnidades; }
    public int getIdxExtra()            { return idxExtra; }
    public boolean tieneExtra()         { return idxExtra >= 0; }

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

    /** Nombre de archivo del template específico para un cliente y categoría */
    public String getTemplateFileName(String codCliente, String codCategoria) {
        return templateBaseName + "_" + codCliente.trim().toUpperCase()
                + "_" + codCategoria.trim().toUpperCase() + ".xlsx";
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
