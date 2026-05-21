package py.com.jaimeferreira.ccr.lt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import py.com.jaimeferreira.ccr.lt.entity.LtPersona;
import java.util.Optional;

public interface LtPersonaRepository extends JpaRepository<LtPersona, Long> {
    Optional<LtPersona> findByPuntoAndNroTicketAndIdentificacion(Integer punto, String nroTicket, String identificacion);
}
