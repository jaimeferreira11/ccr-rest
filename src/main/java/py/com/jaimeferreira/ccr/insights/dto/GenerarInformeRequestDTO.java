package py.com.jaimeferreira.ccr.insights.dto;

/**
 * DTO de request para iniciar la generación de un informe Insights.
 *
 * @author Jaime Ferreira
 */
public class GenerarInformeRequestDTO {

    private String codCliente;
    private String tipoReporte;

    public String getCodCliente() {
        return codCliente;
    }

    public void setCodCliente(String codCliente) {
        this.codCliente = codCliente;
    }

    public String getTipoReporte() {
        return tipoReporte;
    }

    public void setTipoReporte(String tipoReporte) {
        this.tipoReporte = tipoReporte;
    }
}
