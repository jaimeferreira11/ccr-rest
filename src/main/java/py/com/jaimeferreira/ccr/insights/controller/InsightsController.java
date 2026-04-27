package py.com.jaimeferreira.ccr.insights.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import py.com.jaimeferreira.ccr.commons.exception.InternalServerErrorException;
import py.com.jaimeferreira.ccr.commons.exception.UnknownResourceException;
import py.com.jaimeferreira.ccr.insights.dto.CategoriaDTO;
import py.com.jaimeferreira.ccr.insights.dto.InformeDTO;
import py.com.jaimeferreira.ccr.insights.dto.InformePageDTO;
import py.com.jaimeferreira.ccr.insights.entity.ClienteIns;
import py.com.jaimeferreira.ccr.insights.entity.EstadoInforme;
import py.com.jaimeferreira.ccr.insights.entity.InformeIns;
import py.com.jaimeferreira.ccr.insights.entity.Pais;
import py.com.jaimeferreira.ccr.insights.entity.TipoReporte;
import py.com.jaimeferreira.ccr.insights.service.CategoriaService;
import py.com.jaimeferreira.ccr.insights.service.ClienteInsService;
import py.com.jaimeferreira.ccr.insights.service.InformeInsService;
import py.com.jaimeferreira.ccr.insights.service.PaisInsService;
import py.com.jaimeferreira.ccr.insights.service.ReporteInsService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;




/**
 *
 * @author Jaime Ferreira
 */
