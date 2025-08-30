
package py.com.jaimeferreira.ccr.nestle.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.nestle.entity.UsuarioDistribuidorNest;

/**
 *
 * @author Jaime Ferreira
 */
public interface UsuarioDistribuidorNestRepository extends JpaRepository<UsuarioDistribuidorNest, Long> {

    List<UsuarioDistribuidorNest> findByUsuario(String usuario);

}
