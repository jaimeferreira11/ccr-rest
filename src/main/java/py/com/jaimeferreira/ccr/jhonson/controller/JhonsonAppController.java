
package py.com.jaimeferreira.ccr.jhonson.controller;

import java.util.List;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import py.com.jaimeferreira.ccr.jhonson.entity.BocaSCJ;
import py.com.jaimeferreira.ccr.jhonson.entity.CabeceraSCJ;
import py.com.jaimeferreira.ccr.jhonson.entity.ItemSCJ;
import py.com.jaimeferreira.ccr.jhonson.entity.RespuestaCabSCJ;
import py.com.jaimeferreira.ccr.jhonson.service.BocasSCJService;
import py.com.jaimeferreira.ccr.jhonson.service.CabecerasSCJService;
import py.com.jaimeferreira.ccr.jhonson.service.ItemsSCJService;
import py.com.jaimeferreira.ccr.jhonson.service.RespuestaCabSCJService;

/**
 * Controller para la app mobile de SCJ (Johnson).
 * Endpoints de recopilación de datos: items, cabeceras, bocas, respuestas, upload de imágenes.
 *
 * @author Jaime Ferreira
 */

@RestController
@RequestMapping(value = "jhonson/api/v1")
public class JhonsonAppController {

    private static final Logger LOGGER = LoggerFactory.getLogger(JhonsonAppController.class);

    @Autowired
    private ItemsSCJService itemsService;

    @Autowired
    private CabecerasSCJService cabecerasService;

    @Autowired
    private BocasSCJService bocasService;

    @Autowired
    private RespuestaCabSCJService respuestaCabService;

    @Autowired
    private AutenticacionService autenticacionService;

    @Autowired
    private ManejadorDeArchivos manejadorDeArchivos;

    /*
     * Items y Cabeceras
     */

    @GetMapping(value = "/items", produces = "application/json")
    public ResponseEntity<List<ItemSCJ>> items() {
        LOGGER.info("Obteniendo todos los items...");
        return ResponseEntity.status(HttpStatus.OK).body(itemsService.list());
    }

    @GetMapping(value = "/cabeceras", produces = "application/json")
    public ResponseEntity<List<CabeceraSCJ>> cabeceras() {
        LOGGER.info("Obteniendo todas las cabeceras...");
        return ResponseEntity.status(HttpStatus.OK).body(cabecerasService.list());
    }

    /*
     * Bocas
     */

    @GetMapping(value = "/bocas", produces = "application/json")
    public ResponseEntity<List<BocaSCJ>> bocasByAuditor() {
        LOGGER.info("Obteniendo bocas por auditor...");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        return ResponseEntity.status(HttpStatus.OK).body(bocasService.findByAuditor(currentUsername));
    }

    @GetMapping(value = "/bocas/all", produces = "application/json")
    public ResponseEntity<List<BocaSCJ>> bocasAll() {
        LOGGER.info("Obteniendo todas las bocas activas...");
        return ResponseEntity.status(HttpStatus.OK).body(bocasService.list());
    }

    /*
     * Respuestas
     */

    @GetMapping(value = "/respuestas", produces = "application/json")
    public ResponseEntity<List<RespuestaCabSCJ>> respuestas() {
        LOGGER.info("Obteniendo todas las respuestas...");
        return ResponseEntity.status(HttpStatus.OK).body(respuestaCabService.list());
    }

    @PostMapping(value = "/respuestas", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Void>
           saveRespuesta(@Valid @RequestBody List<RespuestaCabSCJ> respuestas) {
        try {
            respuestaCabService.saveRespuestas(respuestas);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
        catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /*
     * Usuarios
     */

    @PutMapping(value = "/usuarios/change-password", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<String>
           changePassword(@Valid @RequestBody ChangePasswordDTO changePass) {
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

    /*
     * Upload imagenes
     */

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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
