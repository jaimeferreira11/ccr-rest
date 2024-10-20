
package py.com.jaimeferreira.ccr.bebidaspy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.bebidaspy.entity.Boca;

/**
 *
 * @author Jaime Ferreira
 */
public interface BocasRepository extends JpaRepository<Boca, Long> {

    List<Boca> findByActivoTrue();

}
