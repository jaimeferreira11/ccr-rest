package py.com.jaimeferreira.ccr.commons.dto;

public class CarpetaDTO {
    private String nombre;
    private String path;

    public CarpetaDTO() { }

    public CarpetaDTO(String nombre, String path) {
        this.nombre = nombre;
        this.path = path;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}
