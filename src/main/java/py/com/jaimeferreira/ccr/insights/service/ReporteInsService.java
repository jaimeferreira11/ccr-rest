package py.com.jaimeferreira.ccr.insights.service;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.DefaultTempFileCreationStrategy;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.TempFile;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import py.com.jaimeferreira.ccr.commons.entity.Cotizacion;
import py.com.jaimeferreira.ccr.commons.exception.UnknownResourceException;
import py.com.jaimeferreira.ccr.commons.service.CotizacionService;
import py.com.jaimeferreira.ccr.insights.entity.ClienteIns;
import py.com.jaimeferreira.ccr.insights.entity.EstadoInforme;
import py.com.jaimeferreira.ccr.insights.entity.InformeIns;
import py.com.jaimeferreira.ccr.insights.entity.TipoReporte;

import javax.annotation.PostConstruct;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Servicio de generación asíncrona de reportes Excel para el módulo Insights.
 *
 * Flujo:
 * 1. Lee el CSV de filtros fijo en resources/insights/filtros.csv.
 * 2. Aplica esos filtros al CSV de datos subido por el usuario.
 * 3. Abre el template Excel correspondiente al tipo de reporte.
 * 4. Pobla las hojas FACT y Total Empresa con la data filtrada y mapeada.
 * 5. Marca el workbook para recálculo automático al abrirse en Excel.
 * 6. Guarda el archivo en disco y actualiza el registro en la BD.
 *
 * @author Jaime Ferreira
 */
