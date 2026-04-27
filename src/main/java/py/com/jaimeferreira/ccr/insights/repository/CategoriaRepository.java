package py.com.jaimeferreira.ccr.insights.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.insights.entity.Categoria;

/**
 *
 * @author Jaime Ferreira
 */
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findAllByOrderByCliente_CodigoAscCodigoAsc();

    List<Categoria> findByCliente_CodigoOrderByCodigoAsc(String codCliente);

    List<Categoria> findByCliente_CodigoAndEnabledTrueOrderByCodigoAsc(String codCliente);

    Optional<Categoria> findByCliente_CodigoAndCodigo(String codCliente, String codigo);

}
