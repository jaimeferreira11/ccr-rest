
package py.com.jaimeferreira.ccr.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.entity.Cabecera;
import py.com.jaimeferreira.ccr.repository.CabecerasRepository;

/**
 *
 * @author Jaime Ferreira
 */

@Service
public class CabecerasService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CabecerasService.class);

    @Autowired
    CabecerasRepository repository;

    public List<Cabecera> list() {
        return repository.findByActivoTrue();
    }

}