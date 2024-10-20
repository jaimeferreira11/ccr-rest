package py.com.jaimeferreira.ccr.commons.generico;

import java.util.List;

public class PaginadorGenerico<T> {
    
    private int total;
    private int paginaActual;
    private int porPagina;
    private int ultimaPagina;
    private List<T> data;

    public PaginadorGenerico(int total, int paginaActual, int porPagina, int ultimaPagina, List<T> data) {
        this.total = total;
        this.paginaActual = paginaActual;
        this.porPagina = porPagina;
        this.ultimaPagina = ultimaPagina;
        this.data = data;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getPaginaActual() {
        return paginaActual;
    }

    public void setPaginaActual(int paginaActual) {
        this.paginaActual = paginaActual;
    }

    public int getPorPagina() {
        return porPagina;
    }

    public void setPorPagina(int porPagina) {
        this.porPagina = porPagina;
    }

    public int getUltimaPagina() {
        return ultimaPagina;
    }

    public void setUltimaPagina(int ultimaPagina) {
        this.ultimaPagina = ultimaPagina;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }
    
    
    

}

