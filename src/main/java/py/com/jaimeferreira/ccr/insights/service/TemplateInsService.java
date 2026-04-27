package py.com.jaimeferreira.ccr.insights.service;

import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.xmlbeans.XmlCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import py.com.jaimeferreira.ccr.commons.exception.InternalServerErrorException;
import py.com.jaimeferreira.ccr.commons.exception.UnknownResourceException;
import py.com.jaimeferreira.ccr.insights.entity.TipoReporte;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Servicio para gestión de templates Excel del módulo Insights.
 * Los templates se guardan en {path.directory.server}/{path.directory.server_path_clientes_insights}/{CODCLIENTE}/
 * con la nomenclatura: template_{tipo}_{CODCLIENTE}_{CODCATEGORIA}.xlsx
 *
 * Al guardar un template nuevo, se sanitiza automáticamente:
 * - Se eliminan las filas de datos de las hojas FACT, Total Empresa y Calendario (preservando headers).
 * - Se ajustan los rangos de las tablas Excel al mínimo (header + 1 fila).
 * - Se desconectan las tablas de fuentes externas (Power Query / Data Model).
 * - Se eliminan las conexiones externas del workbook.
 *
 * @author Jaime Ferreira
 */
@Service
public class TemplateInsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateInsService.class);

    @PostConstruct
    public void init() {
        IOUtils.setByteArrayMaxOverride(Integer.MAX_VALUE);
        LOGGER.info("POI byte array max override configurado a Integer.MAX_VALUE");
    }

    /** Hojas de datos que se limpian al sanitizar el template. */
    private static final Set<String> HOJAS_DATOS = new HashSet<>(Arrays.asList("FACT", "Total Empresa", "Calendario"));

    @Value("${path.directory.server}")
    private String directorioServer;

    @Value("${path.directory.server_path_clientes_insights}")
    private String clientesSubdir;

    /**
     * Directorio de templates para un cliente específico.
     * Los templates ahora se guardan junto con filtros_base y datos_base del cliente.
     */
    public String getTemplatesDir(String codCliente) {
        return directorioServer + File.separator + clientesSubdir
               + File.separator + codCliente.trim().toUpperCase();
    }

    /**
     * Guarda el archivo Excel subido como template específico para el cliente, categoría y tipo de reporte dados.
     * Después de guardar, sanitiza el template eliminando datos residuales y conexiones externas.
     *
     * @param archivo      archivo Excel (.xlsx) subido por el usuario
     * @param codCliente   código del cliente (ej: BIMBO)
     * @param codCategoria código de la categoría (ej: BEBIDAS)
     * @param tipoReporte  tipo de reporte (NORMAL o CADENA)
     * @return nombre del archivo guardado
     */
    public String guardarTemplate(MultipartFile archivo, String codCliente, String codCategoria,
                                   TipoReporte tipoReporte, boolean sanitizar) {
        validarArchivoExcel(archivo);

        String nombreArchivo = buildNombreArchivo(codCliente, codCategoria, tipoReporte);

        try {
            File directorio = Paths.get(getTemplatesDir(codCliente)).toFile();

            if (!directorio.exists()) {
                directorio.mkdirs();
            }

            File destino = new File(directorio, nombreArchivo);
            archivo.transferTo(destino);

            LOGGER.info("Template guardado: {}", destino.getAbsolutePath());

            if (sanitizar) {
                sanitizarTemplate(destino);
            } else {
                LOGGER.info("Sanitización omitida por parámetro del usuario.");
            }

            return nombreArchivo;

        } catch (IOException e) {
            LOGGER.error("Error al guardar template '{}': {}", nombreArchivo, e.getMessage(), e);
            throw new InternalServerErrorException("Error al guardar el template: " + e.getMessage());
        }
    }

    /**
     * Sanitiza un template Excel en disco:
     * 1. Elimina filas de datos de FACT, Total Empresa y Calendario (preserva headers).
     * 2. Desconecta tablas de fuentes externas (Power Query / queryTable).
     * 3. Ajusta los rangos de las tablas al mínimo (header + 1 fila).
     * 4. Elimina las conexiones externas del workbook.
     */
    private void sanitizarTemplate(File archivo) throws IOException {
        LOGGER.info("Sanitizando template: {}", archivo.getAbsolutePath());
        long tamanoOriginal = archivo.length();

        try (FileInputStream fis = new FileInputStream(archivo);
             XSSFWorkbook wb = new XSSFWorkbook(fis)) {

            int filasEliminadas = 0;
            int tablasDesconectadas = 0;

            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                XSSFSheet sheet = wb.getSheetAt(i);
                String sheetName = sheet.getSheetName();

                // 1. Limpiar filas de datos de las hojas conocidas
                if (HOJAS_DATOS.contains(sheetName)) {
                    filasEliminadas += limpiarDatosHoja(sheet);
                }

                // 2. Desconectar tablas de fuentes externas y ajustar rangos
                for (XSSFTable table : sheet.getTables()) {
                    if (desconectarTabla(table)) {
                        tablasDesconectadas++;
                    }
                    if (HOJAS_DATOS.contains(sheetName)) {
                        ajustarRangoTabla(table);
                    }
                }
            }

            // 3. Eliminar conexiones externas del workbook
            eliminarConexiones(wb);

            // Guardar el workbook sanitizado
            try (FileOutputStream fos = new FileOutputStream(archivo)) {
                wb.write(fos);
            }

            long tamanoFinal = archivo.length();
            long reduccionKb = (tamanoOriginal - tamanoFinal) / 1024;
            LOGGER.info("Template sanitizado: {} filas eliminadas, {} tablas desconectadas, "
                        + "reducción de {}KB ({} -> {} bytes)",
                        filasEliminadas, tablasDesconectadas, reduccionKb, tamanoOriginal, tamanoFinal);
        }
    }

    /**
     * Elimina todas las filas de datos (preservando el header en fila 0) de una hoja.
     *
     * @return cantidad de filas eliminadas
     */
    private int limpiarDatosHoja(XSSFSheet sheet) {
        int lastRow = sheet.getLastRowNum();
        if (lastRow < 1) return 0;

        int removed = 0;
        for (int i = lastRow; i >= 1; i--) {
            XSSFRow row = sheet.getRow(i);
            if (row != null) {
                sheet.removeRow(row);
                removed++;
            }
        }
        LOGGER.info("  Hoja '{}': eliminadas {} filas de datos.", sheet.getSheetName(), removed);
        return removed;
    }

    /**
     * Elimina los atributos tableType="queryTable" y connectionId de una tabla,
     * convirtiéndola de "conectada a Power Query" a tabla regular de Excel.
     *
     * @return true si la tabla fue modificada
     */
    private boolean desconectarTabla(XSSFTable table) {
        XmlCursor cursor = table.getCTTable().newCursor();
        boolean changed = false;
        try {
            if (cursor.toFirstAttribute()) {
                do {
                    String name = cursor.getName().getLocalPart();
                    if ("tableType".equals(name) || "connectionId".equals(name)) {
                        cursor.removeXml();
                        changed = true;
                    }
                } while (cursor.toNextAttribute());
            }
        } finally {
            cursor.dispose();
        }
        if (changed) {
            LOGGER.info("  Tabla '{}' desconectada de fuente externa.", table.getName());
        }
        return changed;
    }

    /**
     * Ajusta el rango de una tabla al mínimo: header + 1 fila vacía.
     * El código de generación ({@link ReporteInsService#actualizarRangosTablas})
     * redimensiona las tablas al tamaño real de los datos al generar.
     */
    private void ajustarRangoTabla(XSSFTable table) {
        AreaReference oldArea = table.getArea();
        if (oldArea == null) return;

        CellReference lastCell = oldArea.getLastCell();
        CellReference newEnd = new CellReference(1, lastCell.getCol()); // fila 1 = segunda fila (0-based)
        AreaReference newArea = new AreaReference(
                new CellReference(0, 0), newEnd,
                org.apache.poi.ss.SpreadsheetVersion.EXCEL2007);

        String oldRef = oldArea.formatAsString();
        table.setArea(newArea);
        LOGGER.info("  Tabla '{}': rango {} -> {}", table.getName(), oldRef, newArea.formatAsString());
    }

    /**
     * Elimina el elemento &lt;connections&gt; del workbook XML, que contiene las
     * definiciones de Power Query y Data Model. Usa XmlCursor porque
     * poi-ooxml-lite no expone isSetConnections()/unsetConnections() en CTWorkbook.
     */
    private void eliminarConexiones(XSSFWorkbook wb) {
        XmlCursor cursor = wb.getCTWorkbook().newCursor();
        try {
            // Recorrer hijos directos de <workbook> buscando <connections>
            if (cursor.toFirstChild()) {
                do {
                    if ("connections".equals(cursor.getName().getLocalPart())) {
                        cursor.removeXml();
                        LOGGER.info("  Eliminado elemento <connections> del workbook.");
                        break;
                    }
                } while (cursor.toNextSibling());
            }
        } finally {
            cursor.dispose();
        }
    }

    /**
     * Construye el nombre de archivo del template según la nomenclatura del proyecto.
     * Ejemplo: template_normal_BIMBO_BEBIDAS.xlsx
     */
    private String buildNombreArchivo(String codCliente, String codCategoria, TipoReporte tipoReporte) {
        return tipoReporte.getTemplateFileName(codCliente, codCategoria);
    }

    private void validarArchivoExcel(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new UnknownResourceException("El archivo del template no puede estar vacío.");
        }
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || !nombre.toLowerCase().endsWith(".xlsx")) {
            throw new UnknownResourceException("El template debe ser un archivo Excel (.xlsx).");
        }
    }
}
