package py.com.jaimeferreira.ccr.jhonson.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import py.com.jaimeferreira.ccr.jhonson.entity.DistribuidorSCJ;
import py.com.jaimeferreira.ccr.jhonson.repository.DistribuidoresSCJRepository;

/**
 * Endpoints de administración específicos de Johnson.
 *
 * @author Jaime Ferreira
 */
@RestController
@RequestMapping("jhonson/api/v1/admin")
public class JhonsonAdminController {

    private static final Logger LOGGER = LoggerFactory.getLogger(JhonsonAdminController.class);

    @Autowired
    private DistribuidoresSCJRepository distribuidoresSCJRepository;

    @GetMapping(value = "/distribuidores", produces = "application/json")
    public ResponseEntity<List<DistribuidorSCJ>> getDistribuidores() {
        LOGGER.info("Listando distribuidores jhonson");
        return ResponseEntity.ok(distribuidoresSCJRepository.findByActivoTrue());
    }

}
