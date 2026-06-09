package py.com.jaimeferreira.ccr.shell.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import py.com.jaimeferreira.ccr.commons.dto.ImagenAdminDTO;
import py.com.jaimeferreira.ccr.commons.exception.UnknownResourceException;
import py.com.jaimeferreira.ccr.commons.util.ManejadorDeArchivos;

@Service
public class ImagenesShellService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImagenesShellService.class);
    private static final String BRAND = "shell";

    @Value("${path.directory.main_imagenes}")
    private String directorioServer;

    @Value("${path.directory.server_path_images_shell}")
    private String mainPathImages;

    @Autowired
    private ManejadorDeArchivos manejadorDeArchivos;

    public List<ImagenAdminDTO> findAllByMes(int anio, int mes, String codBocaOpcional) {
        Path base = Paths.get(directorioServer + mainPathImages);
        if (!Files.exists(base) || !Files.isDirectory(base)) {
            LOGGER.info("Carpeta base no existe: {}", base);
            return new ArrayList<>();
        }

        List<ImagenAdminDTO> resultado = new ArrayList<>();

        try (Stream<Path> bocaDirs = Files.list(base)) {
            List<Path> bocas = bocaDirs.filter(Files::isDirectory)
                    .filter(p -> codBocaOpcional == null || codBocaOpcional.isEmpty()
                            || p.getFileName().toString().equals(codBocaOpcional))
                    .collect(Collectors.toList());

            for (Path bocaDir : bocas) {
                String codBoca = bocaDir.getFileName().toString();
                try (Stream<Path> files = Files.list(bocaDir)) {
                    files.filter(Files::isRegularFile)
                         .filter(p -> p.toString().toLowerCase().endsWith(".jpg"))
                         .filter(p -> matchesAnioMes(p.getFileName().toString(), anio, mes))
                         .forEach(p -> resultado.add(buildDto(codBoca, p)));
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error iterando carpetas de Shell", e);
        }

        return resultado;
    }

    public void rotarImagen(String pathRelativo) {
        Path archivo = Paths.get(directorioServer, pathRelativo);
        if (!Files.exists(archivo)) {
            throw new UnknownResourceException("Archivo no existe: " + pathRelativo);
        }
        try {
            manejadorDeArchivos.rotateImage(pathRelativo, -90);
        } catch (Exception e) {
            LOGGER.error("Error rotando imagen " + pathRelativo, e);
            throw new RuntimeException("No se pudo rotar la imagen", e);
        }
    }

    private boolean matchesAnioMes(String fileName, int anio, int mes) {
        try {
            String mesStr = mes < 10 ? "0" + mes : String.valueOf(mes);
            // patrón: {codBoca}_{anio}_{mes}_{nro}.jpg
            return fileName.contains("_" + anio + "_" + mesStr + "_");
        } catch (Exception e) {
            return false;
        }
    }

    private ImagenAdminDTO buildDto(String codBoca, Path archivo) {
        String fileName = archivo.getFileName().toString();
        String pathRelativo = mainPathImages + "/" + codBoca + "/" + fileName;
        Integer anio = null;
        Integer mes = null;
        try {
            String[] partes = fileName.replace(".jpg", "").split("_");
            if (partes.length >= 3) {
                anio = Integer.parseInt(partes[1]);
                mes = Integer.parseInt(partes[2]);
            }
        } catch (Exception ignored) { }

        return new ImagenAdminDTO(BRAND, codBoca, null, fileName, pathRelativo,
                buildUrlPublica(pathRelativo), anio, mes);
    }

    private String buildUrlPublica(String pathRelativo) {
        // El controller (Task 6) compone la URL final con cache-buster.
        return pathRelativo;
    }
}
