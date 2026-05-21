package py.com.jaimeferreira.ccr.lt.dto;

import java.math.BigDecimal;

public class SucursalDTO {
    private Integer punto;
    private String direccion;
    private String provincia;
    private String ciudad;
    private BigDecimal mts2;

    public Integer getPunto() { return punto; }
    public void setPunto(Integer punto) { this.punto = punto; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getProvincia() { return provincia; }
    public void setProvincia(String provincia) { this.provincia = provincia; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public BigDecimal getMts2() { return mts2; }
    public void setMts2(BigDecimal mts2) { this.mts2 = mts2; }
}
