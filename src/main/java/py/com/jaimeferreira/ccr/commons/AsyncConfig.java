package py.com.jaimeferreira.ccr.commons;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuración del pool de threads para tareas @Async.
 *
 * Sin esta configuración, Spring usa SimpleAsyncTaskExecutor que crea un thread
 * nuevo por cada tarea sin límite — peligroso bajo carga alta.
 *
 * Con esta configuración:
 * - corePoolSize=1: 1 thread siempre disponible para generación de reportes
 * - maxPoolSize=2: máximo 2 generaciones concurrentes (cada una puede consumir
 *   varios cientos de MB de heap durante el procesamiento del Excel)
 * - queueCapacity=20: hasta 20 requests en cola antes de rechazar
 *
 * @author Jaime Ferreira
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("insights-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
