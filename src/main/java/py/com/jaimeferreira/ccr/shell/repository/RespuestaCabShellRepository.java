
package py.com.jaimeferreira.ccr.shell.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.shell.entity.RespuestaCabShell;

/**
 *
 * @author Jaime Ferreira
 */
public interface RespuestaCabShellRepository extends JpaRepository<RespuestaCabShell, Long> {

    Optional<RespuestaCabShell> findByIdBocaAndUsuarioAndFechaCreacionAndActivoTrue(Long idBoca, String usuario,
                                                                                    String fechaCreacion);

}
