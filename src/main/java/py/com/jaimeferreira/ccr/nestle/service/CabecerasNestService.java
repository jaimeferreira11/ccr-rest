
package py.com.jaimeferreira.ccr.nestle.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.nestle.entity.CabeceraNest;
import py.com.jaimeferreira.ccr.nestle.repository.CabecerasNestRepository;

/**
 *
 * @author Jaime Ferreira
 */

@Service
public class CabecerasNestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CabecerasNestService.class);

    @Autowired
    CabecerasNestRepository repository;

    public List<CabeceraNest> list() {
        return repository.findByActivoTrueOrderByOrden();
    }

}
