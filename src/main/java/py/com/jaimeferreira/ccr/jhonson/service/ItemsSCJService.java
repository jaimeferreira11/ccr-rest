
package py.com.jaimeferreira.ccr.jhonson.service;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.commons.util.ManejadorDeArchivos;
import py.com.jaimeferreira.ccr.jhonson.entity.ItemSCJ;
import py.com.jaimeferreira.ccr.jhonson.repository.ItemsSCJRepository;

/**
 *
 * @author Jaime Ferreira
 */

@Service
public class ItemsSCJService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemsSCJService.class);

    @Autowired
    ItemsSCJRepository repository;

    @Autowired
    private ManejadorDeArchivos manejadorDeArchivos;

    public List<ItemSCJ> list() {
        return repository.findByActivoTrueOrderByOrden().stream().peek(i -> {

            if (i.getImagen() != null && !i.getImagen().isEmpty()) {
                try {
                    i.setImgBase64String(manejadorDeArchivos.imagenToBase64("zoomin-jhonson/items/".concat(i.getImagen())));
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
