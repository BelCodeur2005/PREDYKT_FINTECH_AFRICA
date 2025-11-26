package com.predykt.accounting.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;

/**
 * Configuration Multi-Tenant pour les 3 modes de déploiement
 * 
 * Configuration via ENV :
 * - PREDYKT_TENANT_MODE=SHARED|DEDICATED|CABINET
 * - PREDYKT_TENANT_ID=<tenant_id> (DEDICATED/CABINET uniquement)
 * - PREDYKT_TENANT_CABINET_ID=<cabinet_id> (CABINET uniquement)
 */
@Configuration
@Slf4j
public class MultiTenantConfiguration implements WebMvcConfigurer {
    
    @Value("${predykt.tenant.mode:SHARED}")
    private String tenantMode;
    
    @Value("${spring.datasource.url}")
    private String dbUrl;
    
    @Value("${spring.datasource.username}")
    private String dbUsername;
    
    @Value("${spring.datasource.password}")
    private String dbPassword;
    
    /**
     * Enregistre l'intercepteur tenant
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("🔧 Enregistrement du TenantInterceptor");
        registry.addInterceptor(tenantInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/health/**", "/actuator/**");
    }
    
    @Bean
    public TenantInterceptor tenantInterceptor() {
        return new TenantInterceptor(null); // JwtTokenProvider sera injecté
    }
    
    /**
     * DataSource par défaut (partagée)
     */
    @Bean(name = "defaultDataSource")
    public DataSource defaultDataSource() {
        log.info("🗄️ Configuration DataSource par défaut");
        log.info("📍 URL: {}", dbUrl);
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setDriverClassName("org.postgresql.Driver");
        
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        config.setPoolName("HikariPool-Default");
        
        return new HikariDataSource(config);
    }
    
    /**
     * DataSource Router Multi-Tenant
     * C'est le DataSource PRINCIPAL utilisé par JPA
     */
    @Bean
    @Primary
    @ConditionalOnProperty(
        name = "predykt.tenant.routing.enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public DataSource dataSource() {
        log.info("🌐 Configuration MultiTenantDataSourceRouter en mode: {}", tenantMode);
        
        MultiTenantDataSourceRouter router = new MultiTenantDataSourceRouter(
            tenantMode,
            defaultDataSource()
        );
        
        // En mode SHARED, on utilise uniquement la DataSource par défaut
        // En mode DEDICATED/CABINET, d'autres DataSource seront ajoutées dynamiquement
        
        return router;
    }
    
    /**
     * Bean pour accéder au router (pour ajout dynamique de DataSource)
     */
    @Bean
    public MultiTenantDataSourceRouter dataSourceRouter() {
        return (MultiTenantDataSourceRouter) dataSource();
    }
}

/**
 * Configuration spécifique MODE SHARED (PME mutualisée)
 */
@Configuration
@ConditionalOnProperty(name = "predykt.tenant.mode", havingValue = "SHARED", matchIfMissing = true)
@Slf4j
class SharedModeConfiguration {
    
    @Bean
    public String tenantModeInfo() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🟢 MODE ACTIVÉ : PME MUTUALISÉE (SHARED)");
        log.info("📦 Architecture : Multi-tenant SaaS");
        log.info("🗄️  Base de données : 1 BDD partagée");
        log.info("🔒 Isolation : Logique (company_id)");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        return "SHARED";
    }
}

/**
 * Configuration spécifique MODE DEDICATED (ETI isolée)
 */
@Configuration
@ConditionalOnProperty(name = "predykt.tenant.mode", havingValue = "DEDICATED")
@Slf4j
class DedicatedModeConfiguration {
    
    @Value("${predykt.tenant.id:}")
    private String tenantId;
    
    @Bean
    public String tenantModeInfo() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔵 MODE ACTIVÉ : ETI DÉDIÉE (DEDICATED)");
        log.info("📦 Architecture : Mono-tenant dédié");
        log.info("🆔 Tenant ID : {}", tenantId);
        log.info("🗄️  Base de données : 1 BDD dédiée à ce tenant");
        log.info("🔒 Isolation : Physique totale");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        if (tenantId == null || tenantId.isEmpty()) {
            log.error("❌ ERREUR : PREDYKT_TENANT_ID non configuré en mode DEDICATED !");
            throw new IllegalStateException("TENANT_ID obligatoire en mode DEDICATED");
        }
        
        return "DEDICATED";
    }
}

/**
 * Configuration spécifique MODE CABINET (hybride)
 */
@Configuration
@ConditionalOnProperty(name = "predykt.tenant.mode", havingValue = "CABINET")
@Slf4j
class CabinetModeConfiguration {
    
    @Value("${predykt.tenant.cabinet-id:}")
    private String cabinetId;
    
    @Bean
    public String tenantModeInfo() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🟡 MODE ACTIVÉ : CABINET COMPTABLE (CABINET)");
        log.info("📦 Architecture : Hybride");
        log.info("🏢 Cabinet ID : {}", cabinetId);
        log.info("🗄️  Base de données : 1 BDD dédiée à ce cabinet");
        log.info("🔒 Isolation : Physique entre cabinets + Logique interne");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        if (cabinetId == null || cabinetId.isEmpty()) {
            log.error("❌ ERREUR : PREDYKT_TENANT_CABINET_ID non configuré en mode CABINET !");
            throw new IllegalStateException("CABINET_ID obligatoire en mode CABINET");
        }
        
        return "CABINET";
    }
}