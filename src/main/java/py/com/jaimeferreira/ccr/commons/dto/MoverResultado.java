package py.com.jaimeferreira.ccr.commons.dto;

public class MoverResultado {
    private final String pathRelativo;
    private final String brand;

    public MoverResultado(String pathRelativo, String brand) {
        this.pathRelativo = pathRelativo;
        this.brand = brand;
    }

    public String getPathRelativo() { return pathRelativo; }
    public String getBrand() { return brand; }
}
