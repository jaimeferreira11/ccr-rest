
package py.com.jaimeferreira.ccr.nestle.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.nestle.entity.RespuestaImagenNest;

/**
 *
 * @author Jaime Ferreira
 */
public interface RespuestaImagenNestRepository extends JpaRepository<RespuestaImagenNest, Long> {

    List<RespuestaImagenNest> findByIdRespuestaCab(Long idCabecera);

}
