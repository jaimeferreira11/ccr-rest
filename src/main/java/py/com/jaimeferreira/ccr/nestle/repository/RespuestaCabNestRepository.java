
package py.com.jaimeferreira.ccr.nestle.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.nestle.entity.RespuestaCabNest;

/**
 *
 * @author Jaime Ferreira
 */
public interface RespuestaCabNestRepository extends JpaRepository<RespuestaCabNest, Long> {

    Optional<RespuestaCabNest> findByIdBocaAndUsuarioAndFechaCreacionAndActivoTrue(Long idBoca, String usuario,
                                                                                   String fechaCreacion);

}
