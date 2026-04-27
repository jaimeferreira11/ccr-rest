
package py.com.jaimeferreira.ccr.jhonson.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.jhonson.entity.RespuestaDetSCJ;

/**
 *
 * @author Jaime Ferreira
 */
public interface RespuestaDetSCJRepository extends JpaRepository<RespuestaDetSCJ, Long> {

    List<RespuestaDetSCJ> findByIdRespuestaCab(Long idCabecera);

}
