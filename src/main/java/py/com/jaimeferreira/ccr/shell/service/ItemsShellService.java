
package py.com.jaimeferreira.ccr.shell.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.commons.util.ManejadorDeArchivos;
import py.com.jaimeferreira.ccr.shell.entity.ItemShell;
import py.com.jaimeferreira.ccr.shell.repository.ItemsShellRepository;

/**
 *
 * @author Jaime Ferreira
 */

@Service
public class ItemsShellService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemsShellService.class);

    @Autowired
    ItemsShellRepository repository;

    @Autowired
    private ManejadorDeArchivos manejadorDeArchivos;

    public List<ItemShell> list() {
        return repository.findByActivoTrueOrderByNro();
    }

}
