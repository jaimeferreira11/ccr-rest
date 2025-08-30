
package py.com.jaimeferreira.ccr.nestle.service;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.commons.exception.UnknownResourceException;
import py.com.jaimeferreira.ccr.commons.repository.UsuarioRepository;
import py.com.jaimeferreira.ccr.nestle.entity.BocaNest;
import py.com.jaimeferreira.ccr.nestle.entity.DistribuidorNest;
import py.com.jaimeferreira.ccr.nestle.entity.UsuarioDistribuidorNest;
import py.com.jaimeferreira.ccr.nestle.repository.BocasNestRepository;
import py.com.jaimeferreira.ccr.nestle.repository.DistribuidoresNestRepository;
import py.com.jaimeferreira.ccr.nestle.repository.UsuarioDistribuidorNestRepository;

/**
 *
 * @author Jaime Ferreira
 */

@Service
public class BocasNestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BocasNestService.class);

    @Autowired
    BocasNestRepository repository;
    
    @Autowired
    private DistribuidoresNestRepository distribuidorRepo;
    
    @Autowired
    private UsuarioRepository userRepo;
    
    @Autowired
    private UsuarioDistribuidorNestRepository userDistribuidorRepo;

    public BocaNest findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public List<BocaNest> list() {
        return repository.findByActivoTrue();
    }
    
    public List<BocaNest> findByDistribuidor(String codDistribuidor) {

        LOGGER.info("Buscando bocas del distribuidor: " + codDistribuidor);

        Optional<DistribuidorNest> optionalDis = distribuidorRepo.findByCodigo(codDistribuidor.trim());

        if (!optionalDis.isPresent()) {
            throw new UnknownResourceException("Distrbuidor con codigo " + codDistribuidor + " no encontrado.");
        }

        return repository.findByCodDistribuidorAndActivoTrue(codDistribuidor.trim());
    }
    
    public List<BocaNest> findByUsuario(String usuario) {

        List<UsuarioDistribuidorNest> distribuidores = userDistribuidorRepo.findByUsuario(usuario);
        
        
        LOGGER.info("Distribuidores del usuario " + usuario + ": " + distribuidores.size());

        List<BocaNest> bocas = new ArrayList<>();

        if (distribuidores.isEmpty())
            return bocas;

        distribuidores.stream().forEach(d -> {
            bocas.addAll(findByDistribuidor(d.getCodDistribuidor()));
        });

        return bocas;

    }

    public List<BocaNest> listMesActual() {

        LocalDate today = LocalDate.now();
        String mesActual = today.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES")).toUpperCase();
        LOGGER.info("El mes de las bocas es: " + mesActual);

        return repository.findByMesUltimaMedicionAndActivoTrue(mesActual);
    }

    public void save(BocaNest boca) {
        repository.save(boca);
    }

}
