package py.com.jaimeferreira.ccr.commons.util;

import java.util.HashMap;
import java.util.Map;

public final class ImagenPathValidator {

    private static final Map<String, String> BRAND_TO_FOLDER = new HashMap<>();

    static {
        BRAND_TO_FOLDER.put("jhonson", "zoomin-jhonson");
        BRAND_TO_FOLDER.put("nestle", "zoomin-nestle");
        BRAND_TO_FOLDER.put("shell", "zoomin-shell");
    }

    private ImagenPathValidator() { }

    public static boolean isBrandSoportado(String brand) {
        return brand != null && BRAND_TO_FOLDER.containsKey(brand.toLowerCase());
    }

    public static String getFolderForBrand(String brand) {
        if (brand == null) {
            return null;
        }
        return BRAND_TO_FOLDER.get(brand.toLowerCase());
    }

    /**
     * Valida que un path relativo sea seguro:
     * - no contiene .. ni ~ ni caracteres null
     * - no es absoluto
     * - empieza con la carpeta del brand
     * - termina en .jpg
     */
    public static void validarPathRelativo(String brand, String pathRelativo) {
        if (pathRelativo == null || pathRelativo.isEmpty()) {
            throw new IllegalArgumentException("path es obligatorio");
        }
        if (pathRelativo.contains("..") || pathRelativo.contains("~") || pathRelativo.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("path inválido (contiene secuencias prohibidas)");
        }
        if (pathRelativo.startsWith("/") || pathRelativo.startsWith("\\")) {
            throw new IllegalArgumentException("path inválido (debe ser relativo)");
        }
        if (!pathRelativo.toLowerCase().endsWith(".jpg")) {
            throw new IllegalArgumentException("path inválido (debe terminar en .jpg)");
        }
        String folderEsperado = getFolderForBrand(brand);
        if (folderEsperado == null) {
            throw new IllegalArgumentException("brand no soportado: " + brand);
        }
        if (!pathRelativo.startsWith(folderEsperado + "/")) {
            throw new IllegalArgumentException(
                "path inválido (debe arrancar con " + folderEsperado + "/)");
        }
    }
}
