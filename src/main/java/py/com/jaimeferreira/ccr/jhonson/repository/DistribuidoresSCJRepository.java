
package py.com.jaimeferreira.ccr.jhonson.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.jhonson.entity.DistribuidorSCJ;

/**
 *
 * @author Jaime Ferreira
 */
public interface DistribuidoresSCJRepository extends JpaRepository<DistribuidorSCJ, Long> {

    List<DistribuidorSCJ> findByActivoTrue();

    Optional<DistribuidorSCJ> findByCodigo(String codDistribuidor);

}
