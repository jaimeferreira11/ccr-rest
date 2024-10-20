
package py.com.jaimeferreira.ccr.nestle.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.nestle.entity.CabeceraNest;

/**
 *
 * @author Jaime Ferreira
 */
public interface CabecerasNestRepository extends JpaRepository<CabeceraNest, Long> {

    List<CabeceraNest> findByActivoTrueOrderByOrden();

}
