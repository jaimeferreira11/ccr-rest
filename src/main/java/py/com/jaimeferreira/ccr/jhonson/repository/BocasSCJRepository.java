
package py.com.jaimeferreira.ccr.jhonson.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT DISTINCT b FROM BocaSCJ b LEFT JOIN BocaAuditorSCJ ba ON b.id = ba.idBoca WHERE " +
           "(:busqueda IS NULL OR LOWER(b.codBoca) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(b.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(b.ciudad) LIKE LOWER(CONCAT('%', :busqueda, '%'))) " +
           "AND (:activo IS NULL OR b.activo = :activo) " +
           "AND (:auditor IS NULL OR LOWER(ba.auditor) = LOWER(:auditor))")
    Page<BocaSCJ> buscarPaginado(@Param("busqueda") String busqueda,
                                  @Param("activo") Boolean activo,
                                  @Param("auditor") String auditor,
                                  Pageable pageable);

}
