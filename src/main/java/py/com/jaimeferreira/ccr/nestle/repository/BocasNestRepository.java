
package py.com.jaimeferreira.ccr.nestle.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.nestle.entity.BocaNest;

/**
 *
 * @author Jaime Ferreira
 */
public interface BocasNestRepository extends JpaRepository<BocaNest, Long> {

    List<BocaNest> findByActivoTrue();

    List<BocaNest> findByCodDistribuidorAndActivoTrue(String codDistribuidor);

    List<BocaNest> findByMesUltimaMedicionAndActivoTrue(String mes);

    Optional<BocaNest> findByCodBoca(String codigo);

}
