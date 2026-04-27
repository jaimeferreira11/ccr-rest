
package py.com.jaimeferreira.ccr.jhonson.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.jhonson.entity.ItemSCJ;

/**
 *
 * @author Jaime Ferreira
 */
public interface ItemsSCJRepository extends JpaRepository<ItemSCJ, Long> {

    List<ItemSCJ> findByActivoTrueOrderByOrden();

}
