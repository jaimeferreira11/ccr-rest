
package py.com.jaimeferreira.ccr.jhonson.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.jhonson.entity.DistribuidorSCJ;
import py.com.jaimeferreira.ccr.jhonson.repository.DistribuidoresSCJRepository;

/**
 *
 * @author Jaime Ferreira
 */
@Service
public class DistribuidoresSCJService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistribuidoresSCJService.class);

    @Autowired
    private DistribuidoresSCJRepository repo;

    public List<DistribuidorSCJ> findActivos() {

        return repo.findByActivoTrue();

    }

}
