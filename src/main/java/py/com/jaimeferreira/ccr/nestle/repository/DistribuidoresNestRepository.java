
package py.com.jaimeferreira.ccr.nestle.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.nestle.entity.DistribuidorNest;


/**
 *
 * @author Jaime Ferreira
 */
public interface DistribuidoresNestRepository extends JpaRepository<DistribuidorNest, Long> {

    List<DistribuidorNest> findByActivoTrue();

    Optional<DistribuidorNest> findByCodigo(String codDistribuidor);

}
