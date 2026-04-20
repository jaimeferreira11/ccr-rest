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
 * - corePoolSize=2: 2 threads siempre disponibles para generación de reportes
 * - maxPoolSize=5: máximo 5 generaciones concurrentes (cada una puede usar ~200MB RAM)
 * - queueCapacity=10: hasta 10 requests en cola antes de rechazar
 *
 * @author Jaime Ferreira
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("insights-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
