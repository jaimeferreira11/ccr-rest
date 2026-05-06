
package py.com.jaimeferreira.ccr.jhonson.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "boca_auditor", schema = "jhonson")
public class BocaAuditorSCJ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_boca", nullable = false)
    private Long idBoca;

    @Column(name = "auditor", length = 200, nullable = false)
    private String auditor;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdBoca() {
        return idBoca;
    }

    public void setIdBoca(Long idBoca) {
        this.idBoca = idBoca;
    }

    public String getAuditor() {
        return auditor;
    }

    public void setAuditor(String auditor) {
        this.auditor = auditor;
    }

}
