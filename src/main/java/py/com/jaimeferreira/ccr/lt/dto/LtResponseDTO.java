package py.com.jaimeferreira.ccr.lt.dto;

public class LtResponseDTO {
    private String status;
    private String mensaje;
    private int registros;

    public LtResponseDTO(String status, String mensaje, int registros) {
        this.status = status;
        this.mensaje = mensaje;
        this.registros = registros;
    }

    public String getStatus() { return status; }
    public String getMensaje() { return mensaje; }
    public int getRegistros() { return registros; }
}
