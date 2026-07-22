package py.com.jaimeferreira.ccr.lt.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import py.com.jaimeferreira.ccr.lt.util.GuaraniAmountDeserializer;
import py.com.jaimeferreira.ccr.lt.util.TolerantBigDecimalDeserializer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class TicketDTO {
    private Integer punto;
    private String nroTicket;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime hora;

    private Long eancode;
    private String ean_desc;

    // Unidades: los decimales son válidos (ej. 0,450 kg) -> heurística decimal-tolerante.
    @JsonDeserialize(using = TolerantBigDecimalDeserializer.class)
    private BigDecimal unidades_vendidas;

    // Montos en Gs: sin decimales -> los separadores son de miles.
    @JsonDeserialize(using = GuaraniAmountDeserializer.class)
    private BigDecimal precio_regular;

    @JsonDeserialize(using = GuaraniAmountDeserializer.class)
    private BigDecimal precio_promocional;

    private String tipo_venta;

    public Integer getPunto() { return punto; }
    public void setPunto(Integer punto) { this.punto = punto; }
    public String getNroTicket() { return nroTicket; }
    public void setNroTicket(String nroTicket) { this.nroTicket = nroTicket; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHora() { return hora; }
    public void setHora(LocalTime hora) { this.hora = hora; }
    public Long getEancode() { return eancode; }
    public void setEancode(Long eancode) { this.eancode = eancode; }
    public String getEan_desc() { return ean_desc; }
    public void setEan_desc(String ean_desc) { this.ean_desc = ean_desc; }
    public BigDecimal getUnidades_vendidas() { return unidades_vendidas; }
    public void setUnidades_vendidas(BigDecimal unidades_vendidas) { this.unidades_vendidas = unidades_vendidas; }
    public BigDecimal getPrecio_regular() { return precio_regular; }
    public void setPrecio_regular(BigDecimal precio_regular) { this.precio_regular = precio_regular; }
    public BigDecimal getPrecio_promocional() { return precio_promocional; }
    public void setPrecio_promocional(BigDecimal precio_promocional) { this.precio_promocional = precio_promocional; }
    public String getTipo_venta() { return tipo_venta; }
    public void setTipo_venta(String tipo_venta) { this.tipo_venta = tipo_venta; }
}
