
package py.com.jaimeferreira.ccr.jhonson.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import py.com.jaimeferreira.ccr.jhonson.entity.BocaAuditorSCJ;

public interface BocaAuditorSCJRepository extends JpaRepository<BocaAuditorSCJ, Long> {

    List<BocaAuditorSCJ> findByAuditor(String auditor);

    List<BocaAuditorSCJ> findByIdBoca(Long idBoca);

    void deleteByIdBocaAndAuditor(Long idBoca, String auditor);

}
