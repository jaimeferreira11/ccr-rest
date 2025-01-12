
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import net.sf.jasperreports.engine.JRException;
import py.com.jaimeferreira.ccr.commons.dto.ChangePasswordDTO;
import py.com.jaimeferreira.ccr.commons.dto.ImageUploadDTO;
import py.com.jaimeferreira.ccr.commons.entity.Usuario;
import py.com.jaimeferreira.ccr.commons.exception.UnknownResourceException;
import py.com.jaimeferreira.ccr.commons.service.AutenticacionService;
import py.com.jaimeferreira.ccr.commons.util.ManejadorDeArchivos;
import py.com.jaimeferreira.ccr.jhonson.dto.ImageRequestDTO;
import py.com.jaimeferreira.ccr.jhonson.dto.ImagenBocaMesDTO;
import py.com.jaimeferreira.ccr.jhonson.entity.BocaSCJ;
import py.com.jaimeferreira.ccr.jhonson.entity.DistribuidorSCJ;
import py.com.jaimeferreira.ccr.jhonson.entity.ReporteSCJ;
import py.com.jaimeferreira.ccr.jhonson.service.BocasSCJService;
import py.com.jaimeferreira.ccr.jhonson.service.DistribuidoresSCJService;
import py.com.jaimeferreira.ccr.jhonson.service.ImagenesSCJService;
import py.com.jaimeferreira.ccr.jhonson.service.ReportesSCJService;
import py.com.jaimeferreira.ccr.nestle.entity.CabeceraNest;
import py.com.jaimeferreira.ccr.nestle.entity.ItemNest;
import py.com.jaimeferreira.ccr.nestle.entity.RespuestaCabNest;
import py.com.jaimeferreira.ccr.nestle.service.CabecerasNestService;
import py.com.jaimeferreira.ccr.nestle.service.ItemsNestService;
import py.com.jaimeferreira.ccr.nestle.service.RespuestaCabNestService;

/**
 *
 * @author Jaime Ferreira
 */

@RestController
@RequestMapping(value = "jhonson/api/v1")
public class JhonsonController {

    private static final Logger LOGGER = LoggerFactory.getLogger(JhonsonController.class);

    @Autowired
    private ItemsNestService itemsService;

    @Autowired
    private CabecerasNestService cabecerasService;

    @Autowired
    private BocasSCJService bocasService;

    @Autowired
    private RespuestaCabNestService respuestaCabService;

    @Autowired
    private AutenticacionService autenticacionService;

    @Autowired
    private ManejadorDeArchivos manejadorDeArchivos;

    //
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
    public ResponseEntity<List<BocaSCJ>> bocasbyDistribuidor(@PathVariable("codigo") String codigo) {
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
    public ResponseEntity<List<ReporteSCJ>> getReporte(
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

    // ###########################
    @GetMapping(value = "/items", produces = "application/json")
    public ResponseEntity<List<ItemNest>> items() {
        LOGGER.info("Obteniendo todos los items...");
        return ResponseEntity.status(HttpStatus.OK).body(itemsService.list());
    }

    @GetMapping(value = "/cabeceras", produces = "application/json")
    public ResponseEntity<List<CabeceraNest>> cabeceras() {
        LOGGER.info("Obteniendo todas las cabeceras...");
        return ResponseEntity.status(HttpStatus.OK).body(cabecerasService.list());
    }

    @GetMapping(value = "/respuestas", produces = "application/json")
    public ResponseEntity<List<RespuestaCabNest>> respuestas() {
        LOGGER.info("Obteniendo todas las respuestas...");
        return ResponseEntity.status(HttpStatus.OK).body(respuestaCabService.list());
    }

    @PostMapping(value = "/respuestas", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Void>
           saveRespuesta(@Valid @RequestBody List<RespuestaCabNest> respuestas) {
        try {
            respuestaCabService.saveRespuestas(respuestas);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
        // catch (CustomGeneralException e) {
        // LOGGER.error(e.getMessage());
        // return ResponseEntity.status(HttpStatus.CONFLICT)
        // .body(e.getContenidoError());
        // }
        catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                          .body("Error interno del servidor");
        }
        return null;
    }

    @PutMapping(value = "/usuarios/change-password", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<String>
           saveRespuesta(@Valid @RequestBody ChangePasswordDTO changePass) {
        try {

            Usuario usuario =
                autenticacionService.findByUsernameAndPassword(changePass.getUsuario(), changePass.getOldPassword());

            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            usuario.setPassword(changePass.getNewPassword());

            autenticacionService.updateUser(usuario);

            return ResponseEntity.status(HttpStatus.OK).build();
        }
        catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

    @PostMapping(value = "/upload-image", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Void>
           saveImage(@Valid @RequestBody ImageUploadDTO upload) {
        try {
            manejadorDeArchivos.base64ToImagen(upload.getPathImagen(),
                                               upload.getImgBase64String(), upload.getFechaCreacion(), false);
            return ResponseEntity.status(HttpStatus.OK).build();
        }

        catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                          .body("Error interno del servidor");
        }
        return null;
    }

    @PostMapping(value = "/upload-list-image", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Void>
           saveListImage(@Valid @RequestBody List<ImageUploadDTO> list) {
        try {

            list.stream().forEach(upload -> {

                manejadorDeArchivos.base64ToImagen(upload.getPathImagen(),
                                                   upload.getImgBase64String(), upload.getFechaCreacion(), false);
            });
            return ResponseEntity.status(HttpStatus.OK).build();
        }

        catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                          .body("Error interno del servidor");
        }
        return null;
    }

}
