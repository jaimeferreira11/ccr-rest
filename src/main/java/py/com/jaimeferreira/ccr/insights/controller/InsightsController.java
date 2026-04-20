package py.com.jaimeferreira.ccr.insights.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import py.com.jaimeferreira.ccr.commons.exception.InternalServerErrorException;
import py.com.jaimeferreira.ccr.commons.exception.UnknownResourceException;
import py.com.jaimeferreira.ccr.insights.dto.InformeDTO;
import py.com.jaimeferreira.ccr.insights.entity.ClienteIns;
import py.com.jaimeferreira.ccr.insights.entity.EstadoInforme;
import py.com.jaimeferreira.ccr.insights.entity.InformeIns;
import py.com.jaimeferreira.ccr.insights.entity.Pais;
import py.com.jaimeferreira.ccr.insights.entity.TipoReporte;
import py.com.jaimeferreira.ccr.insights.service.ClienteInsService;
import py.com.jaimeferreira.ccr.insights.service.InformeInsService;
import py.com.jaimeferreira.ccr.insights.service.PaisInsService;
import py.com.jaimeferreira.ccr.insights.service.ReporteInsService;


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

    /**
     * Inicia la generación de un informe Excel de forma asíncrona.
     * Retorna el registro del informe con estado PROCESANDO inmediatamente.
     *
     * @param csvData     archivo CSV con los datos subidos por el usuario
     * @param csvFiltros  archivo CSV con los filtros a aplicar sobre los datos
     * @param codCliente  código del cliente seleccionado
     * @param tipoReporte tipo de reporte: NORMAL o CADENA
     */
    @PostMapping(value = "/reportes/generar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "application/json")
    public ResponseEntity<InformeDTO> generarInforme(
            @RequestParam("csvData") MultipartFile csvData,
            @RequestParam(value = "csvFiltros", required = false) MultipartFile csvFiltros,
            @RequestParam("codCliente") String codCliente,
            @RequestParam("tipoReporte") String tipoReporte) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = auth.getName();

        LOGGER.info("Solicitud de generación de informe. Cliente: {}, Tipo: {}, Usuario: {}, filtrosAdjuntos: {}",
                codCliente, tipoReporte, usuario, (csvFiltros != null && !csvFiltros.isEmpty()));

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

        InformeIns informe = reporteService.iniciarGeneracion(csvData, csvFiltros, codCliente, tipo, usuario);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(InformeDTO.from(informe));
    }

    /**
     * Retorna los últimos 10 informes del usuario autenticado, ordenados por más recientes.
     */
    @GetMapping(value = "/reportes", produces = "application/json")
    public ResponseEntity<List<InformeDTO>> listarInformes(
            @RequestParam(value = "estado", required = false) String estado) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = auth.getName();
        LOGGER.info("Listando últimos informes del usuario: {}, filtro estado: {}", usuario, estado);

        List<InformeIns> informes;
        if (estado != null && !estado.isEmpty()) {
            EstadoInforme estadoEnum = EstadoInforme.valueOf(estado.trim().toUpperCase());
            informes = informeService.findUltimos(usuario, estadoEnum);
        } else {
            informes = informeService.findUltimos(usuario);
        }
        return ResponseEntity.ok(informes.stream().map(InformeDTO::from).collect(Collectors.toList()));
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

}
