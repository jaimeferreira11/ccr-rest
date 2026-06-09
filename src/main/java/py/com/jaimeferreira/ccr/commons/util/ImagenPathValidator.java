package py.com.jaimeferreira.ccr.commons.util;

import java.nio.file.Path;
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
        rechazarSecuenciasInseguras(pathRelativo);
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

    /**
     * Valida un path relativo de DIRECTORIO (puede ser vacío/null = raíz de imágenes).
     * No exige prefijo de brand (navegación cross-brand) ni extensión.
     */
    public static void validarPathDirectorio(String pathRelativo) {
        if (pathRelativo == null || pathRelativo.isEmpty()) {
            return; // raíz
        }
        rechazarSecuenciasInseguras(pathRelativo);
    }

    /**
     * Valida un path relativo de ARCHIVO sin exigir prefijo de brand (mover cross-brand).
     * Exige .jpg, no vacío, sin traversal ni rutas absolutas.
     */
    public static void validarPathArchivoLibre(String pathRelativo) {
        if (pathRelativo == null || pathRelativo.isEmpty()) {
            throw new IllegalArgumentException("path es obligatorio");
        }
        rechazarSecuenciasInseguras(pathRelativo);
        if (!pathRelativo.toLowerCase().endsWith(".jpg")) {
            throw new IllegalArgumentException("path inválido (debe terminar en .jpg)");
        }
    }

    /**
     * Resuelve un path relativo contra la raíz y garantiza que no escape de ella.
     * Defensa autoritativa contra traversal. Operación pura (sin I/O).
     */
    public static Path resolverDentroDe(Path root, String pathRelativo) {
        Path base = root.normalize();
        Path objetivo = base.resolve(pathRelativo == null ? "" : pathRelativo).normalize();
        if (!objetivo.startsWith(base)) {
            throw new IllegalArgumentException("path fuera de la raíz permitida");
        }
        return objetivo;
    }

    /** Rechaza traversal (..), home (~), null byte y rutas absolutas. */
    private static void rechazarSecuenciasInseguras(String pathRelativo) {
        if (pathRelativo.contains("..") || pathRelativo.contains("~") || pathRelativo.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("path inválido (contiene secuencias prohibidas)");
        }
        if (pathRelativo.startsWith("/") || pathRelativo.startsWith("\\")) {
            throw new IllegalArgumentException("path inválido (debe ser relativo)");
        }
    }

    /** Mapeo inverso carpeta -> brand (p. ej. "zoomin-shell" -> "shell"). null si no corresponde. */
    public static String getBrandForFolder(String folder) {
        if (folder == null) {
            return null;
        }
        for (Map.Entry<String, String> e : BRAND_TO_FOLDER.entrySet()) {
            if (e.getValue().equals(folder)) {
                return e.getKey();
            }
        }
        return null;
    }
}
