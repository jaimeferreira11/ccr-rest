
package py.com.jaimeferreira.ccr.shell.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import py.com.jaimeferreira.ccr.shell.entity.ItemShell;

/**
 *
 * @author Jaime Ferreira
 */
public interface ItemsShellRepository extends JpaRepository<ItemShell, Long> {

    List<ItemShell> findByActivoTrueOrderByNro();

    @Query("SELECT i FROM ItemShell i WHERE " +
           "(:busqueda IS NULL OR LOWER(i.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(i.leyenda) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(i.tema) LIKE LOWER(CONCAT('%', :busqueda, '%'))) " +
           "AND (:activo IS NULL OR i.activo = :activo) " +
           "AND (:codCabecera IS NULL OR i.codCabecera = :codCabecera)")
    Page<ItemShell> buscarPaginado(@Param("busqueda") String busqueda,
                                    @Param("activo") Boolean activo,
                                    @Param("codCabecera") String codCabecera,
                                    Pageable pageable);

}
