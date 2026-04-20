package py.com.jaimeferreira.ccr.commons.controller;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone las últimas líneas del log del servidor para visualización desde el front.
 * Requiere JWT válido.
 */
@RestController
@RequestMapping(value = "admin/logs")
public class LogViewerController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogViewerController.class);
    private static final int MAX_LINES = 1000;
    private static final int DEFAULT_LINES = 200;

    @Value("${logging.file.name:./log/ccr-api-rest.log}")
    private String logFilePath;

    @GetMapping(produces = "application/json")
    public ResponseEntity<?> getLogs(@RequestParam(defaultValue = "" + DEFAULT_LINES) int lines) {
        try {
            if (!Files.exists(Paths.get(logFilePath))) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                     .body(Collections.singletonMap("error", "Archivo de log no encontrado"));
            }

            int limit = Math.min(lines, MAX_LINES);
            List<String> lastLines = readLastLines(logFilePath, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("archivo", logFilePath);
            response.put("totalLineas", lastLines.size());
            response.put("lineas", lastLines);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            LOGGER.error("Error leyendo archivo de log: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Collections.singletonMap("error", "Error al leer el log: " + e.getMessage()));
        }
    }

    private List<String> readLastLines(String filePath, int n) throws IOException {
        List<String> result = new ArrayList<>();
        try (RandomAccessFile file = new RandomAccessFile(filePath, "r")) {
            long fileLength = file.length();
            if (fileLength == 0) return result;

            long pointer = fileLength - 1;
            int linesFound = 0;

            while (pointer >= 0 && linesFound < n) {
                file.seek(pointer);
                int b = file.read();
                if (b == '\n' && pointer < fileLength - 1) {
                    linesFound++;
                }
                pointer--;
            }

            file.seek(pointer + 2);
            String line;
            while ((line = file.readLine()) != null) {
                result.add(new String(line.getBytes("ISO-8859-1"), "UTF-8"));
            }
        }
        return result;
    }
}
