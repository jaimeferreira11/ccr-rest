
package py.com.jaimeferreira.ccr.shell.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.shell.entity.CabeceraShell;
import py.com.jaimeferreira.ccr.shell.repository.CabecerasShellRepository;

/**
 *
 * @author Jaime Ferreira
 */

@Service
public class CabecerasShellService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CabecerasShellService.class);

    @Autowired
    CabecerasShellRepository repository;

    public List<CabeceraShell> list() {
        return repository.findByActivoTrueOrderByOrden();
    }

}
