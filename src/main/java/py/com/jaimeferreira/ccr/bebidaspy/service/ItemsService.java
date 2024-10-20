
package py.com.jaimeferreira.ccr.bebidaspy.service;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.bebidaspy.entity.Item;
import py.com.jaimeferreira.ccr.bebidaspy.repository.ItemsRepository;
import py.com.jaimeferreira.ccr.commons.util.ManejadorDeArchivos;

/**
 *
 * @author Jaime Ferreira
 */

@Service
public class ItemsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemsService.class);

    @Autowired
    ItemsRepository repository;

    @Autowired
    private ManejadorDeArchivos manejadorDeArchivos;

    public List<Item> list() {
        return repository.findByActivoTrueOrderById().stream().peek(i -> {

            if (i.getImagen() != null && !i.getImagen().isEmpty()) {
                try {
                    i.setImgBase64String(manejadorDeArchivos.imagenToBase64("zoomin-bebidas/items/".concat(i.getImagen())));
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
