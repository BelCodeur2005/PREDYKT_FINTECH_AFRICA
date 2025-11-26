package com.predykt.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour les statistiques d'un cabinet
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CabinetStatsDTO {

    private Long cabinetId;

    private String cabinetName;

    // Statistiques entreprises
    private Long totalCompanies;

    private Long maxCompanies;

    private Boolean hasReachedCompanyLimit;

    // Statistiques utilisateurs
    private Long totalUsers;

    private Long maxUsers;

    private Boolean hasReachedUserLimit;

    // Statistiques financières
    private BigDecimal totalRevenue;

    private BigDecimal monthlyRevenue;

    private BigDecimal yearlyRevenue;

    private BigDecimal outstandingAmount;

    private BigDecimal overdueAmount;

    // Statistiques factures
    private Long totalInvoices;

    private Long pendingInvoices;

    private Long paidInvoices;

    private Long overdueInvoices;

    private Long cancelledInvoices;

    // Statistiques temps
    private Double totalHoursTracked;

    private Double monthlyHoursTracked;

    private BigDecimal totalBillableAmount;

    private BigDecimal monthlyBillableAmount;
}
