
package py.com.jaimeferreira.ccr.shell.service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import py.com.jaimeferreira.ccr.commons.util.ManejadorDeArchivos;
import py.com.jaimeferreira.ccr.shell.entity.RespuestaMultimediaShell;
import py.com.jaimeferreira.ccr.shell.repository.RespuestaMultimediaShellRepository;

/**
 *
 * @author Jaime Ferreira
 */

@Component
@Service
public class RespuestaMultimediaShellService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RespuestaMultimediaShellService.class);

    @Value("${path.directory.main_imagenes}")
    private String directorioServer;

    @Value("${path.directory.server_path_images_shell}")
    private String mainPathImages;

    @Autowired
    private RespuestaMultimediaShellRepository repo;

    @Autowired
    private ManejadorDeArchivos manejadorDeArchivos;

    public void save(RespuestaMultimediaShell multimedia) {

        Optional<RespuestaMultimediaShell> optional =
            repo.findByIdRespuestaCabAndPath(multimedia.getIdRespuestaCab(), multimedia.getPath());

        if (optional.isPresent()) {
            return;

        }

        repo.save(multimedia);

    }

    public void saveList(List<RespuestaMultimediaShell> multimedias) {

        LOGGER.info("Guardando lista de multimedia");

        multimedias.stream().forEach((m) -> {
            save(m);
        });

    }

    public void saveMultimedia(String path, MultipartFile file, String codBoca) {
        try {

            Path uploadDir = Paths.get(directorioServer + mainPathImages + File.separator + codBoca);
            // Crear directorio temporal si no existe
            if (!Files.exists(uploadDir)) {
                LOGGER.info("No existe el directorio " + uploadDir.toString() + " creando...");
                Files.createDirectories(uploadDir);
            }
            else {
                LOGGER.info("Existe el directorio " + uploadDir.toString());
            }

            // Archivo temporal con ID único (puedes usar formContainerId o un UUID)
            Path filePath = uploadDir.resolve(path);
            try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(filePath,
                                                                                   StandardOpenOption.CREATE,
                                                                                   StandardOpenOption.APPEND))) {

                LOGGER.info("✅ Escribeindo en  {}", filePath.toString());

                out.write(file.getBytes());
            }

            LOGGER.info("✅ Chunk recibido para archivo: {}", path);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

}
