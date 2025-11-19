package com.predykt.accounting.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * Configuration de la base de données dédiée au Tenant (Mono-Tenant)
 * Chaque tenant a sa propre base de données isolée physiquement
 * 
 * Cette configuration est utilisée en mode "prod-tenant" où chaque instance
 * de l'application est dédiée à UN SEUL tenant.
 */
@Configuration
@Profile("prod-tenant")
@Slf4j
public class TenantDatabaseConfig {
    
    @Value("${TENANT_ID}")
    private String tenantId;
    
    @Value("${DB_HOST}")
    private String dbHost;
    
    @Value("${DB_PORT:5432}")
    private int dbPort;
    
    @Value("${DB_NAME}")
    private String dbName;
    
    @Value("${DB_USER}")
    private String dbUser;
    
    @Value("${DB_PASSWORD}")
    private String dbPassword;
    
    /**
     * Configuration du DataSource Hikari dédié au tenant
     * Optimisé pour les performances et la sécurité
     */
    @Bean
    public DataSource dataSource() {
        log.info("🔧 Initialisation DataSource pour Tenant: {}", tenantId);
        log.info("📍 Database: {}:{}/{}", dbHost, dbPort, dbName);
        
        HikariConfig config = new HikariConfig();
        
        // Configuration de connexion
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s", dbHost, dbPort, dbName));
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setDriverClassName("org.postgresql.Driver");
        
        // Pool de connexions (optimisé pour tenant dédié)
        config.setMaximumPoolSize(20);  // Max 20 connexions simultanées
        config.setMinimumIdle(5);       // Minimum 5 connexions maintenues
        config.setConnectionTimeout(30000);  // 30 secondes timeout
        config.setIdleTimeout(600000);       // 10 minutes idle
        config.setMaxLifetime(1800000);      // 30 minutes lifetime max
        
        // Propriétés de performance PostgreSQL
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        // Configuration de sécurité
        config.addDataSourceProperty("sslmode", "prefer");  // Préférer SSL si disponible
        config.addDataSourceProperty("ApplicationName", String.format("PREDYKT-Tenant-%s", tenantId));
        
        // Health checks
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(3000);
        
        // Nom du pool pour monitoring
        config.setPoolName(String.format("HikariPool-Tenant-%s", tenantId));
        
        // Métriques et monitoring
        config.setRegisterMbeans(true);
        
        // Leak detection (détection de fuites de connexions)
        config.setLeakDetectionThreshold(60000);  // 60 secondes
        
        HikariDataSource dataSource = new HikariDataSource(config);
        
        log.info("✅ DataSource initialisé avec succès pour Tenant: {}", tenantId);
        log.info("📊 Pool: {} connexions max, {} idle min", 
                 config.getMaximumPoolSize(), config.getMinimumIdle());
        
        return dataSource;
    }
}