@RestController
@RequestMapping(value = "insights/api/v1")
public class InsightsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(InsightsController.class);

    @Autowired
    private PaisInsService paisService;

    @Autowired
    private ClienteInsService clienteService;

    @Autowired
    private InformeInsService informeService;

    @Autowired
    private ReporteInsService reporteService;

    @Autowired
    private CategoriaService categoriaService;


    @GetMapping(value = "/paises", produces = "application/json")
    public ResponseEntity<List<Pais>> paises() {
        LOGGER.info("Obteniendo todos los paises activos...");
        return ResponseEntity.status(HttpStatus.OK).body(paisService.findActivos());
    }

    @GetMapping(value = "/clientes/{codPais}", produces = "application/json")
    public ResponseEntity<List<ClienteIns>> clientesByPais(@PathVariable String codPais) {
        LOGGER.info("Obteniendo clientes del pais: {}", codPais);
        return ResponseEntity.status(HttpStatus.OK).body(clienteService.findByPais(codPais.trim().toUpperCase()));
    }

    @GetMapping(value = "/categorias/{codCliente}", produces = "application/json")
    public ResponseEntity<List<CategoriaDTO>> categoriasByCliente(@PathVariable String codCliente) {
        LOGGER.info("Obteniendo categorias activas del cliente: {}", codCliente);
        List<CategoriaDTO> categorias = categoriaService.findActivasByCliente(codCliente)
                .stream()
                .map(CategoriaDTO::from)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(categorias);
    }

    /**
     * Inicia la generación de un informe Excel de forma asíncrona.
     * Retorna el registro del informe con estado PROCESANDO inmediatamente.
     *
     * @param csvData      archivo CSV con los datos subidos por el usuario
     * @param csvFiltros   archivo CSV con los filtros a aplicar sobre los datos
     * @param codCliente   código del cliente seleccionado
     * @param codCategoria código de la categoría seleccionada (determina el template a usar)
     * @param tipoReporte  tipo de reporte: NORMAL o CADENA
     */
    @PostMapping(value = "/reportes/generar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "application/json")
    public ResponseEntity<InformeDTO> generarInforme(
            @RequestParam("csvData") MultipartFile csvData,
            @RequestParam(value = "csvFiltros", required = false) MultipartFile csvFiltros,
            @RequestParam("codCliente") String codCliente,
            @RequestParam("codCategoria") String codCategoria,
            @RequestParam("tipoReporte") String tipoReporte) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = auth.getName();

        LOGGER.info("Solicitud de generación de informe. Cliente: {}, Categoria: {}, Tipo: {}, Usuario: {}, filtrosAdjuntos: {}",
                codCliente, codCategoria, tipoReporte, usuario, (csvFiltros != null && !csvFiltros.isEmpty()));

        if (!org.springframework.util.StringUtils.hasText(codCategoria)) {
            throw new UnknownResourceException("El código de categoría es requerido.");
        }

        TipoReporte tipo;
        try {
            tipo = TipoReporte.valueOf(tipoReporte.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new UnknownResourceException("Tipo de reporte inválido: " + tipoReporte
                    + ". Valores válidos: NORMAL, CADENA");
        }

        if (csvData.isEmpty()) {
            throw new UnknownResourceException("El archivo CSV de datos no puede estar vacío.");
        }

        InformeIns informe = reporteService.iniciarGeneracion(csvData, csvFiltros, codCliente, codCategoria, tipo, usuario);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(InformeDTO.from(informe));
    }

    /**
     * Lista los informes del usuario autenticado con paginación y filtros opcionales.
     *
     * @param estado     filtro por estado del informe (PROCESANDO, COMPLETADO, ERROR)
     * @param codCliente filtro por código de cliente
     * @param page       número de página (base 0, default 0)
     * @param size       cantidad de resultados por página (default 10)
     */
    @GetMapping(value = "/reportes", produces = "application/json")
    public ResponseEntity<InformePageDTO> listarInformes(
            @RequestParam(value = "estado", required = false) String estado,
            @RequestParam(value = "codCliente", required = false) String codCliente,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = auth.getName();
        LOGGER.info("Listando informes del usuario: {}, estado: {}, codCliente: {}, page: {}, size: {}",
                usuario, estado, codCliente, page, size);

        EstadoInforme estadoEnum = (estado != null && !estado.isEmpty())
                ? EstadoInforme.valueOf(estado.trim().toUpperCase())
                : null;

        return ResponseEntity.ok(informeService.findUltimos(usuario, codCliente, estadoEnum, page, size));
    }

    /**
     * Retorna el detalle/estado de un informe por ID.
     */
    @GetMapping(value = "/reportes/{id}", produces = "application/json")
    public ResponseEntity<InformeDTO> getInforme(@PathVariable Long id) {
        LOGGER.info("Consultando informe id={}", id);
        return ResponseEntity.ok(InformeDTO.from(informeService.findById(id)));
    }

    /**
     * Descarga el archivo Excel de un informe completado.
     */
    @GetMapping(value = "/reportes/{id}/descargar")
    public ResponseEntity<InputStreamResource> descargarInforme(@PathVariable Long id) {
        LOGGER.info("Descargando informe id={}", id);

        InformeIns informe = informeService.findById(id);

        if (informe.getEstado() == EstadoInforme.PROCESANDO) {
            throw new UnknownResourceException("El informe aun se esta generando, descargue en unos minutos.");
        }

        if (informe.getEstado() != EstadoInforme.COMPLETADO || informe.getNombreArchivo() == null) {
            throw new UnknownResourceException("El informe id=" + id + " no esta disponible (estado: " + informe.getEstado() + ").");
        }

        String ruta = reporteService.getRutaArchivo(informe.getNombreArchivo());
        File archivo = new File(ruta);

        if (!archivo.exists()) {
            throw new UnknownResourceException("Archivo no encontrado en disco: " + informe.getNombreArchivo());
        }

        try {
            InputStreamResource resource = new InputStreamResource(new FileInputStream(archivo));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + informe.getNombreArchivo() + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(archivo.length())
                    .body(resource);
        } catch (IOException e) {
            throw new InternalServerErrorException("Error al leer el archivo del informe: " + e.getMessage());
        }
    }

    /**
     * Elimina físicamente un informe de la base de datos. Solo permite eliminar informes en estado ERROR.
     */
    @DeleteMapping(value = "/reportes/{id}")
    public ResponseEntity<Void> eliminarInforme(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = auth.getName();
        LOGGER.info("Solicitud de eliminacion de informe id={} por usuario: {}", id, usuario);
        informeService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

}
