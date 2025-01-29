
package py.com.jaimeferreira.ccr.nestle.controller;

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
import py.com.jaimeferreira.ccr.nestle.dto.ImageRequestDTO;
import py.com.jaimeferreira.ccr.nestle.dto.ImagenBocaMesDTO;
import py.com.jaimeferreira.ccr.nestle.entity.BocaNest;
import py.com.jaimeferreira.ccr.nestle.entity.DistribuidorNest;
import py.com.jaimeferreira.ccr.nestle.entity.ReporteNest;
import py.com.jaimeferreira.ccr.nestle.service.BocasNestService;
import py.com.jaimeferreira.ccr.nestle.service.DistribuidoresNestService;
import py.com.jaimeferreira.ccr.nestle.service.ImagenesNestService;
import py.com.jaimeferreira.ccr.nestle.service.ReportesNestService;

/**
 *
 * @author Jaime Ferreira
 */

@RestController
@RequestMapping(value = "nestle/d-present/api/v1")
public class NestleDPresentController {

    private static final Logger LOGGER = LoggerFactory.getLogger(NestleDPresentController.class);

    @Autowired
    private BocasNestService bocasService;

    //
    @Autowired
    private ImagenesNestService imagenesService;

    @Autowired
    private DistribuidoresNestService distribuidoresService;

    @Autowired
    private ReportesNestService reportesService;

    /*
     * Distribuidores
     */

    @GetMapping(value = "/distribuidores", produces = "application/json")
    public ResponseEntity<List<DistribuidorNest>> distribuidores() {
        LOGGER.info("Obteniendo todos los distribuidores...");
        return ResponseEntity.status(HttpStatus.OK).body(distribuidoresService.findActivos());
    }

    /*
     * Bocas
     */
    @GetMapping(value = "/bocas", produces = "application/json")
    public ResponseEntity<List<BocaNest>> bocas() {
        LOGGER.info("Obteniendo todas las bocas...");
        return ResponseEntity.status(HttpStatus.OK).body(bocasService.list());
    }

    @GetMapping(value = "/bocas/distribuidor/{codigo}", produces = "application/json")
    public ResponseEntity<List<BocaNest>> bocasbyDistribuidor(@PathVariable("codigo") String codigo) {
        LOGGER.info("Obteniendo todas las bocas por distribuidor...");
        return ResponseEntity.status(HttpStatus.OK).body(bocasService.findByDistribuidor(codigo));
    }

    /*
     * Imagenes
     */

    @GetMapping(value = "/imagenes/directorios", produces = "application/json")
    public ResponseEntity<List<String>> prueba() {
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

    // @PostMapping(value = "/imagenes/externo", produces = "application/json")
    // public ResponseEntity<String> saveExternalImage(@RequestParam(name = "image") String image) {
    // LOGGER.info(image);
    // return ResponseEntity.status(HttpStatus.OK).body(imagenesService.saveImage(image));
    // }

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
           saveReporte(@Valid @RequestBody ReporteNest reporte) {
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
    public ResponseEntity<List<ReporteNest>> getReporte(
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
            // Genera el reporte en formato PDF como arreglo de bytes

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

            // Configura los encabezados para enviar un archivo PDF
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                                                            .filename(fileName.toString())
                                                            .build());

            // Devuelve el archivo PDF como respuesta
            return ResponseEntity.ok()
                                 .headers(headers)
                                 .body(pdfBytes);
        }
        catch (Exception e) {
            e.printStackTrace();
            // Manejo de errores
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(null);
        }
    }

}
