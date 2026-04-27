
package py.com.jaimeferreira.ccr.jhonson.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.jhonson.entity.CabeceraSCJ;

/**
 *
 * @author Jaime Ferreira
 */
public interface CabecerasSCJRepository extends JpaRepository<CabeceraSCJ, Long> {

    List<CabeceraSCJ> findByActivoTrueOrderByOrden();

}
