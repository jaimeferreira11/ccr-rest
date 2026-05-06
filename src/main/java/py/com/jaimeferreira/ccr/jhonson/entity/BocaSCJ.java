
package py.com.jaimeferreira.ccr.jhonson.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 *
 * @author Jaime Ferreira
 */

@Entity
@Table(name = "bocas", schema = "jhonson")
public class BocaSCJ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cod_boca", length = 50, nullable = false)
    private String codBoca;

    @Column(name = "nombre", length = 200, nullable = false)
    private String nombre;

    @Column(name = "direccion", length = 200)
    private String direccion;

    @Column(name = "ciudad", length = 200, nullable = false)
    private String ciudad;

    @Column(name = "canal_ccr", length = 200)
    private String canalCcr;

    @Column(name = "ocasion", length = 200)
    private String ocasion;

    @Column(name = "ACTIVO", nullable = false, columnDefinition = "boolean default true")
    private boolean activo = true;

    @Column(name = "longitud", length = 500)
    private String longitud;

    @Column(name = "latitud", length = 500)
    private String latitud;

    @Column(name = "externo", nullable = false)
    private boolean externo = false;

    @Column(name = "cod_distribuidor", length = 50, nullable = false)
    private String codDistribuidor;

    @Column(name = "fecha_creacion", nullable = false, updatable = false, columnDefinition = "timestamp default now()")
    private LocalDateTime fechaCreacion;

    /** @deprecated Retrocompatibilidad con app mobile. La relación real es N:M en boca_auditor. */
    @Deprecated
    @Transient
    private String auditor;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodBoca() {
        return codBoca;
    }

    public void setCodBoca(String codBoca) {
        this.codBoca = codBoca;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getCanalCcr() {
        return canalCcr;
    }

    public void setCanalCcr(String canalCcr) {
        this.canalCcr = canalCcr;
    }

    public String getOcasion() {
        return ocasion;
    }

    public void setOcasion(String ocasion) {
        this.ocasion = ocasion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getLongitud() {
        return longitud;
    }

    public void setLongitud(String longitud) {
        this.longitud = longitud;
    }

    public String getLatitud() {
        return latitud;
    }

    public void setLatitud(String latitud) {
        this.latitud = latitud;
    }

    public boolean isExterno() {
        return externo;
    }

    public void setExterno(boolean externo) {
        this.externo = externo;
    }

    public String getCodDistribuidor() {
        return codDistribuidor;
    }

    public void setCodDistribuidor(String codDistribuidor) {
        this.codDistribuidor = codDistribuidor;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    /** @deprecated Usar boca_auditor. Se mantiene para retrocompatibilidad con app mobile. */
    @Deprecated
    public String getAuditor() {
        return auditor;
    }

    /** @deprecated Usar boca_auditor. Se mantiene para retrocompatibilidad con app mobile. */
    @Deprecated
    public void setAuditor(String auditor) {
        this.auditor = auditor;
    }

}
