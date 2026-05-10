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
import org.apache.poi.util.IOUtils;
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

import py.com.jaimeferreira.ccr.commons.exception.UnknownResourceException;
import py.com.jaimeferreira.ccr.insights.entity.ClienteIns;
import py.com.jaimeferreira.ccr.insights.entity.EstadoInforme;
import py.com.jaimeferreira.ccr.insights.entity.InformeIns;
import py.com.jaimeferreira.ccr.insights.entity.TipoReporte;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

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

    /**
     * Self-reference para que @Async funcione a través del proxy de Spring (evita self-invocation).
     */
    @Lazy
    @Autowired
    private ReporteInsService self;

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
                                        int mesInicioFiscal, String usuario) {

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

        self.procesarReporte(guardado.getId(), csvBytes, filtroBytes, codCliente, codCategoria, tipoReporte, mesInicioFiscal, usuario);

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
                                int mesInicioFiscal, String usuario) {

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
            for (int fi = 0; fi < filtros.size(); fi++) {
                LOGGER.info("  Filtro [{}]: {}", fi, filtros.get(fi));
            }

            // Mapas de lookup para valores derivados del filtro
            Map<String, String> ordenAperturaMap = buildLookupMap(filtros, "APERTURA", "Orden_Apertura");
            Map<String, String> agrupadorSegmentoMap = buildLookupMap(filtros, "SEGMENTO", "AGR_SEGM");
            LOGGER.info("Mapas de lookup: ordenApertura={}, agrupadorSegmento={}",
                        ordenAperturaMap, agrupadorSegmentoMap);

            // Concatenar datos base + datos del usuario (datos base es opcional)
            byte[] datosBase = leerDatosBase(codClienteNorm, tipoReporte);
            byte[] csvConcatenado = (datosBase != null)
                                                        ? concatenarCsvData(datosBase, csvBytes)
                                                        : csvBytes;
            LOGGER.info("CSV concatenado generado ({} bytes) para cliente {}", csvConcatenado.length, codClienteNorm);

            // Leer y filtrar data del CSV concatenado
            List<String[]> dataFiltrada = leerYFiltrarCsvData(csvConcatenado, filtros);
            int totalDataRows = dataFiltrada.size() - 1; // sin header
            LOGGER.info("Filas resultantes tras filtrado: {}", totalDataRows);

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
            IOUtils.setByteArrayMaxOverride(Integer.MAX_VALUE);
            InputStream templateStream = resolverTemplateStream(tipoReporte, codClienteNorm, codCategoria);
            XSSFWorkbook templateWb = new XSSFWorkbook(templateStream);
            templateStream.close();

            desconectarTablas(templateWb);

            limpiarDatosHoja(templateWb, SHEET_FACT);
            limpiarDatosHoja(templateWb, SHEET_TOTAL_EMPRESA);

            int calendarRows = poblarCalendario(templateWb, dataFiltrada, tipoReporte);

            SXSSFWorkbook workbook = new SXSSFWorkbook(templateWb, 100);
            workbook.setCompressTempFiles(true);

            CellStyle dateCellStyle = workbook.createCellStyle();
            dateCellStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));

            poblarFact(workbook, dataFiltrada, codClienteNorm, clienteLabel, pais, dateCellStyle, tipoReporte, ordenAperturaMap, agrupadorSegmentoMap, idxSubMarca, mesInicioFiscal);
            poblarTotalEmpresa(workbook, dataFiltrada, codClienteNorm, clienteLabel, pais, dateCellStyle, tipoReporte, ordenAperturaMap, agrupadorSegmentoMap, idxSubMarca, mesInicioFiscal);
            actualizarRangosTablas(templateWb, totalDataRows, calendarRows, tipoReporte, idxSubMarca >= 0);
            refrescarTablasDinamicas(templateWb);
            ocultarHoja(workbook, SHEET_FACT);
            ocultarHoja(workbook, SHEET_CALENDARIO);
            ocultarHoja(workbook, SHEET_TOTAL_EMPRESA);
            ocultarHoja(workbook, SHEET_DIM);
            ocultarHoja(workbook, SHEET_HOJA1);
            workbook.setForceFormulaRecalculation(true);

            String nombreArchivo = buildNombreArchivo(codClienteNorm, tipoReporte);
            String rutaCompleta = guardarEnDisco(workbook, nombreArchivo);
            workbook.dispose();

            // POST-PROCESAMIENTO: Refrescar modelo de datos con Excel real (COM)
            // Esto reconstruye el VertiPaq para que las medidas DAX funcionen con los nuevos datos
            refrescarModeloDatos(rutaCompleta);

            // Persistir el CSV concatenado como nuevo datos_base del cliente
            guardarDatosBase(codClienteNorm, tipoReporte, csvConcatenado);

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

    // -------------------------------------------------------------------------
    // Poblar hojas
    // -------------------------------------------------------------------------

    /**
     * Pobla la hoja FACT.
     * Columnas destino: Dist.Fisica, Dist.Ponderada, Facturación, Volumen, Vol.Unidades,
     * Apertura Geografica, Categoría, CLIENTE, Empresa, hash,
     * Marca, PAIS, Segmento, Agrupador Segmento, Orden Apertura,
     * YTD 1er Mes, Fecha [, Extra (solo CADENA)]
     */
    private void poblarFact(Workbook workbook, List<String[]> data,
                            String codCliente, String clienteLabel, String pais,
                            CellStyle dateCellStyle, TipoReporte tipoReporte,
                            Map<String, String> ordenAperturaMap,
                            Map<String, String> agrupadorSegmentoMap,
                            int idxSubMarca, int mesInicioFiscal) {

        Sheet sheet = workbook.getSheet(SHEET_FACT);
        if (sheet == null) {
            LOGGER.warn("Hoja '{}' no encontrada en el template, se crea nueva.", SHEET_FACT);
            sheet = workbook.createSheet(SHEET_FACT);
        }

        int dataRows = data.size() - 1; // sin header

        for (int i = 1; i <= dataRows; i++) {
            String[] csv = data.get(i);
            Row row = sheet.getRow(i);
            if (row == null)
                row = sheet.createRow(i);

            setCellNumeric(row, 0, csv[tipoReporte.getIdxDistFisica()]);
            setCellNumeric(row, 1, csv[tipoReporte.getIdxDistPonderada()]);
            setCellNumeric(row, 2, csv[tipoReporte.getIdxFacturacion()]);
            setCellNumeric(row, 3, csv[tipoReporte.getIdxVolumen()]);
            setCellNumeric(row, 4, csv[tipoReporte.getIdxVolumenUnidades()]);
            setCellString(row, 5, csv[tipoReporte.getIdxApertura()]);
            setCellString(row, 6, csv[tipoReporte.getIdxCategoria()]);
            setCellString(row, 7, clienteLabel);
            setCellString(row, 8, csv[tipoReporte.getIdxEmpresa()]);
            setCellString(row, 9, "");                               // hash
            setCellString(row, 10, csv[tipoReporte.getIdxMarca()]);
            setCellString(row, 11, pais);
            setCellString(row, 12, csv[tipoReporte.getIdxSegmento()]);
            setCellString(row, 13, agrupadorSegmentoMap.getOrDefault(
                    normalizar(csv[tipoReporte.getIdxSegmento()]),
                    derivarAgrupadorSegmento(csv[tipoReporte.getIdxSegmento()])));
            setCellString(row, 14, ordenAperturaMap.getOrDefault(
                    normalizar(csv[tipoReporte.getIdxApertura()]), "0"));
            setCellString(row, 15, derivarYtd(csv[tipoReporte.getIdxMes()], mesInicioFiscal));
            setCellDate(row, 16, csv[tipoReporte.getIdxMes()], csv[tipoReporte.getIdxAno()], dateCellStyle);
            int nextCol = 17;
            if (tipoReporte.tieneExtra()) {
                setCellString(row, nextCol, csv[tipoReporte.getIdxExtra()]);
                nextCol++;
            }
            if (idxSubMarca >= 0 && idxSubMarca < csv.length) {
                setCellString(row, nextCol, csv[idxSubMarca]);
            } else if (idxSubMarca >= 0) {
                setCellString(row, nextCol, "");
            }
        }

        LOGGER.info("Hoja '{}' poblada con {} filas de datos.", SHEET_FACT, dataRows);
    }

    /**
     * Pobla la hoja Total Empresa.
     * Columnas destino: Dist.Fisica, Dist.Ponderada, Apertura Geografica, Categoría,
     * CLIENTE, Empresa, hash, PAIS, Segmento, Agrupador Segmento,
     * Volumen Unidades, YTD 1er Mes, Orden Apertura, Fecha, Marca [, Extra (solo CADENA)]
     */
    private void poblarTotalEmpresa(Workbook workbook, List<String[]> data,
                                    String codCliente, String clienteLabel, String pais,
                                    CellStyle dateCellStyle, TipoReporte tipoReporte,
                                    Map<String, String> ordenAperturaMap,
                                    Map<String, String> agrupadorSegmentoMap,
                                    int idxSubMarca, int mesInicioFiscal) {

        Sheet sheet = workbook.getSheet(SHEET_TOTAL_EMPRESA);
        if (sheet == null) {
            LOGGER.warn("Hoja '{}' no encontrada en el template, se crea nueva.", SHEET_TOTAL_EMPRESA);
            sheet = workbook.createSheet(SHEET_TOTAL_EMPRESA);
        }

        int dataRows = data.size() - 1;

        for (int i = 1; i <= dataRows; i++) {
            String[] csv = data.get(i);
            Row row = sheet.getRow(i);
            if (row == null)
                row = sheet.createRow(i);

            setCellNumeric(row, 0, csv[tipoReporte.getIdxDistFisica()]);
            setCellNumeric(row, 1, csv[tipoReporte.getIdxDistPonderada()]);
            setCellString(row, 2, csv[tipoReporte.getIdxApertura()]);
            setCellString(row, 3, csv[tipoReporte.getIdxCategoria()]);
            setCellString(row, 4, clienteLabel);
            setCellString(row, 5, csv[tipoReporte.getIdxEmpresa()]);
            setCellString(row, 6, "");                               // hash
            setCellString(row, 7, pais);
            setCellString(row, 8, csv[tipoReporte.getIdxSegmento()]);
            setCellString(row, 9, agrupadorSegmentoMap.getOrDefault(
                    normalizar(csv[tipoReporte.getIdxSegmento()]),
                    derivarAgrupadorSegmento(csv[tipoReporte.getIdxSegmento()])));
            setCellNumeric(row, 10, csv[tipoReporte.getIdxVolumenUnidades()]);
            setCellString(row, 11, derivarYtd(csv[tipoReporte.getIdxMes()], mesInicioFiscal));
            setCellString(row, 12, ordenAperturaMap.getOrDefault(
                    normalizar(csv[tipoReporte.getIdxApertura()]), "0"));
            setCellDate(row, 13, csv[tipoReporte.getIdxMes()], csv[tipoReporte.getIdxAno()], dateCellStyle);
            setCellString(row, 14, csv[tipoReporte.getIdxMarca()]);
            int nextCol = 15;
            if (tipoReporte.tieneExtra()) {
                setCellString(row, nextCol, csv[tipoReporte.getIdxExtra()]);
                nextCol++;
            }
            if (idxSubMarca >= 0 && idxSubMarca < csv.length) {
                setCellString(row, nextCol, csv[idxSubMarca]);
            } else if (idxSubMarca >= 0) {
                setCellString(row, nextCol, "");
            }
        }

        LOGGER.info("Hoja '{}' poblada con {} filas de datos.", SHEET_TOTAL_EMPRESA, dataRows);
    }

    /**
     * Pobla la hoja Calendario directamente sobre el XSSFWorkbook (antes de SXSSF).
     * Extrae los pares (Año, Mes) únicos del CSV de datos y genera una fila por cada uno.
     * Columnas: Fecha (date 1ro del mes), Mes Numero (int), Año (int), Mes (nombre en español).
     *
     * @return cantidad de filas de datos escritas (sin header)
     */
    private int poblarCalendario(XSSFWorkbook wb, List<String[]> data, TipoReporte tipoReporte) {
        XSSFSheet sheet = wb.getSheet(SHEET_CALENDARIO);
        if (sheet == null) {
            LOGGER.warn("Hoja '{}' no encontrada en el template, se omite.", SHEET_CALENDARIO);
            return 0;
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

        int rowNum = 1;
        for (Map.Entry<Integer, TreeSet<Integer>> entry : anioMeses.entrySet()) {
            int anio = entry.getKey();
            for (int mes : entry.getValue()) {
                org.apache.poi.xssf.usermodel.XSSFRow row = sheet.createRow(rowNum);

                LocalDate fecha = LocalDate.of(anio, mes, 1);
                Date date = Date.from(fecha.atStartOfDay(ZoneId.systemDefault()).toInstant());
                Cell cellFecha = row.createCell(0, CellType.NUMERIC);
                cellFecha.setCellValue(date);
                cellFecha.setCellStyle(dateCellStyle);

                row.createCell(1, CellType.NUMERIC).setCellValue(mes);
                row.createCell(2, CellType.NUMERIC).setCellValue(anio);
                row.createCell(3, CellType.STRING).setCellValue(MESES_ES[mes - 1]);

                rowNum++;
            }
        }

        int totalRows = rowNum - 1;
        LOGGER.info("Hoja '{}' poblada con {} meses ({} años).",
                    SHEET_CALENDARIO, totalRows, anioMeses.size());
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

        LOGGER.info("Iniciando refresh del modelo de datos para: {}", rutaExcel);
        long inicio = System.currentTimeMillis();

        try {
            // VBScript + cscript.exe en lugar de PowerShell: VBS corre nativamente
            // en STA (Single-Threaded Apartment), evitando el error COM
            // "No se puede obtener la propiedad Open de la clase Workbooks"
            // que ocurre con PowerShell desde procesos de servicio.
            ProcessBuilder pb = new ProcessBuilder(
                "cscript.exe",
                "//Nologo",
                "//B",
                refreshScriptPath,
                rutaExcel
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Leer output del proceso
            String output;
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                output = reader.lines()
                              .collect(java.util.stream.Collectors.joining("\n"));
            }

            // Timeout configurable (el refresh del modelo puede tardar con datasets grandes)
            boolean finished = process.waitFor(refreshTimeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                LOGGER.error("Timeout al refrescar modelo de datos Excel: {}", rutaExcel);
                // NO lanzar excepción - el archivo sigue siendo utilizable,
                // solo que el usuario tendrá que hacer RefreshAll manualmente
                return;
            }

            if (process.exitValue() != 0) {
                LOGGER.error("Error al refrescar modelo de datos (exit={}): {}",
                            process.exitValue(), output);
                // NO lanzar excepción - mismo motivo que arriba
                return;
            }

            long duracion = (System.currentTimeMillis() - inicio) / 1000;
            LOGGER.info("Modelo de datos refrescado en {}s. Output: {}", duracion, output.trim());

        } catch (Exception e) {
            LOGGER.error("Excepción al refrescar modelo de datos: {}", e.getMessage(), e);
            // NO propagar - el reporte se generó correctamente,
            // el refresh es un paso de mejora, no crítico
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
        actualizarTabla(templateWb, SHEET_CALENDARIO, "Calendario", calendarRows, "D");
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

            LOGGER.info("Headers del CSV de datos ({} columnas):", headers.length);
            for (int i = 0; i < headers.length; i++) {
                LOGGER.info("  [{}] '{}'", i, headers[i]);
            }

            Map<String, Integer> colIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++)
                colIndex.put(normalizar(headers[i]), i);

            // Validar que todas las claves de los filtros existen en el CSV
            for (Map<String, String> regla : filtros) {
                for (String clave : regla.keySet()) {
                    if (!colIndex.containsKey(normalizar(clave))) {
                        LOGGER.warn("  *** Clave de filtro '{}' NO encontrada en los headers del CSV ***", clave);
                    }
                    else {
                        LOGGER.info("  Clave de filtro '{}' → columna [{}], valor buscado: '{}'",
                                    clave, colIndex.get(normalizar(clave)), regla.get(clave));
                    }
                }
            }

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

    /** YTD 1er Mes: "1" si el mes coincide con el inicio del año fiscal, vacío en caso contrario. */
    private String derivarYtd(String mes, int mesInicioFiscal) {
        return String.valueOf(mesInicioFiscal).equals(mes != null ? mes.trim() : "") ? "1" : "";
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
        try (FileOutputStream fos = new FileOutputStream(rutaCompleta)) {
            workbook.write(fos);
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
