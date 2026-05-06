
package py.com.jaimeferreira.ccr.jhonson.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import py.com.jaimeferreira.ccr.jhonson.entity.BocaSCJ;

/**
 *
 * @author Jaime Ferreira
 */
public interface BocasSCJRepository extends JpaRepository<BocaSCJ, Long> {

    List<BocaSCJ> findByActivoTrue();

    List<BocaSCJ> findByCodDistribuidorAndActivoTrue(String codDistribuidor);

    @Query("SELECT b FROM BocaSCJ b JOIN BocaAuditorSCJ ba ON b.id = ba.idBoca WHERE ba.auditor = :auditor AND b.activo = true")
    List<BocaSCJ> findByAuditorAndActivoTrue(@Param("auditor") String auditor);

    Optional<BocaSCJ> findByCodBoca(String codigo);

}
