
package py.com.jaimeferreira.ccr.shell.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.shell.entity.ItemShell;

/**
 *
 * @author Jaime Ferreira
 */
public interface ItemsShellRepository extends JpaRepository<ItemShell, Long> {

    List<ItemShell> findByActivoTrueOrderByNro();

}
