
package py.com.jaimeferreira.ccr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.entity.Cabecera;

/**
 *
 * @author Jaime Ferreira
 */
public interface CabecerasRepository extends JpaRepository<Cabecera, Long> {

    List<Cabecera> findByActivoTrue();

}
