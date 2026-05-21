package py.com.jaimeferreira.ccr.lt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import py.com.jaimeferreira.ccr.lt.entity.LtProducto;
import java.util.Optional;

public interface LtProductoRepository extends JpaRepository<LtProducto, Long> {
    Optional<LtProducto> findByEancode(Long eancode);
}
