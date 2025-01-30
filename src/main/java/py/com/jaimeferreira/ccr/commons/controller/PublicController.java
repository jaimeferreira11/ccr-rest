
package py.com.jaimeferreira.ccr.commons.controller;

import java.util.Enumeration;
import java.util.HashMap;

import javax.jws.WebParam;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import py.com.jaimeferreira.ccr.commons.util.ManejadorDeArchivos;

/**
 *
 * @author Jaime Ferreira
 */

@RestController
@RequestMapping(value = "public")
public class PublicController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicController.class);

    @Autowired
    private ManejadorDeArchivos manejadorDeArchivos;

    @GetMapping(value = "", produces = "application/json")
    public ResponseEntity<?> test(HttpServletRequest request) {
        LOGGER.info("Obteniendo todos los items...");
        LOGGER.info("PUBLIC TEST REQUEST FROM " + request.getRemoteAddr());

        HashMap<String, String> JSONROOT = new HashMap<String, String>();

        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            JSONROOT.put(key, value);
        }

        return ResponseEntity.status(HttpStatus.OK).body(JSONROOT);
    }

    @GetMapping("/images/app")
    public ResponseEntity<byte[]> getImages(
                                            @WebParam(name = "path") String path) {
        LOGGER.info("Get imagen " + path);

        byte[] image = null;
        try {
            image = manejadorDeArchivos.getImageFromApp(path);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(image);
    }

    @GetMapping("/images/rotate")
    public ResponseEntity<byte[]> rotateImage(
                                              @WebParam(name = "path") String path) {
        LOGGER.info("Rotate imagen " + path);

        try {
            manejadorDeArchivos.rotateImage(path);
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/images/by-url")
    public ResponseEntity<byte[]> getImage(@WebParam(name = "imageUrl") String imageUrl) {
        try {
            // Descargar la imagen de la URL
            byte[] imageBytes = new RestTemplate().getForObject(imageUrl, byte[].class);

            // Configurar los headers para la respuesta
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "image/jpeg"); // Ajusta según el formato de imagen

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

}
