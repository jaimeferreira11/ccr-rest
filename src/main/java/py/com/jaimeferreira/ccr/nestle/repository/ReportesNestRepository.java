
package py.com.jaimeferreira.ccr.nestle.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.nestle.entity.ReporteNest;

/**
 *
 * @author Jaime Ferreira
 */
public interface ReportesNestRepository extends JpaRepository<ReporteNest, Long> {

    List<ReporteNest> findByUsuarioOrderByFechaCreacionDesc(String usuario);

}
