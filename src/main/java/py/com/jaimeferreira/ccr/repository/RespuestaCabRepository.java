
package py.com.jaimeferreira.ccr.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.entity.RespuestaCab;

/**
 *
 * @author Jaime Ferreira
 */
public interface RespuestaCabRepository extends JpaRepository<RespuestaCab, Long> {

    Optional<RespuestaCab> findByIdBocaAndUsuarioAndFechaCreacionAndActivoTrue(Long idBoca, String usuario, String fechaCreacion);

}
