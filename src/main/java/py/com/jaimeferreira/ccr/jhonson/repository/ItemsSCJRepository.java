
package py.com.jaimeferreira.ccr.jhonson.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import py.com.jaimeferreira.ccr.jhonson.entity.ItemSCJ;

/**
 *
 * @author Jaime Ferreira
 */
public interface ItemsSCJRepository extends JpaRepository<ItemSCJ, Long> {

    List<ItemSCJ> findByActivoTrueOrderByOrden();

    @Query("SELECT i FROM ItemSCJ i WHERE " +
           "(:busqueda IS NULL OR LOWER(i.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(i.leyenda) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(i.categoria) LIKE LOWER(CONCAT('%', :busqueda, '%'))) " +
           "AND (:activo IS NULL OR i.activo = :activo) " +
           "AND (:codCabecera IS NULL OR i.codCabecera = :codCabecera) " +
           "AND (:autoservicio IS NULL OR i.autoservicio = :autoservicio) " +
           "AND (:supermercado IS NULL OR i.supermercado = :supermercado)")
    Page<ItemSCJ> buscarPaginado(@Param("busqueda") String busqueda,
                                  @Param("activo") Boolean activo,
                                  @Param("codCabecera") String codCabecera,
                                  @Param("autoservicio") Boolean autoservicio,
                                  @Param("supermercado") Boolean supermercado,
                                  Pageable pageable);

}
