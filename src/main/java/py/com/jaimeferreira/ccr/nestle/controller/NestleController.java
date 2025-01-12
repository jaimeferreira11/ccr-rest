
package py.com.jaimeferreira.ccr.nestle.controller;

import java.util.List;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import py.com.jaimeferreira.ccr.commons.dto.ChangePasswordDTO;
import py.com.jaimeferreira.ccr.commons.dto.ImageUploadDTO;
import py.com.jaimeferreira.ccr.commons.entity.Usuario;
import py.com.jaimeferreira.ccr.commons.service.AutenticacionService;
import py.com.jaimeferreira.ccr.commons.util.ManejadorDeArchivos;
import py.com.jaimeferreira.ccr.nestle.entity.BocaNest;
import py.com.jaimeferreira.ccr.nestle.entity.CabeceraNest;
import py.com.jaimeferreira.ccr.nestle.entity.ItemNest;
import py.com.jaimeferreira.ccr.nestle.entity.RespuestaCabNest;
import py.com.jaimeferreira.ccr.nestle.service.BocasNestService;
import py.com.jaimeferreira.ccr.nestle.service.CabecerasNestService;
import py.com.jaimeferreira.ccr.nestle.service.ItemsNestService;
import py.com.jaimeferreira.ccr.nestle.service.RespuestaCabNestService;

/**
 *
 * @author Jaime Ferreira
 */

@RestController
@RequestMapping(value = "nestle/api/v1")
public class NestleController {

    private static final Logger LOGGER = LoggerFactory.getLogger(NestleController.class);

    @Autowired
    private ItemsNestService itemsService;

    @Autowired
    private CabecerasNestService cabecerasService;

    @Autowired
    private BocasNestService bocasService;

    @Autowired
    private RespuestaCabNestService respuestaCabService;

    @Autowired
    private AutenticacionService autenticacionService;

    @Autowired
    private ManejadorDeArchivos manejadorDeArchivos;

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

    @GetMapping(value = "/bocas", produces = "application/json")
    public ResponseEntity<List<BocaNest>> bocas() {
        LOGGER.info("Obteniendo todas las cabeceras...");
        return ResponseEntity.status(HttpStatus.OK).body(bocasService.listMesActual());
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
