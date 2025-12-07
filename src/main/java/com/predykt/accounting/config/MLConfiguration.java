package com.predykt.accounting.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration Spring pour ML Matching System
 * Configure le cache, l'async, et le scheduling
 *
 * @author PREDYKT ML Team
 */
@Configuration
@EnableCaching
@EnableScheduling
@EnableAsync
@Slf4j
@ConditionalOnProperty(
    name = "predykt.ml.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class MLConfiguration {

    /**
     * ThreadPool pour entra\u00eenements ML asynchrones
     */
    @Bean(name = "mlTrainingExecutor")
    public Executor mlTrainingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ml-training-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        log.info("ML Training Executor initialis\u00e9: core=2, max=4");
        return executor;
    }

    /**
     * ThreadPool pour pr\u00e9dictions ML en batch
     */
    @Bean(name = "mlPredictionExecutor")
    public Executor mlPredictionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("ml-predict-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        log.info("ML Prediction Executor initialis\u00e9: core=4, max=8");
        return executor;
    }

    /**
     * Configuration du cache Redis pour mod\u00e8les ML
     */
    @Bean
    public org.springframework.cache.CacheManager cacheManager(
        org.springframework.data.redis.connection.RedisConnectionFactory connectionFactory
    ) {
        org.springframework.data.redis.cache.RedisCacheConfiguration config =
            org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(java.time.Duration.ofHours(24))  // Cache 24h
                .disableCachingNullValues()
                .serializeValuesWith(
                    org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer())
                );

        return org.springframework.data.redis.cache.RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }

    /**
     * Param\u00e8tres ML (injectables)
     */
    @Bean
    public MLProperties mlProperties() {
        return new MLProperties();
    }

    /**
     * Classe de propri\u00e9t\u00e9s ML
     */
    @org.springframework.boot.context.properties.ConfigurationProperties(prefix = "predykt.ml")
    @org.springframework.stereotype.Component
    public static class MLProperties {
        private boolean enabled = true;
        private boolean autoTrainingEnabled = true;
        private String modelsBaseDir = "./ml-models";
        private String trainingCron = "0 0 3 * * ?";  // 3h00 daily
        private String cleanupCron = "0 0 4 * * SUN";  // 4h00 Sunday
        private String monitoringCron = "0 0 9 * * MON";  // 9h00 Monday
        private int minTrainingData = 50;
        private double minAccuracy = 0.70;
        private int numTrees = 100;
        private int maxDepth = 20;

        // Getters & Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isAutoTrainingEnabled() {
            return autoTrainingEnabled;
        }

        public void setAutoTrainingEnabled(boolean autoTrainingEnabled) {
            this.autoTrainingEnabled = autoTrainingEnabled;
        }

        public String getModelsBaseDir() {
            return modelsBaseDir;
        }

        public void setModelsBaseDir(String modelsBaseDir) {
            this.modelsBaseDir = modelsBaseDir;
        }

        public String getTrainingCron() {
            return trainingCron;
        }

        public void setTrainingCron(String trainingCron) {
            this.trainingCron = trainingCron;
        }

        public String getCleanupCron() {
            return cleanupCron;
        }

        public void setCleanupCron(String cleanupCron) {
            this.cleanupCron = cleanupCron;
        }

        public String getMonitoringCron() {
            return monitoringCron;
        }

        public void setMonitoringCron(String monitoringCron) {
            this.monitoringCron = monitoringCron;
        }

        public int getMinTrainingData() {
            return minTrainingData;
        }

        public void setMinTrainingData(int minTrainingData) {
            this.minTrainingData = minTrainingData;
        }

        public double getMinAccuracy() {
            return minAccuracy;
        }

        public void setMinAccuracy(double minAccuracy) {
            this.minAccuracy = minAccuracy;
        }

        public int getNumTrees() {
            return numTrees;
        }

        public void setNumTrees(int numTrees) {
            this.numTrees = numTrees;
        }

        public int getMaxDepth() {
            return maxDepth;
        }

        public void setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
        }
    }
}