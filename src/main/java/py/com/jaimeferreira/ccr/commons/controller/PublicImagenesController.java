package py.com.jaimeferreira.ccr.commons.controller;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import py.com.jaimeferreira.ccr.commons.util.ImagenPathValidator;

/**
 * Endpoint público para servir el binario de una imagen.
 *
 * Se sirve sin JWT porque las etiquetas <img src="..."> del browser no envían
 * el header Authorization. La protección se hace por validación estricta de
 * brand + path (ver ImagenPathValidator) y NOFOLLOW_LINKS para evitar symlinks
 * fuera del directorio configurado.
 */
@RestController
@RequestMapping("/public/imagenes")
public class PublicImagenesController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicImagenesController.class);

    @Value("${path.directory.main_imagenes}")
    private String directorioServer;

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
}
