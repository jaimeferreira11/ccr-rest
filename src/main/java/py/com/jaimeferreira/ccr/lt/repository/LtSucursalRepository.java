package py.com.jaimeferreira.ccr.lt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import py.com.jaimeferreira.ccr.lt.entity.LtSucursal;
import java.util.Optional;

public interface LtSucursalRepository extends JpaRepository<LtSucursal, Long> {
    Optional<LtSucursal> findByPunto(Integer punto);
}
