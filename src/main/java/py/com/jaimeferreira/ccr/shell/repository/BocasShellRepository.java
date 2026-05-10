
package py.com.jaimeferreira.ccr.shell.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import py.com.jaimeferreira.ccr.shell.entity.BocaShell;

/**
 *
 * @author Jaime Ferreira
 */
public interface BocasShellRepository extends JpaRepository<BocaShell, Long> {

    List<BocaShell> findByActivoTrue();

    List<BocaShell> findByActivoTrueAndLatitudIsNotNullAndLongitudIsNotNull();

    Optional<BocaShell> findByCodBoca(String codigo);

    @Query("SELECT b FROM BocaShell b WHERE " +
           "(:busqueda IS NULL OR LOWER(b.codBoca) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(b.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(b.ciudad) LIKE LOWER(CONCAT('%', :busqueda, '%'))) " +
           "AND (:activo IS NULL OR b.activo = :activo) " +
           "AND (:zona IS NULL OR LOWER(b.zona) = LOWER(:zona))")
    Page<BocaShell> buscarPaginado(@Param("busqueda") String busqueda,
                                    @Param("activo") Boolean activo,
                                    @Param("zona") String zona,
                                    Pageable pageable);

}
