
package py.com.jaimeferreira.ccr.controller;

import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Jaime Ferreira
 */

@RestController
@RequestMapping(value = "public")
public class PublicController {
    

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicController.class);

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

}
