package com.predykt.accounting.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO pour la modification d'un acompte (Deposit).
 *
 * IMPORTANT: Seules certaines informations peuvent être modifiées après création:
 * - Date de réception (si pas encore imputé)
 * - Client (si pas encore imputé)
 * - Documentation (description, notes, référence commande)
 *
 * INTERDICTIONS:
 * - Les montants (amountHt, vatRate) NE PEUVENT PAS être modifiés (intégrité comptable)
 * - Si besoin de changer les montants: annuler l'acompte et en créer un nouveau
 *
 * @author PREDYKT System Optimizer
 * @since Phase 3 - Conformité OHADA Avancée
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositUpdateRequest {

    @NotNull(message = "La date de réception de l'acompte est obligatoire")
    private LocalDate depositDate;

    private Long customerId;  // Modification client autorisée si pas encore imputé

    @Size(max = 1000, message = "La description ne peut pas dépasser 1000 caractères")
    private String description;

    @Size(max = 100, message = "La référence de commande ne peut pas dépasser 100 caractères")
    private String customerOrderReference;

    @Size(max = 2000, message = "Les notes ne peuvent pas dépasser 2000 caractères")
    private String notes;
}
