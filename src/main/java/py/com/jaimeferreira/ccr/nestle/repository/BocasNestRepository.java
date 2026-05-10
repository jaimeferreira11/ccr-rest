
package py.com.jaimeferreira.ccr.nestle.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import py.com.jaimeferreira.ccr.nestle.entity.BocaNest;

/**
 *
 * @author Jaime Ferreira
 */
public interface BocasNestRepository extends JpaRepository<BocaNest, Long> {

    List<BocaNest> findByActivoTrue();

    List<BocaNest> findByCodDistribuidorAndActivoTrue(String codDistribuidor);

    List<BocaNest> findByMesUltimaMedicionAndActivoTrue(String mes);

    Optional<BocaNest> findByCodBoca(String codigo);

    @Query("SELECT b FROM BocaNest b WHERE " +
           "(:busqueda IS NULL OR LOWER(b.codBoca) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(b.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(b.ciudad) LIKE LOWER(CONCAT('%', :busqueda, '%'))) " +
           "AND (:activo IS NULL OR b.activo = :activo) " +
           "AND (:codDistribuidor IS NULL OR b.codDistribuidor = :codDistribuidor)")
    Page<BocaNest> buscarPaginado(@Param("busqueda") String busqueda,
                                   @Param("activo") Boolean activo,
                                   @Param("codDistribuidor") String codDistribuidor,
                                   Pageable pageable);

}
