package py.com.jaimeferreira.ccr.insights.admin.controller;

import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import py.com.jaimeferreira.ccr.insights.admin.dto.PlataformaEstadoRequestDTO;
import py.com.jaimeferreira.ccr.insights.admin.entity.PlataformaConfig;
import py.com.jaimeferreira.ccr.insights.admin.service.PlataformaService;
import py.com.jaimeferreira.ccr.insights.dto.ClienteInsDTO;
import py.com.jaimeferreira.ccr.insights.dto.PaisDTO;
import py.com.jaimeferreira.ccr.insights.service.ClienteInsService;
import py.com.jaimeferreira.ccr.insights.service.PaisInsService;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import py.com.jaimeferreira.ccr.commons.exception.UnknownResourceException;
import py.com.jaimeferreira.ccr.insights.entity.TipoReporte;
import py.com.jaimeferreira.ccr.insights.service.ReporteInsService;
import py.com.jaimeferreira.ccr.insights.service.TemplateInsService;

/**
 * Endpoints de administración de la plataforma.
 * Requieren JWT válido.
 *
 * @author Jaime Ferreira
 */
@RestController
@RequestMapping("/insights/api/v1/admin")
public class AdminPlataformaController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminPlataformaController.class);

    @Autowired
    private PlataformaService plataformaService;

    @Autowired
    private ClienteInsService clienteInsService;

    @Autowired
    private PaisInsService paisInsService;

    @Autowired
    private ReporteInsService reporteInsService;

    @Autowired
    private TemplateInsService templateInsService;

    /** Retorna el estado actual de la plataforma. */
    @GetMapping(value = "/plataforma", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getEstado() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("activa", plataformaService.isActiva());
        resp.put("mensaje", plataformaService.getMensajeSuspension());
        return ResponseEntity.ok(resp);
    }

    /**
     * Habilita o suspende la plataforma.
     * Body: { "activa": true/false }
     */
    @PutMapping(value = "/plataforma", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> setEstado(@RequestBody PlataformaEstadoRequestDTO req) {
        String usuario = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.warn("Usuario '{}' cambia estado de plataforma a: {}", usuario, req.isActiva());

        PlataformaConfig config = plataformaService.setEstado(req.isActiva(), usuario);

        Map<String, Object> resp = new HashMap<>();
        resp.put("activa", config.getActiva());
        resp.put("mensaje", config.getMensajeSuspension());
        resp.put("actualizadoPor", config.getNombreUsuarioActualizacion());
        return ResponseEntity.ok(resp);
    }

    @GetMapping(value = "/clientes", produces = "application/json")
    public ResponseEntity<List<ClienteInsDTO>> getClientes() {
        LOGGER.info("Listando clientes insights para administracion");
        List<ClienteInsDTO> clientes = clienteInsService.findAll().stream()
                .map(ClienteInsDTO::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(clientes);
    }

    @GetMapping(value = "/clientes/{codigo}", produces = "application/json")
    public ResponseEntity<ClienteInsDTO> getCliente(@PathVariable String codigo) {
        LOGGER.info("Consultando cliente insights: {}", codigo);
        return ResponseEntity.ok(ClienteInsDTO.from(clienteInsService.findByCodigo(codigo)));
    }

    @PostMapping(value = "/clientes", consumes = "application/json", produces = "application/json")
    public ResponseEntity<ClienteInsDTO> crearCliente(@RequestBody ClienteInsDTO req) {
        String usuario = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' crea cliente insights: {}", usuario, req.getCodigo());
        return ResponseEntity.status(HttpStatus.CREATED).body(ClienteInsDTO.from(clienteInsService.save(req, usuario)));
    }

    @PutMapping(value = "/clientes/{codigo}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<ClienteInsDTO> actualizarCliente(@PathVariable String codigo, @RequestBody ClienteInsDTO req) {
        String usuario = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' actualiza cliente insights: {}", usuario, codigo);
        return ResponseEntity.ok(ClienteInsDTO.from(clienteInsService.update(codigo, req, usuario)));
    }

    @DeleteMapping(value = "/clientes/{codigo}", produces = "application/json")
    public ResponseEntity<ClienteInsDTO> eliminarCliente(@PathVariable String codigo) {
        String usuario = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' da de baja cliente insights: {}", usuario, codigo);
        return ResponseEntity.ok(ClienteInsDTO.from(clienteInsService.disable(codigo, usuario)));
    }

    @GetMapping(value = "/paises", produces = "application/json")
    public ResponseEntity<List<PaisDTO>> getPaises() {
        LOGGER.info("Listando paises insights para administracion");
        List<PaisDTO> paises = paisInsService.findAll().stream()
                .map(PaisDTO::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(paises);
    }

    @GetMapping(value = "/paises/{codigo}", produces = "application/json")
    public ResponseEntity<PaisDTO> getPais(@PathVariable String codigo) {
        LOGGER.info("Consultando pais insights: {}", codigo);
        return ResponseEntity.ok(PaisDTO.from(paisInsService.findByCodigo(codigo)));
    }

    @PostMapping(value = "/paises", consumes = "application/json", produces = "application/json")
    public ResponseEntity<PaisDTO> crearPais(@RequestBody PaisDTO req) {
        String usuario = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' crea pais insights: {}", usuario, req.getCodigo());
        return ResponseEntity.status(HttpStatus.CREATED).body(PaisDTO.from(paisInsService.save(req, usuario)));
    }

    @PutMapping(value = "/paises/{codigo}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<PaisDTO> actualizarPais(@PathVariable String codigo, @RequestBody PaisDTO req) {
        String usuario = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' actualiza pais insights: {}", usuario, codigo);
        return ResponseEntity.ok(PaisDTO.from(paisInsService.update(codigo, req, usuario)));
    }

    @DeleteMapping(value = "/paises/{codigo}", produces = "application/json")
    public ResponseEntity<PaisDTO> eliminarPais(@PathVariable String codigo) {
        String usuario = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' da de baja pais insights: {}", usuario, codigo);
        return ResponseEntity.ok(PaisDTO.from(paisInsService.disable(codigo, usuario)));
    }

    /**
     * Sube archivos base para un cliente específico.
     * Los 3 archivos son opcionales; se procesa solo lo que se envía.
     * Si se envía template, tipoReporte es obligatorio.
     *
     * @param codCliente    código del cliente
     * @param template      archivo Excel (.xlsx) del template (opcional)
     * @param tipoReporte   tipo de reporte: NORMAL o CADENA (obligatorio si se envía template)
     * @param csvFiltrosBase archivo CSV de filtros base (opcional)
     * @param csvDatosBase   archivo CSV de datos base (opcional)
     */
    @PostMapping(value = "/clientes/{codCliente}/archivos-base",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
                 produces = "application/json")
    public ResponseEntity<Map<String, String>> subirArchivosBase(
            @PathVariable String codCliente,
            @RequestParam(value = "template", required = false) MultipartFile template,
            @RequestParam(value = "tipoReporte", required = false) String tipoReporte,
            @RequestParam(value = "csvFiltrosBase", required = false) MultipartFile csvFiltrosBase,
            @RequestParam(value = "csvDatosBase", required = false) MultipartFile csvDatosBase) {

        String usuario = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' sube archivos base para cliente: {}", usuario, codCliente);

        boolean hayTemplate = template != null && !template.isEmpty();
        boolean hayFiltros  = csvFiltrosBase != null && !csvFiltrosBase.isEmpty();
        boolean hayDatos    = csvDatosBase != null && !csvDatosBase.isEmpty();

        if (!hayTemplate && !hayFiltros && !hayDatos) {
            throw new UnknownResourceException("Debe adjuntar al menos un archivo.");
        }

        clienteInsService.findByCodigo(codCliente.trim().toUpperCase());

        Map<String, String> response = new HashMap<>();

        if (hayTemplate) {
            if (tipoReporte == null || tipoReporte.trim().isEmpty()) {
                throw new UnknownResourceException("Debe indicar el tipo de reporte (NORMAL o CADENA) al subir un template.");
            }
            TipoReporte tipo;
            try {
                tipo = TipoReporte.valueOf(tipoReporte.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new UnknownResourceException("Tipo de reporte inválido: " + tipoReporte
                        + ". Valores válidos: NORMAL, CADENA");
            }
            String nombreArchivo = templateInsService.guardarTemplate(template, codCliente, tipo);
            response.put("template", nombreArchivo + " guardado correctamente");
            LOGGER.info("Template guardado para cliente: {}", codCliente);
        }

        if (hayFiltros) {
            validarCsv(csvFiltrosBase, "filtros base");
            reporteInsService.guardarArchivoBase(csvFiltrosBase, codCliente, "filtros_base.csv");
            response.put("filtrosBase", "filtros_base.csv guardado correctamente");
            LOGGER.info("Filtros base guardados para cliente: {}", codCliente);
        }

        if (hayDatos) {
            validarCsv(csvDatosBase, "datos base");
            reporteInsService.guardarArchivoBase(csvDatosBase, codCliente, "datos_base.csv");
            response.put("datosBase", "datos_base.csv guardado correctamente");
            LOGGER.info("Datos base guardados para cliente: {}", codCliente);
        }

        response.put("cliente", codCliente.trim().toUpperCase());
        response.put("mensaje", "Archivos base guardados correctamente");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private void validarCsv(MultipartFile archivo, String descripcion) {
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || !nombre.toLowerCase().endsWith(".csv")) {
            throw new UnknownResourceException("El archivo de " + descripcion + " debe ser un CSV (.csv).");
        }
    }
}
