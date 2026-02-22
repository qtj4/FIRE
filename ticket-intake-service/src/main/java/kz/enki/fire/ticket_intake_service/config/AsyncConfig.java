package kz.enki.fire.ticket_intake_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Value("${app.async.ticket.core-pool-size:8}")
    private int corePoolSize;

    @Value("${app.async.ticket.max-pool-size:16}")
    private int maxPoolSize;

    @Value("${app.async.ticket.queue-capacity:500}")
    private int queueCapacity;

    @Bean(name = "ticketTaskExecutor")
    public Executor ticketTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("TicketAsync-");
        executor.initialize();
        return executor;
    }
}
