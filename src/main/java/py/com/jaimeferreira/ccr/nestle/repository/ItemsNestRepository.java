
package py.com.jaimeferreira.ccr.nestle.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import py.com.jaimeferreira.ccr.nestle.entity.ItemNest;

/**
 *
 * @author Jaime Ferreira
 */
public interface ItemsNestRepository extends JpaRepository<ItemNest, Long> {

    List<ItemNest> findByActivoTrueOrderByOrden();

    @Query("SELECT i FROM ItemNest i WHERE " +
           "(:busqueda IS NULL OR LOWER(i.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(i.leyenda) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(i.categoria) LIKE LOWER(CONCAT('%', :busqueda, '%'))) " +
           "AND (:activo IS NULL OR i.activo = :activo) " +
           "AND (:codCabecera IS NULL OR i.codCabecera = :codCabecera) " +
           "AND (:autoservicio IS NULL OR i.autoservicio = :autoservicio) " +
           "AND (:supermercado IS NULL OR i.supermercado = :supermercado) " +
           "AND (:despensa IS NULL OR i.despensa = :despensa) " +
           "AND (:estacionServicio IS NULL OR i.estacionServicio = :estacionServicio)")
    Page<ItemNest> buscarPaginado(@Param("busqueda") String busqueda,
                                   @Param("activo") Boolean activo,
                                   @Param("codCabecera") String codCabecera,
                                   @Param("autoservicio") Boolean autoservicio,
                                   @Param("supermercado") Boolean supermercado,
                                   @Param("despensa") Boolean despensa,
                                   @Param("estacionServicio") Boolean estacionServicio,
                                   Pageable pageable);

}
