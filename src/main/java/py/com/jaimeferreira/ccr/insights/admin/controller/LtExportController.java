package py.com.jaimeferreira.ccr.insights.admin.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import py.com.jaimeferreira.ccr.insights.admin.service.LtExportService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

/**
 * Export administrativo en CSV de las tablas del módulo lt (integración con proveedor POS externo).
 * Requiere JWT válido (fuera de /lt/**, que usa API key estática del proveedor).
 *
 * @author Jaime Ferreira
 */
@RestController
@RequestMapping("/insights/api/v1/admin/lt")
public class LtExportController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LtExportController.class);
    private static final DateTimeFormatter NOMBRE_ARCHIVO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private LtExportService ltExportService;

    @GetMapping("/tickets/export")
    public ResponseEntity<?> exportarTickets(
            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        if (desde.isAfter(hasta)) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Collections.singletonMap("mensaje", "El parámetro 'desde' no puede ser posterior a 'hasta'."));
        }

        String usuario = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' exporta lt.ticket a CSV. desde={}, hasta={}", usuario, desde, hasta);

        byte[] csv = ltExportService.exportarTickets(desde, hasta);
        String nombreArchivo = "lt_tickets_" + desde.format(NOMBRE_ARCHIVO_FORMATTER)
                + "_" + hasta.format(NOMBRE_ARCHIVO_FORMATTER) + ".csv";

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentLength(csv.length)
                .body(new ByteArrayResource(csv));
    }

    @GetMapping("/productos/export")
    public ResponseEntity<?> exportarProductos() {
        String usuario = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' exporta lt.producto a CSV.", usuario);

        byte[] csv = ltExportService.exportarProductos();
        String nombreArchivo = "lt_productos_" + LocalDateTime.now().format(NOMBRE_ARCHIVO_FORMATTER) + ".csv";

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentLength(csv.length)
                .body(new ByteArrayResource(csv));
    }

    @GetMapping("/personas/export")
    public ResponseEntity<?> exportarPersonas() {
        String usuario = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' exporta lt.persona a CSV.", usuario);

        byte[] csv = ltExportService.exportarPersonas();
        String nombreArchivo = "lt_personas_" + LocalDateTime.now().format(NOMBRE_ARCHIVO_FORMATTER) + ".csv";

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentLength(csv.length)
                .body(new ByteArrayResource(csv));
    }

    @GetMapping("/sucursales/export")
    public ResponseEntity<?> exportarSucursales() {
        String usuario = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Usuario '{}' exporta lt.sucursal a CSV.", usuario);

        byte[] csv = ltExportService.exportarSucursales();
        String nombreArchivo = "lt_sucursales_" + LocalDateTime.now().format(NOMBRE_ARCHIVO_FORMATTER) + ".csv";

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentLength(csv.length)
                .body(new ByteArrayResource(csv));
    }
}
