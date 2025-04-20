
package py.com.jaimeferreira.ccr.shell.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.shell.entity.RespuestaMultimediaShell;

/**
 *
 * @author Jaime Ferreira
 */
public interface RespuestaMultimediaShellRepository extends JpaRepository<RespuestaMultimediaShell, Long> {

    List<RespuestaMultimediaShell> findByIdRespuestaCab(Long idCabecera);

    Optional<RespuestaMultimediaShell> findByIdRespuestaCabAndPath(Long idRespuestaCab, String path);

}
