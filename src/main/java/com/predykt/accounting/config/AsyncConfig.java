// ============================================
// AsyncConfig.java
// ============================================
package com.predykt.accounting.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration pour l'exécution asynchrone
 */
@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
public class AsyncConfig {
    
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        log.info("🔧 Configuration du ThreadPoolTaskExecutor pour tâches asynchrones");
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("predykt-async-");
        executor.initialize();
        
        return executor;
    }
}

