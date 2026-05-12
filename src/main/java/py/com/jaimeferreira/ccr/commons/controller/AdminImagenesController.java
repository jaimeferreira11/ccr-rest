package py.com.jaimeferreira.ccr.commons.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import py.com.jaimeferreira.ccr.commons.dto.ImagenAdminDTO;
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

    @Value("${path.directory.main_imagenes}")
    private String directorioServer;

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

    private String buildUrlBinario(String brand, String path, long cacheBuster) {
        return "/api/v1/admin/imagenes/binario?brand=" + brand
                + "&path=" + URLEncoder.encode(path, StandardCharsets.UTF_8)
                + "&v=" + cacheBuster;
    }

    @GetMapping("/binario")
    public ResponseEntity<Resource> binario(
            @RequestParam("brand") String brand,
            @RequestParam("path") String path) {

        if (!ImagenPathValidator.isBrandSoportado(brand)) {
            return ResponseEntity.badRequest().build();
        }
        try {
            ImagenPathValidator.validarPathRelativo(brand, path);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Rechazado intento de leer path inválido. brand={}, path={}, error={}",
                        brand, path, e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        Path archivo = Paths.get(directorioServer, path);
        if (!Files.exists(archivo) || !Files.isRegularFile(archivo, LinkOption.NOFOLLOW_LINKS)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(archivo.toFile());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

    private Map<String, String> error(String mensaje) {
        Map<String, String> m = new HashMap<>();
        m.put("mensaje", mensaje);
        return m;
    }
}
