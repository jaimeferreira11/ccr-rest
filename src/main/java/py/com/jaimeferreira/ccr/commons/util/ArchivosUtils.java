package py.com.jaimeferreira.ccr.commons.util;

import java.io.InputStream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ResourceUtils;

public class ArchivosUtils {

        public static InputStream leerArchivoResourcesFolder(Class<?> clazz, String path, String file) {
        try {
            InputStream is = null;//clazz.getResource(path + file).openStream();
            is = new ClassPathResource( path + file).getInputStream();
            return is;
        } catch (Exception e) {
            e.printStackTrace();

        }
        return null;
    }

}
