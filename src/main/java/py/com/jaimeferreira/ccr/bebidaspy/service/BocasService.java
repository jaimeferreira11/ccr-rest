
package py.com.jaimeferreira.ccr.bebidaspy.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.bebidaspy.entity.Boca;
import py.com.jaimeferreira.ccr.bebidaspy.repository.BocasRepository;

/**
 *
 * @author Jaime Ferreira
 */

@Service
public class BocasService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BocasService.class);

    @Autowired
    BocasRepository repository;

    public List<Boca> list() {
        return repository.findByActivoTrue();
    }

}
