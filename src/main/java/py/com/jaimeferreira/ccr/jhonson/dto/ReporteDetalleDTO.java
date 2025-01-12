
package py.com.jaimeferreira.ccr.jhonson.dto;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Jaime Ferreira
 */
public class ReporteDetalleDTO implements Serializable {

    private static final long serialVersionUID = -3824427513265827071L;

    private String codDistribuidor;
    private String nombreDistribuidor;

    private String codBoca;
    private String nombreBoca;

    @NotNull
    @NotEmpty
    private String fecha;
    private String titulo;

    @NotNull
    @NotEmpty
    private String orientacion;

    @NotNull
    @NotEmpty
    private int cantidadImagenes;

    @JsonProperty("pathImagenes")
    private List<String> pathImagenes;

//    private String pathImagen1;
//
//    private String pathImagen2;

    public String getCodBoca() {
        return codBoca;
    }

    public void setCodBoca(String codBoca) {
        this.codBoca = codBoca;
    }

    public String getNombreBoca() {
        return nombreBoca;
    }

    public void setNombreBoca(String nombreBoca) {
        this.nombreBoca = nombreBoca;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getOrientacion() {
        return orientacion;
    }

    public void setOrientacion(String orientacion) {
        this.orientacion = orientacion;
    }

    public int getCantidadImagenes() {
        return cantidadImagenes;
    }

    public void setCantidadImagenes(int cantidadImagenes) {
        this.cantidadImagenes = cantidadImagenes;
    }

    public List<String> getPathImagenes() {
        return pathImagenes;
    }

    public void setPathImagenes(List<String> pathImagenes) {
        this.pathImagenes = pathImagenes;
    }

    public String getCodDistribuidor() {
        return codDistribuidor;
    }

    public void setCodDistribuidor(String codDistribuidor) {
        this.codDistribuidor = codDistribuidor;
    }

    public String getNombreDistribuidor() {
        return nombreDistribuidor;
    }

    public void setNombreDistribuidor(String nombreDistribuidor) {
        this.nombreDistribuidor = nombreDistribuidor;
    }

//    public String getPathImagen1() {
//        return pathImagen1;
//    }
//
//    public void setPathImagen1(String pathImagen1) {
//        this.pathImagen1 = pathImagen1;
//    }
//
//    public String getPathImagen2() {
//        return pathImagen2;
//    }
//
//    public void setPathImagen2(String pathImagen2) {
//        this.pathImagen2 = pathImagen2;
//    }

}
