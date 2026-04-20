package py.com.jaimeferreira.ccr.insights.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import py.com.jaimeferreira.ccr.commons.exception.InternalServerErrorException;
import py.com.jaimeferreira.ccr.commons.exception.UnknownResourceException;
import py.com.jaimeferreira.ccr.insights.entity.TipoReporte;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * Servicio para gestión de templates Excel del módulo Insights.
 * Los templates se guardan en {path.directory.server}/{path.directory.server_path_templates_insights}/
 * con la nomenclatura: template_{tipo}_{CODCLIENTE}.xlsx
 *
 * @author Jaime Ferreira
 */
@Service
public class TemplateInsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateInsService.class);

    @Value("${path.directory.server}")
    private String directorioServer;

    @Value("${path.directory.server_path_templates_insights}")
    private String templatesSubdir;

    public String getTemplatesDir() {
        return directorioServer + File.separator + templatesSubdir;
    }

    /**
     * Guarda el archivo Excel subido como template específico para el cliente y tipo de reporte dados.
     * El archivo se almacena en el directorio de resources/insights/ del classpath (target/classes/insights/).
     *
     * @param archivo     archivo Excel (.xlsx) subido por el usuario
     * @param codCliente  código del cliente (ej: BIMBO)
     * @param tipoReporte tipo de reporte (NORMAL o CADENA)
     * @return nombre del archivo guardado
     */
    public String guardarTemplate(MultipartFile archivo, String codCliente, TipoReporte tipoReporte) {
        validarArchivoExcel(archivo);

        String nombreArchivo = buildNombreArchivo(codCliente, tipoReporte);

        try {
            File directorio = Paths.get(getTemplatesDir()).toFile();

            if (!directorio.exists()) {
                directorio.mkdirs();
            }

            File destino = new File(directorio, nombreArchivo);
            archivo.transferTo(destino);

            LOGGER.info("Template guardado: {}", destino.getAbsolutePath());
            return nombreArchivo;

        } catch (IOException e) {
            LOGGER.error("Error al guardar template '{}': {}", nombreArchivo, e.getMessage(), e);
            throw new InternalServerErrorException("Error al guardar el template: " + e.getMessage());
        }
    }

    /**
     * Construye el nombre de archivo del template según la nomenclatura del proyecto.
     * Ejemplo: template_normal_BIMBO.xlsx
     */
    private String buildNombreArchivo(String codCliente, TipoReporte tipoReporte) {
        return "template_" + tipoReporte.name().toLowerCase()
                + "_" + codCliente.trim().toUpperCase()
                + ".xlsx";
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
