
package py.com.jaimeferreira.ccr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.entity.RespuestaImagen;

/**
 *
 * @author Jaime Ferreira
 */
public interface RespuestaImagenRepository extends JpaRepository<RespuestaImagen, Long> {

    List<RespuestaImagen> findByIdRespuestaCab(Long idCabecera);

}
