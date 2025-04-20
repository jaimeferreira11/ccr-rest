
package py.com.jaimeferreira.ccr.shell.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.shell.entity.BocaShell;

/**
 *
 * @author Jaime Ferreira
 */
public interface BocasShellRepository extends JpaRepository<BocaShell, Long> {

    List<BocaShell> findByActivoTrue();
    
    List<BocaShell> findByActivoTrueAndLatitudIsNotNullAndLongitudIsNotNull();

    Optional<BocaShell> findByCodBoca(String codigo);

}
