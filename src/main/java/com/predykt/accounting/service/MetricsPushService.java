package com.predykt.accounting.service;

import com.predykt.accounting.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Service de push des métriques vers le serveur central
 * Permet au serveur central de monitorer tous les tenants
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsPushService {
    
    @Value("${TENANT_ID:}")
    private String tenantId;
    
    @Value("${CENTRAL_API_URL:http://central.predykt.com:8000}")
    private String centralApiUrl;
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;
    
    private final GeneralLedgerRepository glRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final CompanyRepository companyRepository;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Push des métriques quotidiennes (toutes les heures)
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void pushDailyMetrics() {
        if (!"prod-tenant".equals(activeProfile)) {
            log.debug("⚠️ Mode développement - Push métriques désactivé");
            return;
        }
        
        if (tenantId == null || tenantId.isEmpty()) {
            log.warn("⚠️ TENANT_ID non défini - Impossible de pusher les métriques");
            return;
        }
        
        try {
            log.info("📊 Collecte des métriques pour Tenant: {}", tenantId);
            
            TenantMetrics metrics = collectMetrics();
            
            log.info("📤 Push métriques vers serveur central...");
            
            String url = centralApiUrl + "/api/v1/metrics/push";
            
            restTemplate.postForEntity(url, metrics, Void.class);
            
            log.info("✅ Métriques pushées avec succès");
            
        } catch (Exception e) {
            log.error("❌ Erreur lors du push des métriques", e);
        }
    }
    
    /**
     * Collecte les métriques actuelles du tenant
     */
    private TenantMetrics collectMetrics() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        
        // Compter les écritures du mois
        long transactionsThisMonth = glRepository.findByCompanyAndEntryDateBetween(
            companyRepository.findAll().get(0), // Supposant 1 seule company par tenant
            startOfMonth,
            today
        ).size();
        
        // Compter les transactions bancaires du mois
        long bankTransactionsThisMonth = bankTransactionRepository.findByCompanyAndTransactionDateBetween(
            companyRepository.findAll().get(0),
            startOfMonth,
            today
        ).size();
        
        // Calculer l'espace disque utilisé (approximatif)
        long storageUsedMb = calculateStorageUsage();
        
        return TenantMetrics.builder()
            .tenantId(tenantId)
            .metricDate(today)
            .totalTransactions((int) transactionsThisMonth)
            .bankTransactions((int) bankTransactionsThisMonth)
            .storageUsedMb(storageUsedMb)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Calcule l'espace disque utilisé (approximation)
     */
    private long calculateStorageUsage() {
        // Estimation basée sur le nombre d'enregistrements
        long glCount = glRepository.count();
        long bankTxCount = bankTransactionRepository.count();
        long companyCount = companyRepository.count();
        
        // Estimation: 1KB par écriture GL, 500B par transaction bancaire
        long estimatedBytes = (glCount * 1024) + (bankTxCount * 512) + (companyCount * 2048);
        
        return estimatedBytes / (1024 * 1024); // Convertir en MB
    }
    
    /**
     * DTO des métriques tenant
     */
    @lombok.Data
    @lombok.Builder
    public static class TenantMetrics {
        private String tenantId;
        private LocalDate metricDate;
        private Integer totalTransactions;
        private Integer bankTransactions;
        private Long storageUsedMb;
        private LocalDateTime timestamp;
    }
}