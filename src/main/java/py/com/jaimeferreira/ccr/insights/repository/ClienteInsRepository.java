package py.com.jaimeferreira.ccr.insights.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.insights.entity.ClienteIns;

/**
 *
 * @author Jaime Ferreira
 */
public interface ClienteInsRepository extends JpaRepository<ClienteIns, Long> {

    List<ClienteIns> findByEnabledTrue();

    List<ClienteIns> findByPais_CodigoAndEnabledTrue(String codPais);

    Optional<ClienteIns> findByCodigo(String codigo);

}
