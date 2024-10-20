
package py.com.jaimeferreira.ccr.nestle.service;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.commons.util.ManejadorDeArchivos;
import py.com.jaimeferreira.ccr.nestle.entity.ItemNest;
import py.com.jaimeferreira.ccr.nestle.repository.ItemsNestRepository;

/**
 *
 * @author Jaime Ferreira
 */

@Service
public class ItemsNestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemsNestService.class);

    @Autowired
    ItemsNestRepository repository;

    @Autowired
    private ManejadorDeArchivos manejadorDeArchivos;

    public List<ItemNest> list() {
        return repository.findByActivoTrueOrderByOrden().stream().peek(i -> {

            if (i.getImagen() != null && !i.getImagen().isEmpty()) {
                try {
                    i.setImgBase64String(manejadorDeArchivos.imagenToBase64("nestle/items/".concat(i.getImagen())));
                }
                catch (FileNotFoundException e) {
                    // Nothing
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }).collect(Collectors.toList());
    }

}
