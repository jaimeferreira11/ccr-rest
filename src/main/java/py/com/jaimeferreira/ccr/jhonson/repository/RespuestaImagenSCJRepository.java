
package py.com.jaimeferreira.ccr.jhonson.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.jhonson.entity.RespuestaImagenSCJ;

/**
 *
 * @author Jaime Ferreira
 */
public interface RespuestaImagenSCJRepository extends JpaRepository<RespuestaImagenSCJ, Long> {

    List<RespuestaImagenSCJ> findByIdRespuestaCab(Long idCabecera);

}
