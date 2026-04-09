package py.com.jaimeferreira.ccr.insights.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.insights.entity.Pais;

/**
 *
 * @author Jaime Ferreira
 */
public interface PaisInsRepository extends JpaRepository<Pais, Long> {

    List<Pais> findByActivoTrue();

    Optional<Pais> findByCodigo(String codigo);

}
