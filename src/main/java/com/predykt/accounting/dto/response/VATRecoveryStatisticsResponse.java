package com.predykt.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Statistiques agrégées sur les calculs de TVA récupérable
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VATRecoveryStatisticsResponse {

    private Long companyId;

    private String companyName;

    private Integer fiscalYear;

    private Integer calculationCount;

    // Montants totaux
    private BigDecimal totalVatAmount;

    private BigDecimal totalRecoverableVat;

    private BigDecimal totalNonRecoverableVat;

    // Taux moyens
    private BigDecimal averageRecoveryRate;

    private BigDecimal averageProrataRate;

    // Impact du prorata
    private BigDecimal recoverableBeforeProrata;

    private BigDecimal prorataImpact;

    private BigDecimal prorataImpactPercentage;

    // Répartition par catégorie
    private Map<String, CategoryStatistics> byCategory;

    /**
     * Statistiques par catégorie de récupération
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStatistics {

        private String categoryName;

        private Integer count;

        private BigDecimal totalVat;

        private BigDecimal totalRecoverable;

        private BigDecimal averageRate;
    }
}