package py.com.jaimeferreira.ccr.commons.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.jsonwebtoken.Claims;
import py.com.jaimeferreira.ccr.commons.dto.AccesoDTO;
import py.com.jaimeferreira.ccr.commons.dto.ResponseTokenDTO;
import py.com.jaimeferreira.ccr.commons.entity.Usuario;
import py.com.jaimeferreira.ccr.commons.service.AutenticacionService;
import py.com.jaimeferreira.ccr.commons.util.FechaUtil;
import py.com.jaimeferreira.ccr.security.JWTAuthorizationFilter;
import py.com.jaimeferreira.ccr.security.JWTAuthorizationUtils;

/***
 * 
 * @author Luis Capdevila [luis_capde@hotmail.com]
 *
 */

@RestController
@RequestMapping(value = "auth")
public class LoginController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private JWTAuthorizationFilter jwtAuthorizationFilter;

    @Autowired
    private JWTAuthorizationUtils jwtAuthorizationUtils;

    @Autowired
    private AutenticacionService autenticacionService;

    @Autowired
    private FechaUtil fechaUtil;

    @PostMapping(path = "/login", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> loginAccess(@RequestBody AccesoDTO accesoDTO) {
        try {
            Usuario usuario =
                autenticacionService.findByUsernameAndPassword(accesoDTO.getUsuario().trim(),
                                                               accesoDTO.getContrasena().trim());

            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No existe el usuario");
            }

            String token = this.jwtAuthorizationUtils.getJWTToken(usuario.getUsuario().trim());
            Claims claims = jwtAuthorizationFilter.validateTokenString(token);
            // autenticacionService.setSessionDeUsuario(usuario, token, claims.getExpiration());

            // if (usuario.getProfesional() != null) {
            // usuarioAppDTO = new UsuarioAppDTO(usuario.getProfesional().getPersona().getNombre(),
            // usuario.getProfesional().getPersona().getApellido(),
            // usuario.getProfesional().getId(), null, null,
            // usuario.getProfesional().getEstado(),
            // token, accesoDTO.getUsuario(),
            // this.fechaUtil.horaStringFromDate(claims.getExpiration()));
            //
            // }
            // if (usuario.getEmpleado() != null) {
            // usuarioAppDTO = new UsuarioAppDTO(usuario.getEmpleado().getPersona().getNombre(),
            // usuario.getEmpleado().getPersona().getApellido(), null,
            // usuario.getEmpleado().getId(), null, usuario.getEmpleado().isEstado(),
            // token, accesoDTO.getUsuario(),
            // this.fechaUtil.horaStringFromDate(claims.getExpiration()));
            // }
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseTokenDTO(usuario, token));

        }
        catch (Exception e) {
            LOGGER.error(e.getLocalizedMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

    }

    // @PostMapping(value = "/validar-token", consumes = "application/json", produces =
    // "application/json")
    // public ResponseEntity<?> validarToken(@RequestBody ValidarTokenDTO validarTokenDTO) {
    // try {
    // Claims claims = jwtAuthorizationFilter.validateTokenString(validarTokenDTO.getToken());
    // ResponseTokenDTO responseTokenDTO = new ResponseTokenDTO(validarTokenDTO.getToken(),
    // claims.getSubject(),
    // this.fechaUtil.horaStringFromDate(claims.getExpiration()),
    // false);
    // return ResponseEntity.status(HttpStatus.OK).body(responseTokenDTO);
    // }
    // catch (ExpiredJwtException e) {
    // e.printStackTrace();
    // LOGGER.error(e.getClass().getCanonicalName() + "; " + e.getLocalizedMessage());
    // ResponseTokenDTO responseTokenDTO = new ResponseTokenDTO(validarTokenDTO.getToken(),
    // validarTokenDTO.getUsuario(),
    // "",
    // true);
    //
    // return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseTokenDTO);
    // }
    // catch (Exception e) {
    // e.printStackTrace();
    // LOGGER.error(e.getClass().getCanonicalName() + "; " + e.getLocalizedMessage());
    // return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    // }
    //
    // }

}
