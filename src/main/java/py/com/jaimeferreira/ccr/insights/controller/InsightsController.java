package py.com.jaimeferreira.ccr.insights.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import py.com.jaimeferreira.ccr.insights.entity.ClienteIns;
import py.com.jaimeferreira.ccr.insights.entity.Pais;
import py.com.jaimeferreira.ccr.insights.service.ClienteInsService;
import py.com.jaimeferreira.ccr.insights.service.PaisInsService;

/**
 *
 * @author Jaime Ferreira
 */
@RestController
@RequestMapping(value = "insights/api/v1")
public class InsightsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(InsightsController.class);

    @Autowired
    private PaisInsService paisService;

    @Autowired
    private ClienteInsService clienteService;

    @GetMapping(value = "/paises", produces = "application/json")
    public ResponseEntity<List<Pais>> paises() {
        LOGGER.info("Obteniendo todos los paises activos...");
        return ResponseEntity.status(HttpStatus.OK).body(paisService.findActivos());
    }

    @GetMapping(value = "/clientes/{codPais}", produces = "application/json")
    public ResponseEntity<List<ClienteIns>> clientesByPais(@PathVariable String codPais) {
        LOGGER.info("Obteniendo clientes del pais: {}", codPais);
        return ResponseEntity.status(HttpStatus.OK).body(clienteService.findByPais(codPais.trim().toUpperCase()));
    }

}
