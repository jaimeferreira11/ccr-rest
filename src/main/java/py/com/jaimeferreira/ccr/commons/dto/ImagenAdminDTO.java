package py.com.jaimeferreira.ccr.commons.dto;

public class ImagenAdminDTO {

    private String brand;
    private String codBoca;
    private String codDistribuidor; // solo Nestle, null para los demás
    private String fileName;
    private String pathRelativo;
    private String urlPublica;
    private Integer anio;
    private Integer mes;

    public ImagenAdminDTO() { }

    public ImagenAdminDTO(String brand, String codBoca, String codDistribuidor, String fileName,
                          String pathRelativo, String urlPublica, Integer anio, Integer mes) {
        this.brand = brand;
        this.codBoca = codBoca;
        this.codDistribuidor = codDistribuidor;
        this.fileName = fileName;
        this.pathRelativo = pathRelativo;
        this.urlPublica = urlPublica;
        this.anio = anio;
        this.mes = mes;
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getCodBoca() { return codBoca; }
    public void setCodBoca(String codBoca) { this.codBoca = codBoca; }
    public String getCodDistribuidor() { return codDistribuidor; }
    public void setCodDistribuidor(String codDistribuidor) { this.codDistribuidor = codDistribuidor; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getPathRelativo() { return pathRelativo; }
    public void setPathRelativo(String pathRelativo) { this.pathRelativo = pathRelativo; }
    public String getUrlPublica() { return urlPublica; }
    public void setUrlPublica(String urlPublica) { this.urlPublica = urlPublica; }
    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }
    public Integer getMes() { return mes; }
    public void setMes(Integer mes) { this.mes = mes; }
}
