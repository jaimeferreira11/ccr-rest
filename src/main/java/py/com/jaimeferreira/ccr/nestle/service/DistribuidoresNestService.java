
package py.com.jaimeferreira.ccr.nestle.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.nestle.entity.DistribuidorNest;
import py.com.jaimeferreira.ccr.nestle.repository.DistribuidoresNestRepository;

/**
 *
 * @author Jaime Ferreira
 */
@Service
public class DistribuidoresNestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistribuidoresNestService.class);

    @Autowired
    private DistribuidoresNestRepository repo;

    public List<DistribuidorNest> findActivos() {

        return repo.findByActivoTrue();

    }

}