@Service
public class ReporteInsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReporteInsService.class);

    private static final char CSV_SEPARATOR = ';';
    private static final String FILTROS_CSV_PATH = "insights/filtros.csv";
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // Nombres de hojas en el template
    private static final String SHEET_FACT = "FACT";
    private static final String SHEET_TOTAL_EMPRESA = "Total Empresa";
    private static final String SHEET_CALENDARIO = "Calendario";
    private static final String SHEET_DIM = "DIM";
    private static final String SHEET_HOJA1 = "Hoja 1";
    private static final String SHEET_INICIO = "INICIO";

    // Nombre definido para el mes de inicio fiscal (referenciado por fórmulas en Calendario)
    private static final String NAMED_RANGE_MES_INICIO_FISCAL = "MesInicioFiscal";

    // Nombres de meses en español para la hoja Calendario
    private static final String[] MESES_ES = {
        "enero",
        "febrero",
        "marzo",
        "abril",
        "mayo",
        "junio",
        "julio",
        "agosto",
        "septiembre",
        "octubre",
        "noviembre",
        "diciembre"
    };

    // Los índices de columnas del CSV de datos ahora están en TipoReporte,
    // ya que NORMAL y CADENA tienen distinto orden y cantidad de columnas.

    @Value("${path.directory.server}")
    private String directorioServer;

    @Value("${reporte.refresh.script.path:scripts/refresh-excel.vbs}")
    private String refreshScriptPath;

    @Value("${reporte.refresh.timeout.seconds:120}")
    private int refreshTimeoutSeconds;

    @Value("${reporte.refresh.enabled:true}")
    private boolean refreshEnabled;

    @Value("${path.directory.server_path_reports_insights}")
    private String insightsReportsSubdir;

    @Value("${path.directory.server_path_clientes_insights}")
    private String insightsClientesSubdir;

    @Autowired
    private InformeInsService informeService;

    @Autowired
    private ClienteInsService clienteService;

    @Autowired
    private TemplateInsService templateService;

    @Autowired
    private CotizacionService cotizacionService;

    /**
     * Self-reference para que @Async funcione a través del proxy de Spring (evita self-invocation).
     */
    @Lazy
    @Autowired
    private ReporteInsService self;

    /**
     * Redirige los archivos temporales de POI (SXSSF) a un directorio estable dentro de
     * directorioServer, evitando el NoSuchFileException que ocurre en Windows cuando el
     * antivirus o el limpiador de temp elimina los archivos bajo java.io.tmpdir.
     */
    @PostConstruct
    public void configurarPoiTempDir() {
        File poiTempDir = new File(directorioServer, "poi-tmp");
        poiTempDir.mkdirs();

        // mkdirs() devuelve false SIN excepción si no hay permisos (típico: /opt es de root
        // en Mac/Linux). Si el dir no quedó escribible, SXSSF no puede volcar las filas de
        // FACT/Total Empresa a sus temporales y la generación revienta con
        // NoSuchFileException: .../poi-sxssf-sheet*.xml. Verificar y caer a un temp
        // garantizado (java.io.tmpdir) para no depender de un directorio no escribible.
        if (!poiTempDir.isDirectory() || !poiTempDir.canWrite()) {
            File fallback = new File(System.getProperty("java.io.tmpdir"), "ccr-poi-tmp");
            fallback.mkdirs();
            LOGGER.warn("POI temp dir '{}' no escribible (mkdirs/canWrite falló). "
                        + "Usando fallback '{}'. En prod, verifique permisos de '{}' "
                        + "y excluya ese dir del antivirus/limpiador de temp.",
                        poiTempDir.getAbsolutePath(), fallback.getAbsolutePath(), directorioServer);
            poiTempDir = fallback;
        }

        TempFile.setTempFileCreationStrategy(new DefaultTempFileCreationStrategy(poiTempDir));
        LOGGER.info("POI temp dir configurado: {}", poiTempDir.getAbsolutePath());

        asegurarDesktopSystemProfile();
    }

    /**
     * Excel COM, ejecutado bajo una cuenta de servicio (típicamente LocalSystem),
     * exige que exista la carpeta Desktop del perfil del sistema. Sin esto,
     * Workbooks.Open falla con "No se puede obtener la propiedad Open de la clase Workbooks"
     * y el refresh del modelo de datos VertiPaq no se ejecuta.
     *
     * Hay dos rutas porque Office 32-bit usa SysWOW64 y Office 64-bit usa System32.
     * Crear ambas vacías es inocuo y resuelve el caso sin importar la arquitectura.
     */
    private void asegurarDesktopSystemProfile() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("win")) {
            return;
        }
        String[] rutas = {
            "C:\\Windows\\System32\\config\\systemprofile\\Desktop",
            "C:\\Windows\\SysWOW64\\config\\systemprofile\\Desktop"
        };
        for (String ruta : rutas) {
            File dir = new File(ruta);
            if (dir.exists()) {
                continue;
            }
            try {
                if (dir.mkdirs()) {
                    LOGGER.info("Carpeta requerida por Excel COM creada: {}", ruta);
                } else {
                    LOGGER.warn("No se pudo crear {} (sin error pero mkdirs devolvió false). " +
                                "Si la cuenta del servicio no es LocalSystem, créela manualmente como Administrador.", ruta);
                }
            } catch (SecurityException e) {
                LOGGER.warn("Sin permisos para crear {}: {}. Créela manualmente como Administrador.",
                            ruta, e.getMessage());
            }
        }
    }

    /**
     * Inicia la generación: crea el registro en BD con estado PROCESANDO y lanza el
     * procesamiento asíncrono. Los bytes de ambos CSV se capturan aquí para evitar que
     * los archivos temporales del request expiren antes de que el hilo async los consuma.
     *
     * @param csvData
     *            archivo CSV con los datos
     * @param csvFiltros
     *            archivo CSV con los filtros a aplicar (puede ser null; si es null se usa el base
     *            del cliente)
     * @param codCategoria
     *            código de la categoría seleccionada (determina el template a usar)
     */
    public InformeIns iniciarGeneracion(MultipartFile csvData, MultipartFile csvFiltros,
                                        String codCliente, String codCategoria, TipoReporte tipoReporte,
                                        int mesInicioFiscal, Integer mesReporte, String usuario) {

        LOGGER.info("Iniciando generación de informe. Cliente: {}, Tipo: {}, Categoría: {}, mesInicioFiscal: {}",
                    codCliente, tipoReporte, codCategoria, mesInicioFiscal);
        LOGGER.info("  csvData: nombre='{}', tamaño={} bytes", csvData.getOriginalFilename(), csvData.getSize());
        LOGGER.info("  csvFiltros: {}", csvFiltros != null && !csvFiltros.isEmpty()
                                                                                    ? "nombre='"
                                                                                            + csvFiltros.getOriginalFilename()
                                                                                            + "', tamaño="
                                                                                            + csvFiltros.getSize()
                                                                                            + " bytes"
                                                                                    : "no proporcionado (se usará base del cliente o classpath)");

        String codClienteNorm = codCliente.trim().toUpperCase();

        // Validar existencia de archivos base ANTES de persistir y lanzar async
        validarArchivosBaseExisten(csvFiltros, codClienteNorm);

        byte[] csvBytes;
        byte[] filtroBytes;
        try {
            csvBytes = csvData.getBytes();
            filtroBytes = (csvFiltros != null && !csvFiltros.isEmpty()) ? csvFiltros.getBytes() : null;
        }
        catch (IOException e) {
            throw new RuntimeException("Error al leer los archivos CSV: " + e.getMessage(), e);
        }

        InformeIns informe = new InformeIns();
        informe.setCodCliente(codClienteNorm);
        informe.setCodCategoria(codCategoria.trim().toUpperCase());
        informe.setTipoReporte(tipoReporte);
        informe.setEstado(EstadoInforme.PROCESANDO);
        informe.setFechaCreacion(LocalDateTime.now());
        informe.setNombreUsuarioCreacion(usuario);

        InformeIns guardado = informeService.save(informe);

        self.procesarReporte(guardado.getId(), csvBytes, filtroBytes, codCliente, codCategoria, tipoReporte, mesInicioFiscal, mesReporte, usuario);

        return guardado;
    }

    /**
     * Valida que existan los archivos base necesarios para generar el informe
     * ANTES de persistir en BD y lanzar el procesamiento async.
     * - Filtros: debe existir el CSV subido, o filtros_base.csv del cliente, o filtros.csv en
     * classpath.
     * - Datos base: opcional; si no existe se usará solo el CSV del usuario.
     */
    private void validarArchivosBaseExisten(MultipartFile csvFiltros, String codCliente) {
        // Validar filtros: subido > base del cliente > classpath
        boolean hayFiltroSubido = csvFiltros != null && !csvFiltros.isEmpty();
        if (!hayFiltroSubido) {
            File filtroBase = Paths.get(getClienteDir(codCliente), FILTROS_BASE_FILENAME).toFile();
            if (!filtroBase.exists()) {
                ClassPathResource classpathFiltro = new ClassPathResource(FILTROS_CSV_PATH);
                if (!classpathFiltro.exists()) {
                    throw new UnknownResourceException(
                                                       "No se encontró un archivo de filtros para el cliente "
                                                               + codCliente
                                                               + ". Debe subir un CSV de filtros o configurar filtros_base.csv desde Administración.");
                }
            }
        }
    }

    /**
     * Procesamiento asíncrono: resuelve filtros, concatena datos base con datos del usuario,
     * genera el Excel y guarda en disco. Al finalizar persiste el CSV concatenado como nuevo
     * datos_base.
     *
     * @param filtroBytes
     *            bytes del CSV de filtros subido por el usuario (null si no se subió)
     */
    @Async
    public void procesarReporte(Long informeId, byte[] csvBytes, byte[] filtroBytes,
                                String codCliente, String codCategoria, TipoReporte tipoReporte,
                                int mesInicioFiscal, Integer mesReporte, String usuario) {

        LOGGER.info("Procesando informe id={} de forma asíncrona", informeId);

        long inicio = System.currentTimeMillis();
        try {
            String codClienteNorm = codCliente.trim().toUpperCase();

            // Obtener país del cliente desde la BD
            ClienteIns cliente = clienteService.findByCodigo(codClienteNorm);
            String pais = cliente.getPais().getDescripcion();
            String clienteLabel = cliente.getDescripcion();

            // Resolver filtros: usuario → base del cliente → classpath fallback
            List<Map<String, String>> filtros = resolverFiltros(filtroBytes, codClienteNorm);
            LOGGER.info("Filtros cargados: {} reglas", filtros.size());

            // Mapas de lookup para valores derivados del filtro
            Map<String, String> ordenAperturaMap = buildLookupMap(filtros, "APERTURA", "Orden_Apertura");
            Map<String, String> agrupadorSegmentoMap = buildLookupMap(filtros, "SEGMENTO", "AGR_SEGM");
            LOGGER.info("Mapas de lookup: ordenApertura={} entradas, agrupadorSegmento={} entradas",
                        ordenAperturaMap.size(), agrupadorSegmentoMap.size());

            // Concatenar datos base + datos del usuario (datos base es opcional)
            byte[] datosBase = leerDatosBase(codClienteNorm, tipoReporte);
            byte[] csvConcatenado = (datosBase != null)
                                                        ? concatenarCsvData(datosBase, csvBytes)
                                                        : csvBytes;
            LOGGER.info("CSV concatenado generado ({} bytes) para cliente {}", csvConcatenado.length, codClienteNorm);

            // Liberar referencias a las copias intermedias del CSV: si datosBase != null,
            // csvConcatenado es una copia nueva y tanto csvBytes como datosBase pueden
            // ser GC'd ahora (~hasta 2x el tamaño del CSV liberado del heap).
            if (datosBase != null) {
                csvBytes = null;
                datosBase = null;
            }

            // Leer y filtrar data del CSV concatenado
            List<String[]> dataFiltrada = leerYFiltrarCsvData(csvConcatenado, filtros);
            int totalDataRows = dataFiltrada.size() - 1; // sin header
            LOGGER.info("Filas resultantes tras filtrado: {}", totalDataRows);

            // Validar header y descartar filas incompletas (defensa contra CSVs malformados:
            // si una fila no llega al índice de Vol.Unidades reventaba con ArrayIndexOutOfBounds
            // en poblarFact/poblarTotalEmpresa)
            int minCols = tipoReporte.getMinCols();
            if (!dataFiltrada.isEmpty() && dataFiltrada.get(0).length < minCols) {
                throw new RuntimeException("El CSV de datos tiene " + dataFiltrada.get(0).length
                        + " columnas, pero el tipo " + tipoReporte + " requiere al menos " + minCols
                        + ". Verifique el formato del archivo.");
            }
            validarHeadersCsv(dataFiltrada.get(0), tipoReporte);
            int descartadas = 0;
            for (int i = dataFiltrada.size() - 1; i >= 1; i--) {
                if (dataFiltrada.get(i).length < minCols) {
                    dataFiltrada.remove(i);
                    descartadas++;
                }
            }
            if (descartadas > 0) {
                LOGGER.warn("Descartadas {} filas con menos de {} columnas (tipo {})",
                            descartadas, minCols, tipoReporte);
                totalDataRows = dataFiltrada.size() - 1;
            }

            // Detectar columna opcional SUB_MARCA en el CSV de datos
            int idxSubMarca = detectarColumnaSubMarca(dataFiltrada.get(0));
            if (idxSubMarca >= 0) {
                LOGGER.info("Columna SUB_MARCA detectada en índice {}", idxSubMarca);
            } else {
                LOGGER.info("Columna SUB_MARCA no presente en el CSV");
            }

            if (totalDataRows <= 0) {
                throw new RuntimeException(
                                           "El CSV de datos no contiene filas que coincidan con los filtros aplicados. "
                                                   + "Verifique que los nombres de columna del filtro coincidan con los del CSV de datos.");
            }

            // Abrir template Excel: busca primero el específico del cliente+categoría en disco,
            // luego el default.
            IOUtils.setByteArrayMaxOverride(500 * 1024 * 1024);
            InputStream templateStream = resolverTemplateStream(tipoReporte, codClienteNorm, codCategoria);
            XSSFWorkbook templateWb = new XSSFWorkbook(templateStream);
            templateStream.close();
            logHito(informeId, inicio, "template abierto");

            desconectarTablas(templateWb);
            escribirMesInicioFiscal(templateWb, mesInicioFiscal);

            limpiarDatosHoja(templateWb, SHEET_FACT);
            limpiarDatosHoja(templateWb, SHEET_TOTAL_EMPRESA);

            // Capturar los headers de FACT y Total Empresa AHORA, sobre el XSSFWorkbook.
            // Una vez envuelto en SXSSF, sheet.getRow(0) devuelve null (SXSSF no da acceso
            // aleatorio a las filas existentes del template) y buildHeaderIndexMap quedaría
            // vacío -> poblarFact/poblarTotalEmpresa saldrían SIN escribir ni una fila
            // (FACT/Total Empresa vacíos en el reporte generado).
            Map<String, Integer> headersFact = buildHeaderIndexMap(templateWb.getSheet(SHEET_FACT));
            Map<String, Integer> headersTotalEmpresa = buildHeaderIndexMap(templateWb.getSheet(SHEET_TOTAL_EMPRESA));

            int calendarRows = poblarCalendario(templateWb, dataFiltrada, tipoReporte, mesInicioFiscal, mesReporte);
            logHito(informeId, inicio, "calendario poblado");

            SXSSFWorkbook workbook = new SXSSFWorkbook(templateWb, 100);
            workbook.setCompressTempFiles(true);

            CellStyle dateCellStyle = workbook.createCellStyle();
            dateCellStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));

            poblarFact(workbook, dataFiltrada, codClienteNorm, clienteLabel, pais, dateCellStyle, tipoReporte, ordenAperturaMap, agrupadorSegmentoMap, idxSubMarca, mesInicioFiscal, headersFact);
            logHito(informeId, inicio, "FACT poblado");
            poblarTotalEmpresa(workbook, dataFiltrada, codClienteNorm, clienteLabel, pais, dateCellStyle, tipoReporte, ordenAperturaMap, agrupadorSegmentoMap, idxSubMarca, mesInicioFiscal, headersTotalEmpresa);
            logHito(informeId, inicio, "Total Empresa poblado");

            // Ya no se vuelve a leer; liberar para que el GC recupere las N filas (String[][])
            // antes del write del Excel, que es el otro pico de memoria.
            dataFiltrada = null;

            actualizarRangosTablas(templateWb, totalDataRows, calendarRows, tipoReporte, idxSubMarca >= 0);
            refrescarTablasDinamicas(templateWb);
            ocultarHoja(workbook, SHEET_FACT);
            ocultarHoja(workbook, SHEET_CALENDARIO);
            ocultarHoja(workbook, SHEET_TOTAL_EMPRESA);
            ocultarHoja(workbook, SHEET_DIM);
            ocultarHoja(workbook, SHEET_HOJA1);
            workbook.setForceFormulaRecalculation(true);

            String nombreArchivo = buildNombreArchivo(codClienteNorm, tipoReporte);
            logHito(informeId, inicio, "iniciando guardado en disco");
            String rutaCompleta = guardarEnDisco(workbook, nombreArchivo);
            workbook.dispose();
            logHito(informeId, inicio, "guardado en disco completado: " + rutaCompleta);

            // POST-PROCESAMIENTO: setear refreshOnLoad=1 en la conexión del Data Model
            // para que Excel reconstruya el modelo VertiPaq al abrir el archivo.
            // Reemplaza el viejo Refresh via Excel COM (scripts/refresh-excel.vbs), que
            // era frágil bajo contexto de servicio. Caveat: el usuario verá una sola vez
            // la barra "Habilitar contenido" la primera vez que abra el archivo.
            logHito(informeId, inicio, "seteando refreshOnLoad en Data Model");
            forzarRefreshOnLoadDataModel(rutaCompleta);
            logHito(informeId, inicio, "refreshOnLoad Data Model OK");

            // Persistir el CSV concatenado como nuevo datos_base del cliente
            guardarDatosBase(codClienteNorm, tipoReporte, csvConcatenado);
            logHito(informeId, inicio, "datos_base persistido");

            LOGGER.info("Informe {} guardado en: {}", informeId, rutaCompleta);
            long duracion = (System.currentTimeMillis() - inicio) / 1000;
            informeService.marcarCompletado(informeId, nombreArchivo, usuario, duracion);
            LOGGER.info("Informe id={} completado en {}s", informeId, duracion);

        }
        catch (Throwable t) {
            LOGGER.error("Error generando informe id={}: {}", informeId, t.getMessage(), t);
            informeService.marcarError(informeId, t.getMessage(), usuario);
        }
    }

    /** Loguea un hito del procesamiento con segundos transcurridos desde el inicio. */
    private void logHito(Long informeId, long inicio, String hito) {
        long seg = (System.currentTimeMillis() - inicio) / 1000;
        LOGGER.info("[informe id={}] [+{}s] {}", informeId, seg, hito);
    }

    /**
     * Valida que los headers del CSV de datos coincidan con los esperados para el tipo de reporte.
     * Primero detecta columnas FALTANTES (set diff): es el error más común y produce un mensaje
     * accionable ("Faltan: Variedad, Año") en vez de una cascada de discrepancias por posición.
     * Si todas las columnas están presentes pero en orden incorrecto, reporta el desorden.
     * Las columnas extra (ej: SUB_MARCA) se ignoran — la lógica de procesamiento usa índices fijos.
     */
    private void validarHeadersCsv(String[] headersActuales, TipoReporte tipoReporte) {
        String[][] esperados = tipoReporte.getExpectedHeaders();

        // Set normalizado de presentes en el CSV
        java.util.Set<String> actualesNorm = new java.util.HashSet<>();
        for (String h : headersActuales) {
            actualesNorm.add(normalizar(h));
        }

        // Faltantes: para cada posición, basta con que alguno de sus alias aparezca en el CSV.
        List<String> faltantes = new ArrayList<>();
        for (String[] aliases : esperados) {
            boolean encontrada = false;
            for (String alias : aliases) {
                if (actualesNorm.contains(normalizar(alias))) {
                    encontrada = true;
                    break;
                }
            }
            if (!encontrada) {
                faltantes.add(aliases[0]);
            }
        }
        if (!faltantes.isEmpty()) {
            String msg = "Faltan columnas en el CSV para tipo " + tipoReporte + ": "
                    + String.join(", ", faltantes);
            LOGGER.error(msg);
            throw new RuntimeException(msg);
        }

        // Todas las columnas están presentes: verificar orden. Si no coincide, los índices
        // hardcodeados de poblarFact/poblarTotalEmpresa leerían datos de columnas incorrectas.
        List<String> desordenadas = new ArrayList<>();
        for (int i = 0; i < esperados.length; i++) {
            String actual = i < headersActuales.length ? headersActuales[i] : "(faltante)";
            String actualNorm = normalizar(actual);
            boolean match = false;
            for (String alias : esperados[i]) {
                if (normalizar(alias).equals(actualNorm)) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                desordenadas.add("posición " + i + ": esperado '" + esperados[i][0]
                        + "', encontrado '" + actual + "'");
            }
        }
        if (!desordenadas.isEmpty()) {
            String msg = "Columnas del CSV en orden incorrecto para tipo " + tipoReporte + ": "
                    + String.join("; ", desordenadas);
            LOGGER.error(msg);
            throw new RuntimeException(msg);
        }
    }

    // -------------------------------------------------------------------------
    // Poblar hojas
    // -------------------------------------------------------------------------

    /**
     * Pobla la hoja FACT.
     *
     * Headers esperados en el template (orden definido por la tabla FACT del .xlsx, no por posición fija):
     * Distribución Fisica, Distribución Ponderada, Facturación, Volumen, Volumen Unidades,
     * Apertura Geografica, Categoría, CLIENTE, Variedad, Empresa, hash, Marca, PAIS,
     * Segmento, Agrupador Segmento, Orden Apertura, YTD 1er Mes, Fecha [, SUB_MARCA].
     *
     * La escritura se hace por NOMBRE de header — si el template reordena/agrega columnas
     * el código sigue funcionando sin tocar índices. Si una columna del código no existe
     * en el template, el dato simplemente no se escribe (no es error).
     */
    private void poblarFact(Workbook workbook, List<String[]> data,
                            String codCliente, String clienteLabel, String pais,
                            CellStyle dateCellStyle, TipoReporte tipoReporte,
                            Map<String, String> ordenAperturaMap,
                            Map<String, String> agrupadorSegmentoMap,
                            int idxSubMarca, int mesInicioFiscal,
                            Map<String, Integer> headers) {

        Sheet sheet = workbook.getSheet(SHEET_FACT);
        if (sheet == null) {
            LOGGER.warn("Hoja '{}' no encontrada en el template, se crea nueva.", SHEET_FACT);
            sheet = workbook.createSheet(SHEET_FACT);
        }

        // El header viene pre-construido desde el XSSFWorkbook (antes de envolver en SXSSF),
        // porque sobre la hoja SXSSF getRow(0) devuelve null. Ver procesarReporte.
        if (headers == null || headers.isEmpty()) {
            LOGGER.error("Hoja '{}' sin headers en fila 1; no se puede poblar.", SHEET_FACT);
            return;
        }

        int dataRows = data.size() - 1; // sin header

        for (int i = 1; i <= dataRows; i++) {
            String[] csv = data.get(i);
            Row row = sheet.getRow(i);
            if (row == null)
                row = sheet.createRow(i);

            String segmento = csv[tipoReporte.getIdxSegmento()];
            String apertura = csv[tipoReporte.getIdxApertura()];
            String variedad = tipoReporte.tieneExtra() ? csv[tipoReporte.getIdxExtra()] : "";
            String subMarca = (idxSubMarca >= 0 && idxSubMarca < csv.length) ? csv[idxSubMarca] : "";

            setCellNumericByHeader(row, headers, "Distribución Fisica",   csv[tipoReporte.getIdxDistFisica()]);
            setCellNumericByHeader(row, headers, "Distribución Ponderada", csv[tipoReporte.getIdxDistPonderada()]);
            setCellNumericByHeader(row, headers, "Facturación",            csv[tipoReporte.getIdxFacturacion()]);
            setCellNumericByHeader(row, headers, "Volumen",                csv[tipoReporte.getIdxVolumen()]);
            setCellNumericByHeader(row, headers, "Volumen Unidades",       csv[tipoReporte.getIdxVolumenUnidades()]);
            setCellStringByHeader (row, headers, "Apertura Geografica",    apertura);
            setCellStringByHeader (row, headers, "Categoría",              csv[tipoReporte.getIdxCategoria()]);
            setCellStringByHeader (row, headers, "CLIENTE",                clienteLabel);
            setCellStringByHeader (row, headers, "Variedad",               variedad);
            setCellStringByHeader (row, headers, "Empresa",                csv[tipoReporte.getIdxEmpresa()]);
            setCellStringByHeader (row, headers, "hash",                   "");
            setCellStringByHeader (row, headers, "Marca",                  csv[tipoReporte.getIdxMarca()]);
            setCellStringByHeader (row, headers, "PAIS",                   pais);
            setCellStringByHeader (row, headers, "Segmento",               segmento);
            setCellStringByHeader (row, headers, "Agrupador Segmento",
                    agrupadorSegmentoMap.getOrDefault(normalizar(segmento), derivarAgrupadorSegmento(segmento)));
            setCellStringByHeader (row, headers, "Orden Apertura",
                    ordenAperturaMap.getOrDefault(normalizar(apertura), "0"));
            setCellIntByHeader    (row, headers, "YTD 1er Mes",
                    derivarYtdInt(csv[tipoReporte.getIdxMes()], mesInicioFiscal));
            setCellDateByHeader   (row, headers, "Fecha",
                    csv[tipoReporte.getIdxMes()], csv[tipoReporte.getIdxAno()], dateCellStyle);
            setCellStringByHeader (row, headers, "SUB_MARCA",              subMarca);
        }

        LOGGER.info("Hoja '{}' poblada con {} filas de datos.", SHEET_FACT, dataRows);
    }

    /**
     * Pobla la hoja Total Empresa.
     *
     * Headers esperados en el template (orden definido por la tabla Total_Empresa del .xlsx):
     * Distribución Fisica, Distribución Ponderada, Apertura Geografica, Categoría, CLIENTE,
     * Empresa, Variedad, hash, PAIS, Segmento, Agrupador Segmento, Orden Apertura,
     * Volumen Unidades, YTD 1er Mes, Fecha, Marca [, SUB_MARCA].
     *
     * Igual que poblarFact, la escritura se hace por NOMBRE de header.
     */
    private void poblarTotalEmpresa(Workbook workbook, List<String[]> data,
                                    String codCliente, String clienteLabel, String pais,
                                    CellStyle dateCellStyle, TipoReporte tipoReporte,
                                    Map<String, String> ordenAperturaMap,
                                    Map<String, String> agrupadorSegmentoMap,
                                    int idxSubMarca, int mesInicioFiscal,
                                    Map<String, Integer> headers) {

        Sheet sheet = workbook.getSheet(SHEET_TOTAL_EMPRESA);
        if (sheet == null) {
            LOGGER.warn("Hoja '{}' no encontrada en el template, se crea nueva.", SHEET_TOTAL_EMPRESA);
            sheet = workbook.createSheet(SHEET_TOTAL_EMPRESA);
        }

        // Header pre-construido desde el XSSFWorkbook (antes de SXSSF). Ver procesarReporte.
        if (headers == null || headers.isEmpty()) {
            LOGGER.error("Hoja '{}' sin headers en fila 1; no se puede poblar.", SHEET_TOTAL_EMPRESA);
            return;
        }

        int dataRows = data.size() - 1;

        for (int i = 1; i <= dataRows; i++) {
            String[] csv = data.get(i);
            Row row = sheet.getRow(i);
            if (row == null)
                row = sheet.createRow(i);

            String segmento = csv[tipoReporte.getIdxSegmento()];
            String apertura = csv[tipoReporte.getIdxApertura()];
            String variedad = tipoReporte.tieneExtra() ? csv[tipoReporte.getIdxExtra()] : "";
            String subMarca = (idxSubMarca >= 0 && idxSubMarca < csv.length) ? csv[idxSubMarca] : "";

            setCellNumericByHeader(row, headers, "Distribución Fisica",   csv[tipoReporte.getIdxDistFisica()]);
            setCellNumericByHeader(row, headers, "Distribución Ponderada", csv[tipoReporte.getIdxDistPonderada()]);
            setCellStringByHeader (row, headers, "Apertura Geografica",    apertura);
            setCellStringByHeader (row, headers, "Categoría",              csv[tipoReporte.getIdxCategoria()]);
            setCellStringByHeader (row, headers, "CLIENTE",                clienteLabel);
            setCellStringByHeader (row, headers, "Empresa",                csv[tipoReporte.getIdxEmpresa()]);
            setCellStringByHeader (row, headers, "Variedad",               variedad);
            setCellStringByHeader (row, headers, "hash",                   "");
            setCellStringByHeader (row, headers, "PAIS",                   pais);
            setCellStringByHeader (row, headers, "Segmento",               segmento);
            setCellStringByHeader (row, headers, "Agrupador Segmento",
                    agrupadorSegmentoMap.getOrDefault(normalizar(segmento), derivarAgrupadorSegmento(segmento)));
            setCellStringByHeader (row, headers, "Orden Apertura",
                    ordenAperturaMap.getOrDefault(normalizar(apertura), "0"));
            setCellNumericByHeader(row, headers, "Volumen Unidades",       csv[tipoReporte.getIdxVolumenUnidades()]);
            setCellIntByHeader    (row, headers, "YTD 1er Mes",
                    derivarYtdInt(csv[tipoReporte.getIdxMes()], mesInicioFiscal));
            setCellDateByHeader   (row, headers, "Fecha",
                    csv[tipoReporte.getIdxMes()], csv[tipoReporte.getIdxAno()], dateCellStyle);
            setCellStringByHeader (row, headers, "Marca",                  csv[tipoReporte.getIdxMarca()]);
            setCellStringByHeader (row, headers, "SUB_MARCA",              subMarca);
        }

        LOGGER.info("Hoja '{}' poblada con {} filas de datos.", SHEET_TOTAL_EMPRESA, dataRows);
    }

    /**
     * Escribe el mes de inicio fiscal en la hoja INICIO (celda B4) y crea/verifica
     * el nombre definido MesInicioFiscal → INICIO!$B$4.
     * Las fórmulas de la columna "Año Fiscal" en Calendario referencian este nombre.
     */
    private void escribirMesInicioFiscal(XSSFWorkbook wb, int mesInicioFiscal) {
        XSSFSheet sheet = wb.getSheet(SHEET_INICIO);
        if (sheet == null) {
            LOGGER.warn("Hoja '{}' no encontrada en el template, se crea nueva.", SHEET_INICIO);
            sheet = wb.createSheet(SHEET_INICIO);
        }

        // Escribir etiqueta en A4 y valor en B4
        org.apache.poi.xssf.usermodel.XSSFRow row = sheet.getRow(3);
        if (row == null) row = sheet.createRow(3);

        Cell labelCell = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        labelCell.setCellValue("Mes Inicio Fiscal");

        Cell valueCell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        valueCell.setCellValue(mesInicioFiscal);

        // Crear/verificar nombre definido a nivel workbook
        org.apache.poi.ss.usermodel.Name existing = wb.getName(NAMED_RANGE_MES_INICIO_FISCAL);
        if (existing == null) {
            org.apache.poi.ss.usermodel.Name name = wb.createName();
            name.setNameName(NAMED_RANGE_MES_INICIO_FISCAL);
            name.setRefersToFormula(SHEET_INICIO + "!$B$4");
            LOGGER.info("Nombre definido '{}' creado → {}!$B$4", NAMED_RANGE_MES_INICIO_FISCAL, SHEET_INICIO);
        } else {
            LOGGER.info("Nombre definido '{}' ya existe, apunta a: {}", NAMED_RANGE_MES_INICIO_FISCAL, existing.getRefersToFormula());
        }

        LOGGER.info("Mes inicio fiscal escrito en {}!B4: {}", SHEET_INICIO, mesInicioFiscal);
    }

    /**
     * Pobla la hoja Calendario directamente sobre el XSSFWorkbook (antes de SXSSF).
     * Genera una fila por cada mes de un rango MENSUAL CONTIGUO (sin huecos) que va desde el
     * primer mes con datos hasta el "tope":
     *   - si {@code mesReporte} != null → tope = (año en curso, mesReporte) — el front elige el
     *     mes del reporte; nunca por debajo del último mes con datos (evita filas FACT huérfanas).
     *   - si {@code mesReporte} == null → tope = último mes con datos.
     *
     * El rango contiguo es requisito de la time-intelligence del modelo: la columna calculada
     * 'FACT'[Fecha_YTD] usa DATEADD, que falla con #ERROR si la tabla de fechas tiene huecos o
     * (en algunas versiones) años parciales; ese #ERROR cascadea a M_YTD MODELO → M_MAESTRA_ACUM.
     * Ver docs/REPORTE-GERENCIAL-GENERACION.md §6.
     *
     * Columnas: Fecha (date 1ro del mes), Mes Numero (int), Año (int), Mes (nombre en español),
     * Año Fiscal (fórmula que referencia MesInicioFiscal), Cotización USD.
     *
     * @return cantidad de filas de datos escritas (sin header)
     */
    private int poblarCalendario(XSSFWorkbook wb, List<String[]> data, TipoReporte tipoReporte,
                                 int mesInicioFiscal, Integer mesReporte) {
        XSSFSheet sheet = wb.getSheet(SHEET_CALENDARIO);
        if (sheet == null) {
            LOGGER.warn("Hoja '{}' no encontrada en el template, se omite.", SHEET_CALENDARIO);
            return 0;
        }

        // Agregar headers en columnas E y F si no existen
        org.apache.poi.xssf.usermodel.XSSFRow headerRow = sheet.getRow(0);
        if (headerRow == null) headerRow = sheet.createRow(0);
        Cell headerAnoFiscal = headerRow.getCell(4, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        headerAnoFiscal.setCellValue("Año Fiscal");
        Cell headerCotizacion = headerRow.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        headerCotizacion.setCellValue("Cotización USD");

        // Agregar tableColumns "Año Fiscal" y "Cotización USD" a la tabla Calendario si no existen
        for (XSSFTable table : sheet.getTables()) {
            if ("Calendario".equals(table.getName())) {
                // Año Fiscal
                boolean existeAnoFiscal = false;
                boolean existeCotizacion = false;
                for (org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumn col : table.getCTTable().getTableColumns().getTableColumnArray()) {
                    if ("Año Fiscal".equals(col.getName())) existeAnoFiscal = true;
                    if ("Cotización USD".equals(col.getName())) existeCotizacion = true;
                }
                if (!existeAnoFiscal) {
                    org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumn newCol = table.getCTTable().getTableColumns().addNewTableColumn();
                    newCol.setId(5);
                    newCol.setName("Año Fiscal");
                    newCol.addNewCalculatedColumnFormula().setStringValue(
                        "[@Año]+IF(AND(MesInicioFiscal>1,[@[Mes Numero]]>=MesInicioFiscal),1,0)");
                    long count = table.getCTTable().getTableColumns().getCount();
                    table.getCTTable().getTableColumns().setCount(count + 1);
                    LOGGER.info("Columna 'Año Fiscal' agregada a tabla Calendario.");
                }
                if (!existeCotizacion) {
                    org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumn newCol = table.getCTTable().getTableColumns().addNewTableColumn();
                    newCol.setId(6);
                    newCol.setName("Cotización USD");
                    long count = table.getCTTable().getTableColumns().getCount();
                    table.getCTTable().getTableColumns().setCount(count + 1);
                    LOGGER.info("Columna 'Cotización USD' agregada a tabla Calendario.");
                }
                break;
            }
        }

        // Limpiar filas de datos previas (preservar header)
        int lastOldRow = sheet.getLastRowNum();
        for (int i = lastOldRow; i >= 1; i--) {
            org.apache.poi.xssf.usermodel.XSSFRow row = sheet.getRow(i);
            if (row != null)
                sheet.removeRow(row);
        }

        // Extraer pares (año, mes) únicos del CSV — TreeMap ordena por año, TreeSet por mes
        TreeMap<Integer, TreeSet<Integer>> anioMeses = new TreeMap<>();
        for (int i = 1; i < data.size(); i++) {
            try {
                int anio = Integer.parseInt(data.get(i)[tipoReporte.getIdxAno()].trim());
                int mes = Integer.parseInt(data.get(i)[tipoReporte.getIdxMes()].trim());
                if (mes >= 1 && mes <= 12 && anio >= 1900 && anio <= 9999) {
                    anioMeses.computeIfAbsent(anio, k -> new TreeSet<>()).add(mes);
                }
            }
            catch (NumberFormatException ignored) {
            }
        }

        // Crear CellStyle de fecha sobre el XSSFWorkbook
        CellStyle dateCellStyle = wb.createCellStyle();
        dateCellStyle.setDataFormat(wb.createDataFormat().getFormat("yyyy-mm-dd"));

        // Fórmula para Año Fiscal: referencia estructurada que usa el nombre definido MesInicioFiscal
        String formulaAnoFiscal = "[@Año]+IF(AND(MesInicioFiscal>1,[@[Mes Numero]]>=MesInicioFiscal),1,0)";

        // Generar un rango MENSUAL CONTIGUO (sin huecos) desde el primer mes con datos hasta
        // el tope. Antes se generaban solo los meses presentes en el CSV: si faltaba un mes
        // intermedio (o el rango quedaba parcial) la columna 'FACT'[Fecha_YTD] (DATEADD) daba
        // #ERROR y cascadeaba a M_YTD MODELO -> M_MAESTRA_ACUM. Los meses agregados sin datos
        // quedan sin filas FACT relacionadas (medidas en blanco), correcto para un calendario.
        // Se usan índices absolutos de mes (anio*12 + (mes-1)) para iterar contiguo cruzando años.
        int rowNum = 1;
        String rangoLog = "(sin datos)";
        if (!anioMeses.isEmpty()) {
            int anioMin = anioMeses.firstKey();
            int idxMin = anioMin * 12 + (anioMeses.get(anioMin).first() - 1);
            int anioMax = anioMeses.lastKey();
            int idxMaxData = anioMax * 12 + (anioMeses.get(anioMax).last() - 1);

            // Tope del calendario: el front manda el mes del reporte → (año en curso, mesReporte);
            // si no viene, el último mes con datos. Nunca por debajo del último mes con datos.
            int idxTope = idxMaxData;
            if (mesReporte != null && mesReporte >= 1 && mesReporte <= 12) {
                int anioEnCurso = LocalDate.now().getYear();
                int idxCutoff = anioEnCurso * 12 + (mesReporte - 1);
                idxTope = Math.max(idxCutoff, idxMaxData);
                if (idxCutoff < idxMaxData) {
                    LOGGER.warn("mesReporte={} (año en curso {}) es anterior al último mes con datos {}-{}; "
                                + "se extiende el calendario hasta los datos para no dejar filas FACT huérfanas.",
                                mesReporte, anioEnCurso, anioMax, anioMeses.get(anioMax).last());
                }
            }

            rangoLog = String.format("%d-%02d a %d-%02d", idxMin / 12, idxMin % 12 + 1,
                                     idxTope / 12, idxTope % 12 + 1);

            for (int idx = idxMin; idx <= idxTope; idx++) {
                int anio = idx / 12;
                int mes = idx % 12 + 1;
                org.apache.poi.xssf.usermodel.XSSFRow row = sheet.createRow(rowNum);

                LocalDate fecha = LocalDate.of(anio, mes, 1);
                Date date = Date.from(fecha.atStartOfDay(ZoneId.systemDefault()).toInstant());
                Cell cellFecha = row.createCell(0, CellType.NUMERIC);
                cellFecha.setCellValue(date);
                cellFecha.setCellStyle(dateCellStyle);

                row.createCell(1, CellType.NUMERIC).setCellValue(mes);
                row.createCell(2, CellType.NUMERIC).setCellValue(anio);
                row.createCell(3, CellType.STRING).setCellValue(MESES_ES[mes - 1]);

                // Columna E: Año Fiscal (fórmula con referencia estructurada de tabla).
                // POI's FormulaParser no soporta [@Col] / [@[Col]], así que se escribe
                // el string directo al XML (mismo enfoque que addNewCalculatedColumnFormula
                // en la definición de la tabla). Excel la evalúa al abrir gracias a
                // setForceFormulaRecalculation(true).
                org.apache.poi.xssf.usermodel.XSSFCell cellAnoFiscal =
                        (org.apache.poi.xssf.usermodel.XSSFCell) row.createCell(4);
                cellAnoFiscal.getCTCell().addNewF().setStringValue(formulaAnoFiscal);

                // Columna F: Cotización USD (último día hábil del mes)
                LocalDate ultimoDiaMes = fecha.withDayOfMonth(fecha.lengthOfMonth());
                java.util.Optional<Cotizacion> cotizacion = cotizacionService.obtenerCotizacion("USD", ultimoDiaMes);
                if (cotizacion.isPresent()) {
                    row.createCell(5, CellType.NUMERIC).setCellValue(Math.round(cotizacion.get().getValor().doubleValue()));
                } else {
                    row.createCell(5, CellType.BLANK);
                    LOGGER.warn("Sin cotización USD para {}-{}, celda en blanco.", anio, mes);
                }

                rowNum++;
            }
        }

        int totalRows = rowNum - 1;
        LOGGER.info("Hoja '{}' poblada con {} meses contiguos [{}] (mesReporte={}).",
                    SHEET_CALENDARIO, totalRows, rangoLog, mesReporte);
        return totalRows;
    }

    /**
     * Elimina TODAS las filas de datos (preservando el header en fila 0) de una hoja
     * del XSSFWorkbook. Debe invocarse ANTES de envolver con SXSSFWorkbook, ya que
     * SXSSF trata las filas existentes del XSSFSheet como "ya escritas a disco"
     * y no permite sobrescribirlas con createRow().
     */
    private void limpiarDatosHoja(XSSFWorkbook wb, String sheetName) {
        XSSFSheet sheet = wb.getSheet(sheetName);
        if (sheet == null)
            return;

        int lastOldRow = sheet.getLastRowNum();
        if (lastOldRow < 1)
            return;

        int removed = 0;
        for (int i = lastOldRow; i >= 1; i--) {
            org.apache.poi.xssf.usermodel.XSSFRow row = sheet.getRow(i);
            if (row != null) {
                sheet.removeRow(row);
                removed++;
            }
        }
        LOGGER.info("Hoja '{}': eliminadas {} filas de datos previas.", sheetName, removed);
    }

    /**
     * Recorre todas las tablas de todas las hojas y elimina los atributos
     * tableType="queryTable" y connectionId, convirtiendo las tablas de
     * "conectadas a Power Query" a tablas regulares de Excel.
     * Esto evita el error "DataSource.NotFound" al abrir el reporte generado.
     */
    private void desconectarTablas(XSSFWorkbook wb) {
        int desconectadas = 0;
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            XSSFSheet sheet = wb.getSheetAt(i);
            for (XSSFTable table : sheet.getTables()) {
                org.apache.xmlbeans.XmlCursor cursor = table.getCTTable().newCursor();
                boolean changed = false;
                if (cursor.toFirstAttribute()) {
                    do {
                        String name = cursor.getName().getLocalPart();
                        if ("tableType".equals(name) || "connectionId".equals(name)) {
                            cursor.removeXml();
                            changed = true;
                        }
                    }
                    while (cursor.toNextAttribute());
                }
                cursor.dispose();
                if (changed) {
                    desconectadas++;
                    LOGGER.debug("Tabla '{}' desconectada de Power Query.", table.getName());
                }
            }
        }
        if (desconectadas > 0) {
            LOGGER.info("{} tabla(s) desconectadas de Power Query / Data Model.", desconectadas);
        }
    }

    private void ocultarHoja(SXSSFWorkbook wb, String sheetName) {
        int idx = wb.getSheetIndex(sheetName);
        if (idx >= 0) {
            wb.setSheetVisibility(idx, org.apache.poi.ss.usermodel.SheetVisibility.HIDDEN);
            LOGGER.info("Hoja '{}' (idx={}) marcada como oculta.", sheetName, idx);
        }
        else {
            LOGGER.warn("Hoja '{}' no encontrada para ocultar.", sheetName);
        }
    }

    /**
     * Actualiza los rangos de referencia de las tablas Excel (FACT, Total_Empresa, Calendario)
     * para que coincidan con la cantidad real de filas escritas.
     * Esto es crucial para que los pivot tables del template reconozcan
     * correctamente el nuevo rango de datos.
     */
    /**
     * Marca todas las tablas dinámicas y sus caches para que Excel reconstruya
     * los datos al abrir el archivo, en lugar de usar el cache del template.
     *
     * Se actúa en dos niveles:
     * 1. PivotTable: refreshOnLoad=true → Excel refresca la tabla al abrir.
     * 2. PivotCacheDefinition: refreshOnLoad=true → Excel reconstruye el cache
     *    desde el rango de datos fuente (FACT, Total Empresa, etc.).
     */
    /**
     * Marca todas las tablas dinámicas y sus caches para que Excel reconstruya
     * los datos al abrir el archivo, en lugar de usar el cache del template.
     *
     * Se actúa en dos niveles:
     * 1. PivotCacheDefinition: refreshOnLoad=true -> Excel reconstruye el cache
     *    desde el rango de datos fuente (FACT, Total Empresa, etc.).
     * 2. Workbook: forceFormulaRecalculation=true -> Excel recalcula fórmulas.
     *
     * Nota: Esto funciona para pivot caches basados en rangos de celdas.
     * Para el modelo de datos VertiPaq (medidas DAX), se requiere además
     * el post-procesamiento con refrescarModeloDatos() que usa Excel COM.
     */
    private void refrescarTablasDinamicas(XSSFWorkbook wb) {
        int ptCount = 0;
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            ptCount += wb.getSheetAt(i).getPivotTables().size();
        }

        // Buscar pivot caches en relaciones directas del workbook
        int cacheCount = 0;
        for (org.apache.poi.ooxml.POIXMLDocumentPart part : wb.getRelations()) {
            if (part instanceof org.apache.poi.xssf.usermodel.XSSFPivotCacheDefinition) {
                org.apache.poi.xssf.usermodel.XSSFPivotCacheDefinition cacheDef =
                        (org.apache.poi.xssf.usermodel.XSSFPivotCacheDefinition) part;
                cacheDef.getCTPivotCacheDefinition().setRefreshOnLoad(true);
                cacheCount++;
            }
        }

        // Fallback: buscar recursivamente en partes hijas si no se encontraron en nivel directo
        if (cacheCount == 0) {
            for (org.apache.poi.ooxml.POIXMLDocumentPart part : wb.getRelations()) {
                for (org.apache.poi.ooxml.POIXMLDocumentPart child : part.getRelations()) {
                    if (child instanceof org.apache.poi.xssf.usermodel.XSSFPivotCacheDefinition) {
                        org.apache.poi.xssf.usermodel.XSSFPivotCacheDefinition cacheDef =
                                (org.apache.poi.xssf.usermodel.XSSFPivotCacheDefinition) child;
                        cacheDef.getCTPivotCacheDefinition().setRefreshOnLoad(true);
                        cacheCount++;
                    }
                }
            }
        }

        // Forzar recálculo global de fórmulas
        wb.setForceFormulaRecalculation(true);

        LOGGER.info("Pivot tables: {} encontradas, {} pivot caches marcados refreshOnLoad=true",
                    ptCount, cacheCount);
    }

    /**
     * Post-procesa el Excel generado invocando PowerShell + COM de Excel
     * para forzar RefreshAll y reconstruir el modelo de datos VertiPaq.
     *
     * Esto es necesario porque Apache POI no puede manipular el modelo de datos
     * binario (xl/model/item.data) que contiene las medidas DAX.
     * Sin este paso, medidas como M_YTD MODELO y M_MAESTRA_ACUM fallan
     * con error de dependencia al usar filtros/slicers.
     *
     * @param rutaExcel ruta absoluta al archivo .xlsx generado
     */
    private void refrescarModeloDatos(String rutaExcel) {
        if (!refreshEnabled) {
            LOGGER.info("Refresh del modelo de datos deshabilitado (reporte.refresh.enabled=false)");
            return;
        }

        // Validar existencia del script ANTES de invocar cscript. Sin //B, cscript
        // ya reporta "Input Error: There is no script file specified.", pero validar
        // acá da un mensaje más claro con la ruta absoluta resuelta.
        File scriptFile = new File(refreshScriptPath);
        if (!scriptFile.exists()) {
            LOGGER.error("Script de refresh no existe: {} (configurado en reporte.refresh.script.path={})",
                         scriptFile.getAbsolutePath(), refreshScriptPath);
            throw new RuntimeException("Script de refresh no existe: " + scriptFile.getAbsolutePath());
        }

        // Limpieza preventiva: cualquier EXCEL.EXE huérfano de una corrida anterior
        // hace que Workbooks.Open devuelva Nothing sin levantar error (síntoma:
        // log con "ERROR 1004 No se puede obtener la propiedad Open"). Matarlo
        // antes de invocar cscript evita ese escenario.
        matarExcelHuerfano();

        LOGGER.info("Iniciando refresh del modelo de datos (timeout={}s, script={}): {}",
                    refreshTimeoutSeconds, scriptFile.getAbsolutePath(), rutaExcel);
        long inicio = System.currentTimeMillis();

        Process process = null;
        Thread drainer = null;
        final StringBuilder output = new StringBuilder();

        try {
            // VBScript + cscript.exe en lugar de PowerShell: VBS corre nativamente
            // en STA (Single-Threaded Apartment), evitando el error COM
            // "No se puede obtener la propiedad Open de la clase Workbooks"
            // que ocurre con PowerShell desde procesos de servicio.
            //
            // NO usar //B (batch mode): suprime mensajes de error del propio cscript
            // (script no encontrado, error de sintaxis), dejando exit=1 con stdout
            // vacío e imposible de diagnosticar. Como redirigimos stderr→stdout y
            // no hay UI interactiva en servicio, //B no aporta nada.
            ProcessBuilder pb = new ProcessBuilder(
                "cscript.exe",
                "//Nologo",
                scriptFile.getAbsolutePath(),
                rutaExcel
            );
            pb.redirectErrorStream(true);

            process = pb.start();

            // Drenar stdout en thread aparte. Sin esto, reader.lines() bloquea hasta EOF
            // del proceso, lo que impide que waitFor con timeout aborte un cscript colgado
            // (y deja el hilo del async pool detenido sin que nadie se entere).
            final Process p = process;
            drainer = new Thread(() -> {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(p.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        synchronized (output) {
                            output.append(line).append('\n');
                        }
                    }
                } catch (IOException ignore) {
                    // El stream se cierra al matar el proceso; no es error.
                }
            }, "refresh-excel-stdout");
            drainer.setDaemon(true);
            drainer.start();

            boolean finished = process.waitFor(refreshTimeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                LOGGER.error("Timeout ({}s) al refrescar modelo de datos. Proceso cscript abortado: {}",
                             refreshTimeoutSeconds, rutaExcel);
                // destroyForcibly mata cscript.exe pero NO mata el EXCEL.EXE hijo que
                // cscript spawneó vía COM. Sin esto, EXCEL.EXE queda como zombi y puede
                // impedir que la próxima corrida abra el archivo
                // ("No se puede obtener la propiedad Open de la clase Workbooks").
                matarExcelHuerfano();
                throw new RuntimeException("Timeout (" + refreshTimeoutSeconds
                        + "s) al refrescar modelo de datos en Excel COM");
            }

            // Asegurar que el drainer termine de copiar los últimos bytes tras EOF
            drainer.join(5000);

            if (process.exitValue() != 0) {
                String out = output.toString().trim();
                LOGGER.error("Error al refrescar modelo de datos (exit={}): {}",
                            process.exitValue(), out);
                // Sin esto, el informe quedaba COMPLETADO con un xlsx no refrescado
                // y el usuario veía el error de dependencia DAX al abrirlo. Mejor
                // marcar ERROR y forzar reintento manual.
                throw new RuntimeException("Refresh del modelo de datos falló (exit="
                        + process.exitValue() + "): " + out);
            }

            long duracion = (System.currentTimeMillis() - inicio) / 1000;
            LOGGER.info("Modelo de datos refrescado en {}s. Output: {}", duracion, output.toString().trim());

        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            LOGGER.error("Excepción al refrescar modelo de datos: {}", e.getMessage(), e);
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
                matarExcelHuerfano();
            }
            throw new RuntimeException("Excepción al refrescar modelo de datos: " + e.getMessage(), e);
        }
    }

    /**
     * Setea refreshOnLoad="1" en la conexión del Data Model (ThisWorkbookDataModel)
     * dentro de xl/connections.xml del .xlsx generado.
     *
     * Sin esto, abrir el archivo refresca los pivot caches pero NO el modelo VertiPaq,
     * dejando las medidas DAX (M_YTD MODELO, M_MAESTRA_ACUM, etc.) apuntando a un
     * cache binario calibrado para el row-count del template original. Resultado:
     * "La medida X depende de Y que tiene un error de dependencia".
     *
     * Con refreshOnLoad=1, al abrir el archivo:
     *   1. Excel reconstruye VertiPaq desde las queries internas del modelo.
     *   2. Esas queries leen las tablas FACT/Calendario/Total Empresa con la data nueva.
     *   3. Pivot caches refrescan después contra el modelo fresco.
     *
     * Caveat para el usuario: primera apertura del archivo muestra barra "Habilitar
     * contenido" (security trust). Después de ese clic único, Excel lo marca como
     * trusted document y no vuelve a preguntar para ese archivo.
     *
     * Reemplaza el post-procesamiento server-side con Excel COM (scripts/refresh-excel.vbs)
     * que es frágil bajo contexto de servicio Windows.
     */
    private void forzarRefreshOnLoadDataModel(String rutaExcel) {
        final String ns = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
        URI zipUri = URI.create("jar:" + Paths.get(rutaExcel).toUri());

        try (FileSystem fs = FileSystems.newFileSystem(zipUri, Collections.<String, Object>emptyMap())) {
            Path connectionsPath = fs.getPath("xl/connections.xml");
            if (!Files.exists(connectionsPath)) {
                LOGGER.info("xl/connections.xml no presente, refreshOnLoad omitido");
                return;
            }

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document doc;
            try (InputStream is = Files.newInputStream(connectionsPath)) {
                doc = dbf.newDocumentBuilder().parse(is);
            }

            NodeList connections = doc.getElementsByTagNameNS(ns, "connection");
            int modificadas = 0;
            int dataModelFound = 0;

            for (int i = 0; i < connections.getLength(); i++) {
                Element conn = (Element) connections.item(i);

                // Identificar la conexión del Data Model. Excel la marca de dos formas:
                // (a) <dbPr command="Model" commandType="1"/> — formato base spreadsheetml.
                // (b) <x15:connection model="1"/> dentro de extLst — formato Office 2013+.
                // Aceptar cualquiera para ser robustos a versiones de Excel distintas.
                boolean isDataModel = false;
                NodeList dbPrList = conn.getElementsByTagNameNS(ns, "dbPr");
                for (int j = 0; j < dbPrList.getLength(); j++) {
                    Element dbPr = (Element) dbPrList.item(j);
                    if ("Model".equals(dbPr.getAttribute("command"))) {
                        isDataModel = true;
                        break;
                    }
                }
                if (!isDataModel) {
                    NodeList x15ConnList = conn.getElementsByTagNameNS(
                            "http://schemas.microsoft.com/office/spreadsheetml/2010/11/main",
                            "connection");
                    for (int j = 0; j < x15ConnList.getLength(); j++) {
                        Element x15Conn = (Element) x15ConnList.item(j);
                        if ("1".equals(x15Conn.getAttribute("model"))) {
                            isDataModel = true;
                            break;
                        }
                    }
                }

                if (!isDataModel) continue;

                dataModelFound++;
                if (!"1".equals(conn.getAttribute("refreshOnLoad"))) {
                    conn.setAttribute("refreshOnLoad", "1");
                    modificadas++;
                    LOGGER.info("refreshOnLoad=1 seteado en conexión Data Model id={}",
                                conn.getAttribute("id"));
                }
            }

            if (dataModelFound == 0) {
                LOGGER.warn("No se encontró conexión Data Model en xl/connections.xml — "
                            + "las medidas DAX podrían no refrescarse al abrir el archivo");
                return;
            }
            if (modificadas == 0) {
                LOGGER.info("Conexión Data Model ya tenía refreshOnLoad=1, nada que modificar");
                return;
            }

            Transformer tf = TransformerFactory.newInstance().newTransformer();
            tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            try (OutputStream os = Files.newOutputStream(connectionsPath,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                tf.transform(new DOMSource(doc), new StreamResult(os));
            }
            LOGGER.info("xl/connections.xml actualizado: {} conexión(es) Data Model con refreshOnLoad=1",
                        modificadas);

        } catch (Exception e) {
            // No fatal: si falla esto el archivo se entrega de todas formas, solo que
            // el usuario va a tener que hacer "Datos → Actualizar Todo" manualmente.
            LOGGER.warn("No se pudo setear refreshOnLoad en Data Model (no fatal): {}",
                        e.getMessage(), e);
        }
    }

    private void matarExcelHuerfano() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return;
        }
        try {
            Process kill = new ProcessBuilder("taskkill", "/F", "/IM", "EXCEL.EXE")
                    .redirectErrorStream(true)
                    .start();
            boolean ok = kill.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
            if (!ok) {
                kill.destroyForcibly();
                LOGGER.warn("taskkill EXCEL.EXE no terminó en 10s, abortado");
                return;
            }
            int code = kill.exitValue();
            // taskkill devuelve 128 cuando no hay procesos que matar; lo tratamos como OK.
            if (code == 0) {
                LOGGER.warn("EXCEL.EXE huérfano detectado y terminado (taskkill exit=0)");
            } else {
                LOGGER.info("taskkill EXCEL.EXE exit={} (sin procesos huérfanos)", code);
            }
        } catch (Exception ex) {
            LOGGER.warn("No se pudo ejecutar taskkill EXCEL.EXE: {}", ex.getMessage());
        }
    }

    private void actualizarRangosTablas(XSSFWorkbook templateWb, int dataRows, int calendarRows,
                                        TipoReporte tipoReporte, boolean tieneSubMarca) {
        String lastColFact;
        String lastColTotalEmpresa;
        if (tieneSubMarca) {
            lastColFact = tipoReporte == TipoReporte.CADENA ? "S" : "R";
            lastColTotalEmpresa = tipoReporte == TipoReporte.CADENA ? "Q" : "P";
        } else {
            lastColFact = tipoReporte == TipoReporte.CADENA ? "R" : "Q";
            lastColTotalEmpresa = tipoReporte == TipoReporte.CADENA ? "P" : "O";
        }
        actualizarTabla(templateWb, SHEET_FACT, "FACT", dataRows, lastColFact);
        actualizarTabla(templateWb, SHEET_TOTAL_EMPRESA, "Total_Empresa", dataRows, lastColTotalEmpresa);
        actualizarTabla(templateWb, SHEET_CALENDARIO, "Calendario", calendarRows, "F");
    }

    private void actualizarTabla(XSSFWorkbook wb, String sheetName, String tableName,
                                 int dataRows, String lastCol) {
        XSSFSheet sheet = wb.getSheet(sheetName);
        if (sheet == null)
            return;
        if (dataRows <= 0) {
            LOGGER.warn("Tabla '{}': 0 filas de datos, se omite actualización de rango.", tableName);
            return;
        }

        for (XSSFTable table : sheet.getTables()) {
            if (table.getName().equals(tableName)) {
                int lastRow = dataRows + 1; // +1 por el header
                CellReference start = new CellReference(0, 0);
                CellReference end = new CellReference(lastRow - 1,
                                                      CellReference.convertColStringToIndex(lastCol));
                AreaReference newArea = new AreaReference(start, end, wb.getSpreadsheetVersion());
                table.setArea(newArea);
                LOGGER.info("Tabla '{}' actualizada: ref={}", tableName, newArea.formatAsString());
                return;
            }
        }
        LOGGER.warn("Tabla '{}' no encontrada en hoja '{}'.", tableName, sheetName);
    }

    // -------------------------------------------------------------------------
    // Resolución de archivos base por cliente
    // -------------------------------------------------------------------------

    private static final String FILTROS_BASE_FILENAME = "filtros_base.csv";

    /** Genera el nombre de archivo de datos base según el tipo de reporte: datos_base_normal.csv / datos_base_cadena.csv */
    public static String datosBaseFilename(TipoReporte tipo) {
        return "datos_base_" + tipo.name().toLowerCase() + ".csv";
    }


    /** Directorio de archivos base para un cliente: {server}/{clientesSubdir}/{codCliente}/ */
    public String getClienteDir(String codCliente) {
        return directorioServer + File.separator + insightsClientesSubdir
               + File.separator + codCliente.trim().toUpperCase();
    }

    /**
     * Resuelve los filtros a utilizar, con la siguiente prioridad:
     * 1. CSV subido por el usuario (filtroBytes != null)
     * 2. Filtro base del cliente en disco ({clienteDir}/filtros_base.csv)
     * 3. Fallback: filtros.csv del classpath
     */
    private List<Map<String, String>> resolverFiltros(byte[] filtroBytes, String codCliente)
                                                                                             throws IOException,
                                                                                             CsvValidationException {

        if (filtroBytes != null && filtroBytes.length > 0) {
            LOGGER.info("Usando filtros subidos por el usuario");
            return leerFiltrosDesdeBytes(filtroBytes);
        }

        File filtroBase = Paths.get(getClienteDir(codCliente), FILTROS_BASE_FILENAME).toFile();
        if (filtroBase.exists()) {
            LOGGER.info("Usando filtro base del cliente en disco: {}", filtroBase.getAbsolutePath());
            return leerFiltrosDesdeBytes(Files.readAllBytes(filtroBase.toPath()));
        }

        LOGGER.info("Filtro base del cliente no encontrado, usando classpath: {}", FILTROS_CSV_PATH);
        return leerFiltrosDesdeResources();
    }

    /**
     * Lee el CSV de datos base del cliente desde disco, específico al tipo de reporte.
     * Busca primero el archivo con sufijo de tipo (datos_base_normal.csv / datos_base_cadena.csv).
     * Como fallback busca el antiguo datos_base.csv (para compatibilidad con datos existentes).
     */
    private byte[] leerDatosBase(String codCliente, TipoReporte tipoReporte) {
        String clienteDir = getClienteDir(codCliente);

        // Buscar primero el archivo específico por tipo
        File datosBase = Paths.get(clienteDir, datosBaseFilename(tipoReporte)).toFile();
        if (!datosBase.exists()) {
            // Fallback al archivo antiguo sin sufijo de tipo (compatibilidad)
            datosBase = Paths.get(clienteDir, "datos_base.csv").toFile();
        }

        LOGGER.info("Buscando datos base en: {}", datosBase.getAbsolutePath());
        if (!datosBase.exists()) {
            LOGGER.info("No existe datos base para cliente {} ({}); se usará solo el CSV del usuario.",
                        codCliente, tipoReporte);
            return null;
        }
        try {
            byte[] bytes = Files.readAllBytes(datosBase.toPath());
            LOGGER.info("Datos base leídos para cliente {}: {} bytes", codCliente, bytes.length);
            return bytes;
        }
        catch (IOException e) {
            throw new RuntimeException("Error al leer datos base del cliente " + codCliente + ": " + e.getMessage(), e);
        }
    }

    /**
     * Concatena dos CSV: toma el header + filas del base, y luego solo las filas (sin header) del
     * usuario.
     * Ambos deben tener el mismo separador (;) y codificación (ISO-8859-1).
     */
    private byte[] concatenarCsvData(byte[] datosBase, byte[] datosUsuario) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(datosBase);
        // Asegurar que el base termina con salto de línea
        if (datosBase.length > 0 && datosBase[datosBase.length - 1] != '\n') {
            out.write('\n');
        }
        // Eliminar la primera línea (header) del CSV del usuario trabajando en bytes
        // para evitar problemas de codificación (UTF-8 vs ISO-8859-1).
        int firstNewline = -1;
        for (int i = 0; i < datosUsuario.length; i++) {
            if (datosUsuario[i] == '\n') {
                firstNewline = i;
                break;
            }
        }
        if (firstNewline >= 0 && firstNewline < datosUsuario.length - 1) {
            out.write(datosUsuario, firstNewline + 1, datosUsuario.length - firstNewline - 1);
        }
        return out.toByteArray();
    }

    /**
     * Guarda el CSV concatenado como datos_base del cliente, específico al tipo de reporte.
     * Archivo: datos_base_normal.csv o datos_base_cadena.csv.
     * Utiliza escritura atómica (temp + rename) para evitar corrupción.
     */
    private void guardarDatosBase(String codCliente, TipoReporte tipoReporte, byte[] csvConcatenado) {
        try {
            java.nio.file.Path clienteDir = Paths.get(getClienteDir(codCliente));
            if (!Files.exists(clienteDir)) {
                Files.createDirectories(clienteDir);
            }
            String filename = datosBaseFilename(tipoReporte);
            java.nio.file.Path destino = clienteDir.resolve(filename);
            java.nio.file.Path temp = clienteDir.resolve(filename + ".tmp");
            Files.write(temp, csvConcatenado);
            Files.move(temp, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            LOGGER.info("Datos base ({}) actualizados para cliente {}: {} bytes",
                        tipoReporte, codCliente, csvConcatenado.length);
        }
        catch (IOException e) {
            LOGGER.error("Error al guardar datos base del cliente {}: {}", codCliente, e.getMessage(), e);
        }
    }

    /**
     * Guarda un archivo base (filtros o datos) subido por administración.
     */
    public String guardarArchivoBase(MultipartFile archivo, String codCliente, String nombreArchivo) {
        try {
            java.nio.file.Path clienteDir = Paths.get(getClienteDir(codCliente));
            if (!Files.exists(clienteDir)) {
                Files.createDirectories(clienteDir);
            }
            java.nio.file.Path destino = clienteDir.resolve(nombreArchivo);
            archivo.transferTo(destino.toFile());
            LOGGER.info("Archivo base '{}' guardado para cliente {}: {}", nombreArchivo, codCliente, destino);
            return destino.toAbsolutePath().toString();
        }
        catch (IOException e) {
            throw new RuntimeException("Error al guardar archivo base: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina el archivo datos_base del cliente para el tipo de reporte indicado.
     *
     * @param codCliente  código del cliente
     * @param tipoReporte tipo de reporte (para resolver datos_base_normal.csv o datos_base_cadena.csv)
     * @return mensaje indicando si se eliminó o no existía
     */
    public String eliminarDatosBase(String codCliente, TipoReporte tipoReporte) {
        String nombreArchivo = datosBaseFilename(tipoReporte);
        File archivo = Paths.get(getClienteDir(codCliente), nombreArchivo).toFile();

        if (archivo.exists()) {
            if (archivo.delete()) {
                LOGGER.info("Archivo eliminado: {}", archivo.getAbsolutePath());
                return nombreArchivo + " eliminado";
            } else {
                LOGGER.error("No se pudo eliminar el archivo: {}", archivo.getAbsolutePath());
                return nombreArchivo + ": error al eliminar";
            }
        } else {
            LOGGER.info("Archivo no existe, nada que eliminar: {}", archivo.getAbsolutePath());
            return nombreArchivo + " no existía";
        }
    }

    // -------------------------------------------------------------------------
    // Lectura y filtrado de CSV
    // -------------------------------------------------------------------------

    private List<Map<String, String>> leerFiltrosDesdeResources() throws IOException, CsvValidationException {
        ClassPathResource resource = new ClassPathResource(FILTROS_CSV_PATH);
        List<Map<String, String>> filtros = new ArrayList<>();

        try (CSVReader reader = new CSVReaderBuilder(
                                                     new InputStreamReader(resource.getInputStream(),
                                                                           StandardCharsets.UTF_8))
                                                                                                   .withCSVParser(new CSVParserBuilder().withSeparator(CSV_SEPARATOR)
                                                                                                                                        .build())
                                                                                                   .build()) {

            String[] headers = reader.readNext();
            if (headers == null)
                return filtros;

            String[] fila;
            while ((fila = reader.readNext()) != null) {
                Map<String, String> regla = new HashMap<>();
                for (int i = 0; i < headers.length && i < fila.length; i++) {
                    String valor = fila[i].trim();
                    if (!valor.isEmpty()) {
                        regla.put(headers[i].trim(), valor);
                    }
                }
                if (!regla.isEmpty())
                    filtros.add(regla);
            }
        }
        return filtros;
    }

    /**
     * Lee los filtros desde los bytes de un CSV subido por el usuario.
     * El formato esperado es el mismo que el de resources/insights/filtros.csv.
     */
    private List<Map<String, String>> leerFiltrosDesdeBytes(byte[] filtroBytes) throws IOException,
                                                                                CsvValidationException {
        List<Map<String, String>> filtros = new ArrayList<>();

        Charset encoding = detectarEncoding(filtroBytes);
        try (CSVReader reader = new CSVReaderBuilder(
                                                     new InputStreamReader(new ByteArrayInputStream(filtroBytes),
                                                                           encoding))
                                                                                                   .withCSVParser(new CSVParserBuilder().withSeparator(CSV_SEPARATOR)
                                                                                                                                        .build())
                                                                                                   .build()) {

            String[] headers = reader.readNext();
            if (headers == null)
                return filtros;

            String[] fila;
            while ((fila = reader.readNext()) != null) {
                Map<String, String> regla = new HashMap<>();
                for (int i = 0; i < headers.length && i < fila.length; i++) {
                    String valor = fila[i].trim();
                    if (!valor.isEmpty()) {
                        regla.put(headers[i].trim(), valor);
                    }
                }
                if (!regla.isEmpty())
                    filtros.add(regla);
            }
        }
        return filtros;
    }

    /**
     * Lee el CSV de datos y retorna las filas que cumplen los filtros.
     * La primera fila retornada siempre es el header.
     */
    private List<String[]> leerYFiltrarCsvData(byte[] csvBytes,
                                               List<Map<String, String>> filtros)
                                                                                  throws IOException,
                                                                                  CsvValidationException {

        List<String[]> resultado = new ArrayList<>();

        // Diagnóstico: primeros 100 bytes del CSV para determinar encoding
//        int diagLen = Math.min(100, csvBytes.length);
//        StringBuilder hexDump = new StringBuilder();
//        StringBuilder charDump = new StringBuilder();
//        for (int i = 0; i < diagLen; i++) {
//            int b = csvBytes[i] & 0xFF;
//            hexDump.append(String.format("%02X ", b));
//            charDump.append((b >= 32 && b < 127) ? (char) b : '.');
//        }
//        LOGGER.info("CSV encoding diagnóstico - primeros {} bytes:", diagLen);
//        LOGGER.info("  HEX:  {}", hexDump.toString().trim());
//        LOGGER.info("  TEXT: {}", charDump);

        Charset encoding = detectarEncoding(csvBytes);
        try (CSVReader reader = new CSVReaderBuilder(
                                                     new InputStreamReader(new ByteArrayInputStream(csvBytes),
                                                                           encoding))
                                                                                                   .withCSVParser(new CSVParserBuilder().withSeparator(CSV_SEPARATOR)
                                                                                                                                        .build())
                                                                                                   .build()) {

            String[] headers = reader.readNext();
            if (headers == null)
                return resultado;

            for (int i = 0; i < headers.length; i++)
                headers[i] = headers[i].trim();
            resultado.add(headers);

            LOGGER.info("Headers del CSV de datos: {} columnas", headers.length);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("  columnas: {}", String.join(" | ", headers));
            }

            Map<String, Integer> colIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++)
                colIndex.put(normalizar(headers[i]), i);

            String[] fila;
            while ((fila = reader.readNext()) != null) {
                if (filaCumpleFiltros(fila, colIndex, filtros))
                    resultado.add(fila);
            }
        }
        return resultado;
    }

    /**
     * Retorna true si la fila cumple al menos una regla de filtro (OR entre filas, AND entre
     * columnas).
     */
    private boolean filaCumpleFiltros(String[] fila, Map<String, Integer> colIndex,
                                      List<Map<String, String>> filtros) {
        if (filtros.isEmpty())
            return true;

        for (Map<String, String> regla : filtros) {
            boolean cumple = true;
            for (Map.Entry<String, String> condicion : regla.entrySet()) {
                Integer idx = colIndex.get(normalizar(condicion.getKey()));
                if (idx == null) {
                    continue; // Clave del filtro no existe en el CSV de datos (ej: AGR_SEGM, Orden_Apertura); se ignora
                }
                if (idx >= fila.length) {
                    cumple = false;
                    break;
                }
                if (!normalizar(fila[idx]).equals(normalizar(condicion.getValue()))) {
                    cumple = false;
                    break;
                }
            }
            if (cumple)
                return true;
        }
        return false;
    }

    /**
     * Busca un valor en una regla de filtro por nombre de clave, comparando de forma normalizada.
     */
    private String findFilterValue(Map<String, String> regla, String key) {
        String normKey = normalizar(key);
        for (Map.Entry<String, String> entry : regla.entrySet()) {
            if (normalizar(entry.getKey()).equals(normKey)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Construye un mapa de lookup a partir de los filtros, asociando los valores normalizados
     * de keyColumn con los valores originales de valueColumn.
     * Ejemplo: buildLookupMap(filtros, "APERTURA", "Orden_Apertura") → {"total paraguay" → "1", ...}
     */
    private Map<String, String> buildLookupMap(List<Map<String, String>> filtros,
                                                String keyColumn, String valueColumn) {
        Map<String, String> map = new HashMap<>();
        for (Map<String, String> regla : filtros) {
            String key = findFilterValue(regla, keyColumn);
            String value = findFilterValue(regla, valueColumn);
            if (key != null && value != null) {
                map.put(normalizar(key), value);
            }
        }
        return map;
    }

    /**
     * Normaliza un valor para comparaciones insensibles a mayúsculas/minúsculas y acentos.
     * <p>
     * Usa descomposición Unicode NFD para separar los caracteres base de sus diacríticos
     * (tildes, diéresis, cedilla, etc.) y luego los elimina con una expresión regular.
     * Esto cubre todos los caracteres del español (á,é,í,ó,ú,ñ,ü,Á,É...) y cualquier
     * otro idioma, sin necesidad de reemplazos individuales.
     *
     * @param valor cadena a normalizar; null devuelve cadena vacía
     * @return cadena sin acentos, en minúsculas y sin espacios extremos
     */
    private String normalizar(String valor) {
        if (valor == null) return "";
        return java.text.Normalizer
                .normalize(valor.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}", "")
                .toLowerCase();
    }

    // -------------------------------------------------------------------------
    // Detección de encoding
    // -------------------------------------------------------------------------

    /**
     * Detecta el encoding de un array de bytes CSV.
     * <ol>
     *   <li>Si comienza con BOM UTF-8 (EF BB BF), retorna UTF-8.</li>
     *   <li>Si todos los bytes forman una secuencia UTF-8 válida y hay al menos
     *       un carácter multibyte (&gt; 0x7F), retorna UTF-8.</li>
     *   <li>Si hay bytes en el rango 0x80-0x9F rodeados de letras ASCII
     *       (típico de letras acentuadas MacRoman dentro de palabras como
     *       "Categor[0x92=í]a"), retorna MacRoman.</li>
     *   <li>En caso contrario retorna cp1252 (Windows-1252), que es el encoding
     *       por defecto de Excel en Windows.</li>
     * </ol>
     */
    private Charset detectarEncoding(byte[] bytes) {
        // 1. BOM UTF-8
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) {
            LOGGER.info("Encoding detectado: UTF-8 (BOM)");
            return StandardCharsets.UTF_8;
        }

        // 2. Validar si es UTF-8 con secuencias multibyte
        boolean hayMultibyte = false;
        int i = 0;
        while (i < bytes.length) {
            int b = bytes[i] & 0xFF;
            if (b <= 0x7F) {
                i++;
            } else if (b >= 0xC2 && b <= 0xDF) {
                if (i + 1 >= bytes.length || (bytes[i + 1] & 0xC0) != 0x80) break;
                hayMultibyte = true;
                i += 2;
            } else if (b >= 0xE0 && b <= 0xEF) {
                if (i + 2 >= bytes.length || (bytes[i + 1] & 0xC0) != 0x80 || (bytes[i + 2] & 0xC0) != 0x80) break;
                hayMultibyte = true;
                i += 3;
            } else if (b >= 0xF0 && b <= 0xF4) {
                if (i + 3 >= bytes.length || (bytes[i + 1] & 0xC0) != 0x80
                        || (bytes[i + 2] & 0xC0) != 0x80 || (bytes[i + 3] & 0xC0) != 0x80) break;
                hayMultibyte = true;
                i += 4;
            } else {
                break;
            }
        }
        if (i == bytes.length && hayMultibyte) {
            LOGGER.info("Encoding detectado: UTF-8 (heurística)");
            return StandardCharsets.UTF_8;
        }

        // 3. Distinguir MacRoman vs cp1252
        // En MacRoman, los bytes 0x80-0x9F son letras acentuadas (á=0x87, é=0x8E, í=0x92,
        // ó=0x97, ú=0x9C, ñ=0x96) que aparecen DENTRO de palabras, rodeadas de letras ASCII.
        // En cp1252, esos mismos bytes son comillas tipográficas, guiones em/en, etc.,
        // que aparecen entre palabras, no dentro.
        int macRomanHits = 0;
        for (int j = 1; j < bytes.length - 1; j++) {
            int b = bytes[j] & 0xFF;
            if (b >= 0x80 && b <= 0x9F) {
                int prev = bytes[j - 1] & 0xFF;
                int next = bytes[j + 1] & 0xFF;
                boolean prevEsLetra = (prev >= 'A' && prev <= 'Z') || (prev >= 'a' && prev <= 'z');
                boolean nextEsLetra = (next >= 'A' && next <= 'Z') || (next >= 'a' && next <= 'z');
                if (prevEsLetra && nextEsLetra) {
                    macRomanHits++;
                }
            }
        }
        if (macRomanHits > 0) {
            LOGGER.info("Encoding detectado: MacRoman ({} bytes acentuados dentro de palabras)", macRomanHits);
            return Charset.forName("MacRoman");
        }

        LOGGER.info("Encoding detectado: cp1252 (fallback Windows)");
        return Charset.forName("cp1252");
    }

    // -------------------------------------------------------------------------
    // Helpers de escritura de celdas
    // -------------------------------------------------------------------------

    private void setCellString(Row row, int col, String value) {
        Cell cell = row.createCell(col, CellType.STRING);
        cell.setCellValue(value != null ? value.trim() : "");
    }

    /**
     * Escribe un valor numérico. Los decimales del CSV vienen con coma (formato europeo).
     * Si el valor está vacío o no es parseable, escribe la celda en blanco.
     */
    private void setCellNumeric(Row row, int col, String value) {
        if (value == null || value.trim().isEmpty()) {
            row.createCell(col, CellType.BLANK);
            return;
        }
        try {
            double d = Double.parseDouble(value.trim().replace(",", "."));
            row.createCell(col, CellType.NUMERIC).setCellValue(d);
        }
        catch (NumberFormatException e) {
            row.createCell(col, CellType.STRING).setCellValue(value.trim());
        }
    }

    /**
     * Combina el Mes (1-12) y Año del CSV en una celda de fecha Excel (primer día del mes).
     */
    private void setCellDate(Row row, int col, String mes, String ano, CellStyle dateCellStyle) {
        try {
            int m = Integer.parseInt(mes.trim());
            int y = Integer.parseInt(ano.trim());
            LocalDate localDate = LocalDate.of(y, m, 1);
            Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Cell cell = row.createCell(col, CellType.NUMERIC);
            cell.setCellValue(date);
            cell.setCellStyle(dateCellStyle);
        }
        catch (Exception e) {
            row.createCell(col, CellType.BLANK);
        }
    }

    /** Escribe un entero o BLANK si es null. Usado para flags como YTD 1er Mes. */
    private void setCellInt(Row row, int col, Integer value) {
        if (value == null) {
            row.createCell(col, CellType.BLANK);
        } else {
            row.createCell(col, CellType.NUMERIC).setCellValue(value.doubleValue());
        }
    }

    /**
     * Lee la fila 1 (headers) de una hoja y devuelve un Map header-normalizado → índice de columna (0-based).
     * Resuelve el desfase de columnas entre el código y el template: el código escribe por nombre
     * de header, no por posición fija, así sobrevive si el template reordena/agrega columnas.
     */
    private Map<String, Integer> buildHeaderIndexMap(Sheet sheet) {
        Map<String, Integer> map = new HashMap<>();
        Row header = sheet.getRow(0);
        if (header == null) return map;
        for (Cell cell : header) {
            if (cell == null) continue;
            String name;
            switch (cell.getCellType()) {
                case STRING:  name = cell.getStringCellValue(); break;
                case NUMERIC: name = String.valueOf(cell.getNumericCellValue()); break;
                default: continue;
            }
            if (name == null || name.trim().isEmpty()) continue;
            map.put(normalizar(name), cell.getColumnIndex());
        }
        return map;
    }

    private Integer headerCol(Map<String, Integer> headers, String headerName) {
        return headers.get(normalizar(headerName));
    }

    private void setCellStringByHeader(Row row, Map<String, Integer> headers, String headerName, String value) {
        Integer col = headerCol(headers, headerName);
        if (col != null) setCellString(row, col, value);
    }

    private void setCellNumericByHeader(Row row, Map<String, Integer> headers, String headerName, String value) {
        Integer col = headerCol(headers, headerName);
        if (col != null) setCellNumeric(row, col, value);
    }

    private void setCellIntByHeader(Row row, Map<String, Integer> headers, String headerName, Integer value) {
        Integer col = headerCol(headers, headerName);
        if (col != null) setCellInt(row, col, value);
    }

    private void setCellDateByHeader(Row row, Map<String, Integer> headers, String headerName,
                                     String mes, String ano, CellStyle dateCellStyle) {
        Integer col = headerCol(headers, headerName);
        if (col != null) setCellDate(row, col, mes, ano, dateCellStyle);
    }

    // -------------------------------------------------------------------------
    // Derivaciones
    // -------------------------------------------------------------------------

    /**
     * Deriva el Agrupador Segmento a partir del valor del Segmento.
     * "TOT.PROD." → "TOT.PROD." | cualquier otro → "POR TIPO"
     */
    private String derivarAgrupadorSegmento(String segmento) {
        if (segmento == null)
            return "POR TIPO";
        return "TOT.PROD.".equalsIgnoreCase(segmento.trim()) ? "TOT.PROD." : "POR TIPO";
    }

    /**
     * Busca la columna SUB_MARCA en los headers del CSV de datos (case-insensitive).
     *
     * @param headers array de nombres de columna del CSV
     * @return índice de la columna SUB_MARCA, o -1 si no existe
     */
    private int detectarColumnaSubMarca(String[] headers) {
        String target = normalizar("SUB_MARCA");
        for (int i = 0; i < headers.length; i++) {
            if (target.equals(normalizar(headers[i]))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * YTD 1er Mes: 1 (Integer) si el mes del registro coincide con el inicio del año fiscal,
     * null en caso contrario. Devuelve Integer (no String) porque la columna en el template
     * está tipada como NÚMERO; escribirla como texto rompe la fórmula DAX
     * {@code DATEADD('FACT'[Fecha]; -MAXX('FACT'; [YTD 1er Mes])+1; MONTH)}.
     */
    private Integer derivarYtdInt(String mes, int mesInicioFiscal) {
        return String.valueOf(mesInicioFiscal).equals(mes != null ? mes.trim() : "") ? Integer.valueOf(1) : null;
    }

    // -------------------------------------------------------------------------
    // Persistencia en disco
    // -------------------------------------------------------------------------

    private String guardarEnDisco(Workbook workbook, String nombreArchivo) throws IOException {
        String dirReportes = directorioServer + File.separator + insightsReportsSubdir;
        File directorio = new File(dirReportes);
        if (!directorio.exists())
            directorio.mkdirs();

        String rutaCompleta = dirReportes + File.separator + nombreArchivo;
        // fsync explícito antes del close: en Windows, el handle del JVM puede
        // quedar marcado "in use" por unos cientos de ms tras un close normal,
        // haciendo que Excel COM (cscript refresh-excel.vbs) reciba un 1004 en
        // su primer Workbooks.Open. Forzar el flush a disco baja drásticamente
        // esa ventana de carrera.
        try (FileOutputStream fos = new FileOutputStream(rutaCompleta)) {
            workbook.write(fos);
            fos.flush();
            fos.getFD().sync();
        }
        return rutaCompleta;
    }

    private String buildNombreArchivo(String codCliente, TipoReporte tipoReporte) {
        return codCliente.toUpperCase() + "_" + tipoReporte.name() + "_"
                + LocalDateTime.now().format(FILE_DATE_FORMAT) + ".xlsx";
    }

    /**
     * Resuelve el InputStream del template a usar para el tipo, cliente y categoría dados.
     * Los templates ahora se buscan en el directorio del cliente (junto con filtros_base y datos_base).
     * Busca: cliente+categoría → sólo cliente → default en dir cliente → classpath.
     */
    private InputStream resolverTemplateStream(TipoReporte tipoReporte, String codCliente,
                                               String codCategoria) throws IOException {
        String dir = templateService.getTemplatesDir(codCliente);

        // 1. Template específico del cliente + categoría en disco
        File clientCatFile = Paths.get(dir, tipoReporte.getTemplateFileName(codCliente, codCategoria)).toFile();
        if (clientCatFile.exists()) {
            LOGGER.info("Usando template de cliente+categoría en disco: {}", clientCatFile.getAbsolutePath());
            return new FileInputStream(clientCatFile);
        }

        // 2. Template específico del cliente (sin categoría) en disco — fallback
        File clientFile = Paths.get(dir, tipoReporte.getTemplateFileName(codCliente)).toFile();
        if (clientFile.exists()) {
            LOGGER.info("Usando template de cliente en disco: {}", clientFile.getAbsolutePath());
            return new FileInputStream(clientFile);
        }

        // 3. Template por defecto en disco del cliente
        File defaultFile = Paths.get(dir, tipoReporte.getDefaultTemplateFileName()).toFile();
        if (defaultFile.exists()) {
            LOGGER.info("Usando template por defecto en disco: {}", defaultFile.getAbsolutePath());
            return new FileInputStream(defaultFile);
        }

        // 4. Fallback: template por defecto en classpath (recursos del proyecto)
        ClassPathResource fallback = new ClassPathResource(tipoReporte.getDefaultTemplatePath());
        LOGGER.warn("Template no encontrado en disco, usando classpath: {}", fallback.getPath());
        return fallback.getInputStream();
    }

    public String getRutaArchivo(String nombreArchivo) {
        return directorioServer + File.separator + insightsReportsSubdir + File.separator + nombreArchivo;
    }

}
