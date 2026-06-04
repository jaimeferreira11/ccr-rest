package py.com.jaimeferreira.ccr.insights.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import py.com.jaimeferreira.ccr.insights.entity.AuditoriaIns;
import py.com.jaimeferreira.ccr.insights.entity.EventoAuditoriaIns;

import java.util.List;

/**
 * @author Jaime Ferreira
 */
public interface AuditoriaInsRepository extends JpaRepository<AuditoriaIns, Long> {

    List<AuditoriaIns> findAllByOrderByFechaHoraDesc(Pageable pageable);

    List<AuditoriaIns> findByEventoOrderByFechaHoraDesc(EventoAuditoriaIns evento, Pageable pageable);

    List<AuditoriaIns> findByCodClienteOrderByFechaHoraDesc(String codCliente, Pageable pageable);

    List<AuditoriaIns> findByEventoAndCodClienteOrderByFechaHoraDesc(
            EventoAuditoriaIns evento, String codCliente, Pageable pageable);

    long countByEvento(EventoAuditoriaIns evento);

    long countByCodCliente(String codCliente);

    long countByEventoAndCodCliente(EventoAuditoriaIns evento, String codCliente);
}
