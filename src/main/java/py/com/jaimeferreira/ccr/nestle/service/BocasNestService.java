
package py.com.jaimeferreira.ccr.nestle.service;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.nestle.entity.BocaNest;
import py.com.jaimeferreira.ccr.nestle.repository.BocasNestRepository;

/**
 *
 * @author Jaime Ferreira
 */

@Service
public class BocasNestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BocasNestService.class);

    @Autowired
    BocasNestRepository repository;

    public List<BocaNest> list() {
        return repository.findByActivoTrue();
    }

    public List<BocaNest> listMesActual() {

        LocalDate today = LocalDate.now();
        String mesActual = today.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES")).toUpperCase();
        LOGGER.info("El mes de las bocas es: " + mesActual);

        return repository.findByMesUltimaMedicionAndActivoTrue(mesActual);
    }

}
