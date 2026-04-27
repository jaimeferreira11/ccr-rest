
package py.com.jaimeferreira.ccr.jhonson.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.jhonson.entity.UsuarioDistribuidorSCJ;

/**
 *
 * @author Jaime Ferreira
 */
public interface UsuarioDistribuidorSCJRepository extends JpaRepository<UsuarioDistribuidorSCJ, Long> {

    List<UsuarioDistribuidorSCJ> findByUsuario(String usuario);

}
