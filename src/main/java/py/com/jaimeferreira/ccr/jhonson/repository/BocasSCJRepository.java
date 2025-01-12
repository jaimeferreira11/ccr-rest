
package py.com.jaimeferreira.ccr.jhonson.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.jhonson.entity.BocaSCJ;

/**
 *
 * @author Jaime Ferreira
 */
public interface BocasSCJRepository extends JpaRepository<BocaSCJ, Long> {

    List<BocaSCJ> findByActivoTrue();

    List<BocaSCJ> findByCodDistribuidorAndActivoTrue(String codDistribuidor);
    
    Optional<BocaSCJ> findByCodBoca(String codigo);

}
