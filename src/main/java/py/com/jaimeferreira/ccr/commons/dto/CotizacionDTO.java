package py.com.jaimeferreira.ccr.commons.dto;

import py.com.jaimeferreira.ccr.commons.entity.Cotizacion;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CotizacionDTO {

    private String moneda;
    private BigDecimal valor;
    private LocalDate fecha;
    private String fuente;

    public static CotizacionDTO from(Cotizacion entity) {
        CotizacionDTO dto = new CotizacionDTO();
        dto.setMoneda(entity.getMoneda());
        dto.setValor(entity.getValor());
        dto.setFecha(entity.getFecha());
        dto.setFuente(entity.getFuente());
        return dto;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getFuente() {
        return fuente;
    }

    public void setFuente(String fuente) {
        this.fuente = fuente;
    }
}
