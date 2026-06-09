package py.com.jaimeferreira.ccr.commons.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import py.com.jaimeferreira.ccr.commons.dto.ImagenAdminDTO;
import py.com.jaimeferreira.ccr.commons.dto.MoverResultado;
import py.com.jaimeferreira.ccr.commons.exception.ArchivoExistenteException;
import py.com.jaimeferreira.ccr.commons.exception.UnknownResourceException;
import py.com.jaimeferreira.ccr.commons.service.ImagenExplorerService;
import py.com.jaimeferreira.ccr.commons.util.ImagenPathValidator;
import py.com.jaimeferreira.ccr.jhonson.service.ImagenesSCJService;
import py.com.jaimeferreira.ccr.nestle.service.ImagenesNestService;
import py.com.jaimeferreira.ccr.shell.service.ImagenesShellService;

@RestController
@RequestMapping("/api/v1/admin/imagenes")
public class AdminImagenesController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminImagenesController.class);

    @Autowired
    private ImagenesSCJService imagenesSCJService;

    @Autowired
    private ImagenesNestService imagenesNestService;

    @Autowired
    private ImagenesShellService imagenesShellService;

    @Autowired
    private ImagenExplorerService imagenExplorerService;

    @GetMapping(value = "/listar", produces = "application/json")
    public ResponseEntity<?> listar(
            @RequestParam("brand") String brand,
            @RequestParam("anio") int anio,
            @RequestParam("mes") int mes,
            @RequestParam(value = "codBoca", required = false) String codBoca,
            @RequestParam(value = "codDistribuidor", required = false) String codDistribuidor) {

        if (!ImagenPathValidator.isBrandSoportado(brand)) {
            return ResponseEntity.badRequest().body(error("brand no soportado: " + brand));
        }
        if (mes < 1 || mes > 12) {
            return ResponseEntity.badRequest().body(error("mes inválido (debe ser 1-12)"));
        }
        if (anio < 2000 || anio > 2100) {
            return ResponseEntity.badRequest().body(error("anio inválido"));
        }

        String brandLower = brand.toLowerCase();
        List<ImagenAdminDTO> resultado;

        switch (brandLower) {
            case "jhonson":
                resultado = imagenesSCJService.findAllByMes(anio, mes, codBoca);
                break;
            case "nestle":
                if (codDistribuidor == null || codDistribuidor.isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(error("codDistribuidor es obligatorio para Nestle"));
                }
                resultado = imagenesNestService.findAllByMes(anio, mes, codDistribuidor, codBoca);
                break;
            case "shell":
                resultado = imagenesShellService.findAllByMes(anio, mes, codBoca);
                break;
            default:
                return ResponseEntity.badRequest().body(error("brand no soportado: " + brand));
        }

        // armar urlPublica con cache-buster
        long cb = System.currentTimeMillis();
        resultado.forEach(dto -> dto.setUrlPublica(buildUrlBinario(brandLower, dto.getPathRelativo(), cb)));

        return ResponseEntity.ok(resultado);
    }

    @PostMapping(value = "/rotar", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> rotar(@RequestBody Map<String, String> body) {
        String brand = body.get("brand");
        String path = body.get("path");

        if (!ImagenPathValidator.isBrandSoportado(brand)) {
            return ResponseEntity.badRequest().body(error("brand no soportado"));
        }

        try {
            ImagenPathValidator.validarPathRelativo(brand, path);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Rechazado intento de rotar path inválido. brand={}, path={}, error={}",
                        brand, path, e.getMessage());
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }

        try {
            switch (brand.toLowerCase()) {
                case "jhonson":
                    imagenesSCJService.rotarImagen(path);
                    break;
                case "nestle":
                    imagenesNestService.rotarImagen(path);
                    break;
                case "shell":
                    imagenesShellService.rotarImagen(path);
                    break;
            }
        } catch (UnknownResourceException e) {
            LOGGER.warn("Archivo a rotar no existe. brand={}, path={}", brand, path);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (Exception e) {
            LOGGER.error("Error rotando imagen brand=" + brand + " path=" + path, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("No se pudo rotar la imagen: " + e.getMessage()));
        }

        long cb = System.currentTimeMillis();
        Map<String, String> resp = new HashMap<>();
        resp.put("path", path);
        resp.put("urlPublica", buildUrlBinario(brand.toLowerCase(), path, cb));
        return ResponseEntity.ok(resp);
    }

    @GetMapping(value = "/explorar", produces = "application/json")
    public ResponseEntity<?> explorar(@RequestParam(value = "path", required = false) String path) {
        try {
            return ResponseEntity.ok(imagenExplorerService.explorar(path));
        } catch (UnknownResourceException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Explorar path inválido. path={}, error={}", path, e.getMessage());
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (Exception e) {
            LOGGER.error("Error explorando path=" + path, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("No se pudo explorar la carpeta"));
        }
    }

    @PostMapping(value = "/mover", produces = "application/json", consumes = "application/json")
    public ResponseEntity<?> mover(@RequestBody Map<String, String> body) {
        String origenPath = body.get("origenPath");
        String destinoDir = body.get("destinoDir");

        try {
            MoverResultado resultado = imagenExplorerService.mover(origenPath, destinoDir);
            Map<String, String> resp = new HashMap<>();
            resp.put("pathRelativo", resultado.getPathRelativo());
            if (resultado.getBrand() != null) {
                long cb = System.currentTimeMillis();
                resp.put("urlPublica", buildUrlBinario(resultado.getBrand(), resultado.getPathRelativo(), cb));
            }
            return ResponseEntity.ok(resp);
        } catch (ArchivoExistenteException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error(e.getMessage()));
        } catch (UnknownResourceException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Mover rechazado. origen={}, destino={}, error={}", origenPath, destinoDir, e.getMessage());
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (Exception e) {
            LOGGER.error("Error moviendo origen=" + origenPath + " destino=" + destinoDir, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("No se pudo mover la imagen"));
        }
    }

    private String buildUrlBinario(String brand, String path, long cacheBuster) {
        try {
            return "/ccr-rest-api/public/imagenes/binario?brand=" + brand
                    + "&path=" + URLEncoder.encode(path, StandardCharsets.UTF_8.name())
                    + "&v=" + cacheBuster;
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 not supported", e);
        }
    }

    private Map<String, String> error(String mensaje) {
        Map<String, String> m = new HashMap<>();
        m.put("mensaje", mensaje);
        return m;
    }
}
