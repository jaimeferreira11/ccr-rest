
package py.com.jaimeferreira.ccr.shell.controller;

import java.io.IOException;
import java.util.List;

import javax.jws.WebParam;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import py.com.jaimeferreira.ccr.commons.dto.ChangePasswordDTO;
import py.com.jaimeferreira.ccr.commons.entity.Usuario;
import py.com.jaimeferreira.ccr.commons.service.AutenticacionService;
import py.com.jaimeferreira.ccr.shell.constants.ShellConstants;
import py.com.jaimeferreira.ccr.shell.entity.BocaShell;
import py.com.jaimeferreira.ccr.shell.entity.CabeceraShell;
import py.com.jaimeferreira.ccr.shell.entity.ItemShell;
import py.com.jaimeferreira.ccr.shell.entity.RespuestaCabShell;
import py.com.jaimeferreira.ccr.shell.entity.RespuestaMultimediaShell;
import py.com.jaimeferreira.ccr.shell.service.BocasShellService;
import py.com.jaimeferreira.ccr.shell.service.CabecerasShellService;
import py.com.jaimeferreira.ccr.shell.service.ItemsShellService;
import py.com.jaimeferreira.ccr.shell.service.RespuestaCabShellService;
import py.com.jaimeferreira.ccr.shell.service.RespuestaMultimediaShellService;

/**
 *
 * @author Jaime Ferreira
 */

@RestController
@RequestMapping(value = "shell/api/v1")
public class ShellController {

    private final CabecerasShellService cabecerasShellService;

    private static final Logger LOGGER = LoggerFactory.getLogger(ShellController.class);

    @Autowired
    private BocasShellService bocasService;

    @Autowired
    private CabecerasShellService cabecerasService;

    @Autowired
    private ItemsShellService itemsService;

    @Autowired
    private RespuestaCabShellService respuestaCabService;

    @Autowired
    private RespuestaMultimediaShellService respuestaMultimediaService;

    @Autowired
    private AutenticacionService autenticacionService;

    ShellController(CabecerasShellService cabecerasShellService) {
        this.cabecerasShellService = cabecerasShellService;
    }

    /// INICIO SHELL

    @PutMapping(value = "/usuarios/change-password", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<String>
           saveRespuesta(@Valid @RequestBody ChangePasswordDTO changePass) {
        try {

            Usuario usuario =
                autenticacionService.findByUsernameAndPasswordAndCliente(changePass.getUsuario(),
                                                                         changePass.getOldPassword(),
                                                                         ShellConstants.COD_CLIENTE);

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

    @GetMapping(value = "/bocas/proximidad", produces = "application/json")
    public ResponseEntity<List<BocaShell>> bocas(@WebParam(name = "latitud") String latitud,
                                                 @WebParam(name = "longitud") String longitud,
                                                 @WebParam(name = "cantidad") Integer cantidad) {
        return ResponseEntity.status(HttpStatus.OK).body(bocasService.findByProximity(latitud, longitud, cantidad));
    }

    @GetMapping(value = "/items", produces = "application/json")
    public ResponseEntity<List<ItemShell>> items() {
        LOGGER.info("Obteniendo todos los items...");
        return ResponseEntity.status(HttpStatus.OK).body(itemsService.list());
    }

    @GetMapping(value = "/cabeceras", produces = "application/json")
    public ResponseEntity<List<CabeceraShell>> cabeceras() {
        LOGGER.info("Obteniendo todas las cabeceras...");
        return ResponseEntity.status(HttpStatus.OK).body(cabecerasService.list());
    }

    @PostMapping(value = "/respuestas", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Long>
           saveRespuesta(@Valid @RequestBody RespuestaCabShell respuestas) {
        try {

            return ResponseEntity.status(HttpStatus.CREATED).body(respuestaCabService.saveRespuesta(respuestas));
        }
        catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                          .body("Error interno del servidor");
        }
        return null;
    }

    @PostMapping(value = "/respuestas-multimedia", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Void>
           saveListImage(@Valid @RequestBody List<RespuestaMultimediaShell> list) {
        try {

            respuestaMultimediaService.saveList(list);
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

    @PostMapping("respuestas/multimedias/photo-chunk")
    public ResponseEntity<?> uploadPhotoChunk(
                                              @RequestParam("path") String path,
                                              @RequestParam("file") MultipartFile file,
                                               @RequestParam("codBoca") String  codBoca,
                                              // @RequestParam("tipo") String tipo,
                                              // @RequestParam("fechaCreacion") String
                                              // fechaCreacion,
                                              @RequestHeader(value = "Content-Range",
                                                             required = false) String contentRange) throws IOException {

        respuestaMultimediaService.saveMultimedia(path, file, codBoca);

        return ResponseEntity.ok().build();
    }

    ///// FIN SHELL

}
