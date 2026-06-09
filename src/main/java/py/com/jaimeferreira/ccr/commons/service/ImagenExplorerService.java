package py.com.jaimeferreira.ccr.commons.service;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import py.com.jaimeferreira.ccr.commons.dto.CarpetaDTO;
import py.com.jaimeferreira.ccr.commons.dto.ExplorarResponseDTO;
import py.com.jaimeferreira.ccr.commons.dto.MoverResultado;
import py.com.jaimeferreira.ccr.commons.exception.ArchivoExistenteException;
import py.com.jaimeferreira.ccr.commons.exception.UnknownResourceException;
import py.com.jaimeferreira.ccr.commons.util.ImagenPathValidator;

@Service
public class ImagenExplorerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImagenExplorerService.class);

    @Value("${path.directory.main_imagenes}")
    private String directorioServer;

    public ExplorarResponseDTO explorar(String pathRelativo) {
        ImagenPathValidator.validarPathDirectorio(pathRelativo);
        Path root = Paths.get(directorioServer).normalize();
        Path dir = ImagenPathValidator.resolverDentroDe(root, pathRelativo);

        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            throw new UnknownResourceException("La carpeta no existe: " + pathRelativo);
        }

        String pathActual = relativizar(root, dir);
        String pathPadre = dir.equals(root) ? null : relativizar(root, dir.getParent());

        List<CarpetaDTO> carpetas = new ArrayList<>();
        List<String> archivos = new ArrayList<>();

        try (Stream<Path> contenido = Files.list(dir)) {
            List<Path> items = contenido.collect(Collectors.toList());
            for (Path p : items) {
                if (Files.isDirectory(p)) {
                    String nombre = p.getFileName().toString();
                    carpetas.add(new CarpetaDTO(nombre, relativizar(root, p)));
                } else if (p.toString().toLowerCase().endsWith(".jpg")) {
                    archivos.add(p.getFileName().toString());
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error listando carpeta {}", dir, e);
            throw new RuntimeException("No se pudo listar la carpeta", e);
        }

        carpetas.sort(Comparator.comparing(CarpetaDTO::getNombre, String.CASE_INSENSITIVE_ORDER));
        archivos.sort(String.CASE_INSENSITIVE_ORDER);

        return new ExplorarResponseDTO(pathActual, pathPadre, carpetas, archivos);
    }

    public MoverResultado mover(String origenPath, String destinoDir) {
        ImagenPathValidator.validarPathArchivoLibre(origenPath);
        ImagenPathValidator.validarPathDirectorio(destinoDir);

        Path root = Paths.get(directorioServer).normalize();
        Path origen = ImagenPathValidator.resolverDentroDe(root, origenPath);
        Path destDir = ImagenPathValidator.resolverDentroDe(root, destinoDir);

        if (!Files.exists(origen) || !Files.isRegularFile(origen)) {
            throw new UnknownResourceException("El archivo origen no existe: " + origenPath);
        }
        if (!Files.exists(destDir) || !Files.isDirectory(destDir)) {
            throw new IllegalArgumentException("La carpeta destino no existe: " + destinoDir);
        }

        String fileName = origen.getFileName().toString();
        Path target = destDir.resolve(fileName);

        if (Files.exists(target)) {
            throw new ArchivoExistenteException("Ya existe un archivo con ese nombre en el destino");
        }

        try {
            Files.move(origen, target);
        } catch (FileAlreadyExistsException e) {
            // Carrera: el archivo apareció en destino entre el chequeo y el move.
            throw new ArchivoExistenteException("Ya existe un archivo con ese nombre en el destino");
        } catch (IOException e) {
            LOGGER.error("Error moviendo {} -> {}", origen, target, e);
            throw new RuntimeException("No se pudo mover la imagen", e);
        }

        String nuevoPath = relativizar(root, target);
        String folderRaiz = nuevoPath.contains("/") ? nuevoPath.substring(0, nuevoPath.indexOf('/')) : nuevoPath;
        String brand = ImagenPathValidator.getBrandForFolder(folderRaiz);

        return new MoverResultado(nuevoPath, brand);
    }

    private String relativizar(Path root, Path objetivo) {
        return root.relativize(objetivo).toString().replace('\\', '/');
    }
}
