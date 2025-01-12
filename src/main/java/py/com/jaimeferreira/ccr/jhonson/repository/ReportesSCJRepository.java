
package py.com.jaimeferreira.ccr.jhonson.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.jhonson.entity.ReporteSCJ;

/**
 *
 * @author Jaime Ferreira
 */
public interface ReportesSCJRepository extends JpaRepository<ReporteSCJ, Long> {
    
    List<ReporteSCJ> findByUsuarioOrderByFechaCreacionDesc(String usuario);

}
