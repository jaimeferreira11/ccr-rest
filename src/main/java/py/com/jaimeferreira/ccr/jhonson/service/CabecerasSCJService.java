
package py.com.jaimeferreira.ccr.jhonson.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.jhonson.entity.CabeceraSCJ;
import py.com.jaimeferreira.ccr.jhonson.repository.CabecerasSCJRepository;

/**
 *
 * @author Jaime Ferreira
 */

@Service
public class CabecerasSCJService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CabecerasSCJService.class);

    @Autowired
    CabecerasSCJRepository repository;

    public List<CabeceraSCJ> list() {
        return repository.findByActivoTrueOrderByOrden();
    }

}
