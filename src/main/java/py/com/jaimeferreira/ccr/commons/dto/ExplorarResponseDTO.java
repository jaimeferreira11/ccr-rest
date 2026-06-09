package py.com.jaimeferreira.ccr.commons.dto;

import java.util.List;

public class ExplorarResponseDTO {
    private String pathActual;
    private String pathPadre;
    private List<CarpetaDTO> carpetas;
    private List<String> archivos;

    public ExplorarResponseDTO() { }

    public ExplorarResponseDTO(String pathActual, String pathPadre,
                               List<CarpetaDTO> carpetas, List<String> archivos) {
        this.pathActual = pathActual;
        this.pathPadre = pathPadre;
        this.carpetas = carpetas;
        this.archivos = archivos;
    }

    public String getPathActual() { return pathActual; }
    public void setPathActual(String pathActual) { this.pathActual = pathActual; }
    public String getPathPadre() { return pathPadre; }
    public void setPathPadre(String pathPadre) { this.pathPadre = pathPadre; }
    public List<CarpetaDTO> getCarpetas() { return carpetas; }
    public void setCarpetas(List<CarpetaDTO> carpetas) { this.carpetas = carpetas; }
    public List<String> getArchivos() { return archivos; }
    public void setArchivos(List<String> archivos) { this.archivos = archivos; }
}
