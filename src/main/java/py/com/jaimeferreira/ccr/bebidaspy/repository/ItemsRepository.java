
package py.com.jaimeferreira.ccr.bebidaspy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.bebidaspy.entity.Item;

/**
 *
 * @author Jaime Ferreira
 */
public interface ItemsRepository extends JpaRepository<Item, Long> {
    
    
    List<Item> findByActivoTrueOrderById();

}
