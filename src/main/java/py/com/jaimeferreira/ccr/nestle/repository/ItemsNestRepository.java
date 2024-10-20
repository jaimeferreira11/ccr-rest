
package py.com.jaimeferreira.ccr.nestle.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.nestle.entity.ItemNest;

/**
 *
 * @author Jaime Ferreira
 */
public interface ItemsNestRepository extends JpaRepository<ItemNest, Long> {

    List<ItemNest> findByActivoTrueOrderByOrden();

}
