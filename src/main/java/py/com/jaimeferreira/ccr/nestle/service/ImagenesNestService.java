
package py.com.jaimeferreira.ccr.nestle.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.commons.exception.UnknownResourceException;
import py.com.jaimeferreira.ccr.commons.util.ManejadorDeArchivos;
import py.com.jaimeferreira.ccr.nestle.constants.ConstantsNest;
import py.com.jaimeferreira.ccr.nestle.dto.ImagenBocaMesDTO;
import py.com.jaimeferreira.ccr.nestle.entity.BocaNest;
import py.com.jaimeferreira.ccr.nestle.repository.BocasNestRepository;
import py.com.jaimeferreira.ccr.nestle.repository.DistribuidoresNestRepository;
import py.com.jaimeferreira.ccr.nestle.service.filter.ImagenesFilter;


/**
 *
 * @author Jaime Ferreira
 */

@Component
@Service
public class ImagenesNestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImagenesNestService.class);

    @Value("${path.directory.main_imagenes}")
    private String directorioServer;

    @Value("${path.directory.server_path_images_nestle}")
    private String mainPathImages;

    @Value("${path.directory.server_path_images_externo_nestle}")
    private String mainPathImagesExterno;
    
    
    

    @Autowired
    private BocasNestRepository bocasRepository;

    @Autowired
    private DistribuidoresNestRepository distribuidoresRepository;

    @Autowired
    private ManejadorDeArchivos manejadorDeArchivos;
    
    @Value("${env.active}")
    private String envProfile;

    // Array de meses en español
    private static final String[] MONTH_NAMES = {
        "Enero",
        "Febrero",
        "Marzo",
        "Abril",
        "Mayo",
        "Junio",
        "Julio",
        "Agosto",
        "Septiembre",
        "Octubre",
        "Noviembre",
        "Diciembre"
    };

    public List<String> readMainFolders() {

        if ("prod".equalsIgnoreCase(envProfile)) {
            this.LOGGER.info("La carpeta principal es: " + envProfile);

            String url = ConstantsNest.URL_PROD_IMAGES;
            List<String> folders = extractLinksFromPreTag(url);

            return folders.stream().filter(d -> !d.contains("scj")).collect(Collectors.toList());
        }

        try {
            Path path = Paths.get(directorioServer + mainPathImages);
            this.LOGGER.info("La carpeta principal es: " + path.toString());

            // Verifica que el directorio existe
            if (Files.exists(path) && Files.isDirectory(path)) {
                return Files.list(path)
                            .filter(Files::isDirectory)
                            .map(Path::getFileName)
                            .map(Path::toString)
                            .collect(Collectors.toList());
            }
            else {
                throw new IOException("El directorio especificado no existe o no es un directorio.");
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>(); // Retorna una lista vacía si hay error
        }
    }

    public List<String> extractLinksFromPreTag(String url) {
        List<String> linksText = new ArrayList<>();

        try {
            // Conexión a la URL y obtención del documento HTML
            Document document = Jsoup.connect(url).get();

            // Seleccionar la etiqueta <pre>
            Element preTag = document.selectFirst("pre");

            if (preTag != null) {
                // Seleccionar todos los enlaces <a> dentro de la etiqueta <pre>
                Elements links = preTag.select("a");

                // Extraer el texto de cada enlace
                for (Element link : links) {
                    String linkText = link.text();
                    if (!"[Al directorio principal]".equals(linkText)) {
                        linksText.add(linkText);
                    }
                }
            }
            else {
                System.out.println("No se encontró la etiqueta <pre> en la página.");
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return linksText;
    }

    public List<String> findByBoca(String codBoca, String anio) {

        if (!bocasRepository.findByCodBoca(codBoca).isPresent()) {
            throw new UnknownResourceException("Boca con codigo " + codBoca + " no encontrado");
        }

        if ("prod".equalsIgnoreCase(envProfile)) {
            String url = ConstantsNest.URL_PROD_IMAGES + codBoca;
            LOGGER.info("Buscando imagenes en la ruta : " + url);

            List<String> folders = extractLinksFromPreTag(url);

            return folders.stream().filter(d -> !d.contains("scj")).collect(Collectors.toList());

        }

        Path path = Paths.get(directorioServer + mainPathImages, codBoca);

        LOGGER.info("Buscando imagenes en la carpeta : " + path.toString());

        if (Files.exists(path) && Files.isDirectory(path)) {
            try {
                return Files.list(path)
                            .filter(Files::isRegularFile)
                            .filter(p -> p.toString().endsWith(".jpg"))
                            .filter(p -> isCurrentYear(p.toString(), anio))
                            .map(Path::getFileName)
                            .map(Path::toString)
                            .collect(Collectors.toList());
            }
            catch (IOException e) {
                e.printStackTrace();
                throw new UnknownResourceException("No existe una carpeta para la boca " + codBoca);
            }
        }

        return new ArrayList<>();
    }

    public List<String> findByBocaAndMes(String codBoca, String mes, String anio) {
        List<String> files = findByBoca(codBoca, anio);

        LOGGER.info("La cantidad total de files es : " + files.size());

        if (mes == null || mes.isEmpty() || files.isEmpty()) {
            return files;
        }

        return files.stream()
                    .filter(fileName -> {
                        try {
                            return isCurrentYear(fileName, anio) && matchMonth(fileName, mes);
                        }
                        catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
    }

    public List<ImagenBocaMesDTO> findMesByBoca(String codigo) {

        Path path = Paths.get(directorioServer + mainPathImages, codigo);

        LOGGER.info("Buscando en la carpeta " + path.toString());

        List<String> filesString = findByBoca(codigo, null);

        int currentYear = LocalDate.now().getYear();

        Map<String, Integer> monthCount = new HashMap<>();

        for (String fileName : filesString) {
            try {
                String datePart = fileName.substring(fileName.lastIndexOf('_') - 7, fileName.lastIndexOf('.'));
                String[] dates = datePart.split("_");

                if (dates[0].toString().equals(String.valueOf(currentYear))) {
                    String month = dates[1];

                    month = MONTH_NAMES[Integer.parseInt(month) - 1];

                    monthCount.put(month, monthCount.getOrDefault(month, 0) + 1);
                }
            }
            catch (Exception e) {
                System.out.println("Error parsing date for file: " + fileName);
            }
        }

        return monthCount.entrySet().stream()
                         .map(entry -> new ImagenBocaMesDTO.Builder()
                                                                     .codBoca(codigo)
                                                                     .mes(entry.getKey())
                                                                     .cantidad(entry.getValue())
                                                                     .build())
                         .collect(Collectors.toList());

    }

    public List<String> findByFilter(ImagenesFilter filters) {

        try {

            Optional<BocaNest> opBoca = bocasRepository.findByCodBoca(filters.getCodDistribuidor());
            if (!opBoca.isPresent()) {
                throw new UnknownResourceException("No existe la boca " + filters.getCodDistribuidor());
            }

            Path path = Paths.get(directorioServer + mainPathImages);
            this.LOGGER.info("La carpeta principal es: " + path.toString());

            // Verifica que el directorio existe
            if (Files.exists(path) && Files.isDirectory(path)) {
                return Files.list(path)
                            .filter(Files::isDirectory)   // Filtra solo los directorios
                            .map(Path::getFileName)       // Obtiene el nombre de cada carpeta
                            .map(Path::toString)          // Convierte a String
                            .collect(Collectors.toList());
            }
            else {
                throw new IOException("El directorio especificado no existe o no es un directorio.");
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

    }

    public String saveImage(String base64FileString, String extension) {
        try {

            if (extension == null) {
                extension = "jpg";
            }

            String fullPath = mainPathImagesExterno + "/IMG_"
                    + new Timestamp(System.currentTimeMillis()).getTime() + "." + extension;

            manejadorDeArchivos.base64ToImagen(fullPath,
                                               base64FileString, null, false);

            return fullPath;
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

    }

    private boolean isCurrentYear(String fileName, String anio) {

        int currentYear = LocalDate.now().getYear();

        if (anio != null && anio.length() == 4) {
            try {
                currentYear = Integer.parseInt(anio);
            }
            catch (Exception e) {
                // TODO: handle exception
                LOGGER.info("No se pudo convertir el anio: " + anio);
            }
        }

        String datePart = fileName.substring(fileName.lastIndexOf('_') - 7, fileName.lastIndexOf('.'));
        String[] dates = datePart.split("_");

        return dates[0].toString().equals(String.valueOf(currentYear));

    }

    private boolean matchMonth(String fileName, String mes) {
        mes = mes.length() == 1 ? "0".concat(mes) : mes;
        String datePart = fileName.substring(fileName.lastIndexOf('_') - 7, fileName.lastIndexOf('.'));
        String[] dates = datePart.split("_");

        String month = dates[1];

        return mes.equals(month);

    }

}
