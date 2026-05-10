package py.com.jaimeferreira.ccr.insights.admin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Servicio que monitorea el archivo de log de la aplicación y emite líneas nuevas
 * a los clientes SSE conectados, simulando un "tail -f" en el navegador.
 */
@Service
public class LogStreamService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogStreamService.class);

    /** Cantidad de líneas históricas que se envían al conectar un nuevo cliente. */
    private static final int INITIAL_LINES = 100;

    @Value("${logging.file.name:./log/ccr-api-rest.log}")
    private String logFilePath;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "log-tail");
        t.setDaemon(true);
        return t;
    });

    private long lastFilePointer = 0;
    private long lastFileLength = 0;

    @PostConstruct
    public void init() {
        File logFile = new File(logFilePath);
        if (logFile.exists()) {
            lastFilePointer = logFile.length();
            lastFileLength = logFile.length();
        }
        scheduler.scheduleWithFixedDelay(this::checkForNewLines, 1, 1, TimeUnit.SECONDS);
        LOGGER.info("LogStreamService iniciado, monitoreando: {}", logFilePath);
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    /**
     * Registra un nuevo cliente SSE. Envía las últimas N líneas como contexto inicial.
     */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // sin timeout
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        // Enviar las últimas líneas como contexto inicial
        try {
            String tail = readTailLines(INITIAL_LINES);
            if (!tail.isEmpty()) {
                emitter.send(SseEmitter.event().name("initial").data(tail));
            }
        } catch (IOException e) {
            LOGGER.warn("Error al enviar líneas iniciales: {}", e.getMessage());
        }

        emitters.add(emitter);
        LOGGER.info("Nuevo cliente SSE de logs conectado. Total activos: {}", emitters.size());
        return emitter;
    }

    /**
     * Polling periódico: lee las líneas nuevas del archivo de log desde la última
     * posición conocida y las envía a todos los clientes SSE conectados.
     */
    private void checkForNewLines() {
        if (emitters.isEmpty()) {
            return;
        }

        File logFile = new File(logFilePath);
        if (!logFile.exists()) {
            return;
        }

        long currentLength = logFile.length();

        // Archivo truncado o rotado (logback rollover): resetear al inicio
        if (currentLength < lastFileLength) {
            lastFilePointer = 0;
        }
        lastFileLength = currentLength;

        if (currentLength <= lastFilePointer) {
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
            raf.seek(lastFilePointer);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = raf.readLine()) != null) {
                // readLine() devuelve ISO-8859-1; re-interpretar como UTF-8
                line = new String(line.getBytes("ISO-8859-1"), "UTF-8");
                sb.append(line).append('\n');
            }
            lastFilePointer = raf.getFilePointer();

            if (sb.length() > 0) {
                String data = sb.toString();
                broadcast(data);
            }
        } catch (IOException e) {
            LOGGER.warn("Error al leer log: {}", e.getMessage());
        }
    }

    private void broadcast(String data) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("log").data(data));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }

    /**
     * Lee las últimas N líneas del archivo de log (para contexto inicial).
     */
    private String readTailLines(int n) throws IOException {
        File logFile = new File(logFilePath);
        if (!logFile.exists() || logFile.length() == 0) {
            return "";
        }

        try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
            long fileLength = raf.length();
            long pos = fileLength - 1;
            int lineCount = 0;

            // Retroceder desde el final contando saltos de línea
            while (pos > 0 && lineCount <= n) {
                raf.seek(pos);
                if (raf.readByte() == '\n') {
                    lineCount++;
                }
                pos--;
            }

            // Posicionar al inicio de la línea encontrada
            raf.seek(pos == 0 ? 0 : pos + 2);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = raf.readLine()) != null) {
                line = new String(line.getBytes("ISO-8859-1"), "UTF-8");
                sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }
}
