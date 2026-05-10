package py.com.jaimeferreira.ccr.nestle.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import py.com.jaimeferreira.ccr.nestle.entity.DistribuidorNest;
import py.com.jaimeferreira.ccr.nestle.repository.DistribuidoresNestRepository;

/**
 * Endpoints de administración específicos de Nestlé.
 *
 * @author Jaime Ferreira
 */
@RestController
@RequestMapping("nestle/api/v1/admin")
public class NestleAdminController {

    private static final Logger LOGGER = LoggerFactory.getLogger(NestleAdminController.class);

    @Autowired
    private DistribuidoresNestRepository distribuidoresNestRepository;

    @GetMapping(value = "/distribuidores", produces = "application/json")
    public ResponseEntity<List<DistribuidorNest>> getDistribuidores() {
        LOGGER.info("Listando distribuidores nestle");
        return ResponseEntity.ok(distribuidoresNestRepository.findByActivoTrue());
    }

}
