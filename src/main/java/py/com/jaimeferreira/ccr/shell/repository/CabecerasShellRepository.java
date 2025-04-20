
package py.com.jaimeferreira.ccr.shell.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.shell.entity.CabeceraShell;

/**
 *
 * @author Jaime Ferreira
 */
public interface CabecerasShellRepository extends JpaRepository<CabeceraShell, Long> {

    List<CabeceraShell> findByActivoTrueOrderByOrden();

}
