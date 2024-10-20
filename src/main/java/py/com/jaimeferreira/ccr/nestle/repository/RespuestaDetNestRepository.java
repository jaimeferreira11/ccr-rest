
package py.com.jaimeferreira.ccr.nestle.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.nestle.entity.RespuestaDetNest;

/**
 *
 * @author Jaime Ferreira
 */
public interface RespuestaDetNestRepository extends JpaRepository<RespuestaDetNest, Long> {

    List<RespuestaDetNest> findByIdRespuestaCab(Long idCabecera);

}
