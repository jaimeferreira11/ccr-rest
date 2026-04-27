
package py.com.jaimeferreira.ccr.jhonson.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import javax.jws.WebParam;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import net.sf.jasperreports.engine.JRException;
import py.com.jaimeferreira.ccr.commons.exception.UnknownResourceException;
import py.com.jaimeferreira.ccr.jhonson.dto.ImageRequestDTO;
import py.com.jaimeferreira.ccr.jhonson.dto.ImagenBocaMesDTO;
import py.com.jaimeferreira.ccr.jhonson.entity.BocaSCJ;
import py.com.jaimeferreira.ccr.jhonson.entity.DistribuidorSCJ;
import py.com.jaimeferreira.ccr.jhonson.entity.ReporteSCJ;
import py.com.jaimeferreira.ccr.jhonson.service.BocasSCJService;
import py.com.jaimeferreira.ccr.jhonson.service.DistribuidoresSCJService;
import py.com.jaimeferreira.ccr.jhonson.service.ImagenesSCJService;
import py.com.jaimeferreira.ccr.jhonson.service.ReportesSCJService;

/**
 * Controller para d-reports de SCJ (Johnson).
 * Endpoints del módulo web de reportes: distribuidores, bocas por distribuidor, imágenes, reportes PDF/PPT.
 *
 * @author Jaime Ferreira
 */

@RestController
@RequestMapping(value = "jhonson/d-reports/api/v1")
public class JhonsonDReportsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(JhonsonDReportsController.class);

    @Autowired
    private BocasSCJService bocasService;

    @Autowired
    private ImagenesSCJService imagenesService;

    @Autowired
    private DistribuidoresSCJService distribuidoresService;

    @Autowired
    private ReportesSCJService reportesService;

    /*
     * Distribuidores
     */

    @GetMapping(value = "/distribuidores", produces = "application/json")
    public ResponseEntity<List<DistribuidorSCJ>> distribuidores() {
        LOGGER.info("Obteniendo todos los distribuidores...");
        return ResponseEntity.status(HttpStatus.OK).body(distribuidoresService.findActivos());
    }

    /*
     * Bocas
     */
    @GetMapping(value = "/bocas", produces = "application/json")
    public ResponseEntity<List<BocaSCJ>> bocas() {
        LOGGER.info("Obteniendo todas las bocas...");
        return ResponseEntity.status(HttpStatus.OK).body(bocasService.list());
    }

    @GetMapping(value = "/bocas/distribuidor/{codigo}", produces = "application/json")
    public ResponseEntity<List<BocaSCJ>> bocasByDistribuidor(@PathVariable("codigo") String codigo) {
        LOGGER.info("Obteniendo todas las bocas por distribuidor...");
        return ResponseEntity.status(HttpStatus.OK).body(bocasService.findByDistribuidor(codigo));
    }

    /*
     * Imagenes
     */

    @GetMapping(value = "/imagenes/directorios", produces = "application/json")
    public ResponseEntity<List<String>> directorios() {
        LOGGER.info("Obteniendo todos los directorios...");
        return ResponseEntity.status(HttpStatus.OK).body(imagenesService.readMainFolders());
    }

    @GetMapping(value = "/imagenes/boca/{codigo}", produces = "application/json")
    public ResponseEntity<List<String>> imagenesBoca(@PathVariable("codigo") String codigo,
                                                     @WebParam(name = "mes") String mes,
                                                     @WebParam(name = "anio") String anio) {
        return ResponseEntity.status(HttpStatus.OK).body(imagenesService.findByBocaAndMes(codigo, mes, anio));
    }

    @GetMapping(value = "/imagenes/boca/{codigo}/meses", produces = "application/json")
    public ResponseEntity<List<ImagenBocaMesDTO>> imagenesBocaMeses(@PathVariable("codigo") String codigo) {
        return ResponseEntity.status(HttpStatus.OK).body(imagenesService.findMesByBoca(codigo));
    }

    @PostMapping(value = "/imagenes/externo", produces = "application/json")
    public ResponseEntity<String> saveExternalImage(@Valid @RequestBody ImageRequestDTO body) {
        LOGGER.info(body.getExtension());
        return ResponseEntity.status(HttpStatus.OK)
                             .body(imagenesService.saveImage(body.getImage(), body.getExtension()));
    }

    /*
     * Reportes
     */

    @PostMapping(value = "/reportes")
    @ResponseBody
    public ResponseEntity<?>
           saveReporte(@Valid @RequestBody ReporteSCJ reporte) {
        try {
            int id = reportesService.save(reporte);
            return ResponseEntity.status(HttpStatus.CREATED).body(id);

        }
        catch (UnknownResourceException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(e.getMessage());
        }
        catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error interno del servidor");
        }
    }

    @GetMapping("/reportes/last")
    public ResponseEntity<List<ReporteSCJ>> getUltimosReportes(
                                                                HttpServletResponse response) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        return ResponseEntity.ok(reportesService.getLastByUser(currentUsername, 10));
    }

    @GetMapping("/reportes/{idReporte}")
    public ResponseEntity<byte[]> getReporte(@PathVariable("idReporte") Integer idReporte,
                                             @WebParam(name = "format") String format)
                                                                                       throws JRException, SQLException,
                                                                                       IOException {
        try {
            if (format == null || format.isEmpty()) {
                format = "pdf";
            }
            byte[] pdfBytes;
            StringBuilder fileName = new StringBuilder().append("reporte_").append(idReporte.toString())
                                                        .append("_")
                                                        .append(new Timestamp(System.currentTimeMillis()).getTime());

            if (format.equalsIgnoreCase("ppt")) {
                pdfBytes = reportesService.generarReportePPT(idReporte);
                fileName.append(".pptx");
            }
            else {
                pdfBytes = reportesService.generarReporte(idReporte);
                fileName.append(".pdf");
            }

            LOGGER.info("Descargar archivo: " + fileName.toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                                                            .filename(fileName.toString())
                                                            .build());

            return ResponseEntity.ok()
                                 .headers(headers)
                                 .body(pdfBytes);
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(null);
        }
    }

}
