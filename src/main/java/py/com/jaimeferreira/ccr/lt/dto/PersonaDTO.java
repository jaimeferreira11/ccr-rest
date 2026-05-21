package py.com.jaimeferreira.ccr.lt.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public class PersonaDTO {
    private Integer punto;
    private String nroTicket;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;

    private String identificacion;
    private String nombreyapellidoempresa;

    public Integer getPunto() { return punto; }
    public void setPunto(Integer punto) { this.punto = punto; }
    public String getNroTicket() { return nroTicket; }
    public void setNroTicket(String nroTicket) { this.nroTicket = nroTicket; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String identificacion) { this.identificacion = identificacion; }
    public String getNombreyapellidoempresa() { return nombreyapellidoempresa; }
    public void setNombreyapellidoempresa(String nombreyapellidoempresa) { this.nombreyapellidoempresa = nombreyapellidoempresa; }
}
