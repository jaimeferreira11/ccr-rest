package py.com.jaimeferreira.ccr.exception;

public enum EnumErrors {
    
    CONNECTION_ERROR("0", "Database connection error"),
    DUPLICATE_KEY("409", "Duplicate"),
    DATA_INTEGRITY("3", "Data integrity violation"),
    TIMEOUT("999999999", "Timeout exceeded"),
    UNKNOWN_ERROR("-1", "Unknown error"),
    NO_CONTENT("204", "No se encontraron datos");

	

    private final String code;
    private final String message;

    private EnumErrors(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
