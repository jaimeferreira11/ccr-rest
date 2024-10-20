package py.com.jaimeferreira.ccr.commons.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/***
 * 
 * @author Luis Capdevila [luis_capde@hotmail.com]
 *
 */


@MappedSuperclass
public abstract class BaseEntidad {

	@Column(name = "fecha_creacion")
	private LocalDateTime fechaCreacion;

	@Column(name = "fecha_actualizacion")
	private LocalDateTime fechaActualizacion;

	@Column(name = "nombre_usuario_creacion")
	private String nombreUsuarioCreacion;

	@Column(name = "nombre_usuario_actualizacion")
	private String nombreUsuarioActualizacion;

	public LocalDateTime getFechaCreacion() {
		return fechaCreacion;
	}

	public void setFechaCreacion(LocalDateTime fechaCreacion) {
		this.fechaCreacion = fechaCreacion;
	}

	public LocalDateTime getFechaActualizacion() {
		return fechaActualizacion;
	}

	public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
		this.fechaActualizacion = fechaActualizacion;
	}

	public String getNombreUsuarioCreacion() {
		return nombreUsuarioCreacion;
	}

	public void setNombreUsuarioCreacion(String nombreUsuarioCreacion) {
		this.nombreUsuarioCreacion = nombreUsuarioCreacion;
	}

	public String getNombreUsuarioActualizacion() {
		return nombreUsuarioActualizacion;
	}

	public void setNombreUsuarioActualizacion(String nombreUsuarioActualizacion) {
		this.nombreUsuarioActualizacion = nombreUsuarioActualizacion;
	}

}