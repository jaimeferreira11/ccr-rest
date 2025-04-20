
package py.com.jaimeferreira.ccr.shell.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.shell.entity.RespuestaDetShell;

/**
 *
 * @author Jaime Ferreira
 */
public interface RespuestaDetShellRepository extends JpaRepository<RespuestaDetShell, Long> {

    List<RespuestaDetShell> findByIdRespuestaCab(Long idCabecera);

}
