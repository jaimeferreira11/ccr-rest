
package py.com.jaimeferreira.ccr.jhonson.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.commons.exception.UnknownResourceException;
import py.com.jaimeferreira.ccr.jhonson.entity.BocaSCJ;
import py.com.jaimeferreira.ccr.jhonson.entity.DistribuidorSCJ;
import py.com.jaimeferreira.ccr.jhonson.repository.BocasSCJRepository;
import py.com.jaimeferreira.ccr.jhonson.repository.DistribuidoresSCJRepository;

/**
 *
 * @author Jaime Ferreira
 */
@Service
public class BocasSCJService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BocasSCJService.class);

    @Autowired
    private BocasSCJRepository repository;

    @Autowired
    private DistribuidoresSCJRepository distribuidorRepo;

    public List<BocaSCJ> list() {
        return repository.findByActivoTrue();
    }

    public List<BocaSCJ> findByDistribuidor(String codDistribuidor) {

        LOGGER.info("Buscando bocas del distribuidor: " + codDistribuidor);

        Optional<DistribuidorSCJ> optionalDis = distribuidorRepo.findByCodigo(codDistribuidor.trim());

        if (!optionalDis.isPresent()) {
            throw new UnknownResourceException("Distrbuidor con codigo " + codDistribuidor + " no encontrado.");
        }

        return repository.findByCodDistribuidorAndActivoTrue(codDistribuidor.trim());
    }
    
    
    public List<BocaSCJ> findByDistribuidor(String codDistribuidor, String mes) {

        LOGGER.info("Buscando bocas del distribuidor: " + codDistribuidor);

        Optional<DistribuidorSCJ> optionalDis = distribuidorRepo.findByCodigo(codDistribuidor.trim());

        if (!optionalDis.isPresent()) {
            throw new UnknownResourceException("Distrbuidor con codigo " + codDistribuidor + " no encontrado.");
        }

        return repository.findByCodDistribuidorAndActivoTrue(codDistribuidor.trim());
    }

}
