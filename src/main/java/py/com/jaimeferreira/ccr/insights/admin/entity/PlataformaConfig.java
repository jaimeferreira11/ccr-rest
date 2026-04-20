package py.com.jaimeferreira.ccr.insights.admin.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Configuración global de la plataforma.
 * Se usa para habilitar/deshabilitar el acceso en caso de falta de pago.
 *
 * @author Jaime Ferreira
 */
@Entity
@Table(name = "plataforma_config", schema = "ccr")
public class PlataformaConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "activa", nullable = false)
    private Boolean activa;

    @Column(name = "mensaje_suspension", columnDefinition = "TEXT")
    private String mensajeSuspension;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "nombre_usuario_actualizacion", length = 200)
    private String nombreUsuarioActualizacion;

    public Long getId() { return id; }

    public Boolean getActiva() { return activa; }
    public void setActiva(Boolean activa) { this.activa = activa; }

    public String getMensajeSuspension() { return mensajeSuspension; }
    public void setMensajeSuspension(String mensajeSuspension) { this.mensajeSuspension = mensajeSuspension; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public String getNombreUsuarioActualizacion() { return nombreUsuarioActualizacion; }
    public void setNombreUsuarioActualizacion(String v) { this.nombreUsuarioActualizacion = v; }
}
