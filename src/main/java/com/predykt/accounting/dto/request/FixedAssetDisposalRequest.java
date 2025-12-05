package com.predykt.accounting.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Requête de cession/sortie d'une immobilisation
 * Conforme OHADA - Génère automatiquement l'écriture comptable de cession
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedAssetDisposalRequest {

    @NotNull(message = "La date de cession est obligatoire")
    @PastOrPresent(message = "La date de cession ne peut être dans le futur")
    private LocalDate disposalDate;

    @NotNull(message = "Le montant de cession est obligatoire")
    @DecimalMin(value = "0.00", message = "Le montant de cession ne peut être négatif")
    @Digits(integer = 18, fraction = 2, message = "Le montant ne peut avoir plus de 2 décimales")
    private BigDecimal disposalAmount; // Prix de vente ou valeur de mise au rebut

    @NotBlank(message = "Le motif de cession est obligatoire")
    @Size(max = 500, message = "Le motif ne peut dépasser 500 caractères")
    private String disposalReason; // Ex: Vente à tiers, Mise au rebut, Don, Destruction

    // === TYPE DE CESSION (pour écriture comptable) ===

    /**
     * Type de cession:
     * - SALE: Vente à un tiers (génère un compte 754 - Produits de cession)
     * - SCRAP: Mise au rebut (pas de produit de cession)
     * - DONATION: Don (régime fiscal spécial)
     * - DESTRUCTION: Destruction (sinistre, obsolescence)
     */
    @NotBlank(message = "Le type de cession est obligatoire")
    @Pattern(regexp = "^(SALE|SCRAP|DONATION|DESTRUCTION)$",
             message = "Type de cession invalide (SALE, SCRAP, DONATION, DESTRUCTION)")
    private String disposalType;

    // === INFORMATIONS ACHETEUR (si vente) ===

    @Size(max = 200, message = "Le nom de l'acheteur ne peut dépasser 200 caractères")
    private String buyerName; // Nom de l'acheteur (si vente)

    @Size(max = 50, message = "Le NIU de l'acheteur ne peut dépasser 50 caractères")
    private String buyerNiu; // NIU acheteur (important pour fiscalité camerounaise)

    @Size(max = 100, message = "Le numéro de facture ne peut dépasser 100 caractères")
    private String invoiceNumber; // Numéro de facture de vente

    // === COMPTABILISATION AUTOMATIQUE ===

    @AssertTrue(message = "Le montant de cession doit être supérieur à 0 pour une vente")
    public boolean isDisposalAmountValidForSale() {
        if (disposalType == null) {
            return true;
        }

        if ("SALE".equals(disposalType)) {
            return disposalAmount != null && disposalAmount.compareTo(BigDecimal.ZERO) > 0;
        }

        return true;
    }

    @AssertTrue(message = "Les informations acheteur sont obligatoires pour une vente")
    public boolean isBuyerInfoValidForSale() {
        if (disposalType == null) {
            return true;
        }

        if ("SALE".equals(disposalType)) {
            return buyerName != null && !buyerName.isBlank();
        }

        return true;
    }
}
