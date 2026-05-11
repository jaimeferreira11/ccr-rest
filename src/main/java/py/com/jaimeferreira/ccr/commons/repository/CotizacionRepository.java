package py.com.jaimeferreira.ccr.commons.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import py.com.jaimeferreira.ccr.commons.entity.Cotizacion;

import java.time.LocalDate;
import java.util.Optional;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {

    Optional<Cotizacion> findByMonedaAndFecha(String moneda, LocalDate fecha);

    Optional<Cotizacion> findTopByMonedaAndFechaLessThanEqualOrderByFechaDesc(String moneda, LocalDate fecha);
}
