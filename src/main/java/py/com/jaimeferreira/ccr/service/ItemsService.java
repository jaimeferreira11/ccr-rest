
package py.com.jaimeferreira.ccr.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.entity.Item;
import py.com.jaimeferreira.ccr.repository.ItemsRepository;

/**
 *
 * @author Jaime Ferreira
 */

@Service
public class ItemsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemsService.class);

    @Autowired
    ItemsRepository repository;

    public List<Item> list() {
        return repository.findByActivoTrue();
    }

}
