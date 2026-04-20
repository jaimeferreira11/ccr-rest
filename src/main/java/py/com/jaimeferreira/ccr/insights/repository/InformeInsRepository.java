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

    List<InformeIns> findByEstadoAndFechaCreacionBefore(EstadoInforme estado, LocalDateTime fechaLimite);
}
