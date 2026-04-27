
package py.com.jaimeferreira.ccr.jhonson.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.jhonson.entity.RespuestaCabSCJ;

/**
 *
 * @author Jaime Ferreira
 */
public interface RespuestaCabSCJRepository extends JpaRepository<RespuestaCabSCJ, Long> {

    Optional<RespuestaCabSCJ> findByIdBocaAndUsuarioAndFechaCreacionAndActivoTrue(Long idBoca, String usuario,
                                                                                   String fechaCreacion);

}
