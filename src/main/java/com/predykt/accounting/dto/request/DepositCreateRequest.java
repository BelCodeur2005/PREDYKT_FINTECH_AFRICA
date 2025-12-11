package com.predykt.accounting.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO pour la création d'un acompte (Deposit).
 * Conforme OHADA - Compte 4191 "Clients - Avances et acomptes".
 *
 * Validation:
 * - Date de réception obligatoire
 * - Montant HT > 0
 * - Taux TVA par défaut: 19.25% (Cameroun)
 * - Customer ID optionnel (acompte peut être reçu avant identification client)
 *
 * @author PREDYKT System Optimizer
 * @since Phase 3 - Conformité OHADA Avancée
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositCreateRequest {

    @NotNull(message = "La date de réception de l'acompte est obligatoire")
    private LocalDate depositDate;

    @NotNull(message = "Le montant HT est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant HT doit être strictement positif")
    @Digits(integer = 13, fraction = 2, message = "Le montant HT doit avoir au maximum 13 chiffres avant la virgule et 2 après")
    private BigDecimal amountHt;

    @DecimalMin(value = "0.00", message = "Le taux de TVA ne peut pas être négatif")
    @DecimalMax(value = "100.00", message = "Le taux de TVA ne peut pas dépasser 100%")
    @Builder.Default
    private BigDecimal vatRate = new BigDecimal("19.25");  // Taux TVA Cameroun par défaut

    // Relations optionnelles

    private Long customerId;  // Optionnel: acompte peut être reçu avant identification client

    private Long paymentId;  // Optionnel: lien avec paiement existant

    // Documentation

    @Size(max = 1000, message = "La description ne peut pas dépasser 1000 caractères")
    private String description;

    @Size(max = 100, message = "La référence de commande ne peut pas dépasser 100 caractères")
    private String customerOrderReference;

    @Size(max = 2000, message = "Les notes ne peuvent pas dépasser 2000 caractères")
    private String notes;
}
