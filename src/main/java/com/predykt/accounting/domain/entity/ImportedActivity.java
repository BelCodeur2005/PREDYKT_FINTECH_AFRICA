package com.predykt.accounting.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entité représentant une activité importée depuis un fichier CSV
 * Conserve les données brutes avant traitement et mapping aux comptes OHADA
 */
@Entity
@Table(name = "imported_activities", indexes = {
    @Index(name = "idx_imported_activities_company", columnList = "company_id"),
    @Index(name = "idx_imported_activities_date", columnList = "activity_date"),
    @Index(name = "idx_imported_activities_status", columnList = "import_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportedActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 50)
    private String category;

    // Données brutes du CSV
    @Column(name = "raw_account", length = 100)
    private String rawAccount;

    @Column(name = "raw_type", length = 50)
    private String rawType;

    // Résultat du mapping automatique
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mapped_account_id")
    private ChartOfAccounts mappedAccount;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    // Statut d'import
    @Column(name = "import_status", nullable = false, length = 20)
    @Builder.Default
    private String importStatus = "PENDING";

    @Column(name = "import_notes", columnDefinition = "TEXT")
    private String importNotes;

    // Référence à l'écriture comptable créée
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private GeneralLedger journalEntry;

    @Column(name = "imported_at", nullable = false)
    @Builder.Default
    private LocalDateTime importedAt = LocalDateTime.now();

    @Column(name = "imported_by", length = 100)
    private String importedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * Marque l'activité comme mappée avec succès
     */
    public void markAsMapped(ChartOfAccounts account, BigDecimal confidence) {
        this.mappedAccount = account;
        this.confidenceScore = confidence;
        this.importStatus = "MAPPED";
    }

    /**
     * Marque l'activité comme validée et liée à une écriture comptable
     */
    public void markAsProcessed(GeneralLedger entry) {
        this.journalEntry = entry;
        this.importStatus = "PROCESSED";
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Marque l'activité comme rejetée
     */
    public void markAsRejected(String reason) {
        this.importStatus = "REJECTED";
        this.importNotes = reason;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Vérifie si le mapping automatique est fiable (confidence >= 80%)
     */
    public boolean isHighConfidence() {
        return confidenceScore != null &&
               confidenceScore.compareTo(BigDecimal.valueOf(80)) >= 0;
    }

    /**
     * Vérifie si l'activité a été traitée
     */
    public boolean isProcessed() {
        return "PROCESSED".equals(importStatus);
    }
}
