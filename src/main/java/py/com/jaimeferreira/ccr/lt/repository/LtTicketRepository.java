package py.com.jaimeferreira.ccr.lt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import py.com.jaimeferreira.ccr.lt.entity.LtTicket;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LtTicketRepository extends JpaRepository<LtTicket, Long> {
    Optional<LtTicket> findByPuntoAndNroTicketAndEancode(Integer punto, String nroTicket, String eancode);

    List<LtTicket> findByFechaBetweenOrderByFechaAscPuntoAscNroTicketAsc(LocalDate desde, LocalDate hasta);
}
