package py.com.jaimeferreira.ccr.commons.controller;

import java.util.Set;
import java.util.stream.Collectors;

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
import io.jsonwebtoken.ExpiredJwtException;
import py.com.jaimeferreira.ccr.commons.dto.AccesoDTO;
import py.com.jaimeferreira.ccr.commons.dto.ResponseTokenDTO;
import py.com.jaimeferreira.ccr.commons.dto.ValidarTokenDTO;
import py.com.jaimeferreira.ccr.commons.entity.UserRole;
import py.com.jaimeferreira.ccr.commons.entity.Usuario;
import py.com.jaimeferreira.ccr.commons.repository.UserRolesRepository;
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
    private UserRolesRepository userRolesRepository;

    @Autowired
    private FechaUtil fechaUtil;

    @PostMapping(path = "/login", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> loginAccess(@RequestBody AccesoDTO accesoDTO) {

        try {
            Usuario usuario = autenticacionService.findByUsernameAndPassword(accesoDTO.getUsuario().trim(),
                                                                             accesoDTO.getContrasena().trim());
            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No existe el usuario");
            }
            if (accesoDTO.getCodCliente() != null && !accesoDTO.getCodCliente().isEmpty()) {
                // Flujo nuevo, multi cliente

                if (usuario.getCodCliente() != null && !usuario.getCodCliente().isEmpty()) {
                    if (!usuario.getCodCliente().equalsIgnoreCase(accesoDTO.getCodCliente())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No existe el usuario");
                    }
                }

            }

            if (accesoDTO.getRoles() != null && accesoDTO.getRoles().size() > 0) {
                // verificar los roles del usuario

                Set<String> rolesUsuario = userRolesRepository.findByUsuarioIgnoreCase(usuario.getUsuario().trim())
                                                              .stream()
                                                              .map(UserRole::getRol)
                                                              .collect(Collectors.toSet());

                if (rolesUsuario.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Permisos insuficientes");
                }

                // Verificar si todos los roles requeridos están contenidos en los roles del usuario
                boolean tienePermisos = accesoDTO.getRoles().stream()
                                                 .allMatch(rolesUsuario::contains);

                if (!tienePermisos) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Permisos insuficientes");
                }
            }
            // autenticacionService.setSessionDeUsuario(usuario, token, claims.getExpiration());

            String token = this.jwtAuthorizationUtils.getJWTToken(usuario.getUsuario().trim());
            jwtAuthorizationFilter.validateTokenString(token);
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseTokenDTO(usuario, token));

        }
        catch (Exception e) {
            LOGGER.error(e.getLocalizedMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

    }

    @PostMapping(value = "/validar-token", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> validarToken(@RequestBody ValidarTokenDTO validarTokenDTO) {
        try {
            Claims claims = jwtAuthorizationFilter.validateTokenString(validarTokenDTO.getToken());

            Usuario usuario = autenticacionService.findByUsuario(validarTokenDTO.getUsuario().trim());
            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No existe el usuario");
            }

            return ResponseEntity.status(HttpStatus.OK).body(new ResponseTokenDTO(usuario, validarTokenDTO.getToken()));
        }
        catch (ExpiredJwtException e) {
            e.printStackTrace();
            LOGGER.error(e.getClass().getCanonicalName() + "; " + e.getLocalizedMessage());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getClass().getCanonicalName() + "; " + e.getLocalizedMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

    }

}
