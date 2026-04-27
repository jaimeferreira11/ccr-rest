package py.com.jaimeferreira.ccr.insights.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import py.com.jaimeferreira.ccr.insights.entity.EstadoInforme;
import py.com.jaimeferreira.ccr.insights.entity.InformeIns;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Jaime Ferreira
 */
public interface InformeInsRepository extends JpaRepository<InformeIns, Long> {

    List<InformeIns> findByNombreUsuarioCreacionOrderByFechaCreacionDesc(String usuario, Pageable pageable);

    List<InformeIns> findByNombreUsuarioCreacionAndEstadoOrderByFechaCreacionDesc(String usuario, EstadoInforme estado, Pageable pageable);

    List<InformeIns> findByNombreUsuarioCreacionAndCodClienteOrderByFechaCreacionDesc(String usuario, String codCliente, Pageable pageable);

    List<InformeIns> findByNombreUsuarioCreacionAndCodClienteAndEstadoOrderByFechaCreacionDesc(String usuario, String codCliente, EstadoInforme estado, Pageable pageable);

    long countByNombreUsuarioCreacion(String usuario);
    long countByNombreUsuarioCreacionAndEstado(String usuario, EstadoInforme estado);
    long countByNombreUsuarioCreacionAndCodCliente(String usuario, String codCliente);
    long countByNombreUsuarioCreacionAndCodClienteAndEstado(String usuario, String codCliente, EstadoInforme estado);

    List<InformeIns> findByEstadoAndFechaCreacionBefore(EstadoInforme estado, LocalDateTime fechaLimite);
}
