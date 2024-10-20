
package py.com.jaimeferreira.ccr.bebidaspy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.bebidaspy.entity.RespuestaDet;

/**
 *
 * @author Jaime Ferreira
 */
public interface RespuestaDetRepository extends JpaRepository<RespuestaDet, Long> {
    
    List<RespuestaDet> findByIdRespuestaCab(Long idCabecera);

}
