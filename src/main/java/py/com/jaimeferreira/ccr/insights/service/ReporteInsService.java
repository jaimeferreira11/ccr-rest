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

    // Nombres de meses en español para la hoja Calendario
    private static final String[] MESES_ES = {
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    };

    // Índices de columnas en el CSV de datos (0-based)
    private static final int CSV_CATEGORIA       = 0;
    private static final int CSV_APERTURA        = 1;
    private static final int CSV_EMPRESA         = 2;
    private static final int CSV_MARCA           = 3;
    private static final int CSV_SEGMENTO        = 4;
    private static final int CSV_MES             = 5;
    private static final int CSV_ANO             = 6;
    private static final int CSV_DIST_FISICA     = 7;
    private static final int CSV_DIST_PONDERADA  = 8;
    private static final int CSV_FACTURACION     = 9;
    // CSV[10] = Precio (no se usa en las hojas)
    private static final int CSV_VOLUMEN         = 11;
    private static final int CSV_VOLUMEN_UNIDADES = 12;

    @Value("${path.directory.server}")
    private String directorioServer;

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

    /** Self-reference para que @Async funcione a través del proxy de Spring (evita self-invocation). */
    @Lazy
    @Autowired
    private ReporteInsService self;

    /**
     * Inicia la generación: crea el registro en BD con estado PROCESANDO y lanza el
     * procesamiento asíncrono. Los bytes de ambos CSV se capturan aquí para evitar que
     * los archivos temporales del request expiren antes de que el hilo async los consuma.
     *
     * @param csvData    archivo CSV con los datos
     * @param csvFiltros archivo CSV con los filtros a aplicar (puede ser null; si es null se usa el base del cliente)
     */
    public InformeIns iniciarGeneracion(MultipartFile csvData, MultipartFile csvFiltros,
                                        String codCliente, TipoReporte tipoReporte, String usuario) {

        LOGGER.info("Iniciando generación de informe. Cliente: {}, Tipo: {}", codCliente, tipoReporte);

        String codClienteNorm = codCliente.trim().toUpperCase();

        // Validar existencia de archivos base ANTES de persistir y lanzar async
        validarArchivosBaseExisten(csvFiltros, codClienteNorm);

        byte[] csvBytes;
        byte[] filtroBytes;
        try {
            csvBytes    = csvData.getBytes();
            filtroBytes = (csvFiltros != null && !csvFiltros.isEmpty()) ? csvFiltros.getBytes() : null;
        } catch (IOException e) {
            throw new RuntimeException("Error al leer los archivos CSV: " + e.getMessage(), e);
        }

        InformeIns informe = new InformeIns();
        informe.setCodCliente(codClienteNorm);
        informe.setTipoReporte(tipoReporte);
        informe.setEstado(EstadoInforme.PROCESANDO);
        informe.setFechaCreacion(LocalDateTime.now());
        informe.setNombreUsuarioCreacion(usuario);

        InformeIns guardado = informeService.save(informe);

        self.procesarReporte(guardado.getId(), csvBytes, filtroBytes, codCliente, tipoReporte, usuario);

        return guardado;
    }

    /**
     * Valida que existan los archivos base necesarios para generar el informe
     * ANTES de persistir en BD y lanzar el procesamiento async.
     * - Filtros: debe existir el CSV subido, o filtros_base.csv del cliente, o filtros.csv en classpath.
     * - Datos base: debe existir datos_base.csv del cliente en disco.
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
                            "No se encontró un archivo de filtros para el cliente " + codCliente
                            + ". Debe subir un CSV de filtros o configurar filtros_base.csv desde Administración.");
                }
            }
        }

        // Validar datos base: debe existir en disco
        File datosBase = Paths.get(getClienteDir(codCliente), DATOS_BASE_FILENAME).toFile();
        if (!datosBase.exists()) {
            throw new UnknownResourceException(
                    "No se encontró el archivo de datos base para el cliente " + codCliente
                    + ". Debe subir datos_base.csv desde Administración antes de generar informes.");
        }
    }

    /**
     * Procesamiento asíncrono: resuelve filtros, concatena datos base con datos del usuario,
     * genera el Excel y guarda en disco. Al finalizar persiste el CSV concatenado como nuevo datos_base.
     *
     * @param filtroBytes bytes del CSV de filtros subido por el usuario (null si no se subió)
     */
    @Async
    public void procesarReporte(Long informeId, byte[] csvBytes, byte[] filtroBytes,
                                String codCliente, TipoReporte tipoReporte, String usuario) {

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

            // Leer datos base del cliente (obligatorio)
            byte[] datosBase = leerDatosBase(codClienteNorm);

            // Concatenar datos base + datos del usuario
            byte[] csvConcatenado = concatenarCsvData(datosBase, csvBytes);
            LOGGER.info("CSV concatenado generado ({} bytes) para cliente {}", csvConcatenado.length, codClienteNorm);

            // Leer y filtrar data del CSV concatenado
            List<String[]> dataFiltrada = leerYFiltrarCsvData(csvConcatenado, filtros);
            int totalDataRows = dataFiltrada.size() - 1; // sin header
            LOGGER.info("Filas resultantes tras filtrado: {}", totalDataRows);

            if (totalDataRows <= 0) {
                throw new RuntimeException(
                        "El CSV de datos no contiene filas que coincidan con los filtros aplicados. "
                        + "Verifique que los nombres de columna del filtro coincidan con los del CSV de datos.");
            }

            // Abrir template Excel: busca primero el específico del cliente en disco, luego el default.
            IOUtils.setByteArrayMaxOverride(Integer.MAX_VALUE);
            InputStream templateStream = resolverTemplateStream(tipoReporte, codClienteNorm);
            XSSFWorkbook templateWb = new XSSFWorkbook(templateStream);
            templateStream.close();

            desconectarTablas(templateWb);

            limpiarDatosHoja(templateWb, SHEET_FACT);
            limpiarDatosHoja(templateWb, SHEET_TOTAL_EMPRESA);

            int calendarRows = poblarCalendario(templateWb, dataFiltrada);

            SXSSFWorkbook workbook = new SXSSFWorkbook(templateWb, 100);
            workbook.setCompressTempFiles(true);

            CellStyle dateCellStyle = workbook.createCellStyle();
            dateCellStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));

            poblarFact(workbook, dataFiltrada, codClienteNorm, clienteLabel, pais, dateCellStyle);
            poblarTotalEmpresa(workbook, dataFiltrada, codClienteNorm, clienteLabel, pais, dateCellStyle);
            actualizarRangosTablas(templateWb, totalDataRows, calendarRows);
            ocultarHoja(workbook, SHEET_FACT);
            ocultarHoja(workbook, SHEET_CALENDARIO);
            workbook.setForceFormulaRecalculation(true);

            String nombreArchivo = buildNombreArchivo(codClienteNorm, tipoReporte);
            String rutaCompleta = guardarEnDisco(workbook, nombreArchivo);
            workbook.dispose();

            // Persistir el CSV concatenado como nuevo datos_base del cliente
            guardarDatosBase(codClienteNorm, csvConcatenado);

            LOGGER.info("Informe {} guardado en: {}", informeId, rutaCompleta);
            long duracion = (System.currentTimeMillis() - inicio) / 1000;
            informeService.marcarCompletado(informeId, nombreArchivo, usuario, duracion);
            LOGGER.info("Informe id={} completado en {}s", informeId, duracion);

        } catch (Throwable t) {
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
     *                   Apertura Geografica, Categoría, CLIENTE, Empresa, hash,
     *                   Marca, PAIS, Segmento, Agrupador Segmento, Orden Apertura,
     *                   YTD 1er Mes, Fecha
     */
    private void poblarFact(Workbook workbook, List<String[]> data,
                             String codCliente, String clienteLabel, String pais,
                             CellStyle dateCellStyle) {

        Sheet sheet = workbook.getSheet(SHEET_FACT);
        if (sheet == null) {
            LOGGER.warn("Hoja '{}' no encontrada en el template, se crea nueva.", SHEET_FACT);
            sheet = workbook.createSheet(SHEET_FACT);
        }

        int lastOldRow = sheet.getLastRowNum();
        int dataRows = data.size() - 1; // sin header

        for (int i = 1; i <= dataRows; i++) {
            String[] csv = data.get(i);
            Row row = sheet.getRow(i);
            if (row == null) row = sheet.createRow(i);

            setCellNumeric(row,  0, csv[CSV_DIST_FISICA]);
            setCellNumeric(row,  1, csv[CSV_DIST_PONDERADA]);
            setCellNumeric(row,  2, csv[CSV_FACTURACION]);
            setCellNumeric(row,  3, csv[CSV_VOLUMEN]);
            setCellNumeric(row,  4, csv[CSV_VOLUMEN_UNIDADES]);
            setCellString(row,   5, csv[CSV_APERTURA]);
            setCellString(row,   6, csv[CSV_CATEGORIA]);
            setCellString(row,   7, clienteLabel);
            setCellString(row,   8, csv[CSV_EMPRESA]);
            setCellString(row,   9, "");                               // hash
            setCellString(row,  10, csv[CSV_MARCA]);
            setCellString(row,  11, pais);
            setCellString(row,  12, csv[CSV_SEGMENTO]);
            setCellString(row,  13, derivarAgrupadorSegmento(csv[CSV_SEGMENTO]));
            setCellString(row,  14, "0");                              // Orden Apertura
            setCellString(row,  15, derivarYtd(csv[CSV_MES]));
            setCellDate(row,    16, csv[CSV_MES], csv[CSV_ANO], dateCellStyle);
        }

        LOGGER.info("Hoja '{}' poblada con {} filas de datos.", SHEET_FACT, dataRows);
    }

    /**
     * Pobla la hoja Total Empresa.
     * Columnas destino: Dist.Fisica, Dist.Ponderada, Apertura Geografica, Categoría,
     *                   CLIENTE, Empresa, hash, PAIS, Segmento, Agrupador Segmento,
     *                   Volumen Unidades, YTD 1er Mes, Orden Apertura, Fecha, Marca
     */
    private void poblarTotalEmpresa(Workbook workbook, List<String[]> data,
                                     String codCliente, String clienteLabel, String pais,
                                     CellStyle dateCellStyle) {

        Sheet sheet = workbook.getSheet(SHEET_TOTAL_EMPRESA);
        if (sheet == null) {
            LOGGER.warn("Hoja '{}' no encontrada en el template, se crea nueva.", SHEET_TOTAL_EMPRESA);
            sheet = workbook.createSheet(SHEET_TOTAL_EMPRESA);
        }

        int lastOldRow = sheet.getLastRowNum();
        int dataRows = data.size() - 1;

        for (int i = 1; i <= dataRows; i++) {
            String[] csv = data.get(i);
            Row row = sheet.getRow(i);
            if (row == null) row = sheet.createRow(i);

            setCellNumeric(row,  0, csv[CSV_DIST_FISICA]);
            setCellNumeric(row,  1, csv[CSV_DIST_PONDERADA]);
            setCellString(row,   2, csv[CSV_APERTURA]);
            setCellString(row,   3, csv[CSV_CATEGORIA]);
            setCellString(row,   4, clienteLabel);
            setCellString(row,   5, csv[CSV_EMPRESA]);
            setCellString(row,   6, "");                               // hash
            setCellString(row,   7, pais);
            setCellString(row,   8, csv[CSV_SEGMENTO]);
            setCellString(row,   9, derivarAgrupadorSegmento(csv[CSV_SEGMENTO]));
            setCellNumeric(row, 10, csv[CSV_VOLUMEN_UNIDADES]);
            setCellString(row,  11, derivarYtd(csv[CSV_MES]));
            setCellString(row,  12, "0");                              // Orden Apertura
            setCellDate(row,    13, csv[CSV_MES], csv[CSV_ANO], dateCellStyle);
            setCellString(row,  14, csv[CSV_MARCA]);
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
    private int poblarCalendario(XSSFWorkbook wb, List<String[]> data) {
        XSSFSheet sheet = wb.getSheet(SHEET_CALENDARIO);
        if (sheet == null) {
            LOGGER.warn("Hoja '{}' no encontrada en el template, se omite.", SHEET_CALENDARIO);
            return 0;
        }

        // Limpiar filas de datos previas (preservar header)
        int lastOldRow = sheet.getLastRowNum();
        for (int i = lastOldRow; i >= 1; i--) {
            org.apache.poi.xssf.usermodel.XSSFRow row = sheet.getRow(i);
            if (row != null) sheet.removeRow(row);
        }

        // Extraer pares (año, mes) únicos del CSV — TreeMap ordena por año, TreeSet por mes
        TreeMap<Integer, TreeSet<Integer>> anioMeses = new TreeMap<>();
        for (int i = 1; i < data.size(); i++) {
            try {
                int anio = Integer.parseInt(data.get(i)[CSV_ANO].trim());
                int mes = Integer.parseInt(data.get(i)[CSV_MES].trim());
                anioMeses.computeIfAbsent(anio, k -> new TreeSet<>()).add(mes);
            } catch (NumberFormatException ignored) { }
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
        if (sheet == null) return;

        int lastOldRow = sheet.getLastRowNum();
        if (lastOldRow < 1) return;

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
                    } while (cursor.toNextAttribute());
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
        } else {
            LOGGER.warn("Hoja '{}' no encontrada para ocultar.", sheetName);
        }
    }

    /**
     * Actualiza los rangos de referencia de las tablas Excel (FACT, Total_Empresa, Calendario)
     * para que coincidan con la cantidad real de filas escritas.
     * Esto es crucial para que los pivot tables del template reconozcan
     * correctamente el nuevo rango de datos.
     */
    private void actualizarRangosTablas(XSSFWorkbook templateWb, int dataRows, int calendarRows) {
        actualizarTabla(templateWb, SHEET_FACT, "FACT", dataRows, "Q");
        actualizarTabla(templateWb, SHEET_TOTAL_EMPRESA, "Total_Empresa", dataRows, "O");
        actualizarTabla(templateWb, SHEET_CALENDARIO, "Calendario", calendarRows, "D");
    }

    private void actualizarTabla(XSSFWorkbook wb, String sheetName, String tableName,
                                  int dataRows, String lastCol) {
        XSSFSheet sheet = wb.getSheet(sheetName);
        if (sheet == null) return;
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
    private static final String DATOS_BASE_FILENAME = "datos_base.csv";
    private static final String DATOS_BASE_BACKUP_FILENAME = "datos_base_backup.csv";

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
            throws IOException, CsvValidationException {

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
     * Lee el CSV de datos base del cliente desde disco.
     * @throws UnknownResourceException si el archivo no existe
     */
    private byte[] leerDatosBase(String codCliente) {
        File datosBase = Paths.get(getClienteDir(codCliente), DATOS_BASE_FILENAME).toFile();
        if (!datosBase.exists()) {
            throw new UnknownResourceException(
                    "No se encontró el archivo de datos base para el cliente " + codCliente
                    + ". Debe subir un archivo datos_base.csv desde Administración antes de generar informes.");
        }
        try {
            byte[] bytes = Files.readAllBytes(datosBase.toPath());
            LOGGER.info("Datos base leídos para cliente {}: {} bytes", codCliente, bytes.length);
            return bytes;
        } catch (IOException e) {
            throw new RuntimeException("Error al leer datos base del cliente " + codCliente + ": " + e.getMessage(), e);
        }
    }

    /**
     * Concatena dos CSV: toma el header + filas del base, y luego solo las filas (sin header) del usuario.
     * Ambos deben tener el mismo separador (;) y codificación (ISO-8859-1).
     */
    private byte[] concatenarCsvData(byte[] datosBase, byte[] datosUsuario) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(datosBase);
        // Asegurar que el base termina con salto de línea
        if (datosBase.length > 0 && datosBase[datosBase.length - 1] != '\n') {
            out.write('\n');
        }
        // Eliminar la primera línea (header) del CSV del usuario
        String usuarioStr = new String(datosUsuario, StandardCharsets.ISO_8859_1);
        int firstNewline = usuarioStr.indexOf('\n');
        if (firstNewline >= 0 && firstNewline < usuarioStr.length() - 1) {
            byte[] sinHeader = usuarioStr.substring(firstNewline + 1).getBytes(StandardCharsets.ISO_8859_1);
            out.write(sinHeader);
        }
        return out.toByteArray();
    }

    /**
     * Guarda el CSV concatenado como nuevo datos_base.csv del cliente.
     * Utiliza escritura atómica (temp + rename) para evitar corrupción.
     */
    private void guardarDatosBase(String codCliente, byte[] csvConcatenado) {
        try {
            java.nio.file.Path clienteDir = Paths.get(getClienteDir(codCliente));
            if (!Files.exists(clienteDir)) {
                Files.createDirectories(clienteDir);
            }
            java.nio.file.Path destino = clienteDir.resolve(DATOS_BASE_FILENAME);
            // Backup del archivo actual antes de sobreescribir
            backupDatosBase(clienteDir, destino);
            java.nio.file.Path temp = clienteDir.resolve(DATOS_BASE_FILENAME + ".tmp");
            Files.write(temp, csvConcatenado);
            Files.move(temp, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            LOGGER.info("Datos base actualizados para cliente {}: {} bytes", codCliente, csvConcatenado.length);
        } catch (IOException e) {
            LOGGER.error("Error al guardar datos base del cliente {}: {}", codCliente, e.getMessage(), e);
        }
    }

    /**
     * Copia el datos_base.csv actual a datos_base_backup.csv (sobreescribiendo el backup anterior).
     * Permite hacer rollback manual ante anomalías.
     */
    private void backupDatosBase(java.nio.file.Path clienteDir, java.nio.file.Path datosBaseActual) {
        try {
            if (Files.exists(datosBaseActual)) {
                java.nio.file.Path backup = clienteDir.resolve(DATOS_BASE_BACKUP_FILENAME);
                Files.copy(datosBaseActual, backup, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Backup de datos base creado: {}", backup);
            }
        } catch (IOException e) {
            LOGGER.warn("No se pudo crear backup de datos base: {}", e.getMessage());
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
            // Backup si se está reemplazando datos_base.csv
            if (DATOS_BASE_FILENAME.equals(nombreArchivo)) {
                backupDatosBase(clienteDir, destino);
            }
            archivo.transferTo(destino.toFile());
            LOGGER.info("Archivo base '{}' guardado para cliente {}: {}", nombreArchivo, codCliente, destino);
            return destino.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar archivo base: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Lectura y filtrado de CSV
    // -------------------------------------------------------------------------

    private List<Map<String, String>> leerFiltrosDesdeResources() throws IOException, CsvValidationException {
        ClassPathResource resource = new ClassPathResource(FILTROS_CSV_PATH);
        List<Map<String, String>> filtros = new ArrayList<>();

        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))
                .withCSVParser(new CSVParserBuilder().withSeparator(CSV_SEPARATOR).build())
                .build()) {

            String[] headers = reader.readNext();
            if (headers == null) return filtros;

            String[] fila;
            while ((fila = reader.readNext()) != null) {
                Map<String, String> regla = new HashMap<>();
                for (int i = 0; i < headers.length && i < fila.length; i++) {
                    String valor = fila[i].trim();
                    if (!valor.isEmpty()) {
                        regla.put(headers[i].trim(), valor);
                    }
                }
                if (!regla.isEmpty()) filtros.add(regla);
            }
        }
        return filtros;
    }

    /**
     * Lee los filtros desde los bytes de un CSV subido por el usuario.
     * El formato esperado es el mismo que el de resources/insights/filtros.csv.
     */
    private List<Map<String, String>> leerFiltrosDesdeBytes(byte[] filtroBytes) throws IOException, CsvValidationException {
        List<Map<String, String>> filtros = new ArrayList<>();

        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(new ByteArrayInputStream(filtroBytes), StandardCharsets.UTF_8))
                .withCSVParser(new CSVParserBuilder().withSeparator(CSV_SEPARATOR).build())
                .build()) {

            String[] headers = reader.readNext();
            if (headers == null) return filtros;

            String[] fila;
            while ((fila = reader.readNext()) != null) {
                Map<String, String> regla = new HashMap<>();
                for (int i = 0; i < headers.length && i < fila.length; i++) {
                    String valor = fila[i].trim();
                    if (!valor.isEmpty()) {
                        regla.put(headers[i].trim(), valor);
                    }
                }
                if (!regla.isEmpty()) filtros.add(regla);
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
            throws IOException, CsvValidationException {

        List<String[]> resultado = new ArrayList<>();

        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(new ByteArrayInputStream(csvBytes), StandardCharsets.ISO_8859_1))
                .withCSVParser(new CSVParserBuilder().withSeparator(CSV_SEPARATOR).build())
                .build()) {

            String[] headers = reader.readNext();
            if (headers == null) return resultado;

            for (int i = 0; i < headers.length; i++) headers[i] = headers[i].trim();
            resultado.add(headers);

            LOGGER.info("Headers del CSV de datos ({} columnas):", headers.length);
            for (int i = 0; i < headers.length; i++) {
                LOGGER.info("  [{}] '{}'", i, headers[i]);
            }

            Map<String, Integer> colIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) colIndex.put(headers[i], i);

            // Validar que todas las claves de los filtros existen en el CSV
            for (Map<String, String> regla : filtros) {
                for (String clave : regla.keySet()) {
                    if (!colIndex.containsKey(clave)) {
                        LOGGER.warn("  *** Clave de filtro '{}' NO encontrada en los headers del CSV ***", clave);
                    } else {
                        LOGGER.info("  Clave de filtro '{}' → columna [{}], valor buscado: '{}'",
                                clave, colIndex.get(clave), regla.get(clave));
                    }
                }
            }

            String[] fila;
            while ((fila = reader.readNext()) != null) {
                if (filaCumpleFiltros(fila, colIndex, filtros)) resultado.add(fila);
            }
        }
        return resultado;
    }

    /**
     * Retorna true si la fila cumple al menos una regla de filtro (OR entre filas, AND entre columnas).
     */
    private boolean filaCumpleFiltros(String[] fila, Map<String, Integer> colIndex,
                                       List<Map<String, String>> filtros) {
        if (filtros.isEmpty()) return true;

        for (Map<String, String> regla : filtros) {
            boolean cumple = true;
            for (Map.Entry<String, String> condicion : regla.entrySet()) {
                Integer idx = colIndex.get(condicion.getKey());
                if (idx == null || idx >= fila.length) { cumple = false; break; }
                if (!fila[idx].trim().equalsIgnoreCase(condicion.getValue())) { cumple = false; break; }
            }
            if (cumple) return true;
        }
        return false;
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
        } catch (NumberFormatException e) {
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
        } catch (Exception e) {
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
        if (segmento == null) return "POR TIPO";
        return "TOT.PROD.".equalsIgnoreCase(segmento.trim()) ? "TOT.PROD." : "POR TIPO";
    }

    /** YTD 1er Mes: "1" si es enero, vacío en caso contrario. */
    private String derivarYtd(String mes) {
        return "1".equals(mes != null ? mes.trim() : "") ? "1" : "";
    }

    // -------------------------------------------------------------------------
    // Persistencia en disco
    // -------------------------------------------------------------------------

    private String guardarEnDisco(Workbook workbook, String nombreArchivo) throws IOException {
        String dirReportes = directorioServer + File.separator + insightsReportsSubdir;
        File directorio = new File(dirReportes);
        if (!directorio.exists()) directorio.mkdirs();

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
     * Resuelve el InputStream del template a usar para el tipo y cliente dados.
     * Busca en disco primero (disco → default en disco → classpath como último recurso).
     */
    private InputStream resolverTemplateStream(TipoReporte tipoReporte, String codCliente) throws IOException {
        String dir = templateService.getTemplatesDir();

        // 1. Template específico del cliente en disco
        File clientFile = Paths.get(dir, tipoReporte.getTemplateFileName(codCliente)).toFile();
        if (clientFile.exists()) {
            LOGGER.info("Usando template de cliente en disco: {}", clientFile.getAbsolutePath());
            return new FileInputStream(clientFile);
        }

        // 2. Template por defecto en disco
        File defaultFile = Paths.get(dir, tipoReporte.getDefaultTemplateFileName()).toFile();
        if (defaultFile.exists()) {
            LOGGER.info("Usando template por defecto en disco: {}", defaultFile.getAbsolutePath());
            return new FileInputStream(defaultFile);
        }

        // 3. Fallback: template por defecto en classpath (recursos del proyecto)
        ClassPathResource fallback = new ClassPathResource(tipoReporte.getDefaultTemplatePath());
        LOGGER.warn("Template no encontrado en disco, usando classpath: {}", fallback.getPath());
        return fallback.getInputStream();
    }

    public String getRutaArchivo(String nombreArchivo) {
        return directorioServer + File.separator + insightsReportsSubdir + File.separator + nombreArchivo;
    }
    
}

