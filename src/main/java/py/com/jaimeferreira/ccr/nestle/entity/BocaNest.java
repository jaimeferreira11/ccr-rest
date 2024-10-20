
package py.com.jaimeferreira.ccr.nestle.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author Jaime Ferreira
 */
@Entity
@Table(name = "bocas", schema = "nestle")
public class BocaNest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @NotNull
    @Column(name = "COD_BOCA", nullable = false)
    private String codBoca;

    @NotNull
    @Size(max = 200)
    @Column(name = "NOMBRE", nullable = false, length = 200)
    private String nombre;

    @Size(max = 200)
    @Column(name = "DIRECCION", length = 200)
    private String direccion;

    @NotNull
    @Size(max = 200)
    @Column(name = "CIUDAD", nullable = false, length = 200)
    private String ciudad;

    @Size(max = 200)
    @NotNull
    @Column(name = "CANAL_CCR", length = 200, nullable = false)
    private String canalCcr;

    @NotNull
    @Column(name = "ACTIVO", nullable = false, columnDefinition = "NUMBER(1) DEFAULT 1 CHECK (ACTIVO IN (0,1))")
    private Boolean activo;

    @Size(max = 200)
    @Column(name = "OCASION")
    private String ocasion;

    @Column(name = "mes_ultima_medicion")
    private String mesUltimaMedicion;
    
    
    @Size(max = 500)
    @Column(name = "LONGITUD", length = 500)
    private String longitud;

    
    @Size(max = 500)
    @Column(name = "LATITUD", length = 500)
    private String latitud;

    // Getters and Setters

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

    public String getCanalCcr() {
        return canalCcr;
    }

    public void setCanalCcr(String canalCcr) {
        this.canalCcr = canalCcr;
    }

    public String getMesUltimaMedicion() {
        return mesUltimaMedicion;
    }

    public void setMesUltimaMedicion(String mesUltimaMedicion) {
        this.mesUltimaMedicion = mesUltimaMedicion;
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

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public String getOcasion() {
        return ocasion;
    }

    public void setOcasion(String ocasion) {
        this.ocasion = ocasion;
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
    
    

}